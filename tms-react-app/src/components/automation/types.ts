export type AutomationTemplate =
    | 'OPEN_PAGE'
    | 'ENTER_TEXT'
    | 'CLICK_ELEMENT'
    | 'VERIFY_TEXT_EQUALS'
    | 'VERIFY_TEXT_CONTAINS'
    | 'VERIFY_ELEMENT_STATE'
    | 'SELECT_OPTION'
    | 'SET_CHECKBOX'
    | 'PRESS_KEY'
    | 'VERIFY_URL'
    | 'VERIFY_ATTRIBUTE';

export type LocatorType = 'ID' | 'CSS' | 'XPATH' | 'ROLE_NAME' | 'TEST_ID' | 'NAMED';
export type ElementState = 'VISIBLE' | 'HIDDEN';
export type OptionSelectBy = 'LABEL' | 'VALUE';
export type UrlComparison = 'EQUALS' | 'CONTAINS';
export type JobStatus = 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'INFRASTRUCTURE_ERROR' | 'CANCELLED';

export const SUPPORTED_KEY_OPTIONS = [
    'Enter', 'Tab', 'Escape', 'ArrowDown', 'ArrowUp', 'ArrowLeft', 'ArrowRight',
    'Backspace', 'Delete', 'Space', 'Home', 'End', 'PageUp', 'PageDown'
] as const;

export interface AutomationStepModel {
    order: number;
    keyword?: string; // Given, When, Then, And, But
    template: AutomationTemplate;
    locatorType?: LocatorType;
    locatorValue?: string;
    textValue?: string;
    stateValue?: ElementState;
    selectBy?: OptionSelectBy;
    checkedValue?: boolean;
    keyValue?: string;
    urlComparison?: UrlComparison;
    attributeName?: string;
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
    failureEvidenceId?: number | null;
}

export interface AutomationLivePreview {
    jobId: number;
    sequence: number;
    updatedAt: string;
    screenshot?: string | null;
    progress: {
        currentStepIndex: number;
        steps: Array<{
            stepOrder: number;
            keyword?: string;
            description: string;
            status: 'PENDING' | 'RUNNING' | 'PASSED' | 'FAILED';
            durationMs?: number;
            errorMessage?: string;
        }>;
    };
}

export const TEMPLATE_LABELS: Record<AutomationTemplate, string> = {
    OPEN_PAGE: 'Open page',
    ENTER_TEXT: 'Enter text',
    CLICK_ELEMENT: 'Click element',
    VERIFY_TEXT_EQUALS: 'Verify text equals',
    VERIFY_TEXT_CONTAINS: 'Verify text contains',
    VERIFY_ELEMENT_STATE: 'Verify element visible or hidden',
    SELECT_OPTION: 'Select option',
    SET_CHECKBOX: 'Set checkbox',
    PRESS_KEY: 'Press key',
    VERIFY_URL: 'Verify URL',
    VERIFY_ATTRIBUTE: 'Verify attribute',
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
    selectBy?: OptionSelectBy;
    checkedValue?: boolean;
    keyValue?: string;
    urlComparison?: UrlComparison;
    attributeName?: string;
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

export type BatchStatus = 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

export interface BatchCasePreviewItem {
    caseId: number;
    caseName: string;
    testType: string;
    assignee: string;
    stepCount: number;
}

export interface BatchExcludedCaseItem {
    caseId: number;
    caseName: string;
    testType: string;
    category: 'MANUAL' | 'INVALID_CONFIG' | 'ALREADY_PENDING' | 'UNAUTHORIZED';
    reason: string;
}

export interface BatchPreviewResponse {
    planId: number;
    planName: string;
    planStatus: string;
    scope: 'ALL_ELIGIBLE' | 'SELECTED';
    canExecute: boolean;
    maxBatchSize?: number;
    totalInScope: number;
    readyCount: number;
    readyCases: BatchCasePreviewItem[];
    manualSkippedCases: BatchExcludedCaseItem[];
    invalidConfigCases: BatchExcludedCaseItem[];
    alreadyPendingCases: BatchExcludedCaseItem[];
    unauthorizedCases: BatchExcludedCaseItem[];
}

export interface BatchJobSummary {
    jobId: number;
    caseId: number;
    caseName: string;
    testType: string;
    status: JobStatus;
    durationMs: number | null;
    errorMessage: string | null;
    stepSummary: string | null;
    startedAt: string | null;
    finishedAt: string | null;
    assignedWorkerId: string | null;
    canCancel?: boolean;
}

export interface BatchDetailResponse {
    batchId: number;
    planId: number;
    planName: string;
    projectId: number;
    batchType: string;
    status: BatchStatus;
    totalCases: number;
    counts: {
        queued: number;
        running: number;
        passed: number;
        failed: number;
        infrastructureError: number;
        cancelled: number;
    };
    percentComplete: number;
    isFinished: boolean;
    canCancel?: boolean;
    canRetryFailed?: boolean;
    createdAt: string;
    finishedAt: string | null;
    createdBy: string | null;
    cancelledAt: string | null;
    cancelledBy: string | null;
    jobs: BatchJobSummary[];
}

export interface HistoryJobRow {
    jobId: number;
    planId: number;
    planName: string;
    batchId: number | null;
    caseId: number;
    caseName: string;
    status: JobStatus;
    testType: string;
    createdAt: string;
    startedAt: string | null;
    finishedAt: string | null;
    durationMs: number | null;
    errorMessage: string | null;
    hasStepResults: boolean;
    hasDiagnosticLogs: boolean;
    failureEvidenceId: number | null;
    createdBy: string | null;
}

export interface HistoryPageResponse {
    content: HistoryJobRow[];
    totalElements: number;
    page: number;
    size: number;
    totalPages: number;
}

export interface AttemptSummary {
    jobId: number;
    attemptNumber: number;
    status: JobStatus;
    createdAt: string;
    finishedAt: string | null;
    durationMs: number | null;
}

export interface StepCompareResult {
    stepOrder: number;
    description: string;
    passedA: boolean | null;
    durationMsA: number | null;
    errorMessageA: string | null;
    onlyInA: boolean;
    passedB: boolean | null;
    durationMsB: number | null;
    errorMessageB: string | null;
    onlyInB: boolean;
}

export interface CompareAttemptResponse {
    jobIdA: number;
    jobIdB: number;
    planId: number;
    caseId: number;
    caseName: string;
    statusA: string;
    statusB: string;
    durationMsA: number | null;
    durationMsB: number | null;
    steps: StepCompareResult[];
    hasChangedSteps: boolean;
}

