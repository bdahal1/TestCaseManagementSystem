package com.tcms.automation;

import com.tcms.automation.batch.BatchCreateRequest;
import com.tcms.automation.batch.BatchDetailResponse;
import com.tcms.automation.batch.BatchPreviewRequest;
import com.tcms.automation.batch.BatchPreviewResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/automation/batches")
public class AutomationBatchController {
    private final AutomationBatchService batchService;

    public AutomationBatchController(AutomationBatchService batchService) {
        this.batchService = batchService;
    }

    @PostMapping("/plan/{planId}/preview")
    @PreAuthorize("@projectAccess.execution(#p0)")
    public ResponseEntity<BatchPreviewResponse> previewBatch(
            @PathVariable int planId,
            @RequestBody(required = false) BatchPreviewRequest request) {
        String scope = request != null ? request.getScope() : "ALL_ELIGIBLE";
        var caseIds = request != null ? request.getCaseIds() : null;
        BatchPreviewResponse response = batchService.previewBatch(planId, caseIds, scope);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/plan/{planId}/create")
    @PreAuthorize("@projectAccess.execution(#p0)")
    public ResponseEntity<BatchDetailResponse> createBatch(
            @PathVariable int planId,
            @RequestBody BatchCreateRequest request) {
        if (request == null) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Batch request is required");
        AutomationBatch batch = batchService.createBatch(
                planId,
                request.getCaseIds(),
                request.getBatchType() != null ? request.getBatchType() : request.getScope()
        );
        BatchDetailResponse detail = batchService.getBatch(batch.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(detail);
    }

    @GetMapping("/{batchId}")
    public ResponseEntity<BatchDetailResponse> getBatchStatus(@PathVariable Long batchId) {
        BatchDetailResponse detail = batchService.getBatch(batchId);
        return ResponseEntity.ok(detail);
    }

    @GetMapping("/plan/{planId}/latest")
    @PreAuthorize("@projectAccess.execution(#p0)")
    public ResponseEntity<BatchDetailResponse> getLatestBatchForPlan(@PathVariable int planId) {
        var latestOpt = batchService.getLatestBatchForPlan(planId);
        return latestOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/{batchId}/cancel")
    public ResponseEntity<BatchDetailResponse> cancelBatch(@PathVariable Long batchId) {
        BatchDetailResponse detail = batchService.cancelBatch(batchId);
        return ResponseEntity.ok(detail);
    }

    @PostMapping("/{batchId}/retry-failed")
    public ResponseEntity<BatchDetailResponse> retryFailedCases(@PathVariable Long batchId) {
        AutomationBatch newBatch = batchService.retryFailed(batchId);
        BatchDetailResponse detail = batchService.getBatch(newBatch.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(detail);
    }
}
