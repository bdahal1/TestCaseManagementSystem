package com.tcms.automation;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/automation/projects")
public class AutomationImportController {

    private final AutomationImportService importService;

    public AutomationImportController(AutomationImportService importService) {
        this.importService = importService;
    }

    @PostMapping(value = "/{projectId}/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@projectAccess.project(#p0)")
    public ResponseEntity<ImportPreviewResponse> previewImportFile(
            @PathVariable int projectId,
            @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }
        try {
            byte[] bytes = file.getBytes();
            ImportPreviewResponse response = importService.previewImport(projectId, file.getOriginalFilename(), bytes);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read uploaded file: " + e.getMessage());
        }
    }

    @PostMapping(value = "/{projectId}/import/preview-text", consumes = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("@projectAccess.project(#p0)")
    public ResponseEntity<ImportPreviewResponse> previewImportText(
            @PathVariable int projectId,
            @RequestParam(value = "fileName", defaultValue = "scenario.feature") String fileName,
            @RequestBody String content) {
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content is empty");
        }
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        ImportPreviewResponse response = importService.previewImport(projectId, fileName, bytes);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{projectId}/import/save")
    @PreAuthorize("@projectAccess.project(#p0)")
    public ResponseEntity<ImportSaveResponse> saveImport(
            @PathVariable int projectId,
            @RequestBody ImportSaveRequest request) {
        ImportSaveResponse response = importService.saveImport(projectId, request);
        return ResponseEntity.ok(response);
    }
}
