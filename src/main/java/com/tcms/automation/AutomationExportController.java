package com.tcms.automation;

import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/automation/projects")
public class AutomationExportController {
    private final AutomationProjectExportService exportService;

    public AutomationExportController(AutomationProjectExportService exportService) {
        this.exportService = exportService;
    }

    public record ExportRequest(List<Integer> caseIds) {}

    @PostMapping("/{projectId}/export")
    @PreAuthorize("@projectAccess.project(#p0)")
    public ResponseEntity<byte[]> exportProject(
            @PathVariable int projectId,
            @RequestBody ExportRequest request) {
        byte[] zipBytes = exportService.exportProjectZip(projectId, request.caseIds());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("automation-project-" + projectId + ".zip").build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(zipBytes);
    }
}
