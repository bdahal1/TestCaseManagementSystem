package com.tcms.services;

import com.tcms.dto.reports.ReportResponse;
import com.tcms.dto.reports.ReportResponse.*;
import com.tcms.enums.*;
import com.tcms.models.*;
import com.tcms.repositories.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly=true)
public class ProjectReports {
    private final ProjectRepository projects;
    private final TestCaseRepository cases;
    private final TestExecutionRepository plans;
    private final UserRepository users;
    private final TestCaseSnapshots snapshots;
    public ProjectReports(ProjectRepository projects, TestCaseRepository cases, TestExecutionRepository plans, UserRepository users, TestCaseSnapshots snapshots) {
        this.projects=projects; this.cases=cases; this.plans=plans; this.users=users; this.snapshots=snapshots;
    }
    private TestResult status(TestCaseExecutions link) { return link.getResultStatus()==null ? TestResult.NOT_RUN : link.getResultStatus(); }
    private boolean complete(TestCaseExecutions link) { return Set.of(TestResult.PASS,TestResult.FAIL,TestResult.SKIPPED).contains(status(link)); }
    private String person(Users user) { return user==null ? "Unassigned" : user.getFirstName()+" "+user.getLastName(); }
    private static <E extends Enum<E>> Map<E,Long> counts(Class<E> type) {
        var result=new EnumMap<E,Long>(type); for (E value:type.getEnumConstants()) result.put(value,0L); return result;
    }
    private <E extends Enum<E>> Map<E,Long> classify(List<TestCase> library, Class<E> type, Function<TestCase,E> key) {
        var result=counts(type); library.forEach(tc -> result.merge(key.apply(tc),1L,Long::sum)); return result;
    }
    private Double percentage(long numerator,long denominator) { return denominator==0 ? null : Math.round(numerator*1000.0/denominator)/10.0; }
    private Map<TestResult,Long> results(Collection<TestCaseExecutions> links) {
        var result=counts(TestResult.class); links.forEach(link -> result.merge(status(link),1L,Long::sum)); return result;
    }
    private Double passRate(Map<TestResult,Long> results) { return percentage(results.get(TestResult.PASS),results.get(TestResult.PASS)+results.get(TestResult.FAIL)); }
    private CasePriority priority(TestCase tc) { return tc.getPriority()==null ? CasePriority.MEDIUM : tc.getPriority(); }
    private AutomationStatus automation(TestCase tc) { return tc.getAutomationStatus()==null ? AutomationStatus.NOT_AUTOMATED : tc.getAutomationStatus(); }
    public ReportResponse report(int projectId, LocalDate from, LocalDate to, String environment, String release) {
        if (projects.findById(projectId)==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Project not found");
        LocalDate end=to==null ? LocalDate.now(ZoneOffset.UTC) : to;
        LocalDate start=from==null ? end.minusDays(29) : from;
        if (start.isAfter(end) || ChronoUnit.DAYS.between(start,end)>365 || environment.length()>100 || release.length()>100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose an activity range of up to 366 days and valid filters");
        List<TestCase> library=cases.findByProjectsIdOrderByIdAsc(projectId);
        List<TestExecutions> allPlans=plans.findByProjectsIdOrderByIdDesc(projectId);
        List<TestExecutions> selected=allPlans.stream().filter(p -> environment.isEmpty() || environment.equals(p.getEnvironment()))
                .filter(p -> release.isEmpty() || release.equals(p.getReleaseName())).toList();
        List<TestCaseExecutions> executions=selected.stream().filter(p -> p.getExecutionStatus()!=ExecutionStatus.DRAFT)
                .flatMap(p -> p.getTestCaseExecutions().stream()).toList();
        Map<TestResult,Long> outcomes=results(executions);
        Map<Integer,TestCaseExecutions> latest=new HashMap<>();
        executions.stream().filter(l -> l.getRecordedAt()!=null).forEach(link -> latest.merge(link.getTestCase().getId(),link,
                (a,b) -> a.getRecordedAt().compareTo(b.getRecordedAt())>0 || a.getRecordedAt().equals(b.getRecordedAt()) && a.getId()>b.getId() ? a : b));
        Set<String> defects=executions.stream().map(TestCaseExecutions::getDefectUrl).filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
        Library librarySummary=new Library(library.size(),classify(library,CasePriority.class,this::priority),classify(library,AutomationStatus.class,this::automation),
                classify(library,TestTypes.class,TestCase::getTestType),(int)library.stream().filter(tc -> tc.getOwnerId()==null).count(),
                library.stream().filter(tc -> tc.getEstimatedMinutes()!=null).mapToLong(TestCase::getEstimatedMinutes).sum(),(int)library.stream().filter(tc -> tc.getEstimatedMinutes()!=null).count());
        long completed=executions.stream().filter(this::complete).count();
        int tested=(int)latest.values().stream().filter(link -> status(link)!=TestResult.NOT_RUN).count();
        Execution execution=new Execution((int)selected.stream().filter(p -> p.getExecutionStatus()==ExecutionStatus.IN_PROGRESS).count(),
                (int)selected.stream().filter(p -> p.getExecutionStatus()==ExecutionStatus.CLOSED).count(),
                (int)selected.stream().filter(p -> p.getExecutionStatus()==ExecutionStatus.DRAFT).count(),executions.size(),completed,
                passRate(outcomes),percentage(completed,executions.size()),tested,library.size()-tested,defects.size(),outcomes);
        List<Plan> planRows=selected.stream().map(p -> {
            var links=p.getTestCaseExecutions(); var result=results(links);
            return new Plan(p.getId(),p.getExecutionName(),p.getReleaseName(),p.getBuildName(),p.getEnvironment(),p.getExecutionStatus(),links.size(),
                    links.stream().filter(this::complete).count(),passRate(result),(int)links.stream().filter(l -> l.getAssignee()==null).count(),result);
        }).toList();
        Map<LocalDate,Map<TestResult,Long>> daily=new TreeMap<>();
        start.datesUntil(end.plusDays(1)).forEach(day -> daily.put(day,counts(TestResult.class)));
        executions.forEach(link -> {
            // Attempts are activity, not unique tests; reruns remain visible.
            if (link.getHistory().isEmpty() && link.getRecordedAt()!=null) addActivity(daily,link.getRecordedAt(),status(link));
            else link.getHistory().forEach(attempt -> addActivity(daily,attempt.getRecordedAt(),attempt.getStatus()));
        });
        Map<Integer,String> names=new HashMap<>();
        library.stream().map(TestCase::getOwnerId).filter(Objects::nonNull).distinct().forEach(id -> {
            var owner=users.findById(id.intValue()); names.put(id,owner==null ? "Former user #"+id : person(owner));
        });
        List<CaseRow> caseRows=library.stream().map(tc -> {
            TestCaseExecutions recent=latest.get(tc.getId());
            return new CaseRow(tc.getId(),tc.getTestProjectId(),tc.getTestName(),priority(tc),automation(tc),tc.getComponent(),tc.getOwnerId()==null ? "Unassigned" : names.get(tc.getOwnerId()),
                    tc.getEstimatedMinutes(),recent==null ? TestResult.NOT_RUN : status(recent),recent==null ? null : recent.getTestExecutions().getId(),
                    recent==null ? null : recent.getTestExecutions().getExecutionName(),recent==null ? null : recent.getRecordedAt(),recent==null ? null : recent.getDefectUrl(),
                    recent!=null && snapshots.definitionChanged(recent));
        }).toList();
        // Group by ID rather than display name: two testers may have the same name.
        Map<Integer,List<TestCaseExecutions>> groups=selected.stream().filter(p -> p.getExecutionStatus()==ExecutionStatus.IN_PROGRESS)
                .flatMap(p -> p.getTestCaseExecutions().stream()).collect(Collectors.groupingBy(l -> l.getAssignee()==null ? 0 : l.getAssignee().getId()));
        List<Workload> workload=groups.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(entry -> {
            var links=entry.getValue(); var pending=links.stream().filter(l -> !complete(l)).toList();
            long minutes=0; int missing=0;
            for (var link:pending) {
                var details=snapshots.read(link).details(); Integer estimate=details==null ? null : details.estimatedMinutes();
                if (estimate==null) missing++; else minutes+=estimate;
            }
            return new Workload(person(links.getFirst().getAssignee()),links.size(),pending.size(),(int)pending.stream().filter(l -> status(l)==TestResult.BLOCKED).count(),minutes,missing);
        }).toList();
        return new ReportResponse(Instant.now(),start,end,allPlans.stream().map(TestExecutions::getEnvironment).filter(Objects::nonNull).filter(s -> !s.isBlank()).distinct().sorted().toList(),
                allPlans.stream().map(TestExecutions::getReleaseName).filter(Objects::nonNull).filter(s -> !s.isBlank()).distinct().sorted().toList(),librarySummary,execution,planRows,
                daily.entrySet().stream().map(entry -> new Day(entry.getKey(),entry.getValue())).toList(),caseRows,workload);
    }
    private void addActivity(Map<LocalDate,Map<TestResult,Long>> daily,Instant when,TestResult status) {
        var day=daily.get(when.atZone(ZoneOffset.UTC).toLocalDate()); if (day!=null) day.merge(status,1L,Long::sum);
    }
}
