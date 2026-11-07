package com.tcms.automation;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class AutomationValidator {

    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getJoinedMessage() {
            return String.join("; ", errors);
        }
    }

    public ValidationResult validate(List<AutomationStepModel> steps, Map<String, AutomationTestDefinition.LocatorDefinition> locators) {
        ValidationResult result = new ValidationResult();
        if (steps == null || steps.isEmpty()) {
            result.addError("At least one automation step is required");
            return result;
        }

        for (int i = 0; i < steps.size(); i++) {
            AutomationStepModel step = steps.get(i);
            int stepNum = step.getOrder() > 0 ? step.getOrder() : (i + 1);
            if (step.getTemplate() == null) {
                result.addError("Step " + stepNum + ": Automation template must be selected");
                continue;
            }

            switch (step.getTemplate()) {
                case OPEN_PAGE -> {
                    if (step.getTextValue() == null || step.getTextValue().trim().isEmpty()) {
                        result.addError("Step " + stepNum + ": Page URL is required for 'Open page'");
                    }
                }
                case ENTER_TEXT -> {
                    validateLocator(step, stepNum, locators, result);
                    if (step.getTextValue() == null || step.getTextValue().trim().isEmpty()) {
                        result.addError("Step " + stepNum + ": Text value to enter is required for 'Enter text'");
                    }
                }
                case CLICK_ELEMENT -> {
                    validateLocator(step, stepNum, locators, result);
                }
                case VERIFY_TEXT_EQUALS -> {
                    validateLocator(step, stepNum, locators, result);
                    if (step.getTextValue() == null || step.getTextValue().trim().isEmpty()) {
                        result.addError("Step " + stepNum + ": Expected text is required for 'Verify text equals'");
                    }
                }
                case VERIFY_TEXT_CONTAINS -> {
                    validateLocator(step, stepNum, locators, result);
                    if (step.getTextValue() == null || step.getTextValue().trim().isEmpty()) {
                        result.addError("Step " + stepNum + ": Expected text is required for 'Verify text contains'");
                    }
                }
                case VERIFY_ELEMENT_STATE -> {
                    validateLocator(step, stepNum, locators, result);
                    if (step.getStateValue() == null) {
                        result.addError("Step " + stepNum + ": Element state (VISIBLE or HIDDEN) is required for verification");
                    }
                }
            }
        }
        return result;
    }

    private void validateLocator(AutomationStepModel step, int stepNum, Map<String, AutomationTestDefinition.LocatorDefinition> locators, ValidationResult result) {
        String loc = step.getLocatorValue();
        if (loc == null || loc.trim().isEmpty()) {
            result.addError("Step " + stepNum + ": Target locator is required");
            return;
        }
        loc = loc.trim();
        LocatorType type = step.getLocatorType();

        // If explicitly NAMED or if matches standard named element pattern (e.g. Login.Username)
        if (type == LocatorType.NAMED || (type == null && isNamedLocatorPattern(loc))) {
            if (locators == null || !locators.containsKey(loc)) {
                result.addError("Step " + stepNum + ": Reusable named locator '" + loc + "' is not defined in project locators");
            }
            return;
        }

        // Check for common prefix notations if type is null
        if (type == null) {
            if (loc.startsWith("css=") || loc.startsWith("xpath=") || loc.startsWith("id=") || loc.startsWith("testid=") || loc.startsWith("role=")) {
                return; // Valid explicit prefix
            }
            // Check if it's a known named locator in project locators
            if (locators != null && locators.containsKey(loc)) {
                return;
            }
            // Raw CSS or XPath check
            if (loc.startsWith("//") || loc.startsWith("./") || loc.startsWith("(") || loc.startsWith("#") || loc.startsWith(".") || loc.startsWith("[")) {
                return;
            }
            // If it doesn't look like raw CSS/XPath and isn't a known named locator, flag as unknown target
            if (loc.contains(".")) {
                result.addError("Step " + stepNum + ": Named locator '" + loc + "' is not defined in project locators");
            }
        }
    }

    private boolean isNamedLocatorPattern(String loc) {
        return loc.matches("^[A-Za-z0-9_]+\\.[A-Za-z0-9_]+$");
    }
}
