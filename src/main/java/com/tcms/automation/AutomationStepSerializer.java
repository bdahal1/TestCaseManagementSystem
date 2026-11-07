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
        String loc = step.getLocatorValue() == null ? "" : step.getLocatorValue();
        String val = step.getTextValue() == null ? "" : step.getTextValue();
        String state = step.getStateValue() == null ? "visible" : step.getStateValue().name().toLowerCase();

        return switch (step.getTemplate()) {
            case OPEN_PAGE -> keyword + " open page \"" + val + "\"";
            case ENTER_TEXT -> keyword + " enter text \"" + val + "\" into \"" + loc + "\"";
            case CLICK_ELEMENT -> keyword + " click element \"" + loc + "\"";
            case VERIFY_TEXT_EQUALS -> keyword + " verify text of \"" + loc + "\" equals \"" + val + "\"";
            case VERIFY_TEXT_CONTAINS -> keyword + " verify text of \"" + loc + "\" contains \"" + val + "\"";
            case VERIFY_ELEMENT_STATE -> keyword + " verify element \"" + loc + "\" is " + state;
        };
    }

    public String toKeywordDescription(AutomationStepModel step) {
        if (step == null || step.getTemplate() == null) return "";
        String loc = step.getLocatorValue() == null ? "" : step.getLocatorValue();
        String val = step.getTextValue() == null ? "" : step.getTextValue();
        String state = step.getStateValue() == null ? "visible" : step.getStateValue().name().toLowerCase();

        return switch (step.getTemplate()) {
            case OPEN_PAGE -> "Open page \"" + val + "\"";
            case ENTER_TEXT -> "Enter text \"" + val + "\" into \"" + loc + "\"";
            case CLICK_ELEMENT -> "Click element \"" + loc + "\"";
            case VERIFY_TEXT_EQUALS -> "Verify text of \"" + loc + "\" equals \"" + val + "\"";
            case VERIFY_TEXT_CONTAINS -> "Verify text of \"" + loc + "\" contains \"" + val + "\"";
            case VERIFY_ELEMENT_STATE -> "Verify element \"" + loc + "\" is " + state;
        };
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
