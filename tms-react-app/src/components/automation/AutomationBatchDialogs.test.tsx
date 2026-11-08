import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AutomationBatchPreviewDialog from './AutomationBatchPreviewDialog';
import AutomationBatchViewDialog from './AutomationBatchViewDialog';
import api from '../../services/api';
import { type BatchPreviewResponse, type BatchDetailResponse } from './types';

vi.mock('../../services/api', () => ({
    default: {
        get: vi.fn(),
        post: vi.fn(),
    },
}));

beforeEach(() => {
    vi.mocked(api.post).mockReset();
    vi.mocked(api.get).mockReset();
});
afterEach(cleanup);

describe('AutomationBatchPreviewDialog', () => {
    const mockPreview: BatchPreviewResponse = {
        planId: 101,
        planName: 'Sprint 42 Plan',
        planStatus: 'IN_PROGRESS',
        scope: 'ALL_ELIGIBLE',
        totalInScope: 4,
        readyCount: 2,
        canExecute: true,
        readyCases: [
            {
                caseId: 1,
                caseName: 'Login Test',
                testType: 'CUCUMBER_AUTOMATION',
                assignee: 'Alice Tester',
                stepCount: 3,
            },
            {
                caseId: 2,
                caseName: 'Checkout Flow',
                testType: 'KEYWORD_DRIVEN',
                assignee: 'Bob Tester',
                stepCount: 5,
            },
        ],
        manualSkippedCases: [
            {
                caseId: 3,
                caseName: 'Manual Sanity',
                testType: 'MANUAL',
                category: 'MANUAL',
                reason: 'Manual cases cannot be executed as automated browser jobs.',
            },
        ],
        invalidConfigCases: [
            {
                caseId: 4,
                caseName: 'Broken Steps',
                testType: 'KEYWORD_DRIVEN',
                category: 'INVALID_CONFIG',
                reason: 'Step 1 is missing a selector.',
            },
        ],
        alreadyPendingCases: [],
        unauthorizedCases: [],
    };

    it('renders preview with ready and excluded counts and switches tabs', async () => {
        vi.mocked(api.post).mockResolvedValueOnce({ data: mockPreview });

        await act(async () => {
            render(
                <AutomationBatchPreviewDialog
                    open={true}
                    planId={101}
                    planName="Sprint 42 Plan"
                    scope="ALL_ELIGIBLE"
                    onClose={vi.fn()}
                    onConfirmed={vi.fn()}
                />
            );
        });

        await waitFor(() => {
            expect(screen.getByText('Batch Automation Run Preview')).toBeInTheDocument();
            expect(screen.getByText('2 Ready to Run')).toBeInTheDocument();
            expect(screen.getByText('1 Manual Skipped')).toBeInTheDocument();
        });

        // Ready cases are listed
        expect(screen.getByText('Login Test')).toBeInTheDocument();
        expect(screen.getByText('Checkout Flow')).toBeInTheDocument();

        // Switch to Excluded tab
        const excludedTab = screen.getByRole('tab', { name: /Excluded Cases \(2\)/i });
        await act(async () => {
            fireEvent.click(excludedTab);
        });

        expect(screen.getByText('Manual Sanity')).toBeInTheDocument();
        expect(screen.getByText('Manual cases cannot be executed as automated browser jobs.')).toBeInTheDocument();
        expect(screen.getByText('Broken Steps')).toBeInTheDocument();
        expect(screen.getByText('Step 1 is missing a selector.')).toBeInTheDocument();
    });

    it('confirms and calls onConfirmed with created batch', async () => {
        vi.mocked(api.post).mockResolvedValueOnce({ data: mockPreview });
        const mockCreatedBatch: BatchDetailResponse = {
            batchId: 99,
            planId: 101,
            planName: 'Sprint 42 Plan',
            projectId: 1,
            batchType: 'ALL_ELIGIBLE',
            status: 'QUEUED',
            totalCases: 2,
            counts: { queued: 2, running: 0, passed: 0, failed: 0, infrastructureError: 0, cancelled: 0 },
            percentComplete: 0,
            isFinished: false,
            createdAt: '2026-11-07T08:00:00Z',
            finishedAt: null,
            createdBy: 'tester',
            cancelledAt: null,
            cancelledBy: null,
            jobs: [],
        };
        vi.mocked(api.post).mockResolvedValueOnce({ data: mockCreatedBatch });

        const handleConfirmed = vi.fn();
        await act(async () => {
            render(
                <AutomationBatchPreviewDialog
                    open={true}
                    planId={101}
                    planName="Sprint 42 Plan"
                    scope="ALL_ELIGIBLE"
                    onClose={vi.fn()}
                    onConfirmed={handleConfirmed}
                />
            );
        });

        await waitFor(() => {
            expect(screen.getByText('Confirm & Run 2 Cases')).toBeInTheDocument();
        });

        await act(async () => {
            fireEvent.click(screen.getByText('Confirm & Run 2 Cases'));
        });

        await waitFor(() => {
            expect(handleConfirmed).toHaveBeenCalledWith(mockCreatedBatch);
        });
    });
});

