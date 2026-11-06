export const priorities = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'] as const;
export const automationStates = ['NOT_AUTOMATED', 'PLANNED', 'AUTOMATED', 'NOT_APPLICABLE'] as const;
export type Priority = (typeof priorities)[number];
export type AutomationState = (typeof automationStates)[number];
export interface CaseDetails {
    priority?: Priority | null;
    automationStatus?: AutomationState | null;
    component?: string | null;
    ownerId?: number | null;
    estimatedMinutes?: number | null;
    description?: string | null;
    preconditions?: string | null;
    customFields?: Record<string, string> | null;
}
export interface DetailsDraft {
    priority: Priority;
    automationStatus: AutomationState;
    component: string;
    ownerId: number;
    estimatedMinutes: string;
    description: string;
    preconditions: string;
    customFields: { name: string; value: string }[];
}
export const detailLabel = (value: string) =>
    value
        .toLowerCase()
        .replace(/_/g, ' ')
        .replace(/^./, (c) => c.toUpperCase());
export const priorityColor = (priority: string): 'error' | 'warning' | 'info' | 'default' =>
    (({ CRITICAL: 'error', HIGH: 'warning', MEDIUM: 'info', LOW: 'default' }) as const)[priority as Priority] ??
    'default';
export function detailsDraft(details: CaseDetails = {}): DetailsDraft {
    return {
        priority: details.priority ?? 'MEDIUM',
        automationStatus: details.automationStatus ?? 'NOT_AUTOMATED',
        component: details.component ?? '',
        ownerId: details.ownerId ?? 0,
        estimatedMinutes: details.estimatedMinutes == null ? '' : String(details.estimatedMinutes),
        description: details.description ?? '',
        preconditions: details.preconditions ?? '',
        customFields: Object.entries(details.customFields ?? {}).map(([name, value]) => ({ name, value })),
    };
}
export function detailsPayload(draft: DetailsDraft) {
    const fields: Record<string, string> = {};
    if (
        draft.estimatedMinutes &&
        (!/^\d+$/.test(draft.estimatedMinutes) || +draft.estimatedMinutes < 1 || +draft.estimatedMinutes > 10080)
    )
        throw new Error('Estimated duration must be between 1 and 10,080 minutes, or left blank.');
    for (const field of draft.customFields) {
        const name = field.name.trim();
        if (!name) throw new Error('Give every custom field a name, or remove the empty field.');
        if (Object.prototype.hasOwnProperty.call(fields, name)) throw new Error('Custom field names must be unique.');
        Object.defineProperty(fields, name, { value: field.value, enumerable: true, configurable: true });
    }
    return {
        ...draft,
        component: draft.component.trim(),
        estimatedMinutes: draft.estimatedMinutes ? Number(draft.estimatedMinutes) : 0,
        customFields: fields,
    };
}
