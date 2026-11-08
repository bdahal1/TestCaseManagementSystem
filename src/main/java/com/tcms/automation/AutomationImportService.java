package com.tcms.automation;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.tcms.enums.TestTypes;
import com.tcms.helper.util.Util;
import com.tcms.models.Projects;
import com.tcms.models.TestCase;
import com.tcms.models.TestSteps;
import com.tcms.models.Users;
import com.tcms.repositories.ProjectRepository;
import com.tcms.repositories.TestCaseRepository;
import com.tcms.repositories.TestStepsRepository;
import com.tcms.repositories.UserRepository;
import io.cucumber.gherkin.GherkinParser;
import io.cucumber.messages.types.Envelope;
import io.cucumber.messages.types.Feature;
import io.cucumber.messages.types.FeatureChild;
import io.cucumber.messages.types.GherkinDocument;
import io.cucumber.messages.types.ParseError;
import io.cucumber.messages.types.Scenario;
import io.cucumber.messages.types.Step;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AutomationImportService {

    public static final int MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024; // 2MB
    public static final int MAX_CASES_PER_IMPORT = 100;

    private static final String A = GherkinStrings.ARGUMENT;
    private static final Pattern OPEN_PAGE_PATTERN = Pattern.compile("(?i)^open\\s+page\\s+" + A + "$");
    private static final Pattern ENTER_TEXT_PATTERN = Pattern.compile("(?i)^enter\\s+text\\s+" + A + "\\s+into\\s+" + A + "$");
    private static final Pattern CLICK_ELEMENT_PATTERN = Pattern.compile("(?i)^click\\s+element\\s+" + A + "$");
    private static final Pattern VERIFY_TEXT_EQUALS_PATTERN = Pattern.compile("(?i)^verify\\s+text\\s+of\\s+" + A + "\\s+equals\\s+" + A + "$");
    private static final Pattern VERIFY_TEXT_CONTAINS_PATTERN = Pattern.compile("(?i)^verify\\s+text\\s+of\\s+" + A + "\\s+contains\\s+" + A + "$");
    private static final Pattern VERIFY_ELEMENT_STATE_PATTERN = Pattern.compile("(?i)^verify\\s+element\\s+" + A + "\\s+is\\s+(visible|hidden)$");
    private static final Pattern SELECT_OPTION_PATTERN = Pattern.compile("(?i)^select\\s+option\\s+" + A + "(?:\\s+by\\s+(label|value))?\\s+(?:in|from)\\s+" + A + "$");
    private static final Pattern SET_CHECKBOX_PATTERN = Pattern.compile("(?i)^set\\s+checkbox\\s+" + A + "\\s+to\\s+(?:\"(checked|unchecked)\"|'(checked|unchecked)'|(checked|unchecked))$");
    private static final Pattern PRESS_KEY_PATTERN = Pattern.compile("(?i)^press\\s+key\\s+" + A + "\\s+on(?:\\s+element)?\\s+" + A + "$");
    private static final Pattern VERIFY_URL_PATTERN = Pattern.compile("(?i)^verify\\s+url\\s+(equals|contains)\\s+" + A + "$");
    private static final Pattern VERIFY_ATTRIBUTE_PATTERN = Pattern.compile("(?i)^verify\\s+attribute\\s+" + A + "\\s+of\\s+" + A + "\\s+equals\\s+" + A + "$");

    private final ProjectRepository projectRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestStepsRepository testStepsRepository;
    private final UserRepository userRepository;
    private final ProjectLocatorRepository locatorRepository;
    private final ProjectVariableRepository variableRepository;
    private final AutomationValidator validator;
    private final AutomationStepSerializer serializer;
    private final ObjectMapper mapper;
    private final GherkinParser gherkinParser = GherkinParser.builder().includeSource(false).includePickles(false).build();

    public AutomationImportService(ProjectRepository projectRepository,
                                   TestCaseRepository testCaseRepository,
                                   TestStepsRepository testStepsRepository,
                                   UserRepository userRepository,
                                   ProjectLocatorRepository locatorRepository,
                                   ProjectVariableRepository variableRepository,
                                   AutomationValidator validator,
                                   AutomationStepSerializer serializer,
                                   ObjectMapper mapper) {
        this.projectRepository = projectRepository;
        this.testCaseRepository = testCaseRepository;
        this.testStepsRepository = testStepsRepository;
        this.userRepository = userRepository;
        this.locatorRepository = locatorRepository;
        this.variableRepository = variableRepository;
        this.validator = validator;
        this.serializer = serializer;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ImportPreviewResponse previewImport(int projectId, String fileName, byte[] content) {
        Projects project = projectRepository.findById(projectId);
        if (project == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project #" + projectId + " not found");
        }

        if (content == null || content.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file is empty");
        }

        if (content.length > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size exceeds maximum limit of 2MB");
        }

        String safeFileName = (fileName == null || fileName.isBlank()) ? "uploaded_file" : fileName.trim();
        String lowerName = safeFileName.toLowerCase();

        // Get secrets to mask
        Map<String, String> secretValues = getProjectSecrets(projectId);

        // Locators map for validation
        Map<String, AutomationTestDefinition.LocatorDefinition> locatorsMap = getProjectLocators(projectId);

        // Existing project cases for duplicate detection
        List<TestCase> existingCases = testCaseRepository.findByProjectsIdOrderByIdAsc(projectId);

        if (lowerName.endsWith(".feature")) {
            return finishPreview(previewFeature(safeFileName, content, project, existingCases, locatorsMap, secretValues), secretValues);
        } else if (lowerName.endsWith(".json")) {
            return finishPreview(previewKeywordJson(safeFileName, content, project, existingCases, locatorsMap, secretValues), secretValues);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported file type. Only Cucumber .feature and Keyword Driven .json files are supported for import.");
        }
    }

    private ImportPreviewResponse previewFeature(String fileName,
                                                 byte[] content,
                                                 Projects project,
                                                 List<TestCase> existingCases,
                                                 Map<String, AutomationTestDefinition.LocatorDefinition> locatorsMap,
                                                 Map<String, String> secretValues) {
        List<String> globalErrors = new ArrayList<>();
        List<ImportCasePreview> cases = new ArrayList<>();

        List<Envelope> envelopes;
        try {
            envelopes = gherkinParser.parse(fileName, content).toList();
        } catch (Exception e) {
            return ImportPreviewResponse.builder()
                    .fileName(fileName)
                    .fileType("FEATURE")
                    .hasErrors(true)
                    .canSave(false)
                    .globalErrors(List.of("Gherkin parser failed: " + e.getMessage()))
                    .build();
        }

        for (Envelope env : envelopes) {
            if (env.getParseError().isPresent()) {
                ParseError pe = env.getParseError().get();
                String location = pe.getSource().getLocation().map(l -> "Line " + l.getLine() + ": ").orElse("");
                globalErrors.add("Syntax error in feature file: " + location + pe.getMessage().replaceFirst("^\\([^)]*\\):?\\s*", ""));
            }
        }

        if (!globalErrors.isEmpty()) {
            return ImportPreviewResponse.builder()
                    .fileName(fileName)
                    .fileType("FEATURE")
                    .hasErrors(true)
                    .canSave(false)
                    .globalErrors(globalErrors)
                    .build();
        }

        Optional<Feature> featureOpt = envelopes.stream()
                .map(Envelope::getGherkinDocument)
                .flatMap(Optional::stream)
                .map(GherkinDocument::getFeature)
                .flatMap(Optional::stream)
                .findFirst();

        if (featureOpt.isEmpty()) {
            return ImportPreviewResponse.builder()
                    .fileName(fileName)
                    .fileType("FEATURE")
                    .hasErrors(true)
                    .canSave(false)
                    .globalErrors(List.of("No Feature definition found in the uploaded .feature file"))
                    .build();
        }

        Feature feature = featureOpt.get();
        String featureName = feature.getName() == null || feature.getName().isBlank() ? "Imported Feature" : feature.getName().trim();

        // 1. Collect background steps (ordered)
        List<Step> backgroundSteps = new ArrayList<>();
        for (FeatureChild child : feature.getChildren()) {
            if (child.getBackground().isPresent()) {
                backgroundSteps.addAll(child.getBackground().get().getSteps());
            }
        }

        record ScopedScenario(Scenario scenario, List<Step> background) {}
        List<ScopedScenario> scenarios = new ArrayList<>();
        for (FeatureChild child : feature.getChildren()) {
            child.getScenario().ifPresent(scenario -> scenarios.add(new ScopedScenario(scenario, backgroundSteps)));
            child.getRule().ifPresent(rule -> {
                List<Step> ruleBackground = new ArrayList<>(backgroundSteps);
                rule.getChildren().forEach(nested -> nested.getBackground().ifPresent(background -> ruleBackground.addAll(background.getSteps())));
                rule.getChildren().forEach(nested -> nested.getScenario().ifPresent(scenario ->
                        scenarios.add(new ScopedScenario(scenario, ruleBackground))));
            });
        }

        if (scenarios.isEmpty()) {
            return ImportPreviewResponse.builder()
                    .fileName(fileName)
                    .fileType("FEATURE")
                    .hasErrors(true)
                    .canSave(false)
                    .globalErrors(List.of("Feature contains no Scenarios to import"))
                    .build();
        }

        if (scenarios.size() > MAX_CASES_PER_IMPORT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Import limit exceeded: found " + scenarios.size() + " scenarios, maximum allowed is " + MAX_CASES_PER_IMPORT);
        }

        boolean hasAnyErrors = false;
        int totalStepCount = 0;

        for (ScopedScenario scoped : scenarios) {
            Scenario scenario = scoped.scenario();
            String scenarioName = scenario.getName() == null || scenario.getName().isBlank()
                    ? featureName + " - Scenario " + (cases.size() + 1)
                    : scenario.getName().trim();

            int scenarioLine = scenario.getLocation().getLine().intValue();
            String caseUnsupported = null;
            if (scenario.getKeyword().contains("Outline") || scenario.getKeyword().contains("Template") || !scenario.getExamples().isEmpty()) {
                caseUnsupported = "Scenario Outlines and Examples are not supported for automation import in this milestone (Line " + scenarioLine + ")";
                hasAnyErrors = true;
            }

            // Conflict detection
            Optional<TestCase> existingMatch = existingCases.stream()
                    .filter(tc -> tc.getTestName() != null && tc.getTestName().trim().equalsIgnoreCase(scenarioName))
                    .findFirst();

            ImportCasePreview.ImportCasePreviewBuilder caseBuilder = ImportCasePreview.builder()
                    .name(scenarioName)
                    .testType(TestTypes.CUCUMBER_AUTOMATION)
                    .sourceLine(scenarioLine)
                    .unsupportedSyntax(caseUnsupported)
                    .conflict(existingMatch.isPresent())
                    .action(ImportAction.CREATE_NEW);

            if (existingMatch.isPresent()) {
                TestCase match = existingMatch.get();
                caseBuilder.existingCaseId(match.getId())
                        .existingCaseProjectId(match.getTestProjectId())
                        .existingCaseName(match.getTestName());
            }

            List<ImportStepPreview> stepPreviews = new ArrayList<>();
            int stepOrder = 1;

            // Combine Background steps + Scenario steps
            List<Step> allSteps = new ArrayList<>();
            allSteps.addAll(scoped.background());
            allSteps.addAll(scenario.getSteps());

            int unmappedCount = 0;
            int errorCount = caseUnsupported != null ? 1 : 0;

            for (Step gherkinStep : allSteps) {
                int line = gherkinStep.getLocation().getLine().intValue();
                String rawKeyword = gherkinStep.getKeyword() == null ? "When" : gherkinStep.getKeyword().trim();
                String stepText = gherkinStep.getText() == null ? "" : gherkinStep.getText().trim();

                String stepUnsupported = null;
                if (gherkinStep.getDataTable().isPresent()) {
                    stepUnsupported = "Data tables are not supported for automation import in this milestone (Line " + line + ")";
                } else if (gherkinStep.getDocString().isPresent()) {
                    stepUnsupported = "Doc strings are not supported for automation import in this milestone (Line " + line + ")";
                }

                ImportStepPreview stepPreview = matchGherkinStep(stepText, rawKeyword, line, stepOrder++, locatorsMap, secretValues);
                if (stepUnsupported != null) {
                    stepPreview.setUnsupportedSyntax(stepUnsupported);
                    errorCount++;
                    hasAnyErrors = true;
                }

                if (!stepPreview.isMapped()) {
                    unmappedCount++;
                    errorCount++;
                    hasAnyErrors = true;
                } else if (stepPreview.getValidationError() != null) {
                    errorCount++;
                    hasAnyErrors = true;
                }

                stepPreviews.add(stepPreview);
            }

            totalStepCount += stepPreviews.size();

            ImportCasePreview casePreview = caseBuilder
                    .steps(stepPreviews)
                    .unmappedCount(unmappedCount)
                    .errorCount(errorCount)
                    .build();

            cases.add(casePreview);
        }

        return ImportPreviewResponse.builder()
                .fileName(fileName)
                .fileType("FEATURE")
                .cases(cases)
                .hasErrors(hasAnyErrors)
                .canSave(!hasAnyErrors)
                .globalErrors(globalErrors)
                .totalCases(cases.size())
                .totalSteps(totalStepCount)
                .build();
    }

    private ImportPreviewResponse previewKeywordJson(String fileName,
                                                     byte[] content,
                                                     Projects project,
                                                     List<TestCase> existingCases,
                                                     Map<String, AutomationTestDefinition.LocatorDefinition> locatorsMap,
                                                     Map<String, String> secretValues) {
        List<String> globalErrors = new ArrayList<>();
        List<ImportCasePreview> cases = new ArrayList<>();

        JsonNode root;
        try {
            root = mapper.readTree(content);
        } catch (Exception e) {
            return ImportPreviewResponse.builder()
                    .fileName(fileName)
                    .fileType("KEYWORD_JSON")
                    .hasErrors(true)
                    .canSave(false)
                    .globalErrors(List.of("Invalid JSON syntax: " + e.getMessage()))
                    .build();
        }

        List<JsonNode> caseNodes = new ArrayList<>();
        if (root.isArray()) {
            root.forEach(caseNodes::add);
        } else if (root.has("cases") && root.get("cases").isArray()) {
            root.get("cases").forEach(caseNodes::add);
        } else if (root.isObject()) {
            caseNodes.add(root);
        } else {
            return ImportPreviewResponse.builder()
                    .fileName(fileName)
                    .fileType("KEYWORD_JSON")
                    .hasErrors(true)
                    .canSave(false)
                    .globalErrors(List.of("Expected JSON object or array of test cases"))
                    .build();
        }

        if (caseNodes.isEmpty()) {
            return ImportPreviewResponse.builder()
                    .fileName(fileName)
                    .fileType("KEYWORD_JSON")
                    .hasErrors(true)
                    .canSave(false)
                    .globalErrors(List.of("No test cases found in JSON"))
                    .build();
        }

        if (caseNodes.size() > MAX_CASES_PER_IMPORT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Import limit exceeded: found " + caseNodes.size() + " cases, maximum allowed is " + MAX_CASES_PER_IMPORT);
        }

        boolean hasAnyErrors = false;
        int totalStepCount = 0;

        for (int i = 0; i < caseNodes.size(); i++) {
            JsonNode caseNode = caseNodes.get(i);
            String caseName = caseNode.hasNonNull("name") ? caseNode.get("name").asText().trim() : "Imported Keyword Case " + (i + 1);

            TestTypes testType = TestTypes.KEYWORD_DRIVEN;
            if (caseNode.hasNonNull("type")) {
                String typeStr = caseNode.get("type").asText();
                try {
                    testType = TestTypes.valueOf(typeStr);
                } catch (IllegalArgumentException ignored) { testType = null; }
            }

            Optional<TestCase> existingMatch = existingCases.stream()
                    .filter(tc -> tc.getTestName() != null && tc.getTestName().trim().equalsIgnoreCase(caseName))
                    .findFirst();

            ImportCasePreview.ImportCasePreviewBuilder caseBuilder = ImportCasePreview.builder()
                    .name(caseName)
                    .testType(testType)
                    .sourceLine(i + 1)
                    .conflict(existingMatch.isPresent())
                    .action(ImportAction.CREATE_NEW);

            if (existingMatch.isPresent()) {
                TestCase match = existingMatch.get();
                caseBuilder.existingCaseId(match.getId())
                        .existingCaseProjectId(match.getTestProjectId())
                        .existingCaseName(match.getTestName());
            }

            List<ImportStepPreview> stepPreviews = new ArrayList<>();
            JsonNode stepsNode = caseNode.get("steps");
            int unmappedCount = 0;
            int errorCount = 0;

            if (stepsNode != null && stepsNode.isArray()) {
                int order = 1;
                for (JsonNode stepNode : stepsNode) {
                    ImportStepPreview stepPreview = parseJsonStep(stepNode, order++, locatorsMap, secretValues);
                    if (!stepPreview.isMapped()) {
                        unmappedCount++;
                        errorCount++;
                        hasAnyErrors = true;
                    } else if (stepPreview.getValidationError() != null) {
                        errorCount++;
                        hasAnyErrors = true;
                    }
                    stepPreviews.add(stepPreview);
                }
            } else {
                errorCount++;
                hasAnyErrors = true;
            }

            totalStepCount += stepPreviews.size();

            ImportCasePreview casePreview = caseBuilder
                    .steps(stepPreviews)
                    .unmappedCount(unmappedCount)
                    .errorCount(errorCount)
                    .build();

            cases.add(casePreview);
        }

        return ImportPreviewResponse.builder()
                .fileName(fileName)
                .fileType("KEYWORD_JSON")
                .cases(cases)
                .hasErrors(hasAnyErrors)
                .canSave(!hasAnyErrors)
                .globalErrors(globalErrors)
                .totalCases(cases.size())
                .totalSteps(totalStepCount)
                .build();
    }

    private ImportStepPreview matchGherkinStep(String text, String keyword, int line, int order,
                                               Map<String, AutomationTestDefinition.LocatorDefinition> locators,
                                               Map<String, String> secrets) {
        try {
            return parseGherkinStep(text, keyword, line, order, locators, secrets);
        } catch (IllegalArgumentException error) {
            return ImportStepPreview.builder().order(order).sourceLine(line).keyword(keyword)
                    .rawText(redact(text, secrets)).mapped(false)
                    .validationError("Invalid quoted string escape. Escape backslashes and quotes before importing.").build();
        }
    }

    private ImportStepPreview parseGherkinStep(String stepText,
                                               String keyword,
                                               int line,
                                               int order,
                                               Map<String, AutomationTestDefinition.LocatorDefinition> locatorsMap,
                                               Map<String, String> secretValues) {
        String safeText = stepText == null ? "" : stepText.trim();
        Matcher m;

        // 1. OPEN_PAGE
        m = OPEN_PAGE_PATTERN.matcher(safeText);
        if (m.matches()) {
            String url = firstNonEmpty(m.group(1), m.group(2), m.group(3));
            AutomationStepModel model = AutomationStepModel.builder()
                    .order(order)
                    .keyword(keyword)
                    .template(AutomationTemplate.OPEN_PAGE)
                    .textValue(url)
                    .build();
            String valErr = validateStepModel(model, locatorsMap);
            return ImportStepPreview.builder()
                    .order(order)
                    .sourceLine(line)
                    .keyword(keyword)
                    .rawText(redact(safeText, secretValues))
                    .template(AutomationTemplate.OPEN_PAGE)
                    .textValue(referenceSecrets(url, secretValues))
                    .mapped(true)
                    .validationError(redact(valErr, secretValues))
                    .build();
        }

        // 2. ENTER_TEXT
        m = ENTER_TEXT_PATTERN.matcher(safeText);
        if (m.matches()) {
            String val = firstNonEmpty(m.group(1), m.group(2), m.group(3));
            String loc = firstNonEmpty(m.group(4), m.group(5), m.group(6));
            LocatorType locType = inferLocatorType(loc, locatorsMap);
            AutomationStepModel model = AutomationStepModel.builder()
                    .order(order)
                    .keyword(keyword)
                    .template(AutomationTemplate.ENTER_TEXT)
                    .locatorType(locType)
                    .locatorValue(loc)
                    .textValue(val)
                    .build();
            String valErr = validateStepModel(model, locatorsMap);
            return ImportStepPreview.builder()
                    .order(order)
                    .sourceLine(line)
                    .keyword(keyword)
                    .rawText(redact(safeText, secretValues))
                    .template(AutomationTemplate.ENTER_TEXT)
                    .locatorType(locType)
                    .locatorValue(referenceSecrets(loc, secretValues))
                    .textValue(referenceSecrets(val, secretValues))
                    .mapped(true)
                    .validationError(redact(valErr, secretValues))
                    .build();
        }

        // 3. CLICK_ELEMENT
        m = CLICK_ELEMENT_PATTERN.matcher(safeText);
        if (m.matches()) {
            String loc = firstNonEmpty(m.group(1), m.group(2), m.group(3));
            LocatorType locType = inferLocatorType(loc, locatorsMap);
            AutomationStepModel model = AutomationStepModel.builder()
                    .order(order)
                    .keyword(keyword)
                    .template(AutomationTemplate.CLICK_ELEMENT)
                    .locatorType(locType)
                    .locatorValue(loc)
                    .build();
            String valErr = validateStepModel(model, locatorsMap);
            return ImportStepPreview.builder()
                    .order(order)
                    .sourceLine(line)
                    .keyword(keyword)
                    .rawText(redact(safeText, secretValues))
                    .template(AutomationTemplate.CLICK_ELEMENT)
                    .locatorType(locType)
                    .locatorValue(referenceSecrets(loc, secretValues))
                    .mapped(true)
                    .validationError(redact(valErr, secretValues))
                    .build();
        }

        // 4. VERIFY_TEXT_EQUALS
        m = VERIFY_TEXT_EQUALS_PATTERN.matcher(safeText);
        if (m.matches()) {
            String loc = firstNonEmpty(m.group(1), m.group(2), m.group(3));
            String val = firstNonEmpty(m.group(4), m.group(5), m.group(6));
            LocatorType locType = inferLocatorType(loc, locatorsMap);
            AutomationStepModel model = AutomationStepModel.builder()
                    .order(order)
                    .keyword(keyword)
                    .template(AutomationTemplate.VERIFY_TEXT_EQUALS)
                    .locatorType(locType)
                    .locatorValue(loc)
                    .textValue(val)
                    .build();
            String valErr = validateStepModel(model, locatorsMap);
            return ImportStepPreview.builder()
                    .order(order)
                    .sourceLine(line)
                    .keyword(keyword)
                    .rawText(redact(safeText, secretValues))
                    .template(AutomationTemplate.VERIFY_TEXT_EQUALS)
                    .locatorType(locType)
                    .locatorValue(referenceSecrets(loc, secretValues))
                    .textValue(referenceSecrets(val, secretValues))
                    .mapped(true)
                    .validationError(redact(valErr, secretValues))
                    .build();
        }

        // 5. VERIFY_TEXT_CONTAINS
        m = VERIFY_TEXT_CONTAINS_PATTERN.matcher(safeText);
        if (m.matches()) {
            String loc = firstNonEmpty(m.group(1), m.group(2), m.group(3));
            String val = firstNonEmpty(m.group(4), m.group(5), m.group(6));
            LocatorType locType = inferLocatorType(loc, locatorsMap);
            AutomationStepModel model = AutomationStepModel.builder()
                    .order(order)
                    .keyword(keyword)
                    .template(AutomationTemplate.VERIFY_TEXT_CONTAINS)
                    .locatorType(locType)
                    .locatorValue(loc)
                    .textValue(val)
                    .build();
            String valErr = validateStepModel(model, locatorsMap);
            return ImportStepPreview.builder()
                    .order(order)
                    .sourceLine(line)
                    .keyword(keyword)
                    .rawText(redact(safeText, secretValues))
                    .template(AutomationTemplate.VERIFY_TEXT_CONTAINS)
                    .locatorType(locType)
                    .locatorValue(referenceSecrets(loc, secretValues))
                    .textValue(referenceSecrets(val, secretValues))
                    .mapped(true)
                    .validationError(redact(valErr, secretValues))
                    .build();
        }

        // 6. VERIFY_ELEMENT_STATE
        m = VERIFY_ELEMENT_STATE_PATTERN.matcher(safeText);
        if (m.matches()) {
            String loc = firstNonEmpty(m.group(1), m.group(2), m.group(3));
            String stateStr = m.group(4);
            ElementState state = "hidden".equalsIgnoreCase(stateStr) ? ElementState.HIDDEN : ElementState.VISIBLE;
            LocatorType locType = inferLocatorType(loc, locatorsMap);
            AutomationStepModel model = AutomationStepModel.builder()
                    .order(order)
                    .keyword(keyword)
                    .template(AutomationTemplate.VERIFY_ELEMENT_STATE)
                    .locatorType(locType)
                    .locatorValue(loc)
                    .stateValue(state)
                    .build();
            String valErr = validateStepModel(model, locatorsMap);
            return ImportStepPreview.builder()
                    .order(order)
                    .sourceLine(line)
                    .keyword(keyword)
                    .rawText(redact(safeText, secretValues))
                    .template(AutomationTemplate.VERIFY_ELEMENT_STATE)
                    .locatorType(locType)
                    .locatorValue(referenceSecrets(loc, secretValues))
                    .stateValue(state)
                    .mapped(true)
                    .validationError(redact(valErr, secretValues))
                    .build();
        }

        // 7. SELECT_OPTION
        m = SELECT_OPTION_PATTERN.matcher(safeText);
        if (m.matches()) {
            String opt = firstNonEmpty(m.group(1), m.group(2), m.group(3));
            String byStr = m.group(4);
            OptionSelectBy selectBy = "value".equalsIgnoreCase(byStr) ? OptionSelectBy.VALUE : OptionSelectBy.LABEL;
            String loc = firstNonEmpty(m.group(5), m.group(6), m.group(7));
            LocatorType locType = inferLocatorType(loc, locatorsMap);
            AutomationStepModel model = AutomationStepModel.builder()
                    .order(order)
                    .keyword(keyword)
                    .template(AutomationTemplate.SELECT_OPTION)
                    .locatorType(locType)
                    .locatorValue(loc)
                    .textValue(opt)
                    .selectBy(selectBy)
                    .build();
            String valErr = validateStepModel(model, locatorsMap);
            return ImportStepPreview.builder()
                    .order(order)
                    .sourceLine(line)
                    .keyword(keyword)
                    .rawText(redact(safeText, secretValues))
                    .template(AutomationTemplate.SELECT_OPTION)
                    .locatorType(locType)
                    .locatorValue(referenceSecrets(loc, secretValues))
                    .textValue(referenceSecrets(opt, secretValues))
                    .selectBy(selectBy)
                    .mapped(true)
                    .validationError(redact(valErr, secretValues))
                    .build();
        }

        // 8. SET_CHECKBOX
        m = SET_CHECKBOX_PATTERN.matcher(safeText);
        if (m.matches()) {
            String loc = firstNonEmpty(m.group(1), m.group(2), m.group(3));
            String stateStr = firstNonEmpty(m.group(4), m.group(5), m.group(6));
            boolean checked = "checked".equalsIgnoreCase(stateStr);
            LocatorType locType = inferLocatorType(loc, locatorsMap);
            AutomationStepModel model = AutomationStepModel.builder()
                    .order(order)
                    .keyword(keyword)
                    .template(AutomationTemplate.SET_CHECKBOX)
                    .locatorType(locType)
                    .locatorValue(loc)
                    .checkedValue(checked)
                    .build();
            String valErr = validateStepModel(model, locatorsMap);
            return ImportStepPreview.builder()
                    .order(order)
                    .sourceLine(line)
                    .keyword(keyword)
                    .rawText(redact(safeText, secretValues))
                    .template(AutomationTemplate.SET_CHECKBOX)
                    .locatorType(locType)
                    .locatorValue(referenceSecrets(loc, secretValues))
                    .checkedValue(checked)
                    .mapped(true)
                    .validationError(redact(valErr, secretValues))
                    .build();
        }

        // 9. PRESS_KEY
        m = PRESS_KEY_PATTERN.matcher(safeText);
        if (m.matches()) {
            String key = firstNonEmpty(m.group(1), m.group(2), m.group(3));
            String loc = firstNonEmpty(m.group(4), m.group(5), m.group(6));
            LocatorType locType = inferLocatorType(loc, locatorsMap);
            AutomationStepModel model = AutomationStepModel.builder()
                    .order(order)
                    .keyword(keyword)
                    .template(AutomationTemplate.PRESS_KEY)
                    .locatorType(locType)
                    .locatorValue(loc)
                    .keyValue(key)
                    .build();
            String valErr = validateStepModel(model, locatorsMap);
            return ImportStepPreview.builder()
                    .order(order)
                    .sourceLine(line)
                    .keyword(keyword)
                    .rawText(redact(safeText, secretValues))
                    .template(AutomationTemplate.PRESS_KEY)
                    .locatorType(locType)
                    .locatorValue(referenceSecrets(loc, secretValues))
                    .keyValue(referenceSecrets(key, secretValues))
                    .mapped(true)
                    .validationError(redact(valErr, secretValues))
                    .build();
        }

        // 10. VERIFY_URL
        m = VERIFY_URL_PATTERN.matcher(safeText);
        if (m.matches()) {
            String compStr = m.group(1);
            UrlComparison comp = "contains".equalsIgnoreCase(compStr) ? UrlComparison.CONTAINS : UrlComparison.EQUALS;
            String expUrl = firstNonEmpty(m.group(2), m.group(3), m.group(4));
            AutomationStepModel model = AutomationStepModel.builder()
                    .order(order)
                    .keyword(keyword)
                    .template(AutomationTemplate.VERIFY_URL)
                    .textValue(expUrl)
                    .urlComparison(comp)
                    .build();
            String valErr = validateStepModel(model, locatorsMap);
            return ImportStepPreview.builder()
                    .order(order)
                    .sourceLine(line)
                    .keyword(keyword)
                    .rawText(redact(safeText, secretValues))
                    .template(AutomationTemplate.VERIFY_URL)
                    .textValue(referenceSecrets(expUrl, secretValues))
                    .urlComparison(comp)
                    .mapped(true)
                    .validationError(redact(valErr, secretValues))
                    .build();
        }

        // 11. VERIFY_ATTRIBUTE
        m = VERIFY_ATTRIBUTE_PATTERN.matcher(safeText);
        if (m.matches()) {
            String attr = firstNonEmpty(m.group(1), m.group(2), m.group(3));
            String loc = firstNonEmpty(m.group(4), m.group(5), m.group(6));
            String val = firstNonEmpty(m.group(7), m.group(8), m.group(9));
            if (val == null) val = "";
            LocatorType locType = inferLocatorType(loc, locatorsMap);
            AutomationStepModel model = AutomationStepModel.builder()
                    .order(order)
                    .keyword(keyword)
                    .template(AutomationTemplate.VERIFY_ATTRIBUTE)
                    .locatorType(locType)
                    .locatorValue(loc)
                    .attributeName(attr)
                    .textValue(val)
                    .build();
            String valErr = validateStepModel(model, locatorsMap);
            return ImportStepPreview.builder()
                    .order(order)
                    .sourceLine(line)
                    .keyword(keyword)
                    .rawText(redact(safeText, secretValues))
                    .template(AutomationTemplate.VERIFY_ATTRIBUTE)
                    .locatorType(locType)
                    .locatorValue(referenceSecrets(loc, secretValues))
                    .attributeName(referenceSecrets(attr, secretValues))
                    .textValue(referenceSecrets(val, secretValues))
                    .mapped(true)
                    .validationError(redact(valErr, secretValues))
                    .build();
        }

        // Unrecognized / unmapped step
        return ImportStepPreview.builder()
                .order(order)
                .sourceLine(line)
                .keyword(keyword)
                .rawText(redact(safeText, secretValues))
                .mapped(false)
                .template(null)
                .validationError("Unrecognized step. Map to a template and provide required values.")
                .build();
    }

    private ImportStepPreview parseJsonStep(JsonNode stepNode,
                                            int order,
                                            Map<String, AutomationTestDefinition.LocatorDefinition> locatorsMap,
                                            Map<String, String> secretValues) {
        String keyword = stepNode.hasNonNull("keyword") ? stepNode.get("keyword").asText() : null;
        String rawText = stepNode.hasNonNull("description") ? stepNode.get("description").asText() :
                stepNode.hasNonNull("rawText") ? stepNode.get("rawText").asText() : "";

        AutomationTemplate template = null;
        if (stepNode.hasNonNull("template")) {
            try {
                template = AutomationTemplate.valueOf(stepNode.get("template").asText());
            } catch (IllegalArgumentException ignored) {}
        }

        String locVal = stepNode.hasNonNull("locatorValue") ? stepNode.get("locatorValue").asText() : null;
        String txtVal = stepNode.hasNonNull("textValue") ? stepNode.get("textValue").asText() : null;
        LocatorType locType = null;
        if (stepNode.hasNonNull("locatorType")) {
            try {
                locType = LocatorType.valueOf(stepNode.get("locatorType").asText());
            } catch (IllegalArgumentException ignored) {}
        } else if (locVal != null) {
            locType = inferLocatorType(locVal, locatorsMap);
        }

        ElementState state = null;
        if (stepNode.hasNonNull("stateValue")) {
            try {
                state = ElementState.valueOf(stepNode.get("stateValue").asText().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        OptionSelectBy selectBy = null;
        if (stepNode.hasNonNull("selectBy")) {
            try {
                selectBy = OptionSelectBy.valueOf(stepNode.get("selectBy").asText().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Step " + order + ": selectBy must be LABEL or VALUE");
            }
        }

        Boolean checkedValue = null;
        if (stepNode.hasNonNull("checkedValue")) {
            if (!stepNode.get("checkedValue").isBoolean())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Step " + order + ": checkedValue must be a JSON boolean");
            checkedValue = stepNode.get("checkedValue").asBoolean();
        }

        String keyValue = stepNode.hasNonNull("keyValue") ? stepNode.get("keyValue").asText() : null;

        UrlComparison urlComparison = null;
        if (stepNode.hasNonNull("urlComparison")) {
            try {
                urlComparison = UrlComparison.valueOf(stepNode.get("urlComparison").asText().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Step " + order + ": urlComparison must be EQUALS or CONTAINS");
            }
        }

        String attributeName = stepNode.hasNonNull("attributeName") ? stepNode.get("attributeName").asText() : null;

        if (template != null) {
            AutomationStepModel model = AutomationStepModel.builder()
                    .order(order)
                    .keyword(keyword)
                    .template(template)
                    .locatorType(locType)
                    .locatorValue(locVal)
                    .textValue(txtVal)
                    .stateValue(state)
                    .selectBy(selectBy)
                    .checkedValue(checkedValue)
                    .keyValue(keyValue)
                    .urlComparison(urlComparison)
                    .attributeName(attributeName)
                    .build();
            String valErr = validateStepModel(model, locatorsMap);
            return ImportStepPreview.builder()
                    .order(order)
                    .sourceLine(order)
                    .keyword(keyword)
                    .rawText(redact(rawText.isBlank() ? serializer.toKeywordDescription(model) : rawText, secretValues))
                    .template(template)
                    .locatorType(locType)
                    .locatorValue(referenceSecrets(locVal, secretValues))
                    .textValue(referenceSecrets(txtVal, secretValues))
                    .stateValue(state)
                    .selectBy(selectBy)
                    .checkedValue(checkedValue)
                    .keyValue(referenceSecrets(keyValue, secretValues))
                    .urlComparison(urlComparison)
                    .attributeName(referenceSecrets(attributeName, secretValues))
                    .mapped(true)
                    .validationError(redact(valErr, secretValues))
                    .build();
        }

        // Try regex match on rawText if template wasn't given directly
        if (!rawText.isBlank()) {
            return matchGherkinStep(rawText, keyword == null ? "When" : keyword, order, order, locatorsMap, secretValues);
        }

        return ImportStepPreview.builder()
                .order(order)
                .sourceLine(order)
                .keyword(keyword)
                .rawText(redact(rawText, secretValues))
                .mapped(false)
                .validationError("Missing step template")
                .build();
    }

    private String validateStepModel(AutomationStepModel model, Map<String, AutomationTestDefinition.LocatorDefinition> locatorsMap) {
        var res = validator.validate(List.of(model), locatorsMap);
        return res.isValid() ? null : res.getJoinedMessage();
    }

    private LocatorType inferLocatorType(String locator, Map<String, AutomationTestDefinition.LocatorDefinition> locatorsMap) {
        if (locator == null || locator.isBlank()) return null;
        String trimmed = locator.trim();
        LocatorType prefixed = LocatorType.prefixedType(trimmed);
        if (prefixed != null) return prefixed;
        if (locatorsMap != null && locatorsMap.containsKey(trimmed)) return LocatorType.NAMED;
        if (trimmed.matches("^[A-Za-z0-9_]+\\.[A-Za-z0-9_]+$")) return LocatorType.NAMED;
        if (trimmed.startsWith("//") || trimmed.startsWith("./") || trimmed.startsWith("(")) return LocatorType.XPATH;
        if (trimmed.startsWith("#") || trimmed.startsWith(".") || trimmed.startsWith("[")) return LocatorType.CSS;
        return null;
    }

    private String firstNonEmpty(String... vals) {
        if (vals == null) return null;
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] != null) return i < 2 ? GherkinStrings.decode(vals[i]) : vals[i];
        }
        return null;
    }

    private String replaceSecrets(String text, Map<String, String> secrets, boolean references) {
        if (text == null || secrets.isEmpty()) return text;
        // One pass, longest literals first. Existing variable references must remain intact.
        String literals = secrets.keySet().stream().map(Pattern::quote).reduce((a, b) -> a + "|" + b).orElseThrow();
        Matcher matches = Pattern.compile("(\\$\\{[a-zA-Z0-9_.-]+\\})|(?:" + literals + ")").matcher(text);
        StringBuilder result = new StringBuilder();
        while (matches.find()) {
            String replacement = matches.group(1) != null ? matches.group() : references ? secrets.get(matches.group()) : "[REDACTED]";
            matches.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matches.appendTail(result);
        return result.toString();
    }

    private String redact(String text, Map<String, String> secrets) { return GherkinStrings.redact(text, secrets.keySet()); }
    private String referenceSecrets(String text, Map<String, String> secrets) { return replaceSecrets(text, secrets, true); }

    private Map<String, String> getProjectSecrets(int projectId) {
        Map<String, String> references = new LinkedHashMap<>();
        variableRepository.findByProjectIdOrderByVariableKeyAsc(projectId).stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsSecret()) && v.getVariableValue() != null && !v.getVariableValue().isBlank())
                .sorted(Comparator.comparingInt((ProjectVariable v) -> v.getVariableValue().length()).reversed())
                .forEach(v -> {
                    if (v.getVariableKey() == null || !v.getVariableKey().matches("[a-zA-Z0-9_.-]+"))
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rename secret variable #" + v.getId() + " using letters, numbers, dots, underscores or hyphens before importing");
                    references.putIfAbsent(v.getVariableValue(), "${" + v.getVariableKey() + "}");
                });
        return references;
    }

    private String caseValidationError(String name, TestTypes type, int stepCount) {
        if (name == null || name.isBlank()) return "Name cannot be empty";
        if (name.length() > 255) return "Keep case names within 255 characters";
        if (type != TestTypes.CUCUMBER_AUTOMATION && type != TestTypes.KEYWORD_DRIVEN)
            return "Only CUCUMBER_AUTOMATION and KEYWORD_DRIVEN test types are supported";
        if (stepCount == 0) return "Must contain at least one step";
        return null;
    }

    private boolean hasMaskedValue(String value) {
        return value != null && value.contains("[REDACTED]");
    }

    private ImportPreviewResponse finishPreview(ImportPreviewResponse preview, Map<String, String> secrets) {
        preview.setFileName(redact(preview.getFileName(), secrets));
        preview.setGlobalErrors(preview.getGlobalErrors().stream().map(error -> redact(error, secrets)).toList());
        for (ImportCasePreview item : preview.getCases()) {
            item.setName(redact(item.getName(), secrets));
            item.setExistingCaseName(redact(item.getExistingCaseName(), secrets));
            item.setExistingCaseProjectId(redact(item.getExistingCaseProjectId(), secrets));
            item.setUnsupportedSyntax(redact(item.getUnsupportedSyntax(), secrets));
            item.setValidationError(caseValidationError(item.getName(), item.getTestType(), item.getSteps().size()));
            int errors = (item.getUnsupportedSyntax() == null ? 0 : 1) + (item.getValidationError() == null ? 0 : 1);
            int unmapped = 0;
            for (ImportStepPreview step : item.getSteps()) {
                step.setKeyword(redact(step.getKeyword(), secrets));
                step.setRawText(redact(step.getRawText(), secrets));
                step.setUnsupportedSyntax(redact(step.getUnsupportedSyntax(), secrets));
                step.setValidationError(redact(step.getValidationError(), secrets));
                if (hasMaskedValue(step.getTextValue()) || hasMaskedValue(step.getLocatorValue()) || hasMaskedValue(step.getKeyValue()) || hasMaskedValue(step.getAttributeName()))
                    step.setValidationError("Replace masked values with a project variable reference such as ${PASSWORD}");
                if (!step.isMapped()) unmapped++;
                if (!step.isMapped() || step.getValidationError() != null) errors++;
                if (step.getUnsupportedSyntax() != null) errors++;
            }
            item.setUnmappedCount(unmapped);
            item.setErrorCount(errors);
        }
        boolean errors = !preview.getGlobalErrors().isEmpty() || preview.getCases().isEmpty()
                || preview.getCases().stream().anyMatch(item -> item.getErrorCount() > 0);
        preview.setHasErrors(errors);
        preview.setCanSave(!errors);
        return preview;
    }

    private Map<String, AutomationTestDefinition.LocatorDefinition> getProjectLocators(int projectId) {
        Map<String, AutomationTestDefinition.LocatorDefinition> map = new HashMap<>();
        locatorRepository.findByProjectIdOrderByLocatorNameAsc(projectId).forEach(loc ->
                map.put(loc.getLocatorName(), AutomationTestDefinition.LocatorDefinition.builder()
                        .type(loc.getLocatorType())
                        .value(loc.getLocatorValue())
                        .description(loc.getDescription())
                        .build())
        );
        return map;
    }

    private String getCurrentUserFullName() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            Users u = userRepository.findByUserName(auth.getName());
            if (u != null) {
                String first = u.getFirstName() == null ? "" : u.getFirstName();
                String last = u.getLastName() == null ? "" : u.getLastName();
                String name = (first + " " + last).trim();
                if (!name.isEmpty()) return name;
            }
            return auth.getName();
        }
        return "Automation Import";
    }

    @Transactional(rollbackFor = Exception.class)
    public ImportSaveResponse saveImport(int projectId, ImportSaveRequest request) {
        Projects project = projectRepository.findById(projectId);
        if (project == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project #" + projectId + " not found");
        }

        if (request == null || request.getCases() == null || request.getCases().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No test cases provided for import save");
        }

        if (request.getCases().size() > MAX_CASES_PER_IMPORT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Import limit exceeded: maximum allowed is " + MAX_CASES_PER_IMPORT + " cases per save");
        }

        Map<String, AutomationTestDefinition.LocatorDefinition> locatorsMap = getProjectLocators(projectId);
        Map<String, String> secretValues = getProjectSecrets(projectId);
        String author = getCurrentUserFullName();
        Timestamp now = Util.parseTimestamp(Util.DATE_TIME_FORMAT.format(new Date()));

        // Pre-validate ALL cases and steps. If ANY step is invalid, the entire operation fails atomically!
        for (int i = 0; i < request.getCases().size(); i++) {
            ImportCaseSaveItem item = request.getCases().get(i);
            if (item == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing case at position " + (i + 1));
            String caseName = redact(item.getCaseName() == null ? "" : item.getCaseName().trim(), secretValues);
            item.setCaseName(caseName);
            String caseError = caseValidationError(caseName, item.getTestType(), item.getSteps() == null ? 0 : item.getSteps().size());
            if (caseError != null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Case " + (i + 1) + ": " + caseError);
            for (AutomationStepModel step : item.getSteps()) {
                if (step == null || hasMaskedValue(step.getTextValue()) || hasMaskedValue(step.getLocatorValue())
                        || hasMaskedValue(step.getKeyValue()) || hasMaskedValue(step.getAttributeName()))
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Case " + (i + 1) + ": Replace missing or masked steps with complete values or project variable references");
            }

            var validation = validator.validate(item.getSteps(), locatorsMap);
            if (!validation.isValid()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        redact("Case '" + caseName + "' step validation error: " + validation.getJoinedMessage(), secretValues));
            }

            if (item.getAction() == ImportAction.REPLACE_EXISTING) {
                if (item.getTargetCaseId() == null || item.getTargetCaseId() <= 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Case " + (i + 1) + ": Target case ID is required for replacement");
                }
                TestCase target = testCaseRepository.findById(item.getTargetCaseId().intValue());
                if (target == null || target.getProjects() == null || !target.getProjects().getId().equals(projectId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Case " + (i + 1) + ": Target case #" + item.getTargetCaseId() + " not found in project");
                }
            }
        }

        int createdCount = 0;
        int replacedCount = 0;
        List<ImportSaveResponse.SavedCaseSummary> savedSummaries = new ArrayList<>();

        for (ImportCaseSaveItem item : request.getCases()) {
            item.setCaseName(redact(item.getCaseName(), secretValues));
            for (AutomationStepModel step : item.getSteps()) {
                step.setTextValue(referenceSecrets(step.getTextValue(), secretValues));
                step.setLocatorValue(referenceSecrets(step.getLocatorValue(), secretValues));
                step.setKeyValue(referenceSecrets(step.getKeyValue(), secretValues));
                step.setAttributeName(referenceSecrets(step.getAttributeName(), secretValues));
            }
            TestCase savedCase;
            String actionDesc;

            if (item.getAction() == ImportAction.REPLACE_EXISTING) {
                TestCase target = testCaseRepository.findById(item.getTargetCaseId().intValue());
                target.setTestName(item.getCaseName().trim());
                target.setTestType(item.getTestType());
                target.setTestModifiedBy(author);
                target.setTestModifiedDate(now);
                testCaseRepository.save(target);

                // Remove old steps for this case of this type
                if (target.getTestStepsSet() != null) {
                    target.getTestStepsSet().removeIf(s -> s.getTestType() == item.getTestType());
                }
                List<TestSteps> oldSteps = testStepsRepository.findTestStepsByTestCaseAndTestTypeOrderByTestStepOrderAsc(target, item.getTestType());
                for (TestSteps oldStep : oldSteps) {
                    testStepsRepository.deleteById(oldStep.getId());
                }

                // Insert new steps
                saveStepsForCase(target, item.getTestType(), item.getSteps(), author, now);

                savedCase = target;
                replacedCount++;
                actionDesc = "REPLACED";
            } else {
                // CREATE_NEW
                TestCase newCase = new TestCase();
                newCase.setProjects(project);
                newCase.setTestName(item.getCaseName().trim());
                newCase.setTestType(item.getTestType());
                newCase.setTestProjectId(project.getProjectInitials() + "-" + (project.getIssueCounter() + 1));
                project.setIssueCounter(project.getIssueCounter() + 1);
                projectRepository.save(project);

                newCase.setTestCreatedBy(author);
                newCase.setTestCreatedDate(now);
                newCase.setTestModifiedBy(author);
                newCase.setTestModifiedDate(now);
                testCaseRepository.save(newCase);

                // Insert steps
                saveStepsForCase(newCase, item.getTestType(), item.getSteps(), author, now);

                savedCase = newCase;
                createdCount++;
                actionDesc = "CREATED";
            }

            savedSummaries.add(ImportSaveResponse.SavedCaseSummary.builder()
                    .id(savedCase.getId())
                    .testProjectId(savedCase.getTestProjectId())
                    .name(savedCase.getTestName())
                    .testType(savedCase.getTestType().name())
                    .action(actionDesc)
                    .build());
        }

        return ImportSaveResponse.builder()
                .createdCount(createdCount)
                .replacedCount(replacedCount)
                .savedCases(savedSummaries)
                .build();
    }

    private void saveStepsForCase(TestCase testCase,
                                  TestTypes testType,
                                  List<AutomationStepModel> steps,
                                  String author,
                                  Timestamp now) {
        for (int i = 0; i < steps.size(); i++) {
            AutomationStepModel stepModel = steps.get(i);
            stepModel.setOrder(i + 1);

            String desc = testType == TestTypes.CUCUMBER_AUTOMATION
                    ? serializer.toGherkinStep(stepModel)
                    : serializer.toKeywordDescription(stepModel);

            TestSteps stepEntity = new TestSteps();
            stepEntity.setTestCase(testCase);
            stepEntity.setTestType(testType);
            stepEntity.setTestStepOrder(i + 1);
            stepEntity.setTestStepDesc(desc);
            stepEntity.setTestStepData(serializer.serializeStep(stepModel));
            stepEntity.setTestExpectedOutput("");
            stepEntity.setTsCreatedBy(author);
            stepEntity.setTsCreatedDate(now);
            stepEntity.setTsModifiedBy(author);
            stepEntity.setTsModifiedDate(now);
            testStepsRepository.save(stepEntity);
            if (testCase.getTestStepsSet() != null) {
                testCase.getTestStepsSet().add(stepEntity);
            }
        }
    }
}
