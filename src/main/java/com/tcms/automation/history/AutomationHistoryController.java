package com.tcms.automation.history;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Read-only history endpoints.  All endpoints require the caller to be a member
 * of the given project (enforced inside AutomationHistoryService via ProjectAccess).
 *
 * No @PreAuthorize annotations are needed here because every method delegates to
 * the service, which always calls requireProjectAccess(projectId) first.
 */
@RestController
@RequestMapping("/automation/history")
public class AutomationHistoryController {

    private final AutomationHistoryService historyService;

    public AutomationHistoryController(AutomationHistoryService historyService) {
        this.historyService = historyService;
    }

    /**
     * GET /automation/history/project/{projectId}
     *
     * Query params (all optional):
     *   planId          – filter to a specific plan
     *   status          – filter to a specific JobStatus value
     *   caseNameContains – case-insensitive substring of the frozen case name
     *   from            – ISO-8601 instant lower bound on createdAt (inclusive)
     *   to              – ISO-8601 instant upper bound on createdAt (inclusive)
     *   page            – 0-based page index (default 0)
     *   size            – page size 1–100 (default 20)
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<HistoryPageResponse> listHistory(
            @PathVariable int projectId,
            @RequestParam(required = false) Integer planId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String caseNameContains,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        HistoryPageResponse response = historyService.getHistory(
                projectId, planId, status, caseNameContains, from, to, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /automation/history/project/{projectId}/job/{jobId}
     *
     * Returns the full job detail for the given job, confirmed to belong to this project.
     * Diagnostic logs and error messages are already masked at ingestion; this endpoint
     * simply forwards the stored (already-redacted) values.
     */
    @GetMapping("/project/{projectId}/job/{jobId}")
    public ResponseEntity<Map<String, Object>> jobDetail(
            @PathVariable int projectId,
            @PathVariable Long jobId) {

        Map<String, Object> detail = historyService.getJobDetail(projectId, jobId);
        return ResponseEntity.ok(detail);
    }

    /**
     * GET /automation/history/project/{projectId}/case/{caseId}/plan/{planId}/attempts
     *
     * Returns all attempt summaries for a case in a plan, ordered oldest-first (attempt 1, 2, 3, …).
     * Used by the UI to build a "Compare with attempt N" picker.
     */
    @GetMapping("/project/{projectId}/case/{caseId}/plan/{planId}/attempts")
    public ResponseEntity<List<AttemptSummary>> listAttempts(
            @PathVariable int projectId,
            @PathVariable int planId,
            @PathVariable int caseId) {

        List<AttemptSummary> attempts = historyService.getAttempts(projectId, planId, caseId);
        return ResponseEntity.ok(attempts);
    }

    /**
     * GET /automation/history/project/{projectId}/compare?jobIdA=&jobIdB=
     *
     * Compares two job attempts step-by-step.  Both jobs must belong to this project.
     * Steps are matched by their (order, description) key from the frozen testDefinition,
     * so changed step definitions are flagged explicitly rather than causing index mismatches.
     */
    @GetMapping("/project/{projectId}/compare")
    public ResponseEntity<CompareAttemptResponse> compare(
            @PathVariable int projectId,
            @RequestParam Long jobIdA,
            @RequestParam Long jobIdB) {

        CompareAttemptResponse response = historyService.compare(projectId, jobIdA, jobIdB);
        return ResponseEntity.ok(response);
    }
}
