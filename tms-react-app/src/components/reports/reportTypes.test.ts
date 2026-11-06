import { describe, expect, it } from 'vitest';
import { csvContent, percent } from './reportTypes';

describe('report exports', () => {
    it('escapes quotes and line breaks and neutralizes spreadsheet formulas', () => {
        const csv = csvContent([
            ['Name', 'Value'],
            ['Two "quotes"\nand a new line', '=HYPERLINK("bad")'],
            ['@SUM(1)', ' -2+3'],
            [null, 0],
        ]);
        expect(csv).toContain('"Two ""quotes""\nand a new line"');
        expect(csv).toContain('"\'=HYPERLINK(""bad"")"');
        expect(csv).toContain('"\'@SUM(1)"');
        expect(csv).toContain('"\' -2+3"');
        expect(csv).toContain('"","0"');
    });
    it('distinguishes missing rates from a measured zero percent', () => {
        expect(percent(null)).toBe('—');
        expect(percent(0)).toBe('0%');
        expect(percent(66.7)).toBe('66.7%');
    });
});
