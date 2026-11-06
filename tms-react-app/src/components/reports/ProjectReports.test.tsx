import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import ProjectReports from './ProjectReports';
import api from '../../services/api';
import type { ProjectReport } from './reportTypes';
vi.mock('../../services/api', () => ({ default: { get: vi.fn() } }));
const counts = { PASS: 1, FAIL: 0, BLOCKED: 1, SKIPPED: 0, NOT_RUN: 0 };
const fixture: ProjectReport = {
    generatedAt: '2026-11-06T10:00:00Z',
    from: '2026-10-08',
    to: '2026-11-06',
    environments: ['Staging'],
    releases: ['2.4'],
    library: {
        total: 1,
        priorities: { CRITICAL: 1, HIGH: 0, MEDIUM: 0, LOW: 0 },
        automation: { NOT_AUTOMATED: 0, PLANNED: 1, AUTOMATED: 0, NOT_APPLICABLE: 0 },
        types: { MANUAL: 1 },
        unowned: 0,
        estimatedMinutes: 8,
        estimatesDefined: 1,
    },
    execution: {
        activePlans: 1,
        closedPlans: 1,
        draftPlans: 1,
        total: 2,
        completed: 1,
        passRate: 100,
        completionRate: 50,
        testedCases: 1,
        neverRunCases: 0,
        linkedDefects: 1,
        results: counts,
    },
    plans: [],
    activity: [{ date: '2026-11-06', results: counts }],
    cases: [
        {
            id: 3,
            key: 'ATL-3',
            name: 'Sign in',
            priority: 'CRITICAL',
            automationStatus: 'PLANNED',
            component: 'Accounts',
            owner: 'Alex QA',
            estimatedMinutes: 8,
            result: 'BLOCKED',
            planId: 7,
            planName: 'Regression',
            recordedAt: '2026-11-06T10:00:00Z',
            defectUrl: null,
            definitionChanged: true,
        },
    ],
    workload: [],
};
afterEach(cleanup);
beforeEach(() => {
    vi.resetAllMocks();
    vi.mocked(api.get).mockResolvedValue({ data: fixture });
});
const open = () =>
    render(
        <MemoryRouter>
            <ProjectReports projectId={2} />
        </MemoryRouter>,
    );
describe('project reports', () => {
    it('states metric denominators and exposes case results with changed-case warnings', async () => {
        const user = userEvent.setup();
        open();
        expect(await screen.findByText('Passed ÷ (passed + failed)')).toBeInTheDocument();
        expect(
            screen.getByText('1 draft plans excluded. A passing result does not establish requirements coverage.'),
        ).toBeInTheDocument();
        await user.click(screen.getByRole('tab', { name: 'Case results' }));
        expect(screen.getByText('Sign in')).toBeInTheDocument();
        expect(screen.getByText('Library changed')).toBeInTheDocument();
        expect(screen.getByRole('link', { name: 'Regression' })).toHaveAttribute(
            'href',
            '/dashboard/project/2/test-plans/7',
        );
        await user.click(screen.getByRole('combobox', { name: 'Outcome' }));
        await user.click(screen.getByRole('option', { name: /^Pass$/ }));
        expect(screen.getByText('No cases match these filters.')).toBeInTheDocument();
    });
    it('applies environment filters on the server', async () => {
        const user = userEvent.setup();
        open();
        await screen.findByText('Passed ÷ (passed + failed)');
        await user.click(screen.getByRole('combobox', { name: 'Environment' }));
        await user.click(screen.getByRole('option', { name: 'Staging' }));
        await waitFor(() =>
            expect(api.get).toHaveBeenLastCalledWith(
                '/projectReports/2',
                expect.objectContaining({ params: expect.objectContaining({ environment: 'Staging' }) }),
            ),
        );
    });
    it('shows a failed load as an error and supports retry without presenting invented zero counts', async () => {
        vi.mocked(api.get).mockRejectedValueOnce(new Error('Network error'));
        const user = userEvent.setup();
        open();
        expect(await screen.findByText('Could not load project reports.')).toBeInTheDocument();
        expect(screen.queryByText('Library cases')).not.toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: 'Retry' }));
        expect(await screen.findByText('Library cases')).toBeInTheDocument();
    });
});
