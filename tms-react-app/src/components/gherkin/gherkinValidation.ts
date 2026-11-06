import { AstBuilder, GherkinClassicTokenMatcher, Parser } from '@cucumber/gherkin';
import { IdGenerator } from '@cucumber/messages';

export const MAX_GHERKIN_LENGTH = 100_000;
export interface GherkinIssue { line: number; column: number; message: string }
export interface GherkinValidation { issues: GherkinIssue[]; scenarios: number; examples: number }
interface ParserFailure extends Error { errors?: ParserFailure[]; location?: { line: number; column?: number } }

export function parserSource(value: string, title = 'Test case'): { source: string; offset: number } {
    const safeTitle = title.replace(/[\r\n]/g, ' ').trim() || 'Test case';
    if (/^\s*Feature\s*:/m.test(value)) return { source: value, offset: 0 };
    if (/^\s*(Scenario(?: Outline| Template)?|Example|Background|Rule)\s*:/m.test(value)) return { source: `Feature: ${safeTitle}\n${value}`, offset: 1 };
    return { source: `Feature: ${safeTitle}\nScenario: ${safeTitle}\n${value}`, offset: 2 };
}
export function validateGherkin(value: string, title?: string): GherkinValidation {
    const issue = (message: string): GherkinValidation => ({ issues: [{ line: 1, column: 1, message }], scenarios: 0, examples: 0 });
    if (!value.trim()) return issue('Write a scenario with Given, When, and Then steps.');
    if (value.length > MAX_GHERKIN_LENGTH) return issue('Keep the scenario below 100,000 characters.');
    const { source, offset } = parserSource(value, title);
    try {
        const document = new Parser(new AstBuilder(IdGenerator.incrementing()), new GherkinClassicTokenMatcher()).parse(source);
        const scenarios = document.feature?.children.flatMap(child => child.scenario ? [child.scenario] : child.rule?.children.flatMap(nested => nested.scenario ? [nested.scenario] : []) ?? []) ?? [];
        if (!scenarios.length) return issue('Add at least one Scenario or Scenario Outline.');
        const issues: GherkinIssue[] = [];
        for (const scenario of scenarios) {
            if (!scenario.steps.length) issues.push({ line: Math.max(1, scenario.location.line - offset), column: 1, message: 'Add steps to this scenario.' });
            for (const step of scenario.steps) if (!step.text.trim()) issues.push({ line: Math.max(1, step.location.line - offset), column: 1, message: `Describe the ${step.keyword.trim()} step.` });
            if (/Outline|Template/.test(scenario.keyword) && !scenario.examples.some(example => example.tableBody.length))
                issues.push({ line: Math.max(1, scenario.location.line - offset), column: 1, message: 'Add an Examples table with at least one data row.' });
        }
        return { issues, scenarios: scenarios.length, examples: scenarios.reduce((count, scenario) => count + scenario.examples.reduce((n, example) => n + example.tableBody.length, 0), 0) };
    } catch (error) {
        const failure = error as ParserFailure;
        return { issues: (failure.errors ?? [failure]).map(e => ({ line: Math.max(1, (e.location?.line ?? 1) - offset), column: e.location?.column ?? 1, message: e.message.replace(/^\([^)]*\):\s*/, '') })), scenarios: 0, examples: 0 };
    }
}
