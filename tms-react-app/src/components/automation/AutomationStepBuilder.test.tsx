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
});
