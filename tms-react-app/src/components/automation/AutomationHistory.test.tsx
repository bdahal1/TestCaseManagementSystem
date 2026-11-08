import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import AutomationHistoryPage from './AutomationHistoryPage';
import AutomationCompareDialog from './AutomationCompareDialog';
import AutomationRunDialog from './AutomationRunDialog';
import api from '../../services/api';
import {
    type AttemptSummary,
    type CompareAttemptResponse,
    type HistoryPageResponse,
} from './types';

vi.mock('../../services/api', () => ({
    default: {
        get: vi.fn(),
        post: vi.fn(),
    },
}));

beforeEach(() => {
    vi.mocked(api.get).mockReset();
    vi.mocked(api.post).mockReset();
});

afterEach(() => {
    cleanup();
    vi.useRealTimers();
});

const mockHistoryResponse: HistoryPageResponse = {
    content: [
        {
            jobId: 101,
            planId: 1,
            planName: 'Sprint 1 Plan',
            batchId: 5,
            caseId: 21,
            caseName: 'Frozen User Login Test',
            status: 'COMPLETED',
            testType: 'CUCUMBER_AUTOMATION',
            createdAt: '2026-11-08T08:00:00Z',
            startedAt: '2026-11-08T08:00:01Z',
            finishedAt: '2026-11-08T08:00:04Z',
            durationMs: 3000,
            errorMessage: null,
            hasStepResults: true,
            hasDiagnosticLogs: true,
            failureEvidenceId: null,
            createdBy: 'Tester A',
        },
        {
            jobId: 102,
            planId: 1,
            planName: 'Sprint 1 Plan',
            batchId: null,
            caseId: 22,
            caseName: 'Checkout Cart Flow',
            status: 'FAILED',
            testType: 'KEYWORD_DRIVEN',
            createdAt: '2026-11-08T08:30:00Z',
            startedAt: '2026-11-08T08:30:01Z',
            finishedAt: '2026-11-08T08:30:03Z',
            durationMs: 2000,
            errorMessage: 'Assertion failed: [REDACTED] not found',
            hasStepResults: true,
            hasDiagnosticLogs: true,
            failureEvidenceId: 42,
            createdBy: 'Tester B',
        },
    ],
    totalElements: 2,
    page: 0,
    size: 20,
    totalPages: 1,
};

it('renders automation history rows with frozen case name, status, batch, and duration', async () => {
    vi.mocked(api.get).mockImplementation(async (url) => {
        if (String(url).includes('/testPlans/project/')) {
            return { data: { plans: [{ id: 1, name: 'Sprint 1 Plan' }] } };
        }
        if (String(url).includes('/automation/history/project/10')) {
            return { data: mockHistoryResponse };
        }
        return { data: null };
    });

    render(<AutomationHistoryPage projectId={10} />);

    await waitFor(() => {
        expect(screen.getByText('Frozen User Login Test')).toBeInTheDocument();
    });

    expect(screen.getByText('Checkout Cart Flow')).toBeInTheDocument();
    expect(screen.getByText('#101')).toBeInTheDocument();
    expect(screen.getByText('#102')).toBeInTheDocument();
    expect(screen.getByText('Batch #5')).toBeInTheDocument();
    expect(screen.getByText('Individual')).toBeInTheDocument();
    expect(screen.getByText('3.0s')).toBeInTheDocument();
    expect(screen.getByText('2.0s')).toBeInTheDocument();
    expect(screen.getByText(/Assertion failed: \[REDACTED\]/)).toBeInTheDocument();
});

it('filters history by case name and triggers request with query parameter', async () => {
    vi.mocked(api.get).mockImplementation(async (url, config) => {
        if (String(url).includes('/testPlans/project/')) {
            return { data: { plans: [] } };
        }
        if (String(url).includes('/automation/history/project/10')) {
            const params = config?.params || {};
            if (params.caseNameContains === 'Login') {
                return {
                    data: {
                        ...mockHistoryResponse,
                        content: [mockHistoryResponse.content[0]],
                        totalElements: 1,
                    },
                };
            }
            return { data: mockHistoryResponse };
        }
        return { data: null };
    });

    render(<AutomationHistoryPage projectId={10} />);

    await waitFor(() => {
        expect(screen.getByText('Frozen User Login Test')).toBeInTheDocument();
    });

    const searchInput = screen.getByLabelText('Case Name');
    fireEvent.change(searchInput, { target: { value: 'Login' } });

    await waitFor(() => {
        expect(api.get).toHaveBeenCalledWith(
            '/automation/history/project/10',
            expect.objectContaining({
                params: expect.objectContaining({ caseNameContains: 'Login' }),
            })
        );
    });
});

