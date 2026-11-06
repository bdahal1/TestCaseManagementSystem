import { describe, expect, it } from 'vitest';
import { detailsDraft, detailsPayload } from './caseDetails';

describe('case detail drafts', () => {
    it('keeps legacy defaults and sends explicit clears for optional numeric fields', () => {
        const payload = detailsPayload(detailsDraft());
        expect(payload).toMatchObject({
            priority: 'MEDIUM',
            automationStatus: 'NOT_AUTOMATED',
            ownerId: 0,
            estimatedMinutes: 0,
            customFields: {},
        });
    });
    it('preserves custom values, trims names, and rejects ambiguous field names', () => {
        const draft = detailsDraft({ preconditions: 'Account exists', customFields: { ' Ticket ': 'AUTH-1' } });
        expect(detailsPayload(draft)).toMatchObject({
            preconditions: 'Account exists',
            customFields: { Ticket: 'AUTH-1' },
        });
        draft.customFields.push({ name: 'Ticket', value: 'AUTH-2' });
        expect(() => detailsPayload(draft)).toThrow('unique');
        draft.customFields = [{ name: '', value: 'Value' }];
        expect(() => detailsPayload(draft)).toThrow('name');
    });
    it('rejects invalid durations and safely retains arbitrary custom field names', () => {
        for (const estimatedMinutes of ['-1', '1.5', '10081', 'NaN'])
            expect(() => detailsPayload({ ...detailsDraft(), estimatedMinutes })).toThrow('duration');
        const payload = detailsPayload({
            ...detailsDraft(),
            customFields: [{ name: '__proto__', value: 'Plain text' }],
        });
        expect(JSON.parse(JSON.stringify(payload)).customFields.__proto__).toBe('Plain text');
    });
});
