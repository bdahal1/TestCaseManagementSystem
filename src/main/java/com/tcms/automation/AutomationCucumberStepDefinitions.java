package com.tcms.automation;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;
import io.cucumber.java.en.But;

public class AutomationCucumberStepDefinitions {
    private static final ThreadLocal<AutomationBrowserDriver> CURRENT_DRIVER = new ThreadLocal<>();

    public static void setDriver(AutomationBrowserDriver driver) {
        CURRENT_DRIVER.set(driver);
    }

    public static AutomationBrowserDriver getDriver() {
        return CURRENT_DRIVER.get();
    }

    public static void clearDriver() {
        CURRENT_DRIVER.remove();
    }

    private AutomationBrowserDriver driver() {
        AutomationBrowserDriver d = getDriver();
        if (d == null) {
            throw new IllegalStateException("AutomationBrowserDriver is not initialized for this execution thread");
        }
        return d;
    }

    private static final String Q = GherkinStrings.QUOTED;

    @Given("^open page " + Q + "$")
    public void openPage(String url) { driver().openPage(GherkinStrings.decode(url)); }

    @When("^enter text " + Q + " into " + Q + "$")
    public void enterText(String value, String locator) { driver().enterText(GherkinStrings.decode(locator), GherkinStrings.decode(value)); }

    @When("^click element " + Q + "$")
    public void clickElement(String locator) { driver().clickElement(GherkinStrings.decode(locator)); }

    @Then("^verify text of " + Q + " equals " + Q + "$")
    public void verifyTextEquals(String locator, String expected) { driver().verifyTextEquals(GherkinStrings.decode(locator), GherkinStrings.decode(expected)); }

    @Then("^verify text of " + Q + " contains " + Q + "$")
    public void verifyTextContains(String locator, String expected) { driver().verifyTextContains(GherkinStrings.decode(locator), GherkinStrings.decode(expected)); }

    @Then("^verify element " + Q + " is (visible|hidden)$")
    public void verifyElementState(String locator, String state) {
        driver().verifyElementState(GherkinStrings.decode(locator), "hidden".equals(state) ? ElementState.HIDDEN : ElementState.VISIBLE);
    }

    @When("^select option " + Q + " by (label|value) (?:in|from) " + Q + "$")
    public void selectOption(String option, String by, String locator) {
        driver().selectOption(GherkinStrings.decode(locator), GherkinStrings.decode(option), "value".equals(by) ? OptionSelectBy.VALUE : OptionSelectBy.LABEL);
    }

    @When("^set checkbox " + Q + " to (checked|unchecked)$")
    public void setCheckbox(String locator, String state) { driver().setCheckbox(GherkinStrings.decode(locator), "checked".equals(state)); }

    @When("^press key " + Q + " on(?: element)? " + Q + "$")
    public void pressKey(String key, String locator) { driver().pressKey(GherkinStrings.decode(locator), GherkinStrings.decode(key)); }

    @Then("^verify url (equals|contains) " + Q + "$")
    public void verifyUrl(String comparison, String url) {
        driver().verifyUrl(GherkinStrings.decode(url), "contains".equals(comparison) ? UrlComparison.CONTAINS : UrlComparison.EQUALS);
    }

    @Then("^verify attribute " + Q + " of " + Q + " equals " + Q + "$")
    public void verifyAttribute(String attribute, String locator, String expected) {
        driver().verifyAttribute(GherkinStrings.decode(locator), GherkinStrings.decode(attribute), GherkinStrings.decode(expected));
    }
}
