package com.tcms.automation;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import com.tcms.enums.TestTypes;
import com.tcms.models.TestCase;
import com.tcms.models.TestSteps;
import com.tcms.repositories.ProjectRepository;
import com.tcms.repositories.TestCaseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutomationProjectExportService {
    private final TestCaseRepository testCaseRepository;
    private final ProjectRepository projectRepository;
    private final ProjectLocatorRepository locatorRepository;
    private final ProjectVariableRepository variableRepository;
    private final AutomationStepSerializer serializer;
    private final AutomationValidator validator;
    private final ObjectMapper mapper;

    public AutomationProjectExportService(TestCaseRepository testCaseRepository,
                                          ProjectRepository projectRepository,
                                          ProjectLocatorRepository locatorRepository,
                                          ProjectVariableRepository variableRepository,
                                          AutomationStepSerializer serializer,
                                          AutomationValidator validator,
                                          ObjectMapper mapper) {
        this.testCaseRepository = testCaseRepository;
        this.projectRepository = projectRepository;
        this.locatorRepository = locatorRepository;
        this.variableRepository = variableRepository;
        this.serializer = serializer;
        this.validator = validator;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public byte[] exportProjectZip(int projectId, List<Integer> caseIds) {
        var project = projectRepository.findById(projectId);
        if (project == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");

        if (caseIds == null || caseIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one automation test case to download");
        }

        List<TestCase> cases = new ArrayList<>();
        for (Integer cid : caseIds) {
            TestCase tc = testCaseRepository.findById(cid.intValue());
            if (tc == null || !tc.getProjects().getId().equals(projectId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case #" + cid + " not found in this project");
            }
            if (tc.getTestType() != TestTypes.CUCUMBER_AUTOMATION && tc.getTestType() != TestTypes.KEYWORD_DRIVEN) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Case #" + cid + " (" + tc.getTestName() + ") is not an automation case (type: " + tc.getTestType() + ")");
            }
            cases.add(tc);
        }

        // Build locators map
        Map<String, AutomationTestDefinition.LocatorDefinition> locatorsMap = new HashMap<>();
        List<ProjectLocator> projectLocators = locatorRepository.findByProjectIdOrderByLocatorNameAsc(projectId);
        for (ProjectLocator loc : projectLocators) {
            locatorsMap.put(loc.getLocatorName(), AutomationTestDefinition.LocatorDefinition.builder()
                    .type(loc.getLocatorType())
                    .value(loc.getLocatorValue())
                    .description(loc.getDescription())
                    .build());
        }

        // Validate all cases before exporting
        Map<Integer, List<AutomationStepModel>> parsedStepsByCase = new HashMap<>();
        for (TestCase tc : cases) {
            List<AutomationStepModel> steps = extractSteps(tc);
            var validation = validator.validate(steps, locatorsMap);
            if (!validation.isValid()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Export blocked: Case #" + tc.getId() + " (" + tc.getTestName() + ") configuration error: " + validation.getJoinedMessage());
            }
            parsedStepsByCase.put(tc.getId(), steps);
        }

        // Build ZIP
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipArchiveOutputStream zos = new ZipArchiveOutputStream(baos)) {

            // 1. pom.xml
            addZipEntry(zos, "pom.xml", generateStandalonePom());

            // 2. Maven wrapper scripts
            addZipEntry(zos, "mvnw", wrapperResource("mvnw"));
            addZipEntry(zos, "mvnw.cmd", wrapperResource("mvnw.cmd"));
            addZipEntry(zos, ".mvn/wrapper/maven-wrapper.properties", wrapperResource(".mvn/wrapper/maven-wrapper.properties"));

            addZipEntry(zos, ".gitignore", "target/\nsrc/test/resources/config.properties\n");

            // 3. README.md
            addZipEntry(zos, "README.md", generateReadme(project.getProjectName(), cases));

            // 4. manifest.json
            addZipEntry(zos, "manifest.json", generateManifest(projectId, project.getProjectName(), cases, parsedStepsByCase));

            // 5. locators.json
            addZipEntry(zos, "src/test/resources/locators.json", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(locatorsMap));

            // 6. config.properties.example
            addZipEntry(zos, "src/test/resources/config.properties.example", generateConfigExample(projectId));

            // 7. Case definitions (Features and Keyword specs)
            for (TestCase tc : cases) {
                List<AutomationStepModel> steps = parsedStepsByCase.get(tc.getId());
                String safeName = sanitizeFilename(tc.getTestName());
                if (tc.getTestType() == TestTypes.CUCUMBER_AUTOMATION) {
                    String featureContent = serializer.toGherkinScenario(tc.getTestName(), steps);
                    addZipEntry(zos, "src/test/resources/features/case_" + tc.getId() + "_" + safeName + ".feature", featureContent);
                } else {
                    String specJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of(
                            "id", tc.getId(),
                            "name", tc.getTestName(),
                            "type", tc.getTestType().name(),
                            "steps", steps
                    ));
                    addZipEntry(zos, "src/test/resources/keywords/case_" + tc.getId() + "_" + safeName + ".json", specJson);
                }
            }

            // 8. Java standalone framework source code
            addZipEntry(zos, "src/test/java/com/tcms/standalone/StandaloneDriver.java", generateStandaloneDriverJava());
            addZipEntry(zos, "src/test/java/com/tcms/standalone/StandaloneSteps.java", generateStandaloneStepsJava());
            if (cases.stream().anyMatch(tc -> tc.getTestType() == TestTypes.CUCUMBER_AUTOMATION))
                addZipEntry(zos, "src/test/java/com/tcms/standalone/CucumberRunnerTest.java", generateCucumberRunnerTestJava());
            addZipEntry(zos, "src/test/java/com/tcms/standalone/KeywordRunnerTest.java", generateKeywordRunnerTestJava());

            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate project archive", e);
        }
    }

    private List<AutomationStepModel> extractSteps(TestCase tc) {
        List<AutomationStepModel> steps = new ArrayList<>();
        if (tc.getTestStepsSet() != null) {
            tc.getTestStepsSet().stream()
                    .filter(s -> s.getTestType() == tc.getTestType())
                    .sorted(Comparator.comparing(TestSteps::getTestStepOrder, Comparator.nullsLast(Integer::compareTo)))
                    .forEach(s -> {
                        if (s.getTestStepData() != null && !s.getTestStepData().isBlank()) {
                            var model = serializer.deserializeStep(s.getTestStepData());
                            if (model != null) {
                                steps.add(model);
                            }
                        }
                    });
        }
        return steps;
    }

    private void addZipEntry(ZipArchiveOutputStream zos, String filename, String content) throws IOException {
        ZipArchiveEntry entry = new ZipArchiveEntry(filename);
        entry.setUnixMode(filename.equals("mvnw") ? 0100755 : 0100644);
        zos.putArchiveEntry(entry);
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeArchiveEntry();
    }

    private String sanitizeFilename(String name) {
        if (name == null) return "case";
        return name.toLowerCase().replaceAll("[^a-z0-9_-]+", "_").replaceAll("^_+|_+$", "");
    }

    private String generateManifest(int projectId, String projectName, List<TestCase> cases, Map<Integer, List<AutomationStepModel>> stepMap) throws IOException {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("manifestVersion", "1.0.0");
        manifest.put("exportedAt", Instant.now().toString());
        manifest.put("projectId", projectId);
        manifest.put("projectName", projectName);

        List<Map<String, Object>> caseList = new ArrayList<>();
        for (TestCase tc : cases) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("caseId", tc.getId());
            c.put("name", tc.getTestName());
            c.put("type", tc.getTestType().name());
            c.put("stepCount", stepMap.get(tc.getId()).size());
            caseList.add(c);
        }
        manifest.put("cases", caseList);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifest);
    }

    private String generateConfigExample(int projectId) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Standalone Automation Project Configuration\n");
        sb.append("# Copy this file to src/test/resources/config.properties\n\n");
        sb.append("baseUrl=http://localhost:8080\n");
        sb.append("headless=true\n");
        sb.append("stepTimeoutMs=8000\n\n");
        sb.append("# Sample Variables (populate with your local testing values):\n");

        List<ProjectVariable> vars = variableRepository.findByProjectIdOrderByVariableKeyAsc(projectId);
        for (ProjectVariable v : vars) {
            if (Boolean.TRUE.equals(v.getIsSecret())) {
                sb.append(v.getVariableKey()).append("=YOUR_").append(v.getVariableKey().toUpperCase()).append("_HERE\n");
            } else {
                sb.append(v.getVariableKey()).append("=").append(v.getVariableValue() != null ? v.getVariableValue() : "").append("\n");
            }
        }
        return sb.toString();
    }

    private String generateStandalonePom() {
        return """
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.tcms.standalone</groupId>
    <artifactId>automation-tests</artifactId>
    <version>1.0.0</version>
    <name>Standalone Automation Tests</name>
    <properties>
        <maven.compiler.release>26</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <playwright.version>1.50.0</playwright.version>
        <cucumber.version>7.21.1</cucumber.version>
        <junit.version>5.12.0</junit.version>
    </properties>
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.junit</groupId>
                <artifactId>junit-bom</artifactId>
                <version>${junit.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.microsoft.playwright</groupId>
            <artifactId>playwright</artifactId>
            <version>${playwright.version}</version>
        </dependency>
        <dependency>
            <groupId>io.cucumber</groupId>
            <artifactId>cucumber-java</artifactId>
            <version>${cucumber.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.cucumber</groupId>
            <artifactId>cucumber-junit-platform-engine</artifactId>
            <version>${cucumber.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.platform</groupId>
            <artifactId>junit-platform-suite</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>tools.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>3.1.7</version>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.14.1</version>
                <configuration>
                    <release>${maven.compiler.release}</release>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.5.6</version>
            </plugin>
        </plugins>
    </build>
</project>
""";
    }

    private String generateReadme(String projectName, List<TestCase> cases) {
        return """
# Standalone Automation Project - %s

This is a standalone test automation project generated by the Test Case Management System.
It runs independently without a running TMS server.

## Prerequisites
- **Java 26**: JDK 26 must be installed and set on your PATH (or `JAVA_HOME`).
- **Maven**: Run with the included `./mvnw` wrapper script.

## Setup
1. **Install Playwright Browsers** (one-time setup):
   ```bash
   ./mvnw exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
   ```
2. **Configure Runtime Environment**:
   Copy the example configuration to `config.properties`:
   ```bash
   cp src/test/resources/config.properties.example src/test/resources/config.properties
   ```
   Edit `config.properties` to set your target `baseUrl` and credentials/secrets.

## Running Tests
- **Run all automated cases**:
  ```bash
  ./mvnw test
  ```
- **Run Cucumber scenarios only**:
  ```bash
  ./mvnw test -Dtest=CucumberRunnerTest
  ```
- **Run Keyword-driven cases only**:
  ```bash
  ./mvnw test -Dtest=KeywordRunnerTest
  ```

## Included Cases (%d cases)
%s
""".formatted(projectName, cases.size(),
                cases.stream().map(c -> "- #" + c.getId() + " [" + c.getTestType() + "] " + c.getTestName()).reduce("", (a, b) -> a + b + "\n"));
    }

    private String wrapperResource(String name) {
        try (var stream = new ClassPathResource("automation-export/" + name).getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) { throw new IllegalStateException("Missing Maven wrapper resource", e); }
    }

    private String generateStandaloneDriverJava() {
        return """
package com.tcms.standalone;

import com.microsoft.playwright.*;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.AriaRole;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StandaloneDriver implements AutoCloseable {
    private final Properties config = new Properties();
    private Map<String, Map<String, String>> locators = new HashMap<>();
    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;
    private double stepTimeoutMs = 8000.0;

    public StandaloneDriver() {
        loadConfig();
        loadLocators();
    }

    private void loadConfig() {
        try (InputStream is = getClass().getResourceAsStream("/config.properties")) {
            if (is != null) config.load(is);
        } catch (Exception ignored) {}
        try (InputStream is = getClass().getResourceAsStream("/config.properties.example")) {
            if (is != null) {
                Properties defaults = new Properties();
                defaults.load(is);
                defaults.forEach((k, v) -> config.putIfAbsent(k, v));
            }
        } catch (Exception ignored) {}

        if (config.containsKey("stepTimeoutMs")) {
            try { stepTimeoutMs = Double.parseDouble(config.getProperty("stepTimeoutMs")); } catch (Exception ignored) {}
        }
    }

    private void loadLocators() {
        try (InputStream is = getClass().getResourceAsStream("/locators.json")) {
            if (is != null) {
                ObjectMapper mapper = new ObjectMapper();
                locators = mapper.readValue(is, new TypeReference<Map<String, Map<String, String>>>() {});
            }
        } catch (Exception ignored) {}
    }

    public void init() {
        boolean headless = Boolean.parseBoolean(config.getProperty("headless", "true"));
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless));
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 720));
        page = context.newPage();
        page.setDefaultTimeout(stepTimeoutMs);
        page.setDefaultNavigationTimeout(stepTimeoutMs);
    }

    public String resolveVariables(String input) {
        if (input == null) return null;
        Pattern pattern = Pattern.compile("\\\\$\\\\{([a-zA-Z0-9_.-]+)\\\\}");
        Matcher matcher = pattern.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String val = config.getProperty(key);
            if (val == null) val = System.getenv(key);
            if (val == null) val = matcher.group(0);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(val));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public Locator resolveLocator(String raw) {
        String resolved = resolveVariables(raw).trim();
        String type = "CSS";
        if (locators.containsKey(resolved)) {
            var def = locators.get(resolved);
            type = def.getOrDefault("type", "CSS");
            resolved = resolveVariables(def.getOrDefault("value", resolved));
        } else if (resolved.startsWith("css=")) {
            type = "CSS"; resolved = resolved.substring(4);
        } else if (resolved.startsWith("xpath=")) {
            type = "XPATH"; resolved = resolved.substring(6);
        } else if (resolved.startsWith("id=")) {
            type = "ID"; resolved = resolved.substring(3);
        } else if (resolved.startsWith("testid=")) {
            type = "TEST_ID"; resolved = resolved.substring(7);
        } else if (resolved.startsWith("role=")) {
            type = "ROLE_NAME"; resolved = resolved.substring(5);
        } else if (resolved.startsWith("//") || resolved.startsWith("(")) {
            type = "XPATH";
        }

        return switch (type.toUpperCase()) {
            case "ID" -> page.locator("#" + resolved);
            case "CSS" -> page.locator(resolved);
            case "XPATH" -> page.locator("xpath=" + resolved);
            case "TEST_ID" -> page.getByTestId(resolved);
            case "ROLE_NAME" -> {
                Pattern rolePat = Pattern.compile("([A-Za-z]+)(?:\\\\[name=['\\\"]([^'\\\"]+)['\\\"]\\\\])?");
                Matcher m = rolePat.matcher(resolved);
                if (m.matches()) {
                    AriaRole role = AriaRole.valueOf(m.group(1).toUpperCase());
                    String name = m.group(2);
                    yield name != null ? page.getByRole(role, new Page.GetByRoleOptions().setName(name)) : page.getByRole(role);
                }
                yield page.locator(resolved);
            }
            default -> page.locator(resolved);
        };
    }

    public void openPage(String url) {
        page.navigate(resolveVariables(url));
        page.waitForLoadState();
    }

    public void enterText(String loc, String val) {
        resolveLocator(loc).fill(resolveVariables(val));
    }

    public void clickElement(String loc) {
        resolveLocator(loc).click();
    }

    public void verifyTextEquals(String loc, String exp) {
        PlaywrightAssertions.assertThat(resolveLocator(loc)).hasText(resolveVariables(exp), new LocatorAssertions.HasTextOptions().setTimeout(stepTimeoutMs));
    }

    public void verifyTextContains(String loc, String exp) {
        PlaywrightAssertions.assertThat(resolveLocator(loc)).containsText(resolveVariables(exp), new LocatorAssertions.ContainsTextOptions().setTimeout(stepTimeoutMs));
    }

    public void verifyElementState(String loc, String state) {
        if ("visible".equalsIgnoreCase(state)) {
            PlaywrightAssertions.assertThat(resolveLocator(loc)).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(stepTimeoutMs));
        } else {
            PlaywrightAssertions.assertThat(resolveLocator(loc)).isHidden(new LocatorAssertions.IsHiddenOptions().setTimeout(stepTimeoutMs));
        }
    }

    @Override
    public void close() {
        try { if (context != null) context.close(); } catch (Exception ignored) {}
        try { if (browser != null) browser.close(); } catch (Exception ignored) {}
        try { if (playwright != null) playwright.close(); } catch (Exception ignored) {}
    }
}
""";
    }

    private String generateStandaloneStepsJava() {
        return """
package com.tcms.standalone;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;

public class StandaloneSteps {
    private StandaloneDriver driver;

    @Before
    public void setup() {
        driver = new StandaloneDriver();
        driver.init();
    }

    @After
    public void teardown() {
        if (driver != null) driver.close();
    }

    @Given("^open page \\"([^\\"]*)\\"$")
    public void openPage(String url) { driver.openPage(url); }

    @When("^enter text \\"([^\\"]*)\\" into \\"([^\\"]*)\\"$")
    public void enterText(String val, String loc) { driver.enterText(loc, val); }

    @When("^click element \\"([^\\"]*)\\"$")
    public void clickElement(String loc) { driver.clickElement(loc); }

    @Then("^verify text of \\"([^\\"]*)\\" equals \\"([^\\"]*)\\"$")
    public void verifyTextEquals(String loc, String exp) { driver.verifyTextEquals(loc, exp); }

    @Then("^verify text of \\"([^\\"]*)\\" contains \\"([^\\"]*)\\"$")
    public void verifyTextContains(String loc, String exp) { driver.verifyTextContains(loc, exp); }

    @Then("^verify element \\"([^\\"]*)\\" is (visible|hidden)$")
    public void verifyElementState(String loc, String state) { driver.verifyElementState(loc, state); }
}
""";
    }

    private String generateCucumberRunnerTestJava() {
        return """
package com.tcms.standalone;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.tcms.standalone")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
public class CucumberRunnerTest {
}
""";
    }

    private String generateKeywordRunnerTestJava() {
        return """
package com.tcms.standalone;

import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class KeywordRunnerTest {

    @Test
    public void runAllKeywordCases() throws Exception {
        URL keywordsDirUrl = getClass().getResource("/keywords");
        if (keywordsDirUrl == null) {
            System.out.println("No keyword specs found, skipping.");
            return;
        }

        Path dirPath = Paths.get(keywordsDirUrl.toURI());
        ObjectMapper mapper = new ObjectMapper();

        try (var stream = Files.list(dirPath)) {
            List<Path> specFiles = stream.filter(p -> p.toString().endsWith(".json")).toList();
            for (Path specFile : specFiles) {
                System.out.println("Executing keyword spec: " + specFile.getFileName());
                Map<String, Object> spec = mapper.readValue(Files.readAllBytes(specFile), new TypeReference<>() {});
                List<Map<String, Object>> steps = (List<Map<String, Object>>) spec.get("steps");

                try (StandaloneDriver driver = new StandaloneDriver()) {
                    driver.init();
                    for (Map<String, Object> step : steps) {
                        String template = (String) step.get("template");
                        String loc = (String) step.get("locatorValue");
                        String val = (String) step.get("textValue");
                        String state = (String) step.get("stateValue");

                        switch (template) {
                            case "OPEN_PAGE" -> driver.openPage(val);
                            case "ENTER_TEXT" -> driver.enterText(loc, val);
                            case "CLICK_ELEMENT" -> driver.clickElement(loc);
                            case "VERIFY_TEXT_EQUALS" -> driver.verifyTextEquals(loc, val);
                            case "VERIFY_TEXT_CONTAINS" -> driver.verifyTextContains(loc, val);
                            case "VERIFY_ELEMENT_STATE" -> driver.verifyElementState(loc, state);
                        }
                    }
                }
            }
        }
    }
}
""";
    }
}
