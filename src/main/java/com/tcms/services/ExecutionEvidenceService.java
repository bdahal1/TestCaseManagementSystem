package com.tcms.services;

import com.tcms.dto.plans.PlanResponses.Evidence;
import com.tcms.models.*;
import com.tcms.repositories.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;

@Service
public class ExecutionEvidenceService {
    private static final int MAX_BYTES=5*1024*1024;
    private final ExecutionEvidenceRepository files;
    private final TestExecutionRepository plans;
    private final TestPlanService service;
    private final ExecutionPolicy policy;
    public ExecutionEvidenceService(ExecutionEvidenceRepository files,TestExecutionRepository plans,TestPlanService service,ExecutionPolicy policy) {
        this.files=files; this.plans=plans; this.service=service; this.policy=policy;
    }
    @Transactional
    public Evidence upload(int planId,int caseId,MultipartFile upload) {
        plans.findForUpdate(planId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Plan not found"));
        TestCaseExecutions link=service.requireCase(planId,caseId); policy.requireRecord(link);
        if (upload.isEmpty() || upload.getSize()>MAX_BYTES) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a file between 1 byte and 5 MB");
        if (files.countByCaseExecutionId(link.getId())>=10) throw new ResponseStatusException(HttpStatus.CONFLICT,"A case can have at most 10 evidence files");
        byte[] data;
        try { data=upload.getBytes(); }
        catch (IOException e) { throw new IllegalStateException("Unable to read evidence",e); }
        String requested=upload.getContentType();
        if ((requested==null || requested.equals("application/octet-stream")) && upload.getOriginalFilename()!=null
                && upload.getOriginalFilename().toLowerCase(java.util.Locale.ROOT).matches(".*\\.(txt|log)$")) requested="text/plain";
        String type=detect(data,requested);
        String original=upload.getOriginalFilename()==null ? "evidence" : upload.getOriginalFilename();
        String name=original.replaceAll("[^a-zA-Z0-9._ -]","_");
        if (name.length()>150) name=name.substring(name.length()-150);
        ExecutionEvidence file=new ExecutionEvidence(); file.setCaseExecution(link); file.setName(name); file.setContentType(type);
        file.setSize(data.length); file.setContent(data); file.setUploadedAt(Instant.now()); file.setUploadedBy(policy.currentName());
        files.save(file);
        link.getEvidence().add(file);
        return new Evidence(file.getId(),name,type,data.length,file.getUploadedAt(),file.getUploadedBy());
    }
    private String detect(byte[] data,String requested) {
        if (data.length>=8 && Arrays.equals(Arrays.copyOf(data,8),new byte[]{(byte)137,80,78,71,13,10,26,10})) return "image/png";
        if (data.length>=3 && data[0]==(byte)255 && data[1]==(byte)216 && data[2]==(byte)255) return "image/jpeg";
        if ("text/plain".equals(requested)) {
            String text=new String(data,StandardCharsets.UTF_8);
            if (text.indexOf('\0')<0 && Arrays.equals(text.getBytes(StandardCharsets.UTF_8),data)) return "text/plain";
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Upload a PNG, JPEG, or UTF-8 text log");
    }
    @Transactional(readOnly=true)
    public ExecutionEvidence download(int planId,int id) {
        ExecutionEvidence file=files.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Evidence not found"));
        if (!file.getCaseExecution().getTestExecutions().getId().equals(planId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Evidence not found");
        return file;
    }
    @Transactional
    public void delete(int planId,int id) {
        plans.findForUpdate(planId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Plan not found"));
        ExecutionEvidence file=download(planId,id); policy.requireRecord(file.getCaseExecution());
        files.delete(file);
    }
}
