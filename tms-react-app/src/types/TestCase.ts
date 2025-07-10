// types/TestCase.ts
export type ResultStatus = 'PASS' | 'FAIL' | 'SKIPPED' | 'BLOCKED' | 'NOT_RUN';

export interface TestStep {
    testStepDesc: string;
    testExpectedOutput: string;
    testStepData: string;
    stepOrder: number;
}

export interface TestCase {
    id: number;
    testName: string;
    testSteps: TestStep[];
    resultStatus?: ResultStatus | null;
    resultComment?: string;
}
export interface TestExecution {
    id: number;
    executionName: string;
}