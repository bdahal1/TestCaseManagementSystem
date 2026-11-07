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

export type ImportAction = 'CREATE_NEW' | 'REPLACE_EXISTING';

export interface ImportStepPreview {
    order: number;
    sourceLine?: number;
    keyword?: string;
    rawText?: string;
    template?: AutomationTemplate;
    locatorType?: LocatorType;
    locatorValue?: string;
    textValue?: string;
    stateValue?: ElementState;
    mapped: boolean;
    unsupportedSyntax?: string;
    validationError?: string;
}

export interface ImportCasePreview {
    name: string;
    testType?: string;
    sourceLine?: number;
    steps: ImportStepPreview[];
    conflict: boolean;
    existingCaseId?: number;
    existingCaseProjectId?: string;
    existingCaseName?: string;
    action: ImportAction;
    unsupportedSyntax?: string;
    validationError?: string;
    unmappedCount: number;
    errorCount: number;
}

export interface ImportPreviewResponse {
    fileName: string;
    fileType: 'FEATURE' | 'KEYWORD_JSON';
    cases: ImportCasePreview[];
    hasErrors: boolean;
    canSave: boolean;
    globalErrors: string[];
    totalCases: number;
    totalSteps: number;
}

export interface ImportCaseSaveItem {
    action: ImportAction;
    targetCaseId?: number;
    caseName: string;
    testType: 'CUCUMBER_AUTOMATION' | 'KEYWORD_DRIVEN';
    steps: AutomationStepModel[];
}

export interface ImportSaveRequest {
    cases: ImportCaseSaveItem[];
}

export interface ImportSaveResponse {
    createdCount: number;
    replacedCount: number;
    savedCases: Array<{
        id: number;
        testProjectId: string;
        name: string;
        testType: string;
        action: string;
    }>;
}
