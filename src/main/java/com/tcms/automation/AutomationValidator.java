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
            if (step == null) { result.addError("Step " + (i + 1) + ": Step is required"); continue; }
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
                case SELECT_OPTION -> {
                    validateLocator(step, stepNum, locators, result);
                    if (step.getTextValue() == null || step.getTextValue().trim().isEmpty()) {
                        result.addError("Step " + stepNum + ": Option label or value is required for 'Select option'");
                    }
                    if (step.getSelectBy() == null) {
                        step.setSelectBy(OptionSelectBy.LABEL);
                    }
                }
                case SET_CHECKBOX -> {
                    validateLocator(step, stepNum, locators, result);
                    if (step.getCheckedValue() == null) {
                        result.addError("Step " + stepNum + ": Checkbox state (checked or unchecked) is required for 'Set checkbox'");
                    }
                }
                case PRESS_KEY -> {
                    validateLocator(step, stepNum, locators, result);
                    if (step.getKeyValue() == null || step.getKeyValue().trim().isEmpty()) {
                        result.addError("Step " + stepNum + ": Key is required for 'Press key'");
                    } else {
                        String key = step.getKeyValue().trim();
                        if (!key.matches(".*\\$\\{[a-zA-Z0-9_.-]+\\}.*") && !isValidKey(key)) {
                            result.addError("Step " + stepNum + ": Unsupported key '" + key + "'. Supported keys: " + String.join(", ", SUPPORTED_KEYS));
                        }
                    }
                }
                case VERIFY_URL -> {
                    if (step.getTextValue() == null || step.getTextValue().trim().isEmpty()) {
                        result.addError("Step " + stepNum + ": Expected URL is required for 'Verify URL'");
                    }
                    if (step.getUrlComparison() == null) {
                        step.setUrlComparison(UrlComparison.EQUALS);
                    }
                }
                case VERIFY_ATTRIBUTE -> {
                    validateLocator(step, stepNum, locators, result);
                    if (step.getAttributeName() == null || step.getAttributeName().trim().isEmpty()) {
                        result.addError("Step " + stepNum + ": Attribute name is required for 'Verify attribute'");
                    }
                    if (step.getTextValue() == null) {
                        result.addError("Step " + stepNum + ": Expected attribute value is required for 'Verify attribute'");
                    }
                }
            }
        }
        return result;
    }

    public static final List<String> SUPPORTED_KEYS = List.of(
        "Enter", "Tab", "Escape", "ArrowDown", "ArrowUp", "ArrowLeft", "ArrowRight",
        "Backspace", "Delete", "Space", "Home", "End", "PageUp", "PageDown"
    );

    public static boolean isValidKey(String key) {
        if (key == null || key.isBlank()) return false;
        for (String k : SUPPORTED_KEYS) {
            if (k.equalsIgnoreCase(key.trim())) return true;
        }
        return key.trim().length() == 1;
    }

    public static String normalizeKey(String key) {
        if (key == null) return "";
        String trimmed = key.trim();
        for (String k : SUPPORTED_KEYS) {
            if (k.equalsIgnoreCase(trimmed)) return k;
        }
        return trimmed;
    }

    private void validateLocator(AutomationStepModel step, int stepNum, Map<String, AutomationTestDefinition.LocatorDefinition> locators, ValidationResult result) {
        String loc = step.getLocatorValue();
        if (loc == null || loc.trim().isEmpty()) {
            result.addError("Step " + stepNum + ": Target locator is required");
            return;
        }
        loc = loc.trim();
        // A variable locator is resolved against this project's values by the worker at runtime.
        if (java.util.regex.Pattern.compile("\\$\\{[a-zA-Z0-9_.-]+\\}").matcher(loc).find()) return;
        LocatorType type = step.getLocatorType();

        // If explicitly NAMED or if matches standard named element pattern (e.g. Login.Username)
        if (type == LocatorType.NAMED || (type == null && (isNamedLocatorPattern(loc) || locators != null && locators.containsKey(loc)))) {
            if (locators == null || !locators.containsKey(loc)) {
                result.addError("Step " + stepNum + ": Reusable named locator '" + loc + "' is not defined in project locators");
            } else {
                var definition = locators.get(loc);
                validateStrategy(definition.getType(), definition.getValue(), stepNum, result);
            }
            return;
        }

        LocatorType prefixType = LocatorType.prefixedType(loc);
        if (prefixType != null || type != null) {
            validateStrategy(prefixType != null ? prefixType : type, loc, stepNum, result);
            return;
        }

        // Check for common prefix notations if type is null
        if (type == null) {
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

    private void validateStrategy(LocatorType type, String value, int stepNum, ValidationResult result) {
        if (type == null) { result.addError("Step " + stepNum + ": Locator type is required"); return; }
        if (LocatorType.prefixedType(value) == type) value = value.substring(type.prefix().length() + 1);
        String error = type.validationError(value);
        if (error != null) result.addError("Step " + stepNum + ": " + error);
    }

    private boolean isNamedLocatorPattern(String loc) {
        return loc.matches("^[A-Za-z0-9_]+\\.[A-Za-z0-9_]+$");
    }
}
