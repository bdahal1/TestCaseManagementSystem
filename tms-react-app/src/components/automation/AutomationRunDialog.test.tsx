import { act, cleanup, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import AutomationRunDialog from './AutomationRunDialog';
import api from '../../services/api';
import { type AutomationJob, type AutomationLivePreview } from './types';

vi.mock('../../services/api', () => ({ default: { get: vi.fn(), post: vi.fn() } }));
beforeEach(() => vi.mocked(api.get).mockReset());
afterEach(() => { cleanup(); vi.useRealTimers(); });

const job = (id = 11): AutomationJob => ({
    jobId: id, planId: 1, caseId: 2, status: 'RUNNING', testType: 'CUCUMBER_AUTOMATION', createdAt: '2026-11-07T00:00:00Z', stepResults: [],
});
const preview = (id = 11): AutomationLivePreview => ({
    jobId: id, sequence: 100, updatedAt: '2026-11-07T00:00:00Z', screenshot: `frame-${id}`,
    progress: { currentStepIndex: 1, steps: [
        { stepOrder: 1, description: 'Given open page', status: 'PASSED' },
        { stepOrder: 2, description: `When click button for job ${id}`, status: 'RUNNING' },
        { stepOrder: 3, description: 'Then verify welcome', status: 'PENDING' },
    ] },
});
const props = { open: true, planId: 1, caseId: 2, caseName: 'Login', testType: 'CUCUMBER_AUTOMATION',
    jobId: 11, onClose: vi.fn(), onFinished: vi.fn().mockResolvedValue(undefined) };

it('shows browser images, current step, and completed and pending steps for the exact job', async () => {
    vi.mocked(api.get).mockImplementation(async path => ({ data: String(path).includes('/preview') ? preview() : job() }));
    render(<AutomationRunDialog {...props} />);
    await waitFor(() => expect(screen.getByText('Running step 2 of 3')).toBeInTheDocument());
    expect(screen.getByRole('img')).toHaveAttribute('src', 'data:image/jpeg;base64,frame-11');
    expect(screen.getByText('PASSED')).toBeInTheDocument();
    expect(screen.getByText('PENDING')).toBeInTheDocument();
    expect(api.get).toHaveBeenCalledWith('/automation/jobs/11/preview?afterSequence=0');
    expect(screen.queryByRole('button', { name: 'Run Again' })).not.toBeInTheDocument();
});

it('does not overlap requests and discards preview responses after closing', async () => {
    let resolve!: (value: { data: AutomationLivePreview }) => void;
    const deferred = new Promise<{ data: AutomationLivePreview }>(done => { resolve = done; });
    vi.mocked(api.get).mockImplementation(async path => {
        if (String(path).includes('/22')) return { data: String(path).includes('/preview') ? null : job(22) };
        return String(path).includes('/preview') ? deferred : { data: job() };
    });
    const view = render(<AutomationRunDialog {...props} />);
    await waitFor(() => expect(api.get).toHaveBeenCalledWith('/automation/jobs/11/preview?afterSequence=0'));
    vi.useFakeTimers();
    await act(async () => { await vi.advanceTimersByTimeAsync(5000); });
    expect(vi.mocked(api.get).mock.calls.filter(([path]) => String(path).includes('/preview'))).toHaveLength(1);
    view.rerender(<AutomationRunDialog {...props} open={false} />);
    await act(async () => { resolve({ data: preview() }); });
    view.rerender(<AutomationRunDialog {...props} open={true} jobId={22} />);
    await act(async () => {});
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
});

it('ignores old job images after switching to a different running job', async () => {
    let resolve!: (value: { data: AutomationLivePreview }) => void;
    const deferred = new Promise<{ data: AutomationLivePreview }>(done => { resolve = done; });
    vi.mocked(api.get).mockImplementation(async path => {
        const id = String(path).includes('/22') ? 22 : 11;
        return String(path).includes('/preview') ? id === 11 ? deferred : { data: preview(22) } : { data: job(id) };
    });
    const view = render(<AutomationRunDialog {...props} />);
    await waitFor(() => expect(api.get).toHaveBeenCalledWith('/automation/jobs/11/preview?afterSequence=0'));
    view.rerender(<AutomationRunDialog {...props} jobId={22} />);
    await waitFor(() => expect(screen.getByRole('img')).toHaveAttribute('src', 'data:image/jpeg;base64,frame-22'));
    await act(async () => { resolve({ data: preview(11) }); });
    expect(screen.getByRole('img')).toHaveAttribute('src', 'data:image/jpeg;base64,frame-22');
});

it('keeps test execution visible when preview transport fails', async () => {
    vi.mocked(api.get).mockImplementation(async path => {
        if (String(path).includes('/preview')) throw new Error('Offline');
        return { data: job() };
    });
    render(<AutomationRunDialog {...props} />);
    await waitFor(() => expect(screen.getByText('Preview temporarily unavailable. The test continues running.')).toBeInTheDocument());
    expect(screen.getByText('RUNNING')).toBeInTheDocument();
});

it('shows the last image for a fast completed run without calling it a running step', async () => {
    vi.mocked(api.get).mockImplementation(async path => ({ data: String(path).includes('/preview') ? preview() : { ...job(), status: 'COMPLETED' } }));
    render(<AutomationRunDialog {...props} />);
    await waitFor(() => expect(screen.getByRole('img')).toBeInTheDocument());
    expect(screen.getByText('Last preview')).toBeInTheDocument();
    expect(screen.queryByText('Running step 2 of 3')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Live step progress')).not.toBeInTheDocument();
});