describe('Batch preview request lifecycle', () => {
    const ready: BatchPreviewResponse={planId:1,planName:'Plan',planStatus:'IN_PROGRESS',scope:'SELECTED',canExecute:true,
        totalInScope:1,readyCount:1,readyCases:[{caseId:1,caseName:'Case',testType:'KEYWORD_DRIVEN',assignee:'QA',stepCount:1}],
        manualSkippedCases:[],invalidConfigCases:[],alreadyPendingCases:[],unauthorizedCases:[]};
    it('allows a second submission after closing and reopening the same preview', async () => {
        const confirmed=vi.fn(); const ids=[1]; const close=vi.fn();
        vi.mocked(api.post).mockResolvedValueOnce({data:ready});
        const props={planId:1,planName:'Plan',scope:'SELECTED' as const,selectedCaseIds:ids,onClose:close,onConfirmed:confirmed};
        const {rerender}=render(<AutomationBatchPreviewDialog {...props} open />);
        await screen.findByText('Confirm & Run 1 Cases');
        vi.mocked(api.post).mockResolvedValueOnce({data:{batchId:1}});
        fireEvent.click(screen.getByText('Confirm & Run 1 Cases'));
        await waitFor(()=>expect(confirmed).toHaveBeenCalledTimes(1));
        rerender(<AutomationBatchPreviewDialog {...props} open={false} />);
        vi.mocked(api.post).mockResolvedValueOnce({data:ready});
        rerender(<AutomationBatchPreviewDialog {...props} open />);
        await waitFor(()=>expect(screen.getByText('Confirm & Run 1 Cases')).not.toBeDisabled());
    });
    it('disables confirmation when the eligible count exceeds the server limit', async () => {
        vi.mocked(api.post).mockResolvedValueOnce({data:{...ready,canExecute:false,readyCount:101,maxBatchSize:100}});
        render(<AutomationBatchPreviewDialog open planId={1} planName="Plan" scope="ALL_ELIGIBLE" onClose={vi.fn()} onConfirmed={vi.fn()} />);
        await screen.findByText('Select at most 100 ready cases for one batch.');
        expect(screen.getByRole('button',{name:/Confirm & Run/})).toBeDisabled();
    });
});

