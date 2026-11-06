package com.tcms.gherkin;

import com.tcms.services.GherkinDocuments;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class GherkinDocumentsTest {
    private final GherkinDocuments documents=new GherkinDocuments();
    @Test void acceptsLegacyFragmentsAndScenarioOutlines() {
        assertThatCode(() -> documents.validate("Given I am signed out\nWhen I sign in\nThen I see my dashboard","Sign in")).doesNotThrowAnyException();
        assertThatCode(() -> documents.validate("Scenario Outline: Sign in\nGiven I am <user>\nThen I see <page>\nExamples:\n | user | page |\n | joe | dashboard |","Sign in")).doesNotThrowAnyException();
    }
    @Test void rejectsMalformedTablesAndIncompleteSteps() {
        assertThatThrownBy(() -> documents.validate("Scenario Outline: Sign in\nGiven I am <user>\nExamples:\n | user | page |\n | joe |","Sign in"))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> documents.validate("Given ","Sign in")).isInstanceOf(ResponseStatusException.class);
    }
    @Test void requiresExamplesForOutlinesAndAtLeastOneScenario() {
        assertThatThrownBy(() -> documents.validate("Scenario Outline: Sign in\nGiven I am <user>","Sign in")).hasMessageContaining("Examples");
        assertThatThrownBy(() -> documents.validate("Feature: Empty","Empty")).hasMessageContaining("Scenario");
    }
    @Test void validatesSizeBeforeParsing() {
        assertThatThrownBy(() -> documents.validate("Given "+"x".repeat(100001),"Large")).hasMessageContaining("100,000");
    }
}
