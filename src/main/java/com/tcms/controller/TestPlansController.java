package com.tcms.controller;

import com.tcms.dto.plans.*;
import com.tcms.services.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/testPlans")
public class TestPlansController {
    private final TestPlanService plans;
    private final TestExecutionService executions;
    private final ExecutionEvidenceService evidence;
    public TestPlansController(TestPlanService plans,TestExecutionService executions,ExecutionEvidenceService evidence) {
        this.plans=plans; this.executions=executions; this.evidence=evidence;
    }
    @GetMapping("/project/{projectId}") @PreAuthorize("@projectAccess.project(#p0)")
    public Map<String,Object> list(@PathVariable int projectId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,
                                   @RequestParam(defaultValue="") String q,@RequestParam(defaultValue="all") String scope) {
        return plans.list(projectId,page,size,q,scope);
    }
    @GetMapping("/project/{projectId}/testers") @PreAuthorize("@projectAccess.project(#p0)")
    public java.util.List<PlanResponses.Person> testers(@PathVariable int projectId) { return plans.testers(projectId); }
    @PostMapping @PreAuthorize("@projectAccess.manager() and @projectAccess.project(#p0.projectId)")
    public ResponseEntity<Map<String,Integer>> create(@Valid @RequestBody PlanRequests.Create request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id",plans.create(request)));
    }
    @GetMapping("/{id}") @PreAuthorize("@projectAccess.execution(#p0)")
    public PlanResponses.Detail detail(@PathVariable int id) { return plans.detail(id); }
    @PutMapping("/{id}") @PreAuthorize("@projectAccess.manager() and @projectAccess.execution(#p0)")
    public ResponseEntity<Void> update(@PathVariable int id,@Valid @RequestBody PlanRequests.Metadata request) { plans.update(id,request); return ResponseEntity.noContent().build(); }
    @PostMapping("/{id}/cases") @PreAuthorize("@projectAccess.manager() and @projectAccess.execution(#p0)")
    public ResponseEntity<Void> add(@PathVariable int id,@Valid @RequestBody PlanRequests.Cases request) { executions.addCases(id,request.caseIds()); return ResponseEntity.noContent().build(); }
    @DeleteMapping("/{id}/cases/{caseId}") @PreAuthorize("@projectAccess.manager() and @projectAccess.execution(#p0)")
    public ResponseEntity<Void> remove(@PathVariable int id,@PathVariable int caseId) { executions.removeCases(id,java.util.List.of(caseId)); return ResponseEntity.noContent().build(); }
    @PutMapping("/{id}/assignments") @PreAuthorize("@projectAccess.manager() and @projectAccess.execution(#p0)")
    public ResponseEntity<Void> assign(@PathVariable int id,@Valid @RequestBody PlanRequests.Assignment request) { plans.assign(id,request); return ResponseEntity.noContent().build(); }
    @PostMapping("/{id}/actions/{action}") @PreAuthorize("@projectAccess.manager() and @projectAccess.execution(#p0)")
    public ResponseEntity<Void> transition(@PathVariable int id,@PathVariable String action) { plans.transition(id,action); return ResponseEntity.noContent().build(); }
    @PostMapping("/{id}/cases/{caseId}/results") @PreAuthorize("@projectAccess.execution(#p0)")
    public ResponseEntity<Void> record(@PathVariable int id,@PathVariable int caseId,@Valid @RequestBody PlanRequests.Result request) { plans.record(id,caseId,request); return ResponseEntity.noContent().build(); }
    @PostMapping("/{id}/cases/{caseId}/evidence") @PreAuthorize("@projectAccess.execution(#p0)")
    public ResponseEntity<PlanResponses.Evidence> upload(@PathVariable int id,@PathVariable int caseId,@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(evidence.upload(id,caseId,file));
    }
    @GetMapping("/{id}/evidence/{fileId}") @PreAuthorize("@projectAccess.execution(#p0)")
    public ResponseEntity<byte[]> download(@PathVariable int id,@PathVariable int fileId) {
        var file=evidence.download(id,fileId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(file.getName(),StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options","nosniff").cacheControl(CacheControl.noStore()).body(file.getContent());
    }
    @DeleteMapping("/{id}/evidence/{fileId}") @PreAuthorize("@projectAccess.execution(#p0)")
    public ResponseEntity<Void> removeEvidence(@PathVariable int id,@PathVariable int fileId) { evidence.delete(id,fileId); return ResponseEntity.noContent().build(); }
}
