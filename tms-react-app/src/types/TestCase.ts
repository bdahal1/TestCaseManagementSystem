// types/TestCase.ts
export type ResultStatus = 'PASS' | 'FAIL' | 'SKIPPED' | 'BLOCKED' | 'NOT_RUN';

export interface TestCase {
    id: number;
    testName: string;
    resultStatus?: ResultStatus | null;
    resultComment?: string;
}
export interface TestExecution {
    id: number;
    executionName: string;
}