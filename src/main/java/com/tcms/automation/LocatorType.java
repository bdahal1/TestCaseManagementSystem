package com.tcms.automation;

public enum LocatorType {
    ID("id"), CSS("css"), XPATH("xpath"), ROLE_NAME("role"), TEST_ID("testid"),
    NAME("name"), CLASS_NAME("class"), LABEL("label"), PLACEHOLDER("placeholder"),
    TEXT("text"), LINK_TEXT("link"), PARTIAL_LINK_TEXT("partiallink"), ALT_TEXT("alt"), TITLE("title"),
    NAMED(null);

    private final String prefix;
    LocatorType(String prefix) { this.prefix = prefix; }
    public String prefix() { return prefix; }

    public static LocatorType prefixedType(String value) {
        if (value != null) for (var type : values())
            if (type.prefix != null && value.startsWith(type.prefix + "=")) return type;
        return null;
    }

    public static String reference(LocatorType type, String value) {
        if (value == null || type == null || type == NAMED || prefixedType(value) != null) return value;
        return type.prefix + "=" + value;
    }

    public String validationError(String value) {
        if (value == null || value.isBlank()) return "Target locator is required";
        if (value.matches("(?s).*\\$\\{[a-zA-Z0-9_.-]+}.*")) return null;
        if (this == CLASS_NAME && value.codePoints().anyMatch(Character::isWhitespace))
            return "Class name accepts one class; use CSS for a combination of classes";
        if (this == ROLE_NAME) {
            var matcher = java.util.regex.Pattern.compile("([A-Za-z]+)(?:\\[name=(['\"])(.*?)\\2\\])?", java.util.regex.Pattern.DOTALL).matcher(value);
            if (matcher.matches()) {
                try { com.microsoft.playwright.options.AriaRole.valueOf(matcher.group(1).toUpperCase(java.util.Locale.ROOT)); return null; }
                catch (IllegalArgumentException ignored) {}
            }
            return "Use a valid role such as button[name='Login'] or textbox; choose Name for an HTML name such as username";
        }
        return null;
    }
}
