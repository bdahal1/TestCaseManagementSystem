import type { AutomationState, Priority } from '../testCases/caseDetails';
import type { PlanStatus, ResultStatus } from '../testPlans/types';
export type ResultCounts = Record<ResultStatus, number>;
export interface ReportCase {
    id: number;
    key: string;
    name: string;
    priority: Priority;
    automationStatus: AutomationState;
    component: string | null;
    owner: string;
    estimatedMinutes: number | null;
    result: ResultStatus;
    planId: number | null;
    planName: string | null;
    recordedAt: string | null;
    defectUrl: string | null;
    definitionChanged: boolean;
}
export interface ReportPlan {
    id: number;
    name: string;
    release: string | null;
    build: string | null;
    environment: string | null;
    status: PlanStatus;
    total: number;
    completed: number;
    passRate: number | null;
    unassigned: number;
    results: ResultCounts;
}
export interface ProjectReport {
    generatedAt: string;
    from: string;
    to: string;
    environments: string[];
    releases: string[];
    library: {
        total: number;
        priorities: Record<Priority, number>;
        automation: Record<AutomationState, number>;
        types: Record<string, number>;
        unowned: number;
        estimatedMinutes: number;
        estimatesDefined: number;
    };
    execution: {
        activePlans: number;
        closedPlans: number;
        draftPlans: number;
        total: number;
        completed: number;
        passRate: number | null;
        completionRate: number | null;
        testedCases: number;
        neverRunCases: number;
        linkedDefects: number;
        results: ResultCounts;
    };
    plans: ReportPlan[];
    activity: { date: string; results: ResultCounts }[];
    cases: ReportCase[];
    workload: {
        tester: string;
        total: number;
        remaining: number;
        blocked: number;
        estimatedRemainingMinutes: number;
        missingEstimates: number;
    }[];
}
export const outcomes: ResultStatus[] = ['PASS', 'FAIL', 'BLOCKED', 'SKIPPED', 'NOT_RUN'];
export const outcomeColors: Record<ResultStatus, string> = {
    PASS: '#059669',
    FAIL: '#dc2626',
    BLOCKED: '#d97706',
    SKIPPED: '#0284c7',
    NOT_RUN: '#94a3b8',
};
export const percent = (value: number | null | undefined) => (value == null ? '—' : `${value}%`);
export const utcDate = (value: string | null) =>
    value ? new Date(value).toLocaleString(undefined, { timeZone: 'UTC' }) + ' UTC' : 'No recorded outcome';
export function csvContent(rows: unknown[][]): string {
    // Quoting alone does not prevent spreadsheet formula execution.
    const cell = (value: unknown) => {
        let text = value == null ? '' : String(value);
        if (/^[\s]*[=+@-]/.test(text) || /^[\t\r\n]/.test(text)) text = `'${text}`;
        return `"${text.replace(/"/g, '""')}"`;
    };
    return '\uFEFF' + rows.map((row) => row.map(cell).join(',')).join('\r\n');
}
export function downloadCsv(name: string, rows: unknown[][]) {
    const url = URL.createObjectURL(new Blob([csvContent(rows)], { type: 'text/csv;charset=utf-8;' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = name;
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
}