describe('AutomationBatchViewDialog', () => {
    const mockBatch: BatchDetailResponse = {
        batchId: 55,
        planId: 101,
        planName: 'Sprint 42 Plan',
        projectId: 1,
        batchType: 'ALL_ELIGIBLE',
        status: 'FAILED',
        canCancel: false,
        canRetryFailed: true,
        totalCases: 2,
        counts: { queued: 0, running: 0, passed: 1, failed: 1, infrastructureError: 0, cancelled: 0 },
        percentComplete: 100,
        isFinished: true,
        createdAt: '2026-11-07T08:00:00Z',
        finishedAt: '2026-11-07T08:01:00Z',
        createdBy: 'admin',
        cancelledAt: null,
        cancelledBy: null,
        jobs: [
            {
                jobId: 201,
                caseId: 1,
                caseName: 'Login Success',
                testType: 'CUCUMBER_AUTOMATION',
                status: 'COMPLETED',
                durationMs: 1420,
                errorMessage: null,
                stepSummary: 'All 3 steps passed',
                startedAt: '2026-11-07T08:00:05Z',
                finishedAt: '2026-11-07T08:00:06Z',
                assignedWorkerId: 'worker-1',
            },
            {
                jobId: 202,
                caseId: 2,
                caseName: 'Checkout Cart',
                testType: 'KEYWORD_DRIVEN',
                status: 'FAILED',
                durationMs: 2500,
                errorMessage: 'Locator #pay-btn not found',
                stepSummary: 'Failed at step 3: Click pay',
                startedAt: '2026-11-07T08:00:06Z',
                finishedAt: '2026-11-07T08:00:08Z',
                assignedWorkerId: 'worker-1',
            },
        ],
    };

    it('renders finished batch details with failure indicators and retry button', async () => {
        vi.mocked(api.get).mockResolvedValueOnce({ data: mockBatch });

        await act(async () => {
            render(
                <AutomationBatchViewDialog
                    open={true}
                    batchId={55}
                    planId={101}
                    onClose={vi.fn()}
                />
            );
        });

        await waitFor(() => {
            expect(screen.getByText('Batch Automation Run #55')).toBeInTheDocument();
            expect(screen.getByText('Login Success')).toBeInTheDocument();
            expect(screen.getByText('Checkout Cart')).toBeInTheDocument();
            expect(screen.getByText('Locator #pay-btn not found')).toBeInTheDocument();
            expect(screen.getByText('Run Failed Cases Again')).toBeInTheDocument();
        });
    });

    it('allows retrying failed cases', async () => {
        vi.mocked(api.get).mockResolvedValueOnce({ data: mockBatch });
        const mockNewBatch: BatchDetailResponse = {
            ...mockBatch,
            batchId: 56,
            status: 'QUEUED',
            canCancel: true,
            canRetryFailed: false,
            totalCases: 1,
            counts: { queued: 1, running: 0, passed: 0, failed: 0, infrastructureError: 0, cancelled: 0 },
            percentComplete: 0,
            isFinished: false,
            jobs: [
                {
                    jobId: 203,
                    caseId: 2,
                    caseName: 'Checkout Cart',
                    testType: 'KEYWORD_DRIVEN',
                    status: 'QUEUED',
                    durationMs: null,
                    errorMessage: null,
                    stepSummary: null,
                    startedAt: null,
                    finishedAt: null,
                    assignedWorkerId: null,
                },
            ],
        };
        vi.mocked(api.post).mockResolvedValueOnce({ data: mockNewBatch });

        await act(async () => {
            render(
                <AutomationBatchViewDialog
                    open={true}
                    batchId={55}
                    planId={101}
                    onClose={vi.fn()}
                />
            );
        });

        await waitFor(() => {
            expect(screen.getByText('Run Failed Cases Again')).toBeInTheDocument();
        });

        await act(async () => {
            fireEvent.click(screen.getByText('Run Failed Cases Again'));
        });

        await waitFor(() => {
            expect(api.post).toHaveBeenCalledWith('/automation/batches/55/retry-failed');
            expect(screen.getByText('Batch Automation Run #56')).toBeInTheDocument();
        });
    });
    it('opens the exact historical job with its run-specific evidence', async () => {
        vi.mocked(api.get).mockResolvedValueOnce({data:mockBatch});
        render(<AutomationBatchViewDialog open batchId={55} planId={101} onClose={vi.fn()} />);
        await screen.findByText('Checkout Cart');
        vi.mocked(api.get).mockResolvedValueOnce({data:{jobId:202,planId:101,caseId:2,status:'FAILED',testType:'KEYWORD_DRIVEN',createdAt:'2026-01-01',
            stepResults:[],diagnosticLogs:'Historical attempt log',failureEvidenceId:10}});
        fireEvent.click(screen.getAllByTitle('View case execution details')[1]);
        await screen.findByText('Historical attempt log');
        expect(api.get).toHaveBeenCalledWith('/automation/jobs/202');
        expect(api.get).not.toHaveBeenCalledWith('/automation/jobs/plan/101/case/2/latest');
        expect(screen.queryByText('Run Again')).not.toBeInTheDocument();
        expect(screen.getByText('Download failure evidence')).toBeInTheDocument();
    });

    it('ignores a late response from a previously opened batch', async () => {
        let resolveOld!: (value: unknown) => void;
        vi.mocked(api.get).mockImplementationOnce(() => new Promise(resolve => {resolveOld=resolve;}) as any);
        const {rerender}=render(<AutomationBatchViewDialog open batchId={54} planId={101} onClose={vi.fn()} />);
        vi.mocked(api.get).mockResolvedValueOnce({data:mockBatch});
        rerender(<AutomationBatchViewDialog open batchId={55} planId={101} onClose={vi.fn()} />);
        await screen.findByText('Checkout Cart');
        await act(async()=>resolveOld({data:{...mockBatch,batchId:54,planName:'Stale plan'}}));
        expect(screen.getByText('Batch Automation Run #55')).toBeInTheDocument();
        expect(screen.queryByText(/Stale plan/)).not.toBeInTheDocument();
    });

    it('notifies the workspace when retry creates a new batch and does not retry cancelled-only batches', async () => {
        vi.mocked(api.get).mockResolvedValueOnce({data:mockBatch});
        const changed=vi.fn();
        render(<AutomationBatchViewDialog open batchId={55} planId={101} onClose={vi.fn()} onBatchChanged={changed} />);
        await screen.findByText('Run Failed Cases Again');
        const retry={...mockBatch,batchId:56,status:'QUEUED',isFinished:false,canRetryFailed:false};
        vi.mocked(api.post).mockResolvedValueOnce({data:retry});
        fireEvent.click(screen.getByText('Run Failed Cases Again'));
        await waitFor(()=>expect(changed).toHaveBeenCalledWith(retry));
    });

});
