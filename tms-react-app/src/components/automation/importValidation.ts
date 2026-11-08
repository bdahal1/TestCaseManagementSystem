import type { ImportPreviewResponse, ImportStepPreview, LocatorType, ProjectLocator } from './types';
import { SUPPORTED_KEY_OPTIONS } from './types';

const variableReference = /\$\{[a-zA-Z0-9_.-]+\}/;

export function inferImportLocatorType(value: string | undefined, locators: ProjectLocator[]): LocatorType | undefined {
    const locator = value?.trim() || '';
    if (!locator) return undefined;
    if (locator.startsWith('id=')) return 'ID';
    if (locator.startsWith('css=')) return 'CSS';
    if (locator.startsWith('xpath=')) return 'XPATH';
    if (locator.startsWith('role=')) return 'ROLE_NAME';
    if (locator.startsWith('testid=')) return 'TEST_ID';
    if (locators.some((entry) => entry.locatorName === locator) || /^[A-Za-z0-9_]+\.[A-Za-z0-9_]+$/.test(locator)) return 'NAMED';
    if (locator.startsWith('//') || locator.startsWith('./') || locator.startsWith('(')) return 'XPATH';
    if (locator.startsWith('#') || locator.startsWith('.') || locator.startsWith('[')) return 'CSS';
    return undefined;
}

function stepError(step: ImportStepPreview, locators: ProjectLocator[]): string | undefined {
    if (!step.template) return 'Map this step to an automation template';
    if (step.locatorValue?.includes('[REDACTED]') || step.textValue?.includes('[REDACTED]')
        || step.keyValue?.includes('[REDACTED]') || step.attributeName?.includes('[REDACTED]'))
        return 'Replace masked values with a project variable reference such as ${PASSWORD}';

    if (step.template !== 'OPEN_PAGE' && step.template !== 'VERIFY_URL') {
        const locator = step.locatorValue?.trim();
        if (!locator) return 'Target locator is required';
        const type = step.locatorType ?? inferImportLocatorType(locator, locators);
        if (!variableReference.test(locator) && type === 'NAMED' && !locators.some((entry) => entry.locatorName === locator))
            return `Reusable named locator '${locator}' is not defined in this project`;
    }

    switch (step.template) {
        case 'OPEN_PAGE':
            if (!step.textValue?.trim()) return 'Page URL is required';
            break;
        case 'ENTER_TEXT':
        case 'VERIFY_TEXT_EQUALS':
        case 'VERIFY_TEXT_CONTAINS':
            if (!step.textValue?.trim()) return 'Text value is required';
            break;
        case 'VERIFY_ELEMENT_STATE':
            if (!step.stateValue) return 'Choose Visible or Hidden';
            break;
        case 'SELECT_OPTION':
            if (!step.textValue?.trim()) return 'Option label or value is required';
            break;
        case 'SET_CHECKBOX':
            if (step.checkedValue === undefined || step.checkedValue === null) return 'Choose Checked or Unchecked';
            break;
        case 'PRESS_KEY': {
            if (!step.keyValue?.trim()) return 'Key is required';
            const key = step.keyValue.trim();
            const isSupported = SUPPORTED_KEY_OPTIONS.some((k) => k.toLowerCase() === key.toLowerCase()) || key.length === 1;
            if (!variableReference.test(key) && !isSupported) return `Unsupported key '${key}'. Supported: ${SUPPORTED_KEY_OPTIONS.join(', ')}`;
            break;
        }
        case 'VERIFY_URL':
            if (!step.textValue?.trim()) return 'Expected URL is required';
            break;
        case 'VERIFY_ATTRIBUTE':
            if (!step.attributeName?.trim()) return 'Attribute name is required';
            if (step.textValue === undefined || step.textValue === null) return 'Expected attribute value is required';
            break;
        case 'CLICK_ELEMENT':
            break;
    }
    return undefined;
}

export function validateImportPreview(preview: ImportPreviewResponse, locators: ProjectLocator[]): ImportPreviewResponse {
    const cases = preview.cases.map((item) => {
        const validationError = !item.name.trim() ? 'Name cannot be empty'
            : item.name.trim().length > 255 ? 'Keep case names within 255 characters'
            : !['CUCUMBER_AUTOMATION', 'KEYWORD_DRIVEN'].includes(item.testType || '')
                ? 'Only CUCUMBER_AUTOMATION and KEYWORD_DRIVEN test types are supported'
                : item.steps.length === 0 ? 'Must contain at least one step' : undefined;
        const steps = item.steps.map((step) => ({ ...step, mapped: !!step.template, validationError: stepError(step, locators) }));
        return {
            ...item, validationError, steps,
            unmappedCount: steps.filter((step) => !step.mapped).length,
            errorCount: (validationError ? 1 : 0) + (item.unsupportedSyntax ? 1 : 0)
                + steps.reduce((count, step) => count + (step.validationError ? 1 : 0) + (step.unsupportedSyntax ? 1 : 0), 0),
        };
    });
    const hasErrors = preview.globalErrors.length > 0 || cases.length === 0 || cases.some((item) => item.errorCount > 0);
    return { ...preview, cases, hasErrors, canSave: !hasErrors };
}
