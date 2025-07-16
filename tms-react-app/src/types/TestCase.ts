// types/TestCase.ts
export type ResultStatus = 'PASS' | 'FAIL' | 'SKIPPED' | 'BLOCKED' | 'NOT_RUN';

export interface TestStep {
    testStepDesc: string
    testExpectedOutput: string
    testStepData: string
    stepOrder: number
    testType:number
}

export enum TestTypes {
    MANUAL = "MANUAL",
    CUCUMBER_MANUAL = "CUCUMBER_MANUAL",
    CUCUMBER_AUTOMATION = "CUCUMBER_AUTOMATION",
    KEYWORD_DRIVEN = "KEYWORD_DRIVEN"
}

export interface TestCase {
    id: number;
    testName: string;
    testSteps: TestStep[];
    resultStatus?: ResultStatus | null;
    resultComment?: string;
    testType?: TestTypes;
}
export interface TestExecution {
    id: number;
    executionName: string;
}