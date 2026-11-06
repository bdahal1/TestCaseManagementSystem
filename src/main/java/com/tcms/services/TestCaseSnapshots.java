package com.tcms.services;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.tcms.dto.plans.PlanResponses.*;
import com.tcms.models.*;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class TestCaseSnapshots {
    private final ObjectMapper mapper;
    public TestCaseSnapshots(ObjectMapper mapper) { this.mapper=mapper; }
    private Snapshot live(TestCase tc) {
        List<Step> steps=(tc.getTestStepsSet()==null ? Set.<TestSteps>of() : tc.getTestStepsSet()).stream()
                .filter(s -> s.getTestType()==tc.getTestType())
                .sorted(Comparator.comparing(TestSteps::getTestStepOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(TestSteps::getId, Comparator.nullsLast(Integer::compareTo)))
                .map(s -> new Step(s.getTestStepOrder(),s.getTestStepDesc(),s.getTestStepData(),s.getTestExpectedOutput())).toList();
        return new Snapshot(tc.getTestName(),tc.getTestType(),steps,new CaseDetails(tc.getPriority(),tc.getAutomationStatus(),tc.getComponent(),tc.getOwnerId(),
                tc.getEstimatedMinutes(),tc.getDescription(),tc.getPreconditions(),tc.getCustomFields()==null ? Map.of() : new LinkedHashMap<>(tc.getCustomFields())));
    }
    public void capture(TestCaseExecutions link) {
        if (link.getCaseSnapshot()!=null) return;
        try { link.setCaseSnapshot(mapper.writeValueAsString(live(link.getTestCase()))); }
        catch (JacksonException e) { throw new IllegalStateException("Unable to preserve test case",e); }
    }
    public Snapshot read(TestCaseExecutions link) {
        if (link.getCaseSnapshot()==null) return live(link.getTestCase());
        try { return mapper.readValue(link.getCaseSnapshot(),Snapshot.class); }
        catch (JacksonException e) { throw new IllegalStateException("Unable to read preserved test case",e); }
    }
    public boolean definitionChanged(TestCaseExecutions link) {
        Snapshot saved=read(link), current=live(link.getTestCase());
        return !Objects.equals(saved.name(),current.name()) || saved.type()!=current.type() || !Objects.equals(saved.steps(),current.steps())
                || saved.details()!=null && !Objects.equals(saved.details(),current.details());
    }
}
