package com.tcms.services;

import com.tcms.enums.TestTypes;
import com.tcms.models.*;
import com.tcms.models.TestCase;
import io.cucumber.gherkin.GherkinParser;
import io.cucumber.messages.types.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Component
public class GherkinDocuments {
    private final GherkinParser parser=GherkinParser.builder().includeSource(false).includePickles(false).build();
    private ResponseStatusException invalid(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,message); }
    public void validate(String document,String title) {
        if (document==null || document.isBlank()) throw invalid("Write a Gherkin scenario before saving");
        if (document.length()>100000) throw invalid("Keep the scenario below 100,000 characters");
        String safeTitle=title==null ? "Test case" : title.replaceAll("[\\r\\n]"," ");
        boolean feature=java.util.regex.Pattern.compile("(?m)^\\s*Feature\\s*:").matcher(document).find();
        boolean scenario=java.util.regex.Pattern.compile("(?m)^\\s*(Scenario(?: Outline| Template)?|Example|Background|Rule)\\s*:").matcher(document).find();
        int offset=feature ? 0 : scenario ? 1 : 2;
        String source=(feature ? "" : "Feature: "+safeTitle+"\n"+(scenario ? "" : "Scenario: "+safeTitle+"\n"))+document;
        var envelopes=parser.parse("test-case.feature",source.getBytes(StandardCharsets.UTF_8)).toList();
        for (var envelope:envelopes) if (envelope.getParseError().isPresent()) {
            var error=envelope.getParseError().orElseThrow();
            String location=error.getSource().getLocation().map(l -> "Line "+Math.max(1,l.getLine()-offset)+": ").orElse("");
            throw invalid(location+error.getMessage().replaceFirst("^\\([^)]*\\):?\\s*",""));
        }
        List<Scenario> scenarios=new ArrayList<>();
        envelopes.stream().map(Envelope::getGherkinDocument).flatMap(Optional::stream).map(GherkinDocument::getFeature).flatMap(Optional::stream)
                .flatMap(f -> f.getChildren().stream()).forEach(child -> {
                    child.getScenario().ifPresent(scenarios::add);
                    child.getRule().ifPresent(rule -> rule.getChildren().forEach(nested -> nested.getScenario().ifPresent(scenarios::add)));
                });
        if (scenarios.isEmpty()) throw invalid("Add at least one Scenario or Scenario Outline");
        for (Scenario item:scenarios) {
            if (item.getSteps().isEmpty() || item.getSteps().stream().anyMatch(step -> step.getText().isBlank())) throw invalid("Describe every step in the scenario");
            if ((item.getKeyword().contains("Outline") || item.getKeyword().contains("Template"))
                    && item.getExamples().stream().noneMatch(example -> !example.getTableBody().isEmpty()))
                throw invalid("Add an Examples table with at least one data row");
        }
    }
    public void replace(TestCase testCase,String document,String author) {
        if (testCase.getTestType()!=TestTypes.CUCUMBER_MANUAL) throw invalid("Gherkin documents belong to Cucumber Manual cases");
        validate(document,testCase.getTestName());
        if (testCase.getTestStepsSet()==null) testCase.setTestStepsSet(new HashSet<>());
        TestSteps step=testCase.getTestStepsSet().stream().filter(s -> s.getTestType()==TestTypes.CUCUMBER_MANUAL)
                .min(Comparator.comparing(TestSteps::getId,Comparator.nullsLast(Integer::compareTo))).orElse(null);
        Timestamp now=Timestamp.from(Instant.now());
        if (step==null) {
            step=new TestSteps(); step.setTestCase(testCase); step.setTestType(TestTypes.CUCUMBER_MANUAL);
            step.setTsCreatedBy(author); step.setTsCreatedDate(now); testCase.getTestStepsSet().add(step);
        }
        TestSteps kept=step;
        testCase.getTestStepsSet().removeIf(other -> other.getTestType()==TestTypes.CUCUMBER_MANUAL && other!=kept);
        step.setTestStepDesc(document); step.setTestStepData(""); step.setTestExpectedOutput(""); step.setTestStepOrder(1);
        step.setTsModifiedBy(author); step.setTsModifiedDate(now);
    }
}
