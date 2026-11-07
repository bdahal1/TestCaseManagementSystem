export type AutomationTemplate =
    | 'OPEN_PAGE'
    | 'ENTER_TEXT'
    | 'CLICK_ELEMENT'
    | 'VERIFY_TEXT_EQUALS'
    | 'VERIFY_TEXT_CONTAINS'
    | 'VERIFY_ELEMENT_STATE';

export type LocatorType = 'ID' | 'CSS' | 'XPATH' | 'ROLE_NAME' | 'TEST_ID' | 'NAMED';
export type ElementState = 'VISIBLE' | 'HIDDEN';
export type JobStatus = 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'INFRASTRUCTURE_ERROR' | 'CANCELLED';

export interface AutomationStepModel {
    order: number;
    keyword?: string; // Given, When, Then, And, But
    template: AutomationTemplate;
    locatorType?: LocatorType;
    locatorValue?: string;
    textValue?: string;
    stateValue?: ElementState;
}

export interface ProjectLocator {
    id?: number;
    locatorName: string;
    locatorType: LocatorType;
    locatorValue: string;
    description?: string;
}

export interface ProjectVariable {
    id?: number;
    variableKey: string;
    variableValue?: string;
    isSecret?: boolean;
    description?: string;
}

export interface StepResult {
    stepOrder: number;
    keyword?: string;
    description: string;
    passed: boolean;
    durationMs: number;
    errorMessage?: string;
    evidenceId?: number;
}

export interface AutomationJob {
    jobId: number;
    planId: number;
    caseId: number;
    status: JobStatus;
    testType: string;
    createdAt: string;
    startedAt?: string;
    finishedAt?: string;
    errorMessage?: string;
    stepResults: StepResult[];
    diagnosticLogs?: string;
}

export const TEMPLATE_LABELS: Record<AutomationTemplate, string> = {
    OPEN_PAGE: 'Open page',
    ENTER_TEXT: 'Enter text',
    CLICK_ELEMENT: 'Click element',
    VERIFY_TEXT_EQUALS: 'Verify text equals',
    VERIFY_TEXT_CONTAINS: 'Verify text contains',
    VERIFY_ELEMENT_STATE: 'Verify element visible or hidden',
};

export const CUCUMBER_KEYWORDS = ['Given', 'When', 'Then', 'And', 'But'] as const;
