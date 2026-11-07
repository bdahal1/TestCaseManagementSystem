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

    @Given("^open page \"([^\"]*)\"$")
    public void openPage(String url) {
        driver().openPage(url);
    }

    @When("^enter text \"([^\"]*)\" into \"([^\"]*)\"$")
    public void enterText(String value, String locator) {
        driver().enterText(locator, value);
    }

    @When("^click element \"([^\"]*)\"$")
    public void clickElement(String locator) {
        driver().clickElement(locator);
    }

    @Then("^verify text of \"([^\"]*)\" equals \"([^\"]*)\"$")
    public void verifyTextEquals(String locator, String expected) {
        driver().verifyTextEquals(locator, expected);
    }

    @Then("^verify text of \"([^\"]*)\" contains \"([^\"]*)\"$")
    public void verifyTextContains(String locator, String expected) {
        driver().verifyTextContains(locator, expected);
    }

    @Then("^verify element \"([^\"]*)\" is (visible|hidden)$")
    public void verifyElementState(String locator, String state) {
        ElementState elementState = "hidden".equalsIgnoreCase(state) ? ElementState.HIDDEN : ElementState.VISIBLE;
        driver().verifyElementState(locator, elementState);
    }
}