it('clicking a history row opens exact job details dialog in read-only mode', async () => {
    vi.mocked(api.get).mockImplementation(async (url) => {
        if (String(url).includes('/testPlans/project/')) {
            return { data: { plans: [{ id: 1, name: 'Sprint 1 Plan' }] } };
        }
        if (String(url).includes('/automation/history/project/10')) {
            return { data: mockHistoryResponse };
        }
        if (String(url) === '/automation/jobs/101') {
            return {
                data: {
                    jobId: 101,
                    planId: 1,
                    caseId: 21,
                    status: 'COMPLETED',
                    testType: 'CUCUMBER_AUTOMATION',
                    createdAt: '2026-11-08T08:00:00Z',
                    finishedAt: '2026-11-08T08:00:04Z',
                    stepResults: [
                        { stepOrder: 1, description: 'Given open page', passed: true, durationMs: 150 },
                    ],
                    diagnosticLogs: 'All 1 steps passed',
                },
            };
        }
        return { data: null };
    });

    render(<AutomationHistoryPage projectId={10} />);

    await waitFor(() => {
        expect(screen.getByText('Frozen User Login Test')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Frozen User Login Test'));

    await waitFor(() => {
        expect(screen.getByText('Automation Run Details #101')).toBeInTheDocument();
    });

    expect(screen.getByText('Historical (Read-Only)')).toBeInTheDocument();
    // Run buttons must NOT be present in historical mode
    expect(screen.queryByRole('button', { name: 'Run Again' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Run Test Now' })).not.toBeInTheDocument();
});

it('displays clear empty states when diagnostics, steps, or evidence are absent', async () => {
    vi.mocked(api.get).mockImplementation(async (url) => {
        if (String(url) === '/automation/jobs/200') {
            return {
                data: {
                    jobId: 200,
                    planId: 1,
                    caseId: 30,
                    status: 'FAILED',
                    testType: 'KEYWORD_DRIVEN',
                    createdAt: '2026-11-08T08:00:00Z',
                    finishedAt: '2026-11-08T08:00:02Z',
                    errorMessage: 'Aborted early',
                    stepResults: [],
                    diagnosticLogs: null,
                    failureEvidenceId: null,
                },
            };
        }
        return { data: null };
    });

    render(
        <AutomationRunDialog
            open={true}
            jobId={200}
            planId={1}
            caseId={30}
            caseName="Empty Test"
            testType="KEYWORD_DRIVEN"
            onClose={vi.fn()}
        />
    );

    await waitFor(() => {
        expect(screen.getByText('Automation Run Details #200')).toBeInTheDocument();
    });

    expect(screen.getByText('No step-level outcomes were recorded for this attempt.')).toBeInTheDocument();
    expect(screen.getByText('No screenshot evidence was attached to this run.')).toBeInTheDocument();
    expect(screen.getByText('No diagnostic execution logs recorded for this attempt.')).toBeInTheDocument();
});

it('compares two attempts side-by-side and flags changed step definitions explicitly', async () => {
    const mockAttempts: AttemptSummary[] = [
        { jobId: 10, attemptNumber: 1, status: 'FAILED', createdAt: '2026-11-08T07:00:00Z', finishedAt: '2026-11-08T07:00:02Z', durationMs: 2000 },
        { jobId: 20, attemptNumber: 2, status: 'COMPLETED', createdAt: '2026-11-08T08:00:00Z', finishedAt: '2026-11-08T08:00:03Z', durationMs: 3000 },
    ];

    const mockCompareResponse: CompareAttemptResponse = {
        jobIdA: 20,
        jobIdB: 10,
        planId: 1,
        caseId: 5,
        caseName: 'Search Test',
        statusA: 'COMPLETED',
        statusB: 'FAILED',
        durationMsA: 3000,
        durationMsB: 2000,
        hasChangedSteps: true,
        steps: [
            {
                stepOrder: 1,
                description: 'Given open page "https://example.com"',
                passedA: true,
                durationMsA: 100,
                errorMessageA: null,
                onlyInA: false,
                passedB: true,
                durationMsB: 120,
                errorMessageB: null,
                onlyInB: false,
            },
            {
                stepOrder: 2,
                description: 'When enter text "query" into "#search"',
                passedA: true,
                durationMsA: 80,
                errorMessageA: null,
                onlyInA: false,
                passedB: false,
                durationMsB: 500,
                errorMessageB: 'Element not interactable',
                onlyInB: false,
            },
            {
                stepOrder: 3,
                description: 'When click element "#new-filter"',
                passedA: true,
                durationMsA: 50,
                errorMessageA: null,
                onlyInA: true,
                passedB: null,
                durationMsB: null,
                errorMessageB: null,
                onlyInB: false,
            },
        ],
    };

    vi.mocked(api.get).mockImplementation(async (url) => {
        if (String(url).includes('/attempts')) {
            return { data: mockAttempts };
        }
        if (String(url).includes('/compare')) {
            return { data: mockCompareResponse };
        }
        return { data: null };
    });

    render(
        <AutomationCompareDialog
            open={true}
            projectId={1}
            planId={1}
            caseId={5}
            currentJobId={20}
            caseName="Search Test"
            onClose={vi.fn()}
        />
    );

    await waitFor(() => {
        expect(screen.getByText('Compare Automation Attempts')).toBeInTheDocument();
    });

    // Check step comparison table
    await waitFor(() => {
        expect(screen.getByText('Given open page "https://example.com"')).toBeInTheDocument();
    });

    expect(screen.getByText('When enter text "query" into "#search"')).toBeInTheDocument();
    expect(screen.getByText('When click element "#new-filter"')).toBeInTheDocument();

    // Check changed step warning alert
    expect(screen.getByText('Step definitions differ between these two attempts.')).toBeInTheDocument();

    // Check diff badges
    expect(screen.getByText('Fixed')).toBeInTheDocument();
    expect(screen.getByText('Only in current')).toBeInTheDocument();
});
