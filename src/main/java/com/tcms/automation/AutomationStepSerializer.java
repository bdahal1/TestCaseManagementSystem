package com.tcms.automation;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class AutomationStepSerializer {
    private final ObjectMapper mapper;

    public AutomationStepSerializer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String serialize(List<AutomationStepModel> steps) {
        if (steps == null) return "[]";
        try {
            return mapper.writeValueAsString(steps);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Unable to serialize automation steps", e);
        }
    }

    public String serializeStep(AutomationStepModel step) {
        if (step == null) return "{}";
        try {
            return mapper.writeValueAsString(step);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Unable to serialize automation step", e);
        }
    }

    public List<AutomationStepModel> deserialize(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return mapper.readValue(json, new TypeReference<List<AutomationStepModel>>() {});
        } catch (JacksonException e) {
            return Collections.emptyList();
        }
    }

    public AutomationStepModel deserializeStep(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return mapper.readValue(json, AutomationStepModel.class);
        } catch (JacksonException e) {
            return null;
        }
    }

    public String toGherkinStep(AutomationStepModel step) {
        if (step == null || step.getTemplate() == null) return "";
        String keyword = (step.getKeyword() == null || step.getKeyword().isBlank()) ? "When" : step.getKeyword().trim();
        String loc = locatorReference(step);
        if (loc == null) loc = "";
        String val = step.getTextValue() == null ? "" : step.getTextValue();
        String state = step.getStateValue() == null ? "visible" : step.getStateValue().name().toLowerCase();
        String key = step.getKeyValue() == null ? "" : step.getKeyValue();
        String attr = step.getAttributeName() == null ? "" : step.getAttributeName();

        return switch (step.getTemplate()) {
            case OPEN_PAGE -> keyword + " open page \"" + escapeGherkin(val) + "\"";
            case ENTER_TEXT -> keyword + " enter text \"" + escapeGherkin(val) + "\" into \"" + escapeGherkin(loc) + "\"";
            case CLICK_ELEMENT -> keyword + " click element \"" + escapeGherkin(loc) + "\"";
            case VERIFY_TEXT_EQUALS -> keyword + " verify text of \"" + escapeGherkin(loc) + "\" equals \"" + escapeGherkin(val) + "\"";
            case VERIFY_TEXT_CONTAINS -> keyword + " verify text of \"" + escapeGherkin(loc) + "\" contains \"" + escapeGherkin(val) + "\"";
            case VERIFY_ELEMENT_STATE -> keyword + " verify element \"" + escapeGherkin(loc) + "\" is " + state;
            case SELECT_OPTION -> keyword + " select option \"" + escapeGherkin(val) + "\" by " + (step.getSelectBy() == OptionSelectBy.VALUE ? "value" : "label") + " in \"" + escapeGherkin(loc) + "\"";
            case SET_CHECKBOX -> keyword + " set checkbox \"" + escapeGherkin(loc) + "\" to " + (Boolean.TRUE.equals(step.getCheckedValue()) ? "checked" : "unchecked");
            case PRESS_KEY -> keyword + " press key \"" + escapeGherkin(key) + "\" on \"" + escapeGherkin(loc) + "\"";
            case VERIFY_URL -> keyword + " verify url " + (step.getUrlComparison() == UrlComparison.CONTAINS ? "contains" : "equals") + " \"" + escapeGherkin(val) + "\"";
            case VERIFY_ATTRIBUTE -> keyword + " verify attribute \"" + escapeGherkin(attr) + "\" of \"" + escapeGherkin(loc) + "\" equals \"" + escapeGherkin(val) + "\"";
        };
    }

    public String toKeywordDescription(AutomationStepModel step) {
        if (step == null || step.getTemplate() == null) return "";
        String loc = locatorReference(step);
        if (loc == null) loc = "";
        String val = step.getTextValue() == null ? "" : step.getTextValue();
        String state = step.getStateValue() == null ? "visible" : step.getStateValue().name().toLowerCase();
        String key = step.getKeyValue() == null ? "" : step.getKeyValue();
        String attr = step.getAttributeName() == null ? "" : step.getAttributeName();

        return switch (step.getTemplate()) {
            case OPEN_PAGE -> "Open page \"" + val + "\"";
            case ENTER_TEXT -> "Enter text \"" + val + "\" into \"" + loc + "\"";
            case CLICK_ELEMENT -> "Click element \"" + loc + "\"";
            case VERIFY_TEXT_EQUALS -> "Verify text of \"" + loc + "\" equals \"" + val + "\"";
            case VERIFY_TEXT_CONTAINS -> "Verify text of \"" + loc + "\" contains \"" + val + "\"";
            case VERIFY_ELEMENT_STATE -> "Verify element \"" + loc + "\" is " + state;
            case SELECT_OPTION -> "Select option \"" + val + "\" by " + (step.getSelectBy() == OptionSelectBy.VALUE ? "value" : "label") + " in \"" + loc + "\"";
            case SET_CHECKBOX -> "Set checkbox \"" + loc + "\" to " + (Boolean.TRUE.equals(step.getCheckedValue()) ? "checked" : "unchecked");
            case PRESS_KEY -> "Press key \"" + key + "\" on \"" + loc + "\"";
            case VERIFY_URL -> "Verify url " + (step.getUrlComparison() == UrlComparison.CONTAINS ? "contains" : "equals") + " \"" + val + "\"";
            case VERIFY_ATTRIBUTE -> "Verify attribute \"" + attr + "\" of \"" + loc + "\" equals \"" + val + "\"";
        };
    }

    private String escapeGherkin(String input) { return GherkinStrings.escape(input); }

    public String locatorReference(AutomationStepModel step) {
        return LocatorType.reference(step.getLocatorType(), step.getLocatorValue());
    }

    public String toGherkinScenario(String scenarioName, List<AutomationStepModel> steps) {
        StringBuilder sb = new StringBuilder();
        String name = (scenarioName == null || scenarioName.isBlank()) ? "Automated Scenario" : scenarioName.replaceAll("[\\r\\n]", " ");
        sb.append("Feature: ").append(name).append("\n\n");
        sb.append("  Scenario: ").append(name).append("\n");
        if (steps != null) {
            for (AutomationStepModel step : steps) {
                sb.append("    ").append(toGherkinStep(step)).append("\n");
            }
        }
        return sb.toString();
    }
}
