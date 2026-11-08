import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import AutomationStepBuilder from './AutomationStepBuilder';
import { type AutomationStepModel } from './types';

// Mock api service to prevent network calls when fetching locators
vi.mock('../../services/api', () => ({
    default: {
        get: vi.fn().mockResolvedValue({ data: [] }),
        post: vi.fn().mockResolvedValue({ data: {} }),
        delete: vi.fn().mockResolvedValue({ data: {} })
    }
}));

afterEach(cleanup);

describe('AutomationStepBuilder', () => {
    const sampleSteps: AutomationStepModel[] = [
        {
            order: 1,
            keyword: 'Given',
            template: 'OPEN_PAGE',
            textValue: 'https://example.com/login'
        },
        {
            order: 2,
            keyword: 'When',
            template: 'CLICK_ELEMENT',
            locatorType: 'CSS',
            locatorValue: '#submit-btn'
        }
    ];

    it('renders Cucumber Automation mode with live Gherkin preview', () => {
        render(
            <AutomationStepBuilder
                mode="CUCUMBER_AUTOMATION"
                steps={sampleSteps}
                projectId={1}
                scenarioTitle="Login Test"
                onChange={vi.fn()}
            />
        );

        expect(screen.getByText('Cucumber Automation Steps')).toBeInTheDocument();
        expect(screen.getByText('Generated Gherkin Preview')).toBeInTheDocument();
        expect(screen.getByText('Open page')).toBeInTheDocument();
        expect(screen.getByText('Click element')).toBeInTheDocument();
    });

    it('renders Keyword Driven mode with step table', () => {
        render(
            <AutomationStepBuilder
                mode="KEYWORD_DRIVEN"
                steps={sampleSteps}
                projectId={1}
                scenarioTitle="Login Test"
                onChange={vi.fn()}
            />
        );

        expect(screen.getByText('Keyword Driven Step Table')).toBeInTheDocument();
        expect(screen.getByText('Open page')).toBeInTheDocument();
        expect(screen.getByText('Click element')).toBeInTheDocument();
    });

    it('renders and supports the five new templates in Cucumber Automation mode', () => {
        const extendedSteps: AutomationStepModel[] = [
            {
                order: 1,
                keyword: 'When',
                template: 'SELECT_OPTION',
                locatorType: 'CSS',
                locatorValue: '#framework-select',
                textValue: 'Playwright',
                selectBy: 'LABEL',
            },
            {
                order: 2,
                keyword: 'When',
                template: 'SET_CHECKBOX',
                locatorType: 'CSS',
                locatorValue: '#subscribe-checkbox',
                checkedValue: true,
            },
            {
                order: 3,
                keyword: 'When',
                template: 'PRESS_KEY',
                locatorType: 'CSS',
                locatorValue: '#keyboard-input',
                keyValue: 'Enter',
            },
            {
                order: 4,
                keyword: 'Then',
                template: 'VERIFY_URL',
                textValue: 'http://localhost:8080/demo/extended',
                urlComparison: 'EQUALS',
            },
            {
                order: 5,
                keyword: 'Then',
                template: 'VERIFY_ATTRIBUTE',
                locatorType: 'CSS',
                locatorValue: '#action-btn',
                attributeName: 'data-status',
                textValue: 'ready',
            },
        ];

        render(
            <AutomationStepBuilder
                mode="CUCUMBER_AUTOMATION"
                steps={extendedSteps}
                projectId={1}
                scenarioTitle="Extended Controls Feature"
                onChange={vi.fn()}
            />
        );

        expect(screen.getByText('Select option')).toBeInTheDocument();
        expect(screen.getByText('Set checkbox')).toBeInTheDocument();
        expect(screen.getByText('Press key')).toBeInTheDocument();
        expect(screen.getByText('Verify URL')).toBeInTheDocument();
        expect(screen.getByText('Verify attribute')).toBeInTheDocument();

        // Gherkin preview strings
        expect(screen.getByText('select option "Playwright" by label in "#framework-select"')).toBeInTheDocument();
        expect(screen.getByText('set checkbox "#subscribe-checkbox" to checked')).toBeInTheDocument();
        expect(screen.getByText('press key "Enter" on "#keyboard-input"')).toBeInTheDocument();
        expect(screen.getByText('verify url equals "http://localhost:8080/demo/extended"')).toBeInTheDocument();
        expect(screen.getByText('verify attribute "data-status" of "#action-btn" equals "ready"')).toBeInTheDocument();
    });

    it('renders the five new templates in Keyword Driven mode', () => {
        const extendedSteps: AutomationStepModel[] = [
            {
                order: 1,
                template: 'SELECT_OPTION',
                locatorValue: '#framework-select',
                textValue: 'Playwright',
                selectBy: 'LABEL',
            },
            {
                order: 2,
                template: 'SET_CHECKBOX',
                locatorValue: '#subscribe-checkbox',
                checkedValue: true,
            },
            {
                order: 3,
                template: 'PRESS_KEY',
                locatorValue: '#keyboard-input',
                keyValue: 'Enter',
            },
            {
                order: 4,
                template: 'VERIFY_URL',
                textValue: 'http://localhost:8080/demo/extended',
                urlComparison: 'EQUALS',
            },
            {
                order: 5,
                template: 'VERIFY_ATTRIBUTE',
                locatorValue: '#action-btn',
                attributeName: 'data-status',
                textValue: 'ready',
            },
        ];

        render(
            <AutomationStepBuilder
                mode="KEYWORD_DRIVEN"
                steps={extendedSteps}
                projectId={1}
                scenarioTitle="Extended Controls Feature"
                onChange={vi.fn()}
            />
        );

        expect(screen.getByText('Keyword Driven Step Table')).toBeInTheDocument();
        expect(screen.getByText('Select option')).toBeInTheDocument();
        expect(screen.getByText('Set checkbox')).toBeInTheDocument();
        expect(screen.getByText('Press key')).toBeInTheDocument();
        expect(screen.getByText('Verify URL')).toBeInTheDocument();
        expect(screen.getByText('Verify attribute')).toBeInTheDocument();
        expect(screen.getByText('(Browser URL)')).toBeInTheDocument();
    });
    it('escapes quotes and backslashes in preview and preserves empty attribute values', () => {
        const value='He said "Hi" \\ path';
        const escaped=JSON.stringify(value).slice(1,-1);
        render(<AutomationStepBuilder mode="CUCUMBER_AUTOMATION" projectId={1} scenarioTitle="Escapes" onChange={vi.fn()}
            steps={[
                {order:1,keyword:'When',template:'SELECT_OPTION',locatorValue:'#opts',selectBy:'LABEL',textValue:value},
                {order:2,keyword:'Then',template:'VERIFY_ATTRIBUTE',locatorValue:'#hidden',attributeName:'data-empty',textValue:''},
            ]} />);
        expect(screen.getByText(`select option "${escaped}" by label in "#opts"`)).toBeInTheDocument();
        expect(screen.getByText('verify attribute "data-empty" of "#hidden" equals ""')).toBeInTheDocument();
    });

});
