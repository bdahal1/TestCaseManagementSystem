package com.tcms.automation;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/automation/jobs")
public class AutomationJobController {
    private final AutomationJobService jobService;
    private final ObjectMapper mapper;

    public AutomationJobController(AutomationJobService jobService, ObjectMapper mapper) {
        this.jobService = jobService;
        this.mapper = mapper;
    }

    @PostMapping("/plan/{planId}/case/{caseId}/run")
    @PreAuthorize("@projectAccess.execution(#p0)")
    public ResponseEntity<Map<String, Object>> runAutomationCase(
            @PathVariable int planId,
            @PathVariable int caseId) {
        AutomationJob job = jobService.createRunJob(planId, caseId);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "jobId", job.getId(),
                "status", job.getJobStatus(),
                "planId", job.getPlanId(),
                "caseId", job.getCaseId(),
                "createdAt", job.getCreatedAt()
        ));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<Map<String, Object>> getJobStatus(@PathVariable Long jobId) {
        AutomationJob job = jobService.getJob(jobId);
        List<AutomationExecutionResult.StepResult> stepResults = Collections.emptyList();
        if (job.getStepResultsJson() != null && !job.getStepResultsJson().isBlank()) {
            try {
                stepResults = mapper.readValue(job.getStepResultsJson(), new TypeReference<List<AutomationExecutionResult.StepResult>>() {});
            } catch (Exception ignored) {}
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("jobId", job.getId());
        resp.put("planId", job.getPlanId());
        resp.put("caseId", job.getCaseId());
        resp.put("status", job.getJobStatus());
        resp.put("testType", job.getTestType());
        resp.put("createdAt", job.getCreatedAt());
        resp.put("startedAt", job.getStartedAt());
        resp.put("finishedAt", job.getFinishedAt());
        resp.put("errorMessage", job.getErrorMessage());
        resp.put("stepResults", stepResults);
        resp.put("diagnosticLogs", job.getDiagnosticLogs());
        resp.put("failureEvidenceId", job.getFailureEvidenceId());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/plan/{planId}/case/{caseId}/latest")
    @PreAuthorize("@projectAccess.execution(#p0)")
    public ResponseEntity<Map<String, Object>> getLatestJob(
            @PathVariable int planId,
            @PathVariable int caseId) {
        var jobOpt = jobService.getLatestJobForCase(planId, caseId);
        if (jobOpt.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return getJobStatus(jobOpt.get().getId());
    }

    @GetMapping("/{jobId}/preview")
    public ResponseEntity<Map<String, Object>> getPreview(@PathVariable Long jobId,
                                                         @RequestParam(defaultValue = "0") long afterSequence) {
        var preview = jobService.getPreview(jobId, afterSequence);
        return ResponseEntity.status(preview == null ? HttpStatus.NO_CONTENT : HttpStatus.OK)
                .header("Cache-Control", "no-store").body(preview);
    }

    @PostMapping("/{jobId}/cancel")
    public ResponseEntity<Void> cancelJob(@PathVariable Long jobId) {
        jobService.cancelJob(jobId);
        return ResponseEntity.ok().build();
    }
}
