import api from '../../services/api';
import type { AvailableCase } from './types';

// Fetch every page so larger project libraries are not silently truncated in the picker.
export async function projectCases(projectId: number): Promise<AvailableCase[]> {
    const all: AvailableCase[] = [];
    let page = 0;
    let pages = 1;
    while (page < pages) {
        const { data } = await api.get('/testCase', { params: { projectId, page, size: 200 } });
        const cases: AvailableCase[] = data.testCase || [];
        all.push(...cases);
        pages = data.totalPages || 0;
        if (!cases.length) break;
        page++;
    }
    return all;
}
