import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PlanEditor from './PlanEditor';
import api from '../../services/api';
vi.mock('../../services/api', () => ({ default: { get: vi.fn(), post: vi.fn(), put: vi.fn() } }));
afterEach(cleanup);
beforeEach(() => {
    vi.resetAllMocks();
    vi.mocked(api.get).mockImplementation(async (url) => ({ data: url === '/testCase' ? { testCase: [{ id: 3, testName: 'Sign in', testType: 'MANUAL' }], totalPages: 1 } : [{ id: 7, name: 'Alex QA' }] }));
});
describe('release plan creation', () => {
    it('creates a plan from release metadata, selected cases, and a tester', async () => {
        const user = userEvent.setup(); const saved = vi.fn();
        vi.mocked(api.post).mockResolvedValue({ data: { id: 11 } });
        render(<PlanEditor open projectId={2} onClose={vi.fn()} onSaved={saved} />);
        await user.type(screen.getByRole('textbox', { name: 'Plan name' }), 'Release 2.4 regression');
        await user.type(screen.getByLabelText('Release'), '2.4');
        await user.click(screen.getByRole('combobox', { name: 'Test cases' }));
        await user.click(await screen.findByRole('option', { name: 'Sign in' }));
        await user.keyboard('{Escape}');
        await user.click(screen.getByRole('combobox', { name: 'Assign selected cases to' }));
        await user.click(screen.getByRole('option', { name: 'Alex QA' }));
        await user.click(screen.getByRole('button', { name: 'Create draft plan' }));
        await waitFor(() => expect(saved).toHaveBeenCalledWith(11));
        expect(api.post).toHaveBeenCalledWith('/testPlans', expect.objectContaining({ projectId: 2, caseIds: [3], assigneeId: 7, metadata: expect.objectContaining({ name: 'Release 2.4 regression', release: '2.4', dueDate: null }) }));
    });
});
