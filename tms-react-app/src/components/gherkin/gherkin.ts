export const stepKeywords = ['Given', 'When', 'Then', 'And', 'But', '*'] as const;
export const keywordColors: Record<string, string> = { Given: '#047857', When: '#1d4ed8', Then: '#7c3aed', And: '#b45309', But: '#b45309', '*': '#b45309' };
export function preferredKeyword(before: string): string {
    const segments = before.split(/^\s*(?:Scenario(?: Outline)?|Example):.*$/m);
    const scenario = segments[segments.length - 1] ?? before;
    const keywords = [...scenario.matchAll(/^\s*(Given|When|Then)\b/gm)];
    const last = keywords[keywords.length - 1]?.[1];
    return last === 'Given' ? 'When' : last === 'When' ? 'Then' : last === 'Then' ? 'And' : 'Given';
}
export function splitTableRow(line: string): string[] {
    let value = line.trim();
    if (value.startsWith('|')) value = value.slice(1);
    if (value.endsWith('|')) value = value.slice(0, -1);
    const cells: string[] = []; let cell = '';
    for (let i = 0; i < value.length; i++) {
        if (value[i] === '\\' && i + 1 < value.length) {
            const next = value[++i]; cell += next === 'n' ? '\n' : next === '|' || next === '\\' ? next : `\\${next}`;
        } else if (value[i] === '|') { cells.push(cell.trim()); cell = ''; }
        else cell += value[i];
    }
    cells.push(cell.trim()); return cells;
}
export function formatTables(source: string): string {
    const lines = source.split('\n'); const out: string[] = []; let delimiter: string | null = null;
    for (let i = 0; i < lines.length; i++) {
        const marker = lines[i].trim().match(/^("""|```)/)?.[1];
        if (marker) delimiter = delimiter === marker ? null : delimiter ?? marker;
        if (!delimiter && /^\s*\|/.test(lines[i])) {
            const indent = lines[i].match(/^\s*/)?.[0] ?? '';
            const rows: string[][] = [];
            while (i < lines.length && /^\s*\|/.test(lines[i])) {
                // Retain escape sequences verbatim while padding columns.
                const raw = lines[i].trim().replace(/^\|/, '').replace(/\|$/, '');
                const cells: string[] = []; let cell = '';
                for (let n = 0; n < raw.length; n++) {
                    if (raw[n] === '\\' && n + 1 < raw.length) cell += raw[n] + raw[++n];
                    else if (raw[n] === '|') { cells.push(cell.trim()); cell = ''; }
                    else cell += raw[n];
                }
                cells.push(cell.trim()); rows.push(cells); i++;
            }
            const widths = rows.reduce<number[]>((max, row) => row.map((cell, column) => Math.max(max[column] ?? 0, cell.length)), []);
            out.push(...rows.map(row => `${indent}| ${row.map((cell, column) => cell.padEnd(widths[column])).join(' | ')} |`)); i--;
        } else out.push(lines[i]);
    }
    return out.join('\n');
}
export function scenarioTemplate(title = 'Describe the behavior', outline = false): string {
    if (outline) return `Scenario Outline: ${title || 'Describe the behavior'}\n  Given I am on the sign in page\n  When I sign in as <user>\n  Then I should see the <page> page\n\n  Examples:\n    | user          | page      |\n    | standard-user | dashboard |\n`;
    return `Scenario: ${title || 'Describe the behavior'}\n  Given \n  When \n  Then `;
}
