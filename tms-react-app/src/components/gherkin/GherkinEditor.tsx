import { useMemo, useRef, useState } from 'react';
import CodeMirror from '@uiw/react-codemirror';
import { autocompletion, acceptCompletion, startCompletion, type CompletionContext, type Completion } from '@codemirror/autocomplete';
import { HighlightStyle, StreamLanguage, syntaxHighlighting } from '@codemirror/language';
import { linter } from '@codemirror/lint';
import { Prec } from '@codemirror/state';
import { EditorView, keymap } from '@codemirror/view';
import { Tag } from '@lezer/highlight';
import { Alert, Box, Button, Chip, Stack, ToggleButton, ToggleButtonGroup, Typography } from '@mui/material';
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';
import GherkinView from './GherkinView';
import { validateGherkin } from './gherkinValidation';
import { formatTables, keywordColors, preferredKeyword, scenarioTemplate, stepKeywords } from './gherkin';

const tokens = Object.fromEntries(['given', 'when', 'then', 'and', 'header', 'parameter', 'string', 'pipe', 'table', 'comment', 'tag'].map(name => [name, Tag.define()]));
const language = StreamLanguage.define<{ docString: string | null }>({
    startState: () => ({ docString: null }),
    token(stream, state) {
        if (stream.sol() && /^\s*("""|```)/.test(stream.string)) {
            const delimiter = stream.string.trim().slice(0, 3); state.docString = state.docString === delimiter ? null : delimiter;
            stream.skipToEnd(); return 'string';
        }
        if (state.docString) { stream.skipToEnd(); return 'string'; }
        if (stream.eatSpace()) return null;
        if (stream.match(/#.*/)) return 'comment';
        if (stream.match(/@[\w-]+/)) return 'tag';
        if (stream.match(/(?:Scenario Outline|Scenario Template|Scenario|Example|Examples|Feature|Rule|Background):/)) return 'header';
        if (stream.match(/Given\b/)) return 'given';
        if (stream.match(/When\b/)) return 'when';
        if (stream.match(/Then\b/)) return 'then';
        if (stream.match(/(?:And|But)\b|\*/)) return 'and';
        if (stream.match(/<[^>\n]+>/)) return 'parameter';
        if (stream.match(/"(?:[^"\\]|\\.)*"|'(?:[^'\\]|\\.)*'/)) return 'string';
        if (stream.match('|')) return 'pipe';
        if (/^\s*\|/.test(stream.string)) { stream.match(/(?:\\.|[^|])+/); return 'table'; }
        stream.next(); return null;
    },
    tokenTable: tokens,
});
const highlighting = HighlightStyle.define([
    ...['given', 'when', 'then', 'and'].map(name => ({ tag: tokens[name], color: keywordColors[{ given: 'Given', when: 'When', then: 'Then', and: 'And' }[name]!], fontWeight: '700' })),
    { tag: tokens.header, color: '#4338ca', fontWeight: '700' }, { tag: tokens.parameter, color: '#0369a1', fontWeight: '600' },
    { tag: tokens.string, color: '#047857' }, { tag: tokens.pipe, color: '#7c3aed', fontWeight: '700' },
    { tag: tokens.table, color: '#475569' }, { tag: tokens.comment, color: '#64748b', fontStyle: 'italic' }, { tag: tokens.tag, color: '#a21caf' },
]);
const hints: Record<string, string> = { Given: 'Set up the context', When: 'Describe the action', Then: 'Describe the outcome', And: 'Add another condition or outcome', But: 'Describe an exception', '*': 'A general step' };

export function completeGherkin(context: CompletionContext, suggestions: string[] = []) {
    const line = context.state.doc.lineAt(context.pos);
    const before = context.state.doc.sliceString(0, line.from);
    const current = line.text.slice(0, context.pos - line.from);
    // Tables and doc strings have their own content rather than step keywords.
    let delimiter: string | null = null;
    for (const previous of before.split('\n')) {
        const match = previous.trim().match(/^("""|```)/);
        if (match) delimiter = delimiter === match[1] ? null : delimiter ? delimiter : match[1];
    }
    if (delimiter || /^\s*[|#@]/.test(current)) return null;
    const trimmed = current.trimStart();
    if (!context.explicit && !trimmed && current.length !== line.text.length) return null;
    const preferred = preferredKeyword(before);
    const options: Completion[] = stepKeywords.map(label => ({ label, apply: `${label} `, type: 'keyword', detail: hints[label], boost: label === preferred ? 50 : 0 }));
    options.push(...['Scenario:', 'Scenario Outline:', 'Examples:', 'Background:'].map(label => ({ label, apply: `${label} `, type: 'keyword' })));
    options.push(...[...new Set(suggestions)].filter(step => /^(Given|When|Then|And|But|\*)\s+\S/.test(step)).slice(0, 200).map(label => ({ label, type: 'text', detail: 'Existing project step', boost: 10 })));
    return { from: line.from + current.length - trimmed.length, to: line.to, options, validFor: /^[^\n|]*$/ };
}
interface Props { value: string; onChange: (value: string) => void; title?: string; suggestions?: string[]; disabled?: boolean }
export default function GherkinEditor({ value, onChange, title = '', suggestions = [], disabled = false }: Props) {
    const viewRef = useRef<EditorView | null>(null);
    const [mode, setMode] = useState('write');
    const validation = useMemo(() => validateGherkin(value, title), [value, title]);
    const extensions = useMemo(() => [
        language, syntaxHighlighting(highlighting), EditorView.lineWrapping,
        EditorView.contentAttributes.of({ 'aria-label': 'Gherkin scenario editor', role: 'textbox', 'aria-multiline': 'true', spellcheck: 'false' }),
        autocompletion({ override: [context => completeGherkin(context, suggestions)], activateOnTyping: true, icons: false }),
        Prec.highest(keymap.of([{ key: 'Tab', run: acceptCompletion }])),
        EditorView.updateListener.of(update => {
            let newline = false;
            update.changes.iterChanges((_a, _b, _c, _d, inserted) => { if (inserted.toString().includes('\n')) newline = true; });
            if (newline && update.view.hasFocus && !update.state.doc.lineAt(update.state.selection.main.head).text.trim())
                queueMicrotask(() => { if (update.view.hasFocus) startCompletion(update.view); });
        }),
        linter(view => !view.state.doc.toString().trim() ? [] : validateGherkin(view.state.doc.toString(), title).issues.map(issue => {
            const line = view.state.doc.line(Math.min(issue.line, view.state.doc.lines));
            const from = Math.min(line.to, line.from + Math.max(0, issue.column - 1));
            return { from, to: Math.min(line.to, from + 1), severity: 'error' as const, message: issue.message };
        }), { delay: 400 }),
        EditorView.theme({
            '&': { backgroundColor: '#fff', color: '#0f172a', fontSize: '14px' }, '&.cm-focused': { outline: '2px solid #c7d2fe', outlineOffset: '-2px' },
            '.cm-scroller': { fontFamily: '"SFMono-Regular", Consolas, "Liberation Mono", monospace', lineHeight: '1.9' },
            '.cm-content': { padding: '18px 10px', minHeight: '320px', caretColor: '#4338ca' }, '.cm-gutters': { backgroundColor: '#f8fafc', color: '#94a3b8', borderRight: '1px solid #e2e8f0' },
            '.cm-activeLine, .cm-activeLineGutter': { backgroundColor: '#f5f7ff' }, '.cm-tooltip-autocomplete': { border: '1px solid #c7d2fe', backgroundColor: '#fff', borderRadius: '8px', boxShadow: '0 8px 24px #0f172a18' },
            '.cm-tooltip-autocomplete ul li[aria-selected]': { backgroundColor: '#e0e7ff', color: '#312e81' }, '.cm-completionDetail': { color: '#64748b', paddingLeft: '12px', fontStyle: 'normal' },
        }),
    ], [suggestions, title]);
    const focus = () => { viewRef.current?.focus(); };
    const insert = (text: string) => {
        const view = viewRef.current;
        if (!view) { onChange(value + text); return; }
        const { from, to } = view.state.selection.main;
        view.dispatch({ changes: { from, to, insert: text }, selection: { anchor: from + text.length } }); focus();
    };
    const keyword = (word: string) => {
        const view = viewRef.current;
        if (!view) return;
        const line = view.state.doc.lineAt(view.state.selection.main.head);
        const existing = line.text.match(/^(\s*)(Given|When|Then|And|But|\*)(\s+|$)/);
        if (existing) {
            const from = line.from + existing[1].length;
            view.dispatch({ changes: { from, to: from + existing[2].length, insert: word } });
        } else if (!line.text.trim()) {
            view.dispatch({ changes: { from: line.from, to: line.to, insert: `  ${word} ` }, selection: { anchor: line.from + word.length + 3 } });
        } else {
            view.dispatch({ changes: { from: line.to, insert: `\n  ${word} ` }, selection: { anchor: line.to + word.length + 4 } });
        }
        focus();
    };
    const template = (outline: boolean) => {
        const content = scenarioTemplate(title, outline);
        if (!value.trim()) onChange(content);
        else insert(`\n\n${content}`);
        focus();
    };
    return <Box sx={{ border: '1px solid #dbe1ec', borderRadius: 2, overflow: 'hidden', bgcolor: 'white' }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" gap={1} sx={{ px: 2, py: 1.5, bgcolor: '#f8fafc', borderBottom: '1px solid #e2e8f0' }}>
            <Box><Typography fontWeight={700}>Behavior scenario</Typography><Typography variant="caption" color="text.secondary">The Then steps describe the expected outcome.</Typography></Box>
            <ToggleButtonGroup size="small" value={mode} exclusive onChange={(_, next: string | null) => { if (next) setMode(next); }} aria-label="Scenario view"><ToggleButton value="write">Write</ToggleButton><ToggleButton value="preview">Preview</ToggleButton></ToggleButtonGroup>
        </Stack>
        {mode === 'write' ? <>
            <Stack direction="row" gap={0.7} flexWrap="wrap" alignItems="center" sx={{ p: 1.5, borderBottom: '1px solid #eef2f7' }}>
                {stepKeywords.slice(0, 5).map(word => <Chip key={word} size="small" label={word} onClick={() => keyword(word)} disabled={disabled} sx={{ color: keywordColors[word], bgcolor: '#f8fafc', fontFamily: 'monospace', fontWeight: 700 }} />)}
                <Box sx={{ flex: 1 }} />
                <Button size="small" disabled={disabled} startIcon={<AutoAwesomeIcon />} onClick={() => { const view = viewRef.current; if (view) { focus(); startCompletion(view); } }}>Suggest step</Button>
            </Stack>
            <CodeMirror value={value} onChange={onChange} onCreateEditor={view => { viewRef.current = view; }} extensions={extensions} editable={!disabled}
                minHeight="320px" maxHeight="520px" basicSetup={{ foldGutter: false, autocompletion: false, highlightActiveLine: true }}
                placeholder={'Scenario: Describe the behavior\n  Given the starting context\n  When an action is performed\n  Then the outcome is visible'} />
            <Stack direction="row" gap={1} flexWrap="wrap" sx={{ px: 1.5, py: 1, borderTop: '1px solid #e2e8f0' }}>
                <Button size="small" disabled={disabled} onClick={() => template(false)}>GWT template</Button>
                <Button size="small" disabled={disabled} onClick={() => template(true)}>Scenario outline</Button>
                <Button size="small" disabled={disabled} onClick={() => insert('\n\n  Examples:\n    | user          | page      |\n    | standard-user | dashboard |\n')}>Add Examples table</Button>
                <Button size="small" disabled={disabled} onClick={() => onChange(formatTables(value))}>Format tables</Button>
            </Stack>
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', px: 2, pb: 1.5 }}>New lines suggest the next keyword. Ctrl+Space opens suggestions; Tab accepts, Esc dismisses. Existing project steps appear as you type.</Typography>
        </> : <Box sx={{ p: 3, minHeight: 320, maxHeight: 580, overflow: 'auto' }}><GherkinView source={value} /></Box>}
        <Box sx={{ px: 2, py: 1.2, bgcolor: '#f8fafc', borderTop: '1px solid #e2e8f0' }}>
            {!value.trim() ? <Typography variant="caption" color="text.secondary">Write a scenario, paste existing Gherkin, or start with a template.</Typography> : validation.issues.length ? <Alert severity="warning" variant="outlined" sx={{ py: 0 }}>{`Line ${validation.issues[0].line}: ${validation.issues[0].message}`}{validation.issues.length > 1 && ` (+${validation.issues.length - 1} more)`}</Alert>
                : <Typography variant="caption" color="success.main">Valid Gherkin · {validation.scenarios} {validation.scenarios === 1 ? 'scenario' : 'scenarios'}{validation.examples > 0 ? ` · ${validation.examples} example rows` : ''}</Typography>}
        </Box>
    </Box>;
}
