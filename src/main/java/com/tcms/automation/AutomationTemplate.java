package com.tcms.automation;

public enum AutomationTemplate {
    OPEN_PAGE("Open page"),
    ENTER_TEXT("Enter text"),
    CLICK_ELEMENT("Click element"),
    VERIFY_TEXT_EQUALS("Verify text equals"),
    VERIFY_TEXT_CONTAINS("Verify text contains"),
    VERIFY_ELEMENT_STATE("Verify element visible or hidden");

    private final String displayName;

    AutomationTemplate(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
