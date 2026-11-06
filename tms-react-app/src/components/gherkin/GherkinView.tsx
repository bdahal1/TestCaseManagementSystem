import type { ReactNode } from 'react';
import { Box, Chip, Paper, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography } from '@mui/material';
import { keywordColors, splitTableRow } from './gherkin';

function textTokens(text: string): ReactNode {
    return text.split(/(<[^<>\n]+>|"[^"\n]*"|'[^'\n]*')/g).map((part, index) =>
        /^(<|"|')/.test(part) ? <Box key={index} component="span" sx={{ color: part.startsWith('<') ? '#0369a1' : '#047857', bgcolor: part.startsWith('<') ? '#e0f2fe' : undefined, borderRadius: 0.5, px: part.startsWith('<') ? 0.4 : 0, fontFamily: 'monospace' }}>{part}</Box> : part);
}
export default function GherkinView({ source, emptyText = 'No scenario has been written yet.' }: { source: string; emptyText?: string }) {
    const lines = source.split('\n'); const blocks: ReactNode[] = [];
    if (!source.trim()) return <Typography color="text.secondary">{emptyText}</Typography>;
    for (let i = 0; i < lines.length; i++) {
        const line = lines[i].trim(); const key = i;
        if (!line) { blocks.push(<Box key={key} sx={{ height: 8 }} />); continue; }
        if (/^("""|```)/.test(line)) {
            const delimiter = line.slice(0, 3); const content: string[] = [];
            while (++i < lines.length && !lines[i].trim().startsWith(delimiter)) content.push(lines[i]);
            blocks.push(<Paper key={key} component="pre" variant="outlined" sx={{ p: 2, my: 1, bgcolor: '#f8fafc', fontSize: 13, whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{content.join('\n')}</Paper>); continue;
        }
        if (line.startsWith('|')) {
            const rows: string[][] = [];
            while (i < lines.length && lines[i].trim().startsWith('|')) rows.push(splitTableRow(lines[i++]));
            i--;
            blocks.push(<TableContainer key={key} component={Paper} variant="outlined" sx={{ my: 1.5, borderRadius: 2 }}><Table size="small" aria-label="Gherkin data table">
                <TableHead><TableRow>{rows[0].map((cell, n) => <TableCell key={n} sx={{ bgcolor: '#eef2ff', color: '#4338ca', fontFamily: 'monospace', textTransform: 'none', py: 1 }}>{textTokens(cell)}</TableCell>)}</TableRow></TableHead>
                <TableBody>{rows.slice(1).map((row, index) => <TableRow key={index}>{row.map((cell, n) => <TableCell key={n} sx={{ py: 1, fontFamily: 'monospace', whiteSpace: 'pre-wrap' }}>{textTokens(cell)}</TableCell>)}</TableRow>)}</TableBody>
            </Table></TableContainer>); continue;
        }
        if (line.startsWith('#')) { blocks.push(<Typography key={key} variant="body2" sx={{ color: '#64748b', fontStyle: 'italic', whiteSpace: 'pre-wrap' }}>{line}</Typography>); continue; }
        if (line.startsWith('@')) { blocks.push(<Stack key={key} direction="row" gap={0.5} flexWrap="wrap">{line.split(/\s+/).map((tag, n) => <Chip key={n} size="small" label={tag} variant="outlined" sx={{ color: '#7c3aed', borderColor: '#ddd6fe' }} />)}</Stack>); continue; }
        const header = line.match(/^(Feature|Rule|Background|Scenario Outline|Scenario Template|Scenario|Example|Examples):\s*(.*)$/);
        if (header) { blocks.push(<Typography key={key} component="div" sx={{ mt: 1.5, mb: 1, fontWeight: 600, overflowWrap: 'anywhere' }}><Box component="span" sx={{ color: '#4338ca' }}>{header[1]}:</Box> {textTokens(header[2])}</Typography>); continue; }
        const step = line.match(/^(Given|When|Then|And|But|\*)(?:\s+|$)(.*)$/);
        blocks.push(step ? <Box key={key} sx={{ display: 'flex', gap: 1.5, py: 0.7, alignItems: 'baseline' }}><Box component="span" sx={{ minWidth: 48, color: keywordColors[step[1]], fontWeight: 700, fontFamily: 'monospace' }}>{step[1]}</Box><Typography component="span" sx={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere', minWidth: 0 }}>{textTokens(step[2])}</Typography></Box>
            : <Typography key={key} variant="body2" sx={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere', color: 'text.secondary' }}>{textTokens(line)}</Typography>);
    }
    return <Box aria-label="Gherkin scenario" sx={{ minWidth: 0 }}>{blocks}</Box>;
}
