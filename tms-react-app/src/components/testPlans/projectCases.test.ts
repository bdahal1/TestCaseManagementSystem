import { expect, it, vi } from 'vitest';
import api from '../../services/api';
import { projectCases } from './projectCases';
vi.mock('../../services/api', () => ({ default: { get: vi.fn() } }));
it('loads all pages of cases for the plan picker', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({ data: { testCase: [{ id: 1 }], totalPages: 2 } }).mockResolvedValueOnce({ data: { testCase: [{ id: 2 }], totalPages: 2 } });
    expect(await projectCases(3)).toEqual([{ id: 1 }, { id: 2 }]);
    expect(api.get).toHaveBeenLastCalledWith('/testCase', { params: { projectId: 3, page: 1, size: 200 } });
});
