export type ResultStatus = 'PASS' | 'FAIL' | 'BLOCKED' | 'SKIPPED' | 'NOT_RUN';
export type PlanStatus = 'DRAFT' | 'IN_PROGRESS' | 'CLOSED';
export interface Person { id: number; name: string }
export interface Metadata { name: string; release: string; build: string; environment: string; dueDate: string; description: string }
export interface PlanSummary extends Metadata {
    id: number; projectId: number; status: PlanStatus; total: number; completed: number; assignedToMe: number; unassigned: number;
    results: Record<ResultStatus, number>;
}
export interface Evidence { id: number; name: string; contentType: string; size: number; uploadedAt: string; uploadedBy: string }
export interface Attempt { id: number; status: ResultStatus; comment: string | null; actualResult: string | null; defectUrl: string | null; recordedAt: string; recordedBy: string }
export interface PlanCase {
    details?: import('../testCases/caseDetails').CaseDetails | null;
    id: number; name: string; type: string; status: ResultStatus; comment: string | null; actualResult: string | null; defectUrl: string | null;
    revision: number; assignee: Person | null; recordedAt: string | null; recordedBy: string | null; canRecord: boolean;
    steps: { order: number; instruction: string; data: string | null; expected: string }[];
    evidence: Evidence[]; history: Attempt[];
}
export interface PlanDetail { plan: PlanSummary; cases: PlanCase[] }
export interface AvailableCase { id: number; testName: string; testType: string }
export const emptyMetadata: Metadata = { name: '', release: '', build: '', environment: '', dueDate: '', description: '' };
export const statusLabel = (status: PlanStatus) => ({ DRAFT: 'Draft', IN_PROGRESS: 'In progress', CLOSED: 'Closed' }[status]);
export const resultColor = (status: ResultStatus): 'success' | 'error' | 'warning' | 'default' | 'info' =>
    ({ PASS: 'success', FAIL: 'error', BLOCKED: 'warning', SKIPPED: 'info', NOT_RUN: 'default' } as const)[status];
export const resultLabel = (status: ResultStatus) => status === 'NOT_RUN' ? 'Not run' : status[0] + status.slice(1).toLowerCase();
export function errorMessage(error: unknown, fallback: string): string {
    const response = (error as { response?: { data?: { description?: string; message?: string } } })?.response?.data;
    return response?.description || response?.message || fallback;
}
export const metadataBody = (metadata: Metadata) => ({ ...metadata, dueDate: metadata.dueDate || null });
