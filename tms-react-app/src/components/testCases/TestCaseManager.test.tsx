import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import TestCaseManager from './TestCaseManager';
import api from '../../services/api';
vi.mock('../../context/AuthContext', () => ({ useAuth: () => ({ user: { userId: '7', roleList: ['ROLE_QAEngineer'] } }) }));
vi.mock('../../services/api', () => ({ default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() } }));
vi.mock('../gherkin/GherkinEditor', () => ({ default: ({ value, onChange }: { value: string; onChange: (value: string) => void }) => <textarea aria-label="Gherkin scenario editor" value={value} onChange={event => onChange(event.target.value)} /> }));
const scenario = 'Scenario: Sign in\nGiven I am signed out\nWhen I sign in\nThen I see my dashboard';
afterEach(() => { cleanup(); vi.restoreAllMocks(); });
beforeEach(() => {
    vi.resetAllMocks();
    vi.mocked(api.get).mockImplementation(async url => ({ data: url === '/project/id/2' ? { projects: [{ id: 2, projectName: 'Atlas' }] } : url === '/tags' ? { tags: [] } : url === '/testCase/gherkin-suggestions' ? [] : { testCase: [] } }));
});
async function openCucumberForm() {
    const user = userEvent.setup(); render(<TestCaseManager projId={2} />);
    await user.click(await screen.findByRole('button', { name: 'Add Test Case' }));
    await user.click(screen.getByRole('textbox', { name: 'Test Case Title' })); await user.paste('Sign in');
    await user.click(screen.getByRole('combobox', { name: 'Test Type' }));
    await user.click(screen.getByRole('option', { name: 'Cucumber Manual' }));
    await user.click(await screen.findByRole('textbox', { name: 'Gherkin scenario editor' })); await user.paste(scenario);
    return user;
}
describe('Cucumber Manual authoring', () => {
    it('sends structured details and custom fields with the scenario', async () => {
        vi.mocked(api.post).mockResolvedValue({ data: { id: 10 } });
        const user = await openCucumberForm();
        await user.click(screen.getByRole('button', { name: /Case details/ }));
        await user.click(screen.getByRole('combobox', { name: 'Priority' })); await user.click(screen.getByRole('option', { name: 'Critical' }));
        await user.click(screen.getByLabelText('Component')); await user.paste('Authentication');
        await user.click(screen.getByLabelText('Preconditions')); await user.paste('An active account');
        await user.click(screen.getByRole('button', { name: 'Add custom field' }));
        await user.click(screen.getByLabelText('Field 1 name')); await user.paste('Requirement');
        await user.click(screen.getByLabelText('Field 1 value')); await user.paste('AUTH-12');
        await user.click(screen.getByRole('button', { name: 'Create Case' }));
        await waitFor(() => expect(api.post).toHaveBeenCalledWith('/testCase', expect.objectContaining({ priority: 'CRITICAL', component: 'Authentication', preconditions: 'An active account', customFields: { Requirement: 'AUTH-12' }, gherkin: scenario })));
    });
    it('saves metadata and the scenario together without a separate expected-result payload', async () => {
        vi.mocked(api.post).mockResolvedValue({ data: { id: 10 } });
        const user = await openCucumberForm();
        await user.click(screen.getByRole('button', { name: 'Create Case' }));
        await waitFor(() => expect(api.post).toHaveBeenCalledWith('/testCase', expect.objectContaining({ testName: 'Sign in', projectId: 2, testType: 'CUCUMBER_MANUAL', gherkin: scenario })));
        expect(api.post).toHaveBeenCalledTimes(1);
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    });
    it('preserves the title and scenario when switching between case types', async () => {
        const user = await openCucumberForm();
        await user.click(screen.getByRole('combobox', { name: 'Test Type' })); await user.click(screen.getByRole('option', { name: /^Manual$/ }));
        await user.click(screen.getByRole('combobox', { name: 'Test Type' })); await user.click(screen.getByRole('option', { name: 'Cucumber Manual' }));
        expect(await screen.findByRole('textbox', { name: 'Gherkin scenario editor' })).toHaveValue(scenario);
        expect(screen.getByRole('textbox', { name: 'Test Case Title' })).toHaveValue('Sign in');
    });
    it('keeps a failed save open so the user does not lose the scenario', async () => {
        vi.spyOn(console, 'error').mockImplementation(() => undefined);
        vi.mocked(api.post).mockRejectedValue({ response: { data: { description: 'Could not save this scenario.' } } });
        const user = await openCucumberForm();
        await user.click(screen.getByRole('button', { name: 'Create Case' }));
        expect(await screen.findByText('Could not save this scenario.')).toBeInTheDocument();
        expect(screen.getByRole('textbox', { name: 'Gherkin scenario editor' })).toHaveValue(scenario);
        expect(screen.getByRole('button', { name: 'Create Case' })).toBeEnabled();
    });
});
