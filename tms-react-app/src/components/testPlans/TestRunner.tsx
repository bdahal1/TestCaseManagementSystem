import { useEffect, useRef, useState } from 'react';
import { Accordion, AccordionDetails, AccordionSummary, Alert, Box, Button, Chip, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle, Divider, IconButton, List, ListItemButton, ListItemText, MenuItem, Paper, Stack, TextField, Typography, useMediaQuery, useTheme } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import AttachFileIcon from '@mui/icons-material/AttachFile';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import api from '../../services/api';
import GherkinView from '../gherkin/GherkinView';
import { detailLabel, priorityColor } from '../testCases/caseDetails';
import { type Evidence, type PlanDetail, type ResultStatus, errorMessage, resultColor, resultLabel } from './types';
import AutomationRunDialog from '../automation/AutomationRunDialog';

interface Props { open: boolean; detail: PlanDetail; initialCaseId?: number; onClose: () => void; onChanged: () => Promise<PlanDetail> }
const results: ResultStatus[] = ['PASS', 'FAIL', 'BLOCKED', 'SKIPPED', 'NOT_RUN'];
export default function TestRunner({ open, detail, initialCaseId, onClose, onChanged }: Props) {
    const contentRef = useRef<HTMLDivElement>(null);
    const theme = useTheme();
    const fullScreen = useMediaQuery(theme.breakpoints.down('md'));
    const [selectedId, setSelectedId] = useState<number>();
    const selected = detail.cases.find(tc => tc.id === selectedId);
    const [status, setStatus] = useState<ResultStatus>('NOT_RUN');
    const [comment, setComment] = useState('');
    const [actual, setActual] = useState('');
    const [defect, setDefect] = useState('');
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    const [notice, setNotice] = useState('');
    const [preview, setPreview] = useState<{ name: string; url: string } | null>(null);
    const [leaving, setLeaving] = useState(false);
    const [autoRunOpen, setAutoRunOpen] = useState(false);
    useEffect(() => {
        if (open) { setSelectedId(initialCaseId ?? detail.cases.find(tc => tc.canRecord && ['NOT_RUN', 'BLOCKED'].includes(tc.status))?.id ?? detail.cases[0]?.id); setLeaving(false); setNotice(''); }
        // The case is chosen once when the runner opens; refreshes must preserve the current case.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [open, initialCaseId]);
    useEffect(() => {
        setStatus(selected?.status ?? 'NOT_RUN'); setComment(selected?.comment ?? ''); setActual(selected?.actualResult ?? ''); setDefect(selected?.defectUrl ?? ''); setError(''); setLeaving(false);
        // Attachment refreshes do not discard a result being drafted.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [selected?.id, selected?.revision, open]);
    useEffect(() => () => { if (preview) URL.revokeObjectURL(preview.url); }, [preview]);
    useEffect(() => { if (contentRef.current) contentRef.current.scrollTop = 0; }, [selected?.id, open]);
    useEffect(() => { if ((error || leaving) && contentRef.current) contentRef.current.scrollTop = 0; }, [error, leaving]);
    const dirty = !!selected && (status !== selected.status || comment !== (selected.comment ?? '') || actual !== (selected.actualResult ?? '') || defect !== (selected.defectUrl ?? ''));
    const close = () => { if (dirty) setLeaving(true); else onClose(); };
    const save = async (next: boolean) => {
        if (!selected) return;
        if (['FAIL', 'BLOCKED'].includes(status) && !actual.trim() && !comment.trim()) {
            setError('Add an actual result or notes to explain the failure or blocker.'); return;
        }
        setSaving(true); setError(''); setNotice('');
        let persisted = false;
        try {
            await api.post(`/testPlans/${detail.plan.id}/cases/${selected.id}/results`, { status, comment, actualResult: actual, defectUrl: defect, expectedRevision: selected.revision });
            persisted = true;
            const updated = await onChanged();
            setNotice('Result saved.');
            if (next) {
                const queue = updated.cases.filter(tc => tc.id !== selected.id && tc.canRecord && ['NOT_RUN', 'BLOCKED'].includes(tc.status));
                const nextCase = queue.find(tc => tc.id > selected.id) ?? queue[0];
                if (nextCase) setSelectedId(nextCase.id);
                else setNotice('Result saved. You have no remaining unrun or blocked cases in this plan.');
            }
        } catch (e) { setError(persisted ? 'Your result was saved, but the view could not refresh. Reload latest before continuing.' : errorMessage(e, 'Could not save your result. Your draft is still here.')); }
        finally { setSaving(false); }
    };
    const upload = async (file?: File) => {
        if (!file || !selected) return;
        if (file.size > 5 * 1024 * 1024) { setError('Choose a screenshot or log smaller than 5 MB.'); return; }
        setSaving(true); setError('');
        try {
            const body = new FormData(); body.append('file', file);
            await api.post(`/testPlans/${detail.plan.id}/cases/${selected.id}/evidence`, body);
            await onChanged(); setNotice('Evidence attached.');
        } catch (e) { setError(errorMessage(e, 'Could not attach evidence.')); }
        finally { setSaving(false); }
    };
    const openEvidence = async (file: Evidence) => {
        setError('');
        try {
            const response = await api.get(`/testPlans/${detail.plan.id}/evidence/${file.id}`, { responseType: 'blob' });
            const url = URL.createObjectURL(new Blob([response.data], { type: file.contentType }));
            if (file.contentType.startsWith('image/')) setPreview({ name: file.name, url });
            else { const link = document.createElement('a'); link.href = url; link.download = file.name; link.click(); URL.revokeObjectURL(url); }
        } catch { setError('Could not open this evidence file.'); }
    };
    const deleteEvidence = async (id: number) => {
        setSaving(true); setError('');
        try { await api.delete(`/testPlans/${detail.plan.id}/evidence/${id}`); await onChanged(); }
        catch (e) { setError(errorMessage(e, 'Could not remove evidence.')); }
        finally { setSaving(false); }
    };
    const reload = async () => {
        setSaving(true);
        try { const latest = await onChanged(); const tc = latest.cases.find(c => c.id === selectedId); if (tc) { setStatus(tc.status); setComment(tc.comment ?? ''); setActual(tc.actualResult ?? ''); setDefect(tc.defectUrl ?? ''); } setError(''); }
        catch (e) { setError(errorMessage(e, 'Could not reload this case.')); }
        finally { setSaving(false); }
    };
    return <>
        <Dialog open={open} onClose={saving ? undefined : close} fullScreen={fullScreen} fullWidth maxWidth="xl" PaperProps={{ sx: { minHeight: '80vh' } }}>
            <DialogTitle><Stack direction="row" justifyContent="space-between" alignItems="center"><Box><Typography variant="h6">{detail.plan.name}</Typography><Typography variant="body2" color="text.secondary">{[detail.plan.build, detail.plan.environment].filter(Boolean).join(' · ')} · {detail.plan.completed} of {detail.plan.total} resolved</Typography></Box>
                <IconButton aria-label="Close runner" onClick={close} disabled={saving}><CloseIcon /></IconButton></Stack></DialogTitle>
            <DialogContent dividers sx={{ p: 0, display: 'flex', flexDirection: { xs: 'column', md: 'row' } }}>
                <Box sx={{ width: { md: 280 }, flexShrink: 0, borderRight: { md: 1 }, borderColor: 'divider', maxHeight: { xs: 180, md: '70vh' }, overflowY: 'auto' }}>
                    <Typography variant="overline" sx={{ px: 2 }}>Case queue</Typography>
                    <List dense>{detail.cases.map(tc => <ListItemButton key={tc.id} selected={tc.id === selectedId} disabled={saving || (dirty && tc.id !== selectedId)} onClick={() => { setSelectedId(tc.id); setNotice(''); }}>
                        <ListItemText primary={tc.name} secondary={tc.assignee?.name ?? 'Unassigned'} /><Chip size="small" label={resultLabel(tc.status)} color={resultColor(tc.status)} /></ListItemButton>)}</List>
                </Box>
                <Box ref={contentRef} sx={{ p: { xs: 2, md: 3 }, flex: 1, minWidth: 0, maxHeight: { md: '70vh' }, overflowY: 'auto' }}>
                    {error && <Alert severity="error" sx={{ mb: 2 }} action={<Button color="inherit" onClick={() => void reload()} disabled={saving}>Reload latest</Button>}>{error}</Alert>}
                    {leaving && <Alert severity="warning" sx={{ mb: 2 }} action={<Button color="inherit" onClick={onClose}>Discard & close</Button>}>Your result has unsaved changes.</Alert>}
                    {selected ? <>
                        <Stack direction="row" alignItems="center" gap={1} flexWrap="wrap" sx={{ mb: 1 }}>
                            <Chip size="small" label={`#${selected.id}`} variant="outlined" />
                            <Chip size="small" label={selected.type} variant="outlined" />
                            <Typography variant="body2" color="text.secondary">Assigned to {selected.assignee?.name ?? 'nobody yet'}</Typography>
                            {(selected.type === 'CUCUMBER_AUTOMATION' || selected.type === 'KEYWORD_DRIVEN') && detail.plan.status === 'IN_PROGRESS' && selected.canRecord && (
                                <Button
                                    variant="contained"
                                    color="success"
                                    size="small"
                                    startIcon={<PlayArrowIcon />}
                                    onClick={() => setAutoRunOpen(true)}
                                    sx={{ ml: 'auto', borderRadius: '8px' }}
                                >
                                    Run Browser Test
                                </Button>
                            )}
                        </Stack>
                        <Typography variant="h5" fontWeight={700} sx={{ mb: 2 }}>{selected.name}</Typography>
                        {selected.details && <Box sx={{ mb: 2 }}>
                            <Stack direction="row" gap={1} flexWrap="wrap" sx={{ mb: 1 }}>
                                {selected.details.priority && <Chip size="small" label={detailLabel(selected.details.priority)} color={priorityColor(selected.details.priority)} variant="outlined" />}
                                {selected.details.component && <Chip size="small" label={selected.details.component} variant="outlined" />}
                                {selected.details.estimatedMinutes != null && <Chip size="small" label={`Estimated ${selected.details.estimatedMinutes} min`} variant="outlined" />}
                            </Stack>
                            {selected.details.description && <Typography sx={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere', mb: 1 }}>{selected.details.description}</Typography>}
                            {selected.details.preconditions && <Paper variant="outlined" sx={{ p: 2, mb: 1 }}><Typography fontWeight={600}>Preconditions</Typography><Typography sx={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{selected.details.preconditions}</Typography></Paper>}
                            {Object.entries(selected.details.customFields ?? {}).map(([key, value]) => <Typography key={key} variant="body2" sx={{ overflowWrap: 'anywhere' }}><strong>{key}:</strong> {value}</Typography>)}
                        </Box>}
                        {!selected.canRecord && <Alert severity="info" sx={{ mb: 2 }}>{detail.plan.status === 'DRAFT' ? 'This plan is a draft. A manager must start it before results can be recorded.' : detail.plan.status === 'CLOSED' ? 'This plan is closed. Its results and evidence are available for review.' : 'You can review this case. Its assigned tester or a manager can record results.'}</Alert>}
                        {selected.type === 'CUCUMBER_MANUAL' ? <>
                            <Typography variant="h6" sx={{ mb: 1 }}>Behavior scenario</Typography>
                            <GherkinView source={selected.steps.map(step => step.instruction).join('\n')} />
                        </> : <>
                            <Typography variant="h6" sx={{ mb: 1 }}>Test steps</Typography>
                            {!selected.steps.length && <Typography color="text.secondary" sx={{ mb: 2 }}>No steps were defined when this case was added to the plan.</Typography>}
                            {selected.steps.map((step, index) => <Paper key={index} variant="outlined" sx={{ p: 2, mb: 1.5 }}>
                                <Typography fontWeight={600} sx={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{index + 1}. {step.instruction}</Typography>
                                {step.data && <Typography variant="body2" sx={{ mt: 1, whiteSpace: 'pre-wrap' }}><strong>Test data:</strong> {step.data}</Typography>}
                                <Typography variant="body2" sx={{ mt: 1, whiteSpace: 'pre-wrap' }}><strong>Expected:</strong> {step.expected}</Typography>
                            </Paper>)}
                        </>}
                        <Divider sx={{ my: 3 }} />
                        <Typography variant="h6" sx={{ mb: 2 }}>Record the outcome</Typography>
                        <Stack gap={2}>
                            <TextField select label="Result" value={status} onChange={e => setStatus(e.target.value as ResultStatus)} disabled={!selected.canRecord || saving}>{results.map(value => <MenuItem key={value} value={value}>{resultLabel(value)}</MenuItem>)}</TextField>
                            <TextField label="Actual result" multiline minRows={3} value={actual} onChange={e => setActual(e.target.value)} disabled={!selected.canRecord || saving} inputProps={{ maxLength: 10000 }} placeholder="What happened when you followed these steps?" />
                            <TextField label="Notes or reason" multiline minRows={2} value={comment} onChange={e => setComment(e.target.value)} disabled={!selected.canRecord || saving} inputProps={{ maxLength: 4000 }} placeholder="Failure details, blocker, or reason for skipping" />
                            <TextField label="Linked bug URL" type="url" value={defect} onChange={e => setDefect(e.target.value)} disabled={!selected.canRecord || saving} inputProps={{ maxLength: 2000 }} placeholder="https://your-tracker.example/BUG-123" />
                            {selected.defectUrl && /^https?:\/\//i.test(selected.defectUrl) && <Button href={selected.defectUrl} target="_blank" rel="noopener noreferrer" sx={{ alignSelf: 'flex-start' }}>Open linked bug</Button>}
                        </Stack>
                        <Typography variant="h6" sx={{ mt: 3, mb: 1 }}>Evidence</Typography>
                        <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>Attach screenshots or text logs. PNG, JPEG, or UTF-8 text; up to 5 MB each, 10 files per case.</Typography>
                        {selected.evidence.map(file => <Stack key={file.id} direction="row" alignItems="center" justifyContent="space-between" gap={1}>
                            <Button onClick={() => void openEvidence(file)} sx={{ justifyContent: 'flex-start', overflowWrap: 'anywhere' }}>{file.name} · {Math.max(1, Math.round(file.size / 1024))} KB</Button>
                            {selected.canRecord && <IconButton aria-label={`Remove ${file.name}`} disabled={saving} onClick={() => void deleteEvidence(file.id)}><DeleteOutlineIcon /></IconButton>}
                        </Stack>)}
                        {selected.canRecord && <Button component="label" startIcon={<AttachFileIcon />} disabled={saving || selected.evidence.length >= 10}>Attach evidence<input aria-label="Attach evidence file" type="file" hidden accept="image/png,image/jpeg,text/plain,.log,.txt" onChange={event => { void upload(event.target.files?.[0]); event.target.value = ''; }} /></Button>}
                        {selected.history.length > 0 && <Accordion sx={{ mt: 3 }}><AccordionSummary expandIcon={<ExpandMoreIcon />}><Typography>Result history · {selected.history.length} saved {selected.history.length === 1 ? 'attempt' : 'attempts'}</Typography></AccordionSummary>
                            <AccordionDetails><Stack gap={2}>{selected.history.map(attempt => <Paper key={attempt.id} variant="outlined" sx={{ p: 2 }}>
                                <Stack direction="row" gap={1} alignItems="center" flexWrap="wrap"><Chip size="small" color={resultColor(attempt.status)} label={resultLabel(attempt.status)} /><Typography variant="caption">{attempt.recordedBy} · {new Date(attempt.recordedAt).toLocaleString()}</Typography></Stack>
                                {attempt.actualResult && <Typography sx={{ mt: 1, whiteSpace: 'pre-wrap' }}>{attempt.actualResult}</Typography>}
                                {attempt.comment && <Typography variant="body2" sx={{ mt: 1, whiteSpace: 'pre-wrap' }}>{attempt.comment}</Typography>}
                                {attempt.defectUrl && /^https?:\/\//i.test(attempt.defectUrl) && <Button href={attempt.defectUrl} target="_blank" rel="noopener noreferrer">Linked bug</Button>}
                            </Paper>)}</Stack></AccordionDetails></Accordion>}
                    </> : <Typography>No cases in this plan.</Typography>}
                </Box>
            </DialogContent>
            <DialogActions sx={{ p: 2, gap: 1, flexWrap: 'wrap' }}>
                {dirty && <Button disabled={saving} onClick={() => { if (selected) { setStatus(selected.status); setComment(selected.comment ?? ''); setActual(selected.actualResult ?? ''); setDefect(selected.defectUrl ?? ''); } }}>Discard changes</Button>}
                <Box sx={{ flex: 1 }}>{notice && <Typography role="status" variant="body2" color="success.main">{notice}</Typography>}</Box>
                {selected?.canRecord && <><Button variant="outlined" disabled={saving} onClick={() => void save(false)}>Save result</Button><Button variant="contained" disabled={saving} onClick={() => void save(true)}>{saving ? <CircularProgress size={20} /> : 'Save & next'}</Button></>}
                {!selected?.canRecord && <Button onClick={close}>Close</Button>}
            </DialogActions>
        </Dialog>
        <Dialog open={!!preview} onClose={() => setPreview(null)} maxWidth="lg" fullWidth><DialogTitle>{preview?.name}</DialogTitle><DialogContent>{preview && <Box component="img" src={preview.url} alt={preview.name} sx={{ width: '100%', objectFit: 'contain' }} />}</DialogContent><DialogActions><Button onClick={() => setPreview(null)}>Close preview</Button></DialogActions></Dialog>
        {selected && (
            <AutomationRunDialog
                open={autoRunOpen}
                planId={detail.plan.id}
                caseId={selected.id}
                caseName={selected.name}
                testType={selected.type}
                onClose={() => setAutoRunOpen(false)}
                onFinished={async () => {
                    await onChanged();
                }}
            />
        )}
    </>;
}
