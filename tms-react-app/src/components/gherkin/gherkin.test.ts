import { describe, expect, it } from 'vitest';
import { CompletionContext } from '@codemirror/autocomplete';
import { EditorState } from '@codemirror/state';
import { completeGherkin } from './GherkinEditor';
import { formatTables, preferredKeyword, scenarioTemplate, splitTableRow } from './gherkin';
import { validateGherkin } from './gherkinValidation';

describe('Gherkin documents', () => {
    it('accepts existing step-only cases and complete feature files', () => {
        const steps = 'Given I am signed out\nWhen I sign in\nThen I see my dashboard';
        expect(validateGherkin(steps).issues).toEqual([]);
        expect(validateGherkin(`Feature: Accounts\nScenario: Sign in\n${steps}`).scenarios).toBe(1);
    });
    it('validates outlines, placeholders, and example rows using the Cucumber grammar', () => {
        const result = validateGherkin(scenarioTemplate('Sign in', true));
        expect(result.issues).toEqual([]); expect(result.examples).toBe(1);
        expect(validateGherkin('Scenario Outline: Sign in\nGiven I am <user>').issues[0].message).toContain('Examples');
    });
    it('reports malformed tables and incomplete steps at user-visible lines', () => {
        const source = 'Scenario Outline: Sign in\n  Given I am <user>\n  Then I see <page>\n  Examples:\n    | user | page |\n    | joe |';
        expect(validateGherkin(source).issues.length).toBeGreaterThan(0);
        expect(validateGherkin('Given ').issues[0]).toEqual(expect.objectContaining({ line: 1 }));
    });
    it('allows data tables and doc strings as step arguments', () => {
        const source = 'Given these accounts\n  | user | active |\n  | joe | true |\nWhen I submit this request\n  """json\n  {"active": true}\n  """\nThen I see the account';
        expect(validateGherkin(source).issues).toEqual([]);
    });
    it('preserves escaped pipes and aligns example tables', () => {
        const source = String.raw`Examples:
 | user | note |
 | joe | left \| right |`;
        const formatted = formatTables(source);
        expect(splitTableRow(' | joe | left \\| right | ')).toEqual(['joe', 'left | right']);
        expect(formatted).toContain('left \\| right');
        expect(formatted.split('\n')[1].length).toBe(formatted.split('\n')[2].length);
        expect(formatTables(formatted)).toBe(formatted);
        const escapedBackslash = String.raw`| path | note |
| C:\\ | example |`;
        expect(formatTables(escapedBackslash).split('\n').map(splitTableRow)).toEqual(escapedBackslash.split('\n').map(splitTableRow));
    });
    it('does not format table-looking content inside doc strings', () => {
        const source = 'Given a document\n  """\n| x|y |\n  """\nThen it is stored';
        expect(formatTables(source)).toBe(source);
        const mixedDelimiters = 'Given a document\n  """\n  ```\n| x|y |\n  """';
        expect(formatTables(mixedDelimiters)).toBe(mixedDelimiters);
    });
});
describe('step suggestions', () => {
    it('suggests the next GWT keyword and resets at a new scenario', () => {
        expect(preferredKeyword('')).toBe('Given');
        expect(preferredKeyword('Given context\n')).toBe('When');
        expect(preferredKeyword('Given context\nWhen action\n')).toBe('Then');
        expect(preferredKeyword('Then outcome\nScenario: Another\n')).toBe('Given');
    });
    it('includes reusable project steps and prioritizes the next keyword on a blank line', () => {
        const state = EditorState.create({ doc: 'Given I am signed in\n  ' });
        const result = completeGherkin(new CompletionContext(state, state.doc.length, true), ['When I open my account', 'When I open my account']);
        expect(result?.options.find(option => option.label === 'When')?.boost).toBe(50);
        expect(result?.options.filter(option => option.label === 'When I open my account')).toHaveLength(1);
    });
    it('does not suggest keywords inside tables or doc strings', () => {
        for (const doc of ['Given data\n | ', 'Given a document\n  """\n']) {
            const state = EditorState.create({ doc });
            expect(completeGherkin(new CompletionContext(state, state.doc.length, true))).toBeNull();
        }
    });
});
