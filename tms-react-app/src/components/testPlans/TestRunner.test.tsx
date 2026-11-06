import { useState } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import TestRunner from './TestRunner';
import api from '../../services/api';
import type { PlanCase, PlanDetail } from './types';
vi.mock('../../services/api', () => ({ default: { post: vi.fn(), get: vi.fn(), delete: vi.fn() } }));
const testCase = (id: number): PlanCase => ({ id, name: `Case ${id}`, type: 'MANUAL', status: 'NOT_RUN', comment: null, actualResult: null, defectUrl: null, revision: 0,
    assignee: { id: 7, name: 'Alex QA' }, recordedAt: null, recordedBy: null, canRecord: true,
    steps: [{ order: 1, instruction: 'Sign in', data: 'Test account', expected: 'Dashboard opens' }], evidence: [], history: [] });
const fixture = (): PlanDetail => ({ plan: { id: 10, projectId: 2, name: 'Release regression', release: '2.4', build: 'rc.1', environment: 'Staging', dueDate: '', description: '', status: 'IN_PROGRESS', total: 2, completed: 0, assignedToMe: 2, unassigned: 0, results: { PASS: 0, FAIL: 0, SKIPPED: 0, BLOCKED: 0, NOT_RUN: 2 } }, cases: [testCase(1), testCase(2)] });
afterEach(() => cleanup());
beforeEach(() => vi.resetAllMocks());
describe('guided test runner', () => {
    it('saves the result with its revision and advances to the next pending case', async () => {
        const user = userEvent.setup();
        vi.mocked(api.post).mockResolvedValue({ data: {} });
        function Harness() {
            const [detail, setDetail] = useState(fixture());
            const refresh = async () => {
                const updated: PlanDetail = { ...detail, cases: detail.cases.map(tc => tc.id === 1 ? { ...tc, status: 'PASS', actualResult: 'Dashboard visible', revision: 1 } : tc) };
                setDetail(updated); return updated;
            };
            return <TestRunner open detail={detail} onClose={() => undefined} onChanged={refresh} />;
        }
        render(<Harness />);
        await user.click(screen.getByRole('combobox', { name: 'Result' }));
        await user.click(screen.getByRole('option', { name: 'Pass' }));
        await user.click(screen.getByLabelText('Actual result'));
        await user.paste('Dashboard visible');
        await user.click(screen.getByRole('button', { name: 'Save & next' }));
        await waitFor(() => expect(api.post).toHaveBeenCalledWith('/testPlans/10/cases/1/results', expect.objectContaining({ status: 'PASS', actualResult: 'Dashboard visible', expectedRevision: 0 })));
        expect(await screen.findByRole('heading', { name: 'Case 2' })).toBeInTheDocument();
    });
    it('keeps the draft and current case when a stale result is rejected', async () => {
        const user = userEvent.setup();
        vi.mocked(api.post).mockRejectedValue({ response: { data: { description: 'This result changed. Reload before saving your result.' } } });
        render(<TestRunner open detail={fixture()} onClose={() => undefined} onChanged={vi.fn()} />);
        await user.click(screen.getByLabelText('Actual result'));
        await user.paste('My unsaved observation');
        await user.click(screen.getByRole('button', { name: 'Save & next' }));
        expect(await screen.findByText('This result changed. Reload before saving your result.')).toBeInTheDocument();
        expect(screen.getByLabelText('Actual result')).toHaveValue('My unsaved observation');
        expect(screen.getByRole('heading', { name: 'Case 1' })).toBeInTheDocument();
    });
    it('requires an explicit discard when closing an unsaved result', async () => {
        const user = userEvent.setup(); const close = vi.fn();
        render(<TestRunner open detail={fixture()} onClose={close} onChanged={vi.fn()} />);
        await user.click(screen.getByLabelText('Notes or reason'));
        await user.paste('Investigating');
        await user.click(screen.getByRole('button', { name: 'Close runner' }));
        expect(close).not.toHaveBeenCalled();
        await user.click(screen.getByRole('button', { name: 'Discard & close' }));
        expect(close).toHaveBeenCalledOnce();
    });
    it('shows read-only results for cases the tester cannot record', () => {
        const detail = fixture(); detail.cases.forEach(tc => { tc.canRecord = false; });
        render(<TestRunner open detail={detail} onClose={() => undefined} onChanged={vi.fn()} />);
        expect(screen.getByLabelText('Actual result')).toBeDisabled();
        expect(screen.queryByRole('button', { name: 'Save & next' })).not.toBeInTheDocument();
    });
    it('preserves a result draft while an evidence upload refreshes the case', async () => {
        const user = userEvent.setup(); vi.mocked(api.post).mockResolvedValue({ data: {} });
        function Harness() {
            const [detail, setDetail] = useState(fixture());
            return <TestRunner open detail={detail} onClose={() => undefined} onChanged={async () => {
                const updated = { ...detail, cases: detail.cases.map(tc => tc.id === 1 ? { ...tc, evidence: [{ id: 5, name: 'failure.png', contentType: 'image/png', size: 20, uploadedBy: 'Alex QA', uploadedAt: '2026-11-05T10:00:00Z' }] } : tc) };
                setDetail(updated); return updated;
            }} />;
        }
        render(<Harness />);
        await user.click(screen.getByLabelText('Actual result'));
        await user.paste('Failure details being drafted');
        const input = screen.getByLabelText('Attach evidence file') as HTMLInputElement;
        await user.upload(input, new File(['png'], 'failure.png', { type: 'image/png' }));
        await screen.findByRole('button', { name: /^failure\.png/ });
        expect(screen.getByLabelText('Actual result')).toHaveValue('Failure details being drafted');
        expect(api.post).toHaveBeenCalledWith('/testPlans/10/cases/1/evidence', expect.any(FormData));
    });
    it('renders Cucumber Manual as a scenario without separate expected-result fields', () => {
        const detail = fixture();
        detail.cases[0].type = 'CUCUMBER_MANUAL';
        detail.cases[0].steps = [{ order: 1, instruction: 'Given I am signed in\nWhen I open my account\nThen I see my details', data: 'Legacy data', expected: 'Legacy expected output' }];
        render(<TestRunner open detail={detail} onClose={() => undefined} onChanged={vi.fn()} />);
        expect(screen.getByRole('heading', { name: 'Behavior scenario' })).toBeInTheDocument();
        expect(screen.getByText('Then')).toBeInTheDocument();
        expect(screen.queryByText('Expected:')).not.toBeInTheDocument();
        expect(screen.queryByText('Legacy expected output')).not.toBeInTheDocument();
        expect(screen.getByLabelText('Actual result')).toBeInTheDocument();
    });
    it('keeps expected results visible for conventional manual cases', () => {
        render(<TestRunner open detail={fixture()} onClose={() => undefined} onChanged={vi.fn()} />);
        expect(screen.getByText('Expected:')).toBeInTheDocument();
    });
});
