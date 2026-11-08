package com.tcms.automation;

import com.sun.net.httpserver.HttpServer;
import com.tcms.enums.TestTypes;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

class AutomationEdgeCasesTest {
    private static HttpServer target;
    private static String url;
    private final AutomationStepSerializer serializer = new AutomationStepSerializer(new ObjectMapper());
    private static final String LABEL = "He said \"Hi\" \\ option";
    private static final String TEXT = "C:\\temp\\file \"quoted\" O'Neil\nsecond\tline";

    @BeforeAll static void startTarget() throws Exception {
        target = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        target.createContext("/controls", exchange -> {
            String page = """
                <html><body>
                <select id='options'><option>Initial</option><option>He said "Hi" \\ option</option></select>
                <input id='check' type='checkbox' onchange="document.getElementById('status').textContent=this.checked ? 'Checked' : 'Unchecked'">
                <div id='status'>Unchecked</div>
                <textarea id='text' onkeydown="if(event.key==='Enter'){event.preventDefault();document.getElementById('result').textContent=this.value}"></textarea>
                <pre id='result'></pre>
                <div id='hidden' hidden data-empty=''></div>
                <button id='later' onclick="setTimeout(()=>document.getElementById('hidden').setAttribute('data-ready','done'),150)">Apply</button>
                </body></html>
                """;
            byte[] bytes = page.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        target.start();
        url = "http://127.0.0.1:" + target.getAddress().getPort() + "/controls?q=a.b+%5B1%5D";
    }

    @AfterAll static void stopTarget() { if (target != null) target.stop(0); }

    private AutomationExecutionResult execute(TestTypes type, List<AutomationStepModel> steps) {
        int order = 1;
        for (var step : steps) { step.setOrder(order++); if (step.getKeyword() == null) step.setKeyword("When"); }
        var definition = AutomationTestDefinition.builder().caseName("Escaped values and controls").testType(type)
                .steps(steps).variables(Map.of("key", "enter"))
                .executionConfig(Map.of("headless", "true", "stepTimeoutMs", "1200")).build();
        return type == TestTypes.CUCUMBER_AUTOMATION
                ? new AutomationCucumberRunner(serializer).execute(definition)
                : new AutomationKeywordRunner(serializer).execute(definition);
    }

    @ParameterizedTest
    @EnumSource(value=TestTypes.class, names={"CUCUMBER_AUTOMATION", "KEYWORD_DRIVEN"})
    void escapedValuesIdempotentCheckboxesAndDelayedHiddenAttributesWork(TestTypes type) {
        var steps = List.of(
                AutomationStepModel.builder().template(AutomationTemplate.OPEN_PAGE).textValue(url).build(),
                AutomationStepModel.builder().template(AutomationTemplate.SELECT_OPTION).locatorValue("#options").selectBy(OptionSelectBy.LABEL).textValue(LABEL).build(),
                AutomationStepModel.builder().template(AutomationTemplate.VERIFY_TEXT_EQUALS).locatorValue("#options option:checked").textValue(LABEL).build(),
                AutomationStepModel.builder().template(AutomationTemplate.SET_CHECKBOX).locatorValue("#check").checkedValue(true).build(),
                AutomationStepModel.builder().template(AutomationTemplate.SET_CHECKBOX).locatorValue("#check").checkedValue(true).build(),
                AutomationStepModel.builder().template(AutomationTemplate.VERIFY_TEXT_EQUALS).locatorValue("#status").textValue("Checked").build(),
                AutomationStepModel.builder().template(AutomationTemplate.SET_CHECKBOX).locatorValue("#check").checkedValue(false).build(),
                AutomationStepModel.builder().template(AutomationTemplate.SET_CHECKBOX).locatorValue("#check").checkedValue(false).build(),
                AutomationStepModel.builder().template(AutomationTemplate.VERIFY_TEXT_EQUALS).locatorValue("#status").textValue("Unchecked").build(),
                AutomationStepModel.builder().template(AutomationTemplate.ENTER_TEXT).locatorValue("#text").textValue(TEXT).build(),
                AutomationStepModel.builder().template(AutomationTemplate.PRESS_KEY).locatorValue("#text").keyValue("${key}").build(),
                AutomationStepModel.builder().template(AutomationTemplate.VERIFY_TEXT_EQUALS).locatorValue("#result").textValue(TEXT).build(),
                AutomationStepModel.builder().template(AutomationTemplate.VERIFY_ATTRIBUTE).locatorValue("#hidden").attributeName("data-empty").textValue("").build(),
                AutomationStepModel.builder().template(AutomationTemplate.CLICK_ELEMENT).locatorValue("#later").build(),
                AutomationStepModel.builder().template(AutomationTemplate.VERIFY_ATTRIBUTE).locatorValue("#hidden").attributeName("data-ready").textValue("done").build(),
                AutomationStepModel.builder().template(AutomationTemplate.VERIFY_URL).urlComparison(UrlComparison.CONTAINS).textValue("/controls?q=a.b+%5B1%5D").build());
        var result = execute(type, steps);
        assertThat(result.getStatus()).withFailMessage(() -> result.getErrorMessage()).isEqualTo(JobStatus.COMPLETED);
        assertThat(result.getStepResults()).hasSize(steps.size()).allMatch(AutomationExecutionResult.StepResult::isPassed);
    }

    @ParameterizedTest
    @EnumSource(value=TestTypes.class, names={"CUCUMBER_AUTOMATION", "KEYWORD_DRIVEN"})
    void missingAttributeIsNotAnEmptyAttribute(TestTypes type) {
        var result = execute(type, List.of(
                AutomationStepModel.builder().template(AutomationTemplate.OPEN_PAGE).textValue(url).build(),
                AutomationStepModel.builder().template(AutomationTemplate.VERIFY_ATTRIBUTE).locatorValue("#hidden").attributeName("data-missing").textValue("").build()));
        assertThat(result.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(result.getErrorMessage()).contains("does not have attribute 'data-missing'");
    }

    @ParameterizedTest
    @ValueSource(strings={"", "He said \"Hi\"", "C:\\temp\\files", "\\\"'", "line\nnext\tcolumn\rend", "unicode: नेपाल"})
    void quotedStringCodecRoundTrips(String value) {
        assertThat(GherkinStrings.decode(GherkinStrings.escape(value))).isEqualTo(value);
    }

    @Test void urlChecksHonorCancellation() {
        try (var driver = new AutomationBrowserDriver(AutomationTestDefinition.builder().build(), () -> true)) {
            assertThatThrownBy(() -> driver.verifyUrl("https://example.test",UrlComparison.EQUALS))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("cancelled");
        }
    }
    @Test void logsAndIngestionMaskEscapedAndOverlappingSecrets() {
        String secret="private\"quote\\path'123";
        String log=secret+" | "+GherkinStrings.escape(secret)+" | ${PASSWORD}";
        try (var driver=new AutomationBrowserDriver(AutomationTestDefinition.builder()
                .variables(Map.of("PASSWORD",secret,"SHORT","path'123")).secretKeys(Set.of("PASSWORD","SHORT")).build())) {
            assertThat(driver.redact(log)).isEqualTo("[REDACTED] | [REDACTED] | ${PASSWORD}");
        }
    }

}
