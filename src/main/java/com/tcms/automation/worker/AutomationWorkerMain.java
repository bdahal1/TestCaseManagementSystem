package com.tcms.automation.worker;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.tcms.automation.*;
import com.tcms.enums.TestTypes;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AutomationWorkerMain {
    private final String tmsUrl;
    private final String workerToken;
    private final String workerId;
    private final boolean oneShot;
    private final int pollIntervalMs;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final AutomationCucumberRunner cucumberRunner;
    private final AutomationKeywordRunner keywordRunner;

    public AutomationWorkerMain(String tmsUrl, String workerToken, String workerId, boolean oneShot, int pollIntervalMs) {
        this.tmsUrl = tmsUrl.endsWith("/") ? tmsUrl.substring(0, tmsUrl.length() - 1) : tmsUrl;
        if (workerToken == null || workerToken.isBlank() || workerToken.length() < 32)
            throw new IllegalArgumentException("Set AUTOMATION_WORKER_TOKEN to a credential of at least 32 characters configured on the server");
        if (!workerId.matches("[A-Za-z0-9._-]{1,120}")) throw new IllegalArgumentException("Invalid worker ID");
        this.workerToken = workerToken;
        this.workerId = workerId;
        this.oneShot = oneShot;
        this.pollIntervalMs = pollIntervalMs;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.mapper = new ObjectMapper();

        AutomationStepSerializer serializer = new AutomationStepSerializer(mapper);
        this.cucumberRunner = new AutomationCucumberRunner(serializer);
        this.keywordRunner = new AutomationKeywordRunner(serializer);
    }

    public static void main(String[] args) {
        var localSettings = java.nio.file.Path.of(".env");
        String tmsUrl = setting("TMS_URL", System.getenv(), localSettings, "http://localhost:8080/dhtcms/api/v1");
        String token = setting("AUTOMATION_WORKER_TOKEN", System.getenv(), localSettings, null);
        String workerId = "worker-" + UUID.randomUUID().toString().substring(0, 8);
        boolean oneShot = false;
        int pollInterval = 2000;

        for (int i = 0; i < args.length; i++) {
            if ("--tms-url".equals(args[i]) && i + 1 < args.length) tmsUrl = args[++i];
            else if ("--token".equals(args[i]) && i + 1 < args.length) token = args[++i];
            else if ("--worker-id".equals(args[i]) && i + 1 < args.length) workerId = args[++i];
            else if ("--one-shot".equals(args[i]) || "--run-once".equals(args[i])) oneShot = true;
            else if ("--poll-interval-ms".equals(args[i]) && i + 1 < args.length) pollInterval = Integer.parseInt(args[++i]);
        }

        System.out.println("=== TCMS Automation Worker Started ===");
        System.out.println("Worker ID: " + workerId);
        System.out.println("TMS URL: " + tmsUrl);
        System.out.println("Mode: " + (oneShot ? "One-shot" : "Continuous polling (" + pollInterval + "ms)"));

        AutomationWorkerMain worker = new AutomationWorkerMain(tmsUrl, token, workerId, oneShot, pollInterval);
        worker.run();
    }

    /** Match the backend's optional local properties file without evaluating shell commands. */
    static String setting(String name, Map<String, String> environment, java.nio.file.Path file, String fallback) {
        if (environment.containsKey(name)) return environment.get(name);
        if (!java.nio.file.Files.exists(file)) return fallback;
        var properties = new java.util.Properties();
        try (var reader = java.nio.file.Files.newBufferedReader(file)) {
            properties.load(reader);
            return properties.getProperty(name, fallback);
        } catch (java.io.IOException | IllegalArgumentException error) {
            throw new IllegalStateException("Cannot read local worker settings from .env");
        }
    }

    public void run() {
        boolean running = true;
        while (running) {
            try {
                boolean hadJob = pollAndExecute();
                if (oneShot && !hadJob) {
                    System.out.println("One-shot mode: no pending jobs found. Worker exiting.");
                    break;
                }
                if (!hadJob) {
                    Thread.sleep(pollIntervalMs);
                }
            } catch (InterruptedException e) {
                System.out.println("Worker interrupted, shutting down.");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Worker polling error: " + e.getMessage());
                try {
                    Thread.sleep(pollIntervalMs);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    public boolean pollAndExecute() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tmsUrl + "/automation/worker/jobs/poll?workerId=" + workerId))
                    .header("X-Worker-Token", workerToken)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 204) {
                return false;
            }
            if (response.statusCode() != 200) {
                System.err.println("Polling failed with HTTP " + response.statusCode() + ": " + response.body());
                return false;
            }

            Map<String, Object> jobData = mapper.readValue(response.body(), new TypeReference<>() {});
            Long jobId = Long.valueOf(jobData.get("jobId").toString());
            String leaseToken = (String) jobData.get("leaseToken");
            java.util.concurrent.atomic.AtomicBoolean leaseLost = new java.util.concurrent.atomic.AtomicBoolean();
            String testTypeStr = (String) jobData.get("testType");
            TestTypes testType = TestTypes.valueOf(testTypeStr);
            AutomationTestDefinition definition = mapper.convertValue(jobData.get("definition"), AutomationTestDefinition.class);

            System.out.println("--> Claimed Job #" + jobId + " [" + testType + "] '" + definition.getCaseName() + "'");

            // Start heartbeat scheduler
            ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
            heartbeatScheduler.scheduleAtFixedRate(() -> {
                if (!sendHeartbeat(jobId, leaseToken)) leaseLost.set(true);
            }, 15, 15, TimeUnit.SECONDS);

            AutomationExecutionResult result;
            var sequence = new java.util.concurrent.atomic.AtomicLong();
            AutomationProgressListener progress = (index, status, results, driver) -> {
                if (leaseLost.get()) return;
                byte[] frame = "RUNNING".equals(status) ? null : driver.takePreviewScreenshot();
                sendProgress(jobId, leaseToken, new AutomationProgress(sequence.incrementAndGet(), index, status, results, frame), leaseLost);
            };
            try {
                if (testType == TestTypes.CUCUMBER_AUTOMATION) {
                    result = cucumberRunner.execute(definition, leaseLost::get, progress);
                } else {
                    result = keywordRunner.execute(definition, leaseLost::get, progress);
                }
            } finally {
                // Let an in-flight heartbeat finish; interrupting it would falsely mark this completed run as lost.
                heartbeatScheduler.shutdown();
            }

            System.out.println("<-- Job #" + jobId + " Finished with status: " + result.getStatus() + " in " + result.getDurationMs() + "ms");
            if (!leaseLost.get()) submitResult(jobId, leaseToken, result);
            else System.err.println("Job lease lost; this attempt was discarded.");
            return true;

        } catch (Exception e) {
            System.err.println("Error processing job: " + e.getMessage());
            return false;
        }
    }

    private boolean sendHeartbeat(Long jobId, String leaseToken) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(tmsUrl + "/automation/worker/jobs/" + jobId + "/heartbeat?workerId=" + workerId))
                    .header("X-Job-Lease", leaseToken)
                    .header("X-Worker-Token", workerToken)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(5))
                    .build();
            return httpClient.send(req, HttpResponse.BodyHandlers.discarding()).statusCode() == 200;
        } catch (Exception ignored) { return false; }
    }

    private void sendProgress(Long jobId, String leaseToken, AutomationProgress progress,
                              java.util.concurrent.atomic.AtomicBoolean leaseLost) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tmsUrl + "/automation/worker/jobs/" + jobId + "/progress?workerId=" + workerId))
                    .header("X-Job-Lease", leaseToken).header("X-Worker-Token", workerToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(progress)))
                    .timeout(Duration.ofSeconds(2)).build();
            int status = httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (status == 403 || status == 409) leaseLost.set(true);
        } catch (Exception ignored) {
            // Preview is best effort. Heartbeats and final result submission remain authoritative.
        }
    }

    private void submitResult(Long jobId, String leaseToken, AutomationExecutionResult result) {
        try {
            String json = mapper.writeValueAsString(result);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(tmsUrl + "/automation/worker/jobs/" + jobId + "/complete?workerId=" + workerId))
                    .header("X-Job-Lease", leaseToken)
                    .header("X-Worker-Token", workerToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(30))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                System.err.println("Warning: Result submission returned HTTP " + resp.statusCode() + ": " + resp.body());
            } else {
                System.out.println("Result for Job #" + jobId + " successfully submitted to TMS.");
            }
        } catch (Exception e) {
            System.err.println("Failed to submit result for Job #" + jobId + ": " + e.getMessage());
        }
    }
}
