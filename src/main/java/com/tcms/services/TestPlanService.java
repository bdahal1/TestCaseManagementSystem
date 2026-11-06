package com.tcms.services;

import com.tcms.dto.plans.PlanRequests;
import com.tcms.dto.plans.PlanResponses;
import com.tcms.dto.plans.PlanResponses.*;
import com.tcms.enums.*;
import com.tcms.models.*;
import com.tcms.repositories.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.time.Instant;
import java.util.*;

@Service
@Transactional(readOnly=true)
public class TestPlanService {
    private final TestExecutionRepository plans;
    private final ProjectRepository projects;
    private final UserRepository users;
    private final TestCaseExecutionRepository links;
    private final ExecutionEvidenceRepository evidence;
    private final TestExecutionService executions;
    private final ExecutionPolicy policy;
    private final TestCaseSnapshots snapshots;

    public TestPlanService(TestExecutionRepository plans, ProjectRepository projects, UserRepository users,
                           TestCaseExecutionRepository links, ExecutionEvidenceRepository evidence,
                           TestExecutionService executions, ExecutionPolicy policy, TestCaseSnapshots snapshots) {
        this.plans=plans; this.projects=projects; this.users=users; this.links=links; this.evidence=evidence;
        this.executions=executions; this.policy=policy; this.snapshots=snapshots;
    }
    public TestExecutions requirePlan(int id) {
        TestExecutions plan=plans.findById(id);
        if (plan==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Test plan not found");
        return plan;
    }
    private TestExecutions lock(int id) {
        return plans.findForUpdate(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Test plan not found"));
    }
    public TestCaseExecutions requireCase(int planId,int caseId) {
        return links.findByTestExecutionsIdAndTestCaseId(planId,caseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Case is not part of this plan"));
    }
    private static TestResult result(TestCaseExecutions link) {
        return link.getResultStatus()==null ? TestResult.NOT_RUN : link.getResultStatus();
    }
    private static boolean completed(TestCaseExecutions link) {
        return EnumSet.of(TestResult.PASS,TestResult.FAIL,TestResult.SKIPPED).contains(result(link));
    }
    private Person person(Users user) { return user==null ? null : new Person(user.getId(),user.getFirstName()+" "+user.getLastName()); }
    private Summary summary(TestExecutions plan) {
        Users me=policy.currentUser();
        Map<TestResult,Long> counts=new EnumMap<>(TestResult.class);
        for (TestResult status:TestResult.values()) counts.put(status,0L);
        for (TestCaseExecutions link:plan.getTestCaseExecutions()) counts.merge(result(link),1L,Long::sum);
        return new Summary(plan.getId(),plan.getProjects().getId(),plan.getExecutionName(),plan.getReleaseName(),plan.getBuildName(),plan.getEnvironment(),
                plan.getDueDate(),plan.getExecutionStatus(),plan.getDescription(),plan.getTestCaseExecutions().size(),
                (int)plan.getTestCaseExecutions().stream().filter(TestPlanService::completed).count(),
                (int)plan.getTestCaseExecutions().stream().filter(l -> l.getAssignee()!=null && l.getAssignee().getId().equals(me.getId())).count(),
                (int)plan.getTestCaseExecutions().stream().filter(l -> l.getAssignee()==null).count(),counts);
    }
    public Map<String,Object> list(int projectId,int page,int size,String query,String scope) {
        if (page<0 || size<1 || size>100) throw new IllegalArgumentException("Invalid page size");
        if (!Set.of("all","mine").contains(scope) || query.length()>200) throw new IllegalArgumentException("Invalid plan filter");
        Page<TestExecutions> found=plans.searchPlans(projectId,query.trim(),scope.equals("mine") ? policy.currentUser().getId() : null,
                PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"id")));
        return Map.of("plans",found.getContent().stream().map(this::summary).toList(),"totalPages",found.getTotalPages(),"totalItems",found.getTotalElements());
    }
    public List<Person> testers(int projectId) {
        return users.findDistinctByProjectsSet_IdAndIsActiveTrueOrderByFirstNameAsc(projectId).stream().map(this::person).toList();
    }
    public Detail detail(int id) {
        TestExecutions plan=requirePlan(id);
        List<PlanResponses.Case> cases=plan.getTestCaseExecutions().stream().sorted(Comparator.comparing(l -> l.getTestCase().getId()))
                .map(link -> {
                    Snapshot saved=snapshots.read(link);
                    List<Evidence> files=evidence.findByCaseExecutionIdOrderByUploadedAtAsc(link.getId()).stream()
                            .map(e -> new Evidence(e.getId(),e.getName(),e.getContentType(),e.getSize(),e.getUploadedAt(),e.getUploadedBy())).toList();
                    List<Attempt> history=link.getHistory().stream().sorted(Comparator.comparing(ExecutionResultHistory::getRecordedAt).reversed())
                            .map(h -> new Attempt(h.getId(),h.getStatus(),h.getComment(),h.getActualResult(),h.getDefectUrl(),h.getRecordedAt(),h.getRecordedBy())).toList();
                    return new PlanResponses.Case(link.getTestCase().getId(),saved.name(),saved.type(),saved.steps(),result(link),link.getResultComment(),
                            link.getActualResult(),link.getDefectUrl(),revision(link),person(link.getAssignee()),link.getRecordedAt(),link.getRecordedBy(),
                            policy.canRecord(link),files,history,saved.details());
                }).toList();
        return new Detail(summary(plan),cases);
    }
    private void metadata(TestExecutions plan,PlanRequests.Metadata request) {
        plan.setExecutionName(request.name().trim()); plan.setReleaseName(request.release()); plan.setBuildName(request.build());
        plan.setEnvironment(request.environment()); plan.setDueDate(request.dueDate()); plan.setDescription(request.description());
    }
    @Transactional
    public int create(PlanRequests.Create request) {
        policy.requireManager();
        Projects project=projects.findById(request.projectId());
        if (project==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Project not found");
        TestExecutions plan=new TestExecutions(); metadata(plan,request.metadata()); plan.setProjects(project);
        plan.setReleasePlan(true); plan.setExecutionStatus(ExecutionStatus.DRAFT); plans.save(plan);
        executions.addCases(plan.getId(),request.caseIds());
        assign(plan.getId(),new PlanRequests.Assignment(request.caseIds(),request.assigneeId()));
        return plan.getId();
    }
    @Transactional
    public void update(int id,PlanRequests.Metadata request) {
        policy.requireManager(); TestExecutions plan=lock(id); policy.requireOpen(plan);
        metadata(plan,request);
    }
    private Users assignee(Integer id,TestExecutions plan) {
        if (id==null) return null;
        Users user=users.findById(id.intValue());
        if (user==null || !Boolean.TRUE.equals(user.getIsActive()) || user.getProjectsSet()==null
                || user.getProjectsSet().stream().noneMatch(p -> p.getId().equals(plan.getProjects().getId())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose an active tester assigned to this project");
        return user;
    }
    @Transactional
    public void assign(int id,PlanRequests.Assignment request) {
        policy.requireManager(); TestExecutions plan=lock(id); policy.requireOpen(plan);
        Users user=assignee(request.assigneeId(),plan);
        List<TestCaseExecutions> selected=request.caseIds().stream().map(caseId -> requireCase(id,caseId)).toList();
        selected.forEach(link -> link.setAssignee(user));
    }
    @Transactional
    public void transition(int id,String action) {
        policy.requireManager(); TestExecutions plan=lock(id);
        switch (action) {
            case "start" -> {
                if (plan.getExecutionStatus()!=ExecutionStatus.DRAFT) throw conflict("Only draft plans can be started");
                if (plan.getTestCaseExecutions().isEmpty()) throw conflict("Add cases before starting");
                if (plan.getTestCaseExecutions().stream().anyMatch(l -> l.getAssignee()==null)) throw conflict("Assign every case before starting");
                plan.getTestCaseExecutions().forEach(link -> assignee(link.getAssignee().getId(),plan));
                plan.getTestCaseExecutions().forEach(snapshots::capture);
                plan.setExecutionStatus(ExecutionStatus.IN_PROGRESS);
            }
            case "close" -> {
                if (plan.getExecutionStatus()!=ExecutionStatus.IN_PROGRESS) throw conflict("Only active plans can be closed");
                if (plan.getTestCaseExecutions().isEmpty() || plan.getTestCaseExecutions().stream().anyMatch(l -> !completed(l))) throw conflict("Resolve blocked and unrun cases before closing");
                plan.setExecutionStatus(ExecutionStatus.CLOSED);
            }
            case "reopen" -> {
                if (plan.getExecutionStatus()!=ExecutionStatus.CLOSED) throw conflict("Only closed plans can be reopened");
                plan.setExecutionStatus(ExecutionStatus.IN_PROGRESS);
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown plan action");
        }
    }
    private ResponseStatusException conflict(String reason) { return new ResponseStatusException(HttpStatus.CONFLICT,reason); }
    private static long revision(TestCaseExecutions link) { return link.getResultRevision()==null ? 0L : link.getResultRevision(); }
    @Transactional
    public void record(int id,int caseId,PlanRequests.Result request) {
        lock(id); TestCaseExecutions link=requireCase(id,caseId); policy.requireRecord(link);
        if (request.expectedRevision()!=revision(link)) throw conflict("This result changed. Reload before saving your result.");
        if (request.defectUrl()!=null && !request.defectUrl().isBlank()) {
            try {
                URI uri=URI.create(request.defectUrl());
                if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) || uri.getHost()==null)
                    throw new IllegalArgumentException();
            } catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter a valid HTTP or HTTPS defect URL"); }
        }
        if (Set.of(TestResult.FAIL,TestResult.BLOCKED).contains(request.status())
                && (request.actualResult()==null || request.actualResult().isBlank())
                && (request.comment()==null || request.comment().isBlank()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Add an actual result or notes to explain the failure or blocker");
        snapshots.capture(link);
        link.setResultStatus(request.status()); link.setResultComment(request.comment()); link.setActualResult(request.actualResult());
        link.setDefectUrl(request.defectUrl()); link.setResultRevision(revision(link)+1); link.setRecordedAt(Instant.now()); link.setRecordedBy(policy.currentName());
        ExecutionResultHistory attempt=new ExecutionResultHistory(); attempt.setCaseExecution(link); attempt.setStatus(request.status());
        attempt.setComment(request.comment()); attempt.setActualResult(request.actualResult()); attempt.setDefectUrl(request.defectUrl());
        attempt.setRecordedAt(link.getRecordedAt()); attempt.setRecordedBy(link.getRecordedBy()); link.getHistory().add(attempt);
    }
}
