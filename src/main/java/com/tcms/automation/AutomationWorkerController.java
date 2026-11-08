package com.tcms.automation;

import tools.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/automation/worker")
public class AutomationWorkerController {
    private final AutomationJobService jobService;
    private final ObjectMapper mapper;

    private final AutomationWorkerAccess workerAccess;

    public AutomationWorkerController(AutomationJobService jobService, ObjectMapper mapper, AutomationWorkerAccess workerAccess) {
        this.jobService = jobService;
        this.mapper = mapper;
        this.workerAccess = workerAccess;
    }

    @PostMapping("/jobs/poll")
    public ResponseEntity<Map<String, Object>> pollJob(
            @RequestHeader(value = "X-Worker-Token", required = false) String token,
            @RequestParam(defaultValue = "default-worker") String workerId) {
        int projectId = workerAccess.authorize(token);
        var jobOpt = jobService.pollNextJob(projectId, workerId);
        if (jobOpt.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        var claim = jobOpt.get();
        var job = claim.job();
        try {
            var definition = mapper.readValue(job.getTestDefinition(), AutomationTestDefinition.class);
            return ResponseEntity.ok(Map.of(
                    "jobId", job.getId(),
                    "leaseToken", claim.leaseToken(),
                    "planId", job.getPlanId(),
                    "caseId", job.getCaseId(),
                    "testType", job.getTestType(),
                    "definition", definition
            ));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to deserialize job definition", e);
        }
    }

    @PostMapping("/jobs/{jobId}/heartbeat")
    public ResponseEntity<Void> heartbeat(
            @RequestHeader(value = "X-Worker-Token", required = false) String token,
            @RequestHeader(value = "X-Job-Lease", required = false) String leaseToken,
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "default-worker") String workerId) {
        int projectId = workerAccess.authorize(token);
        jobService.heartbeat(jobId, projectId, workerId, leaseToken);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/jobs/{jobId}/evidence")
    public ResponseEntity<Void> uploadEvidence(
            @RequestHeader(value = "X-Worker-Token", required = false) String token,
            @RequestHeader(value = "X-Job-Lease", required = false) String leaseToken,
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "default-worker") String workerId,
            @RequestParam("file") MultipartFile file) {
        int projectId = workerAccess.authorize(token);
        jobService.uploadEvidence(jobId, projectId, workerId, leaseToken, file);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/jobs/{jobId}/progress")
    public ResponseEntity<Void> progress(
            @RequestHeader(value = "X-Worker-Token", required = false) String token,
            @RequestHeader(value = "X-Job-Lease", required = false) String leaseToken,
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "default-worker") String workerId,
            @RequestBody AutomationProgress progress) {
        jobService.updateProgress(jobId, workerAccess.authorize(token), workerId, leaseToken, progress);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/jobs/{jobId}/complete")
    public ResponseEntity<Void> completeJob(
            @RequestHeader(value = "X-Worker-Token", required = false) String token,
            @RequestHeader(value = "X-Job-Lease", required = false) String leaseToken,
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "default-worker") String workerId,
            @RequestBody AutomationExecutionResult result) {
        int projectId = workerAccess.authorize(token);
        jobService.completeJob(jobId, projectId, workerId, leaseToken, result);
        return ResponseEntity.ok().build();
    }
}
