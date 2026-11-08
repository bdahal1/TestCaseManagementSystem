import { useCallback, useEffect, useRef, useState } from 'react';
import { Alert, Autocomplete, Box, Button, Checkbox, Chip, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle, IconButton, LinearProgress, MenuItem, Paper, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TextField, Typography } from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import { useNavigate } from 'react-router-dom';
import api from '../../services/api';
import { projectCases } from './projectCases';
import { useAuth } from '../../context/AuthContext';
import PlanEditor from './PlanEditor';
import TestRunner from './TestRunner';
import AutomationBatchPreviewDialog from '../automation/AutomationBatchPreviewDialog';
import AutomationBatchViewDialog from '../automation/AutomationBatchViewDialog';
import { type BatchDetailResponse } from '../automation/types';
import { type AvailableCase, type Person, type PlanCase, type PlanDetail, errorMessage, resultColor, resultLabel, statusLabel } from './types';

export default function TestPlanWorkspace({ projectId, planId }: { projectId: number; planId: number }) {
    const navigate = useNavigate();
    const identity = useRef({ projectId, planId });
    identity.current = { projectId, planId };
    const { user } = useAuth();
    const manager = !!user?.roleList.some(role => ['ROLE_QAManager', 'ROLE_ADMIN'].includes(role));
    const [detail, setDetail] = useState<PlanDetail>();
    const [testers, setTesters] = useState<Person[]>([]);
    const [loading, setLoading] = useState(true);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState('');
    const [notice, setNotice] = useState('');
    const [scope, setScope] = useState('all');
    const [query, setQuery] = useState('');
    const [selected, setSelected] = useState<number[]>([]);
    const [assignTo, setAssignTo] = useState('');
    const [editing, setEditing] = useState(false);
    const [runner, setRunner] = useState(false);
    const [initialCase, setInitialCase] = useState<number>();
    const [addOpen, setAddOpen] = useState(false);
    const [available, setAvailable] = useState<AvailableCase[]>([]);
    const [toAdd, setToAdd] = useState<AvailableCase[]>([]);
    const [removing, setRemoving] = useState<PlanCase>();
    const [closing, setClosing] = useState(false);
    const [batchPreviewOpen, setBatchPreviewOpen] = useState(false);
    const [batchPreviewScope, setBatchPreviewScope] = useState<'ALL_ELIGIBLE' | 'SELECTED'>('ALL_ELIGIBLE');
    const [batchViewOpen, setBatchViewOpen] = useState(false);
    const [activeBatchId, setActiveBatchId] = useState<number | null>(null);
    const [latestBatch, setLatestBatch] = useState<BatchDetailResponse | null>(null);

    const loadLatestBatch = useCallback(async () => {
        try {
            const { data } = await api.get<BatchDetailResponse>(`/automation/batches/plan/${planId}/latest`);
            if (identity.current.projectId === projectId && identity.current.planId === planId) setLatestBatch(data || null);
        } catch {
            if (identity.current.projectId === projectId && identity.current.planId === planId) setLatestBatch(null);
        }
    }, [planId, projectId]);

    const refresh = useCallback(async (): Promise<PlanDetail> => {
        const { data } = await api.get<PlanDetail>(`/testPlans/${planId}`);
        if (identity.current.planId !== planId || identity.current.projectId !== projectId) return data;
        if (data.plan.projectId !== projectId) throw new Error('This plan belongs to another project.');
        setDetail(data); setSelected(previous => previous.filter(id => data.cases.some(tc => tc.id === id)));
        void loadLatestBatch();
        return data;
    }, [planId, projectId, loadLatestBatch]);

    useEffect(() => {
        let active = true;
        setLoading(true); setError(''); setDetail(undefined); setSelected([]);
        setLatestBatch(null); setActiveBatchId(null); setBatchPreviewOpen(false); setBatchViewOpen(false); setRunner(false);
        Promise.all([
            api.get<PlanDetail>(`/testPlans/${planId}`),
            api.get<Person[]>(`/testPlans/project/${projectId}/testers`),
            api.get<BatchDetailResponse>(`/automation/batches/plan/${planId}/latest`).catch(() => ({ data: null })),
        ])
            .then(([plan, people, latest]) => { if (active) {
                if (plan.data.plan.projectId !== projectId) { setError('This plan belongs to another project.'); return; }
                setDetail(plan.data); setTesters(people.data);
                setLatestBatch(latest.data || null);
            } })
            .catch(e => { if (active) setError(errorMessage(e, 'Could not load this plan.')); })
            .finally(() => { if (active) setLoading(false); });
        return () => { active = false; };
    }, [projectId, planId]);
    const perform = async (operation: () => Promise<unknown>, success: string) => {
        setBusy(true); setError(''); setNotice('');
        try { await operation(); await refresh(); setNotice(success); return true; }
        catch (e) { setError(errorMessage(e, 'Could not update this plan.')); return false; }
        finally { setBusy(false); }
    };
    const action = (name: string) => perform(() => api.post(`/testPlans/${planId}/actions/${name}`), name === 'start' ? 'Plan started. Testers can now record results.' : name === 'close' ? 'Plan closed.' : 'Plan reopened.');
    const openAdd = async () => {
        setBusy(true); setError('');
        try {
            const cases = await projectCases(projectId);
            setAvailable(cases.filter((tc: AvailableCase) => !detail?.cases.some(existing => existing.id === tc.id)));
            setToAdd([]); setAddOpen(true);
        } catch (e) { setError(errorMessage(e, 'Could not load project cases.')); }
        finally { setBusy(false); }
    };
    if (loading) return <Box sx={{ p: 6, textAlign: 'center' }}><CircularProgress aria-label="Loading test plan" /></Box>;
    if (!detail) return <Stack gap={2} sx={{ py: 3 }}><Button onClick={() => navigate(`/dashboard/project/${projectId}/test-plans`)}>Back to test plans</Button><Alert severity="error">{error || 'Plan not found.'}</Alert></Stack>;
    const { plan } = detail;
    const editable = manager && plan.status !== 'CLOSED';
    const selectable = editable || plan.status === 'IN_PROGRESS';
    const visible = detail.cases.filter(tc => (scope !== 'mine' || String(tc.assignee?.id) === String(user?.userId)) && tc.name.toLowerCase().includes(query.toLowerCase()));
    const percentage = plan.total ? Math.round(plan.completed / plan.total * 100) : 0;
    const startRunner = (id?: number) => { setInitialCase(id); setRunner(true); };
    const exportAutomation = async () => {
        if (!detail) return;
        const runnable = detail.cases.filter(tc => tc.type === 'CUCUMBER_AUTOMATION' || tc.type === 'KEYWORD_DRIVEN');
        if (!runnable.length) return;
        setBusy(true); setError('');
        try {
            const response = await api.post(`/automation/projects/${projectId}/export`, { caseIds: runnable.map(c => c.id) }, { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/zip' }));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `plan-${planId}-automation.zip`);
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
            setNotice(`Downloaded automation project for ${runnable.length} cases.`);
        } catch (e) {
            setError(errorMessage(e, 'Could not export automation project.'));
        } finally {
            setBusy(false);
        }
    };
    return <Box sx={{ py: 3 }}>
        <Button startIcon={<ArrowBackIcon />} sx={{ mb: 2 }} onClick={() => navigate(`/dashboard/project/${projectId}/test-plans`)}>All test plans</Button>
        <Stack direction={{ xs: 'column', md: 'row' }} justifyContent="space-between" gap={2} sx={{ mb: 3 }}>
            <Box><Stack direction="row" gap={2} alignItems="center"><Typography variant="h4" fontWeight={700}>{plan.name}</Typography><Chip label={statusLabel(plan.status)} color={plan.status === 'CLOSED' ? 'success' : 'primary'} /></Stack>
                <Typography color="text.secondary" sx={{ mt: 1 }}>{[plan.release, plan.build, plan.environment, plan.dueDate ? `Due ${plan.dueDate}` : ''].filter(Boolean).join(' · ') || 'No release details set'}</Typography></Box>
            <Stack direction="row" gap={1} flexWrap="wrap" alignItems="center">
                {editable && <Button onClick={() => setEditing(true)} disabled={busy}>Edit plan</Button>}
                {detail.cases.some(tc => tc.type === 'CUCUMBER_AUTOMATION' || tc.type === 'KEYWORD_DRIVEN') && (
                    <Button variant="outlined" onClick={() => void exportAutomation()} disabled={busy}>Export Automation</Button>
                )}
                {latestBatch && (
                    <Button
                        variant="outlined"
                        color={latestBatch.status === 'FAILED' ? 'error' : latestBatch.status === 'COMPLETED' ? 'success' : 'info'}
                        onClick={() => { setActiveBatchId(latestBatch.batchId); setBatchViewOpen(true); }}
                        disabled={busy}
                    >
                        Batch #{latestBatch.batchId} ({latestBatch.status})
                    </Button>
                )}
                {plan.status === 'IN_PROGRESS' && detail.cases.some(tc => tc.type === 'CUCUMBER_AUTOMATION' || tc.type === 'KEYWORD_DRIVEN') && (
                    <Button
                        variant="contained"
                        color="secondary"
                        startIcon={<PlayArrowIcon />}
                        onClick={() => { setBatchPreviewScope('ALL_ELIGIBLE'); setBatchPreviewOpen(true); }}
                        disabled={busy}
                    >
                        Run All Automated
                    </Button>
                )}
                {manager && plan.status === 'DRAFT' && <Button variant="contained" onClick={() => void action('start')} disabled={busy || !plan.total || plan.unassigned > 0}>Start plan</Button>}
                {manager && plan.status === 'IN_PROGRESS' && <Button variant="outlined" onClick={() => setClosing(true)} disabled={busy || !plan.total || plan.completed < plan.total}>Close plan</Button>}
                {manager && plan.status === 'CLOSED' && <Button variant="outlined" onClick={() => void action('reopen')} disabled={busy}>Reopen plan</Button>}
                {detail.cases.length > 0 && <Button variant="contained" startIcon={<PlayArrowIcon />} onClick={() => startRunner()}>{plan.status === 'IN_PROGRESS' ? manager ? 'Run cases' : 'Run my cases' : 'Review cases'}</Button>}
            </Stack>
        </Stack>
        {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}
        {notice && <Alert severity="success" sx={{ mb: 2 }} onClose={() => setNotice('')}>{notice}</Alert>}
        {plan.description && <Typography sx={{ mb: 3, whiteSpace: 'pre-wrap' }}>{plan.description}</Typography>}
        <Paper variant="outlined" sx={{ p: 3, mb: 3 }}>
            <Stack direction="row" justifyContent="space-between" sx={{ mb: 1 }}><Typography fontWeight={600}>{plan.completed} of {plan.total} cases resolved</Typography><Typography fontWeight={700}>{percentage}%</Typography></Stack>
            <LinearProgress variant="determinate" value={percentage} sx={{ height: 8, borderRadius: 4, mb: 2 }} aria-label="Plan progress" />
            <Stack direction="row" gap={1} flexWrap="wrap">{(['PASS', 'FAIL', 'BLOCKED', 'SKIPPED', 'NOT_RUN'] as const).map(status => <Chip key={status} variant="outlined" color={resultColor(status)} label={`${plan.results[status]} ${resultLabel(status).toLowerCase()}`} />)}
                {plan.unassigned > 0 && <Chip color="warning" label={`${plan.unassigned} unassigned`} />}</Stack>
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1.5 }}>Passed, failed, and skipped cases count as resolved. Blocked and unrun cases must be resolved before closing.</Typography>
        </Paper>
        {manager && !testers.length && <Alert severity="warning" sx={{ mb: 2 }}>No active testers are assigned to this project. Add project memberships in Admin Settings to assign testing work.</Alert>}
        {plan.status === 'DRAFT' && <Alert severity="info" sx={{ mb: 3 }}>{manager ? 'Review the scope and assign every case, then start the plan to enable testing.' : 'Your manager is preparing this plan. You can review its cases now.'}</Alert>}
        <Stack direction={{ xs: 'column', sm: 'row' }} gap={2} sx={{ mb: 2 }}>
            <TextField size="small" label="Search cases" value={query} onChange={e => setQuery(e.target.value)} sx={{ flex: 1 }} />
            <TextField select size="small" label="Show" value={scope} onChange={e => setScope(e.target.value)} sx={{ minWidth: 180 }}><MenuItem value="all">All cases</MenuItem><MenuItem value="mine">Assigned to me</MenuItem></TextField>
            {editable && <Button variant="outlined" disabled={busy} onClick={() => void openAdd()}>Add cases</Button>}
        </Stack>
        {selectable && selected.length > 0 && <Paper variant="outlined" sx={{ p: 2, mb: 2 }}><Stack direction={{ xs: 'column', sm: 'row' }} alignItems={{ sm: 'center' }} gap={2}>
            <Typography>{selected.length} selected</Typography>{editable && <><TextField select size="small" label="Assign to" value={assignTo} onChange={e => setAssignTo(e.target.value)} sx={{ minWidth: 220 }}><MenuItem value="">Unassigned</MenuItem>{testers.map(person => <MenuItem key={person.id} value={person.id}>{person.name}</MenuItem>)}</TextField>
            <Button variant="contained" disabled={busy} onClick={async () => { if (await perform(() => api.put(`/testPlans/${planId}/assignments`, { caseIds: selected, assigneeId: assignTo ? Number(assignTo) : null }), 'Assignments saved.')) setSelected([]); }}>Apply assignment</Button></>}
            {plan.status === 'IN_PROGRESS' && (
                <Button
                    variant="outlined"
                    startIcon={<PlayArrowIcon />}
                    disabled={busy}
                    onClick={() => { setBatchPreviewScope('SELECTED'); setBatchPreviewOpen(true); }}
                >
                    Run selected automation ({selected.length})
                </Button>
            )}
        </Stack></Paper>}
        <TableContainer component={Paper} variant="outlined"><Table aria-label="Plan test cases"><TableHead><TableRow>
            {selectable && <TableCell padding="checkbox"><Checkbox inputProps={{ 'aria-label': 'Select all visible cases' }} checked={visible.length > 0 && visible.every(tc => selected.includes(tc.id))} indeterminate={visible.some(tc => selected.includes(tc.id)) && !visible.every(tc => selected.includes(tc.id))} onChange={(_, checked) => setSelected(checked ? [...new Set([...selected, ...visible.map(tc => tc.id)])] : selected.filter(id => !visible.some(tc => tc.id === id)))} /></TableCell>}
            <TableCell>Test case</TableCell><TableCell>Assigned tester</TableCell><TableCell>Result</TableCell><TableCell>Evidence</TableCell><TableCell align="right">Actions</TableCell>
        </TableRow></TableHead><TableBody>
            {visible.map(tc => <TableRow key={tc.id}>
                {selectable && <TableCell padding="checkbox"><Checkbox inputProps={{ 'aria-label': `Select ${tc.name}` }} checked={selected.includes(tc.id)} onChange={(_, checked) => setSelected(previous => checked ? [...previous, tc.id] : previous.filter(id => id !== tc.id))} /></TableCell>}
                <TableCell><Typography fontWeight={600}>{tc.name}</Typography><Typography variant="caption" color="text.secondary">#{tc.id} · {tc.type}</Typography></TableCell>
                <TableCell>{editable ? <TextField select size="small" value={tc.assignee?.id ?? ''} disabled={busy} inputProps={{ 'aria-label': `Assign ${tc.name}` }} sx={{ minWidth: 180 }} onChange={e => void perform(() => api.put(`/testPlans/${planId}/assignments`, { caseIds: [tc.id], assigneeId: e.target.value ? Number(e.target.value) : null }), 'Tester assigned.')}><MenuItem value="">Unassigned</MenuItem>{testers.map(person => <MenuItem key={person.id} value={person.id}>{person.name}</MenuItem>)}</TextField> : tc.assignee?.name ?? 'Unassigned'}</TableCell>
                <TableCell><Chip size="small" color={resultColor(tc.status)} label={resultLabel(tc.status)} />{tc.recordedBy && <Typography variant="caption" display="block" sx={{ mt: 0.5 }}>{tc.recordedBy}</Typography>}</TableCell>
                <TableCell>{tc.evidence.length ? `${tc.evidence.length} files` : '—'}</TableCell>
                <TableCell align="right"><Button onClick={() => startRunner(tc.id)}>{tc.canRecord ? 'Run case' : 'Review'}</Button>{editable && !tc.history.length && !tc.evidence.length && tc.status === 'NOT_RUN' && <IconButton aria-label={`Remove ${tc.name}`} disabled={busy} onClick={() => setRemoving(tc)}><DeleteOutlineIcon /></IconButton>}</TableCell>
            </TableRow>)}
            {!visible.length && <TableRow><TableCell colSpan={selectable ? 6 : 5}><Typography color="text.secondary" sx={{ p: 3, textAlign: 'center' }}>No cases match this view.</Typography></TableCell></TableRow>}
        </TableBody></Table></TableContainer>
        <PlanEditor open={editing} projectId={projectId} planId={planId} initial={plan} onClose={() => setEditing(false)} onSaved={() => { setEditing(false); void refresh().catch(e => setError(errorMessage(e, 'Could not reload the updated plan.'))); }} />
        <TestRunner open={runner} detail={detail} initialCaseId={initialCase} onClose={() => setRunner(false)} onChanged={refresh} />
        <AutomationBatchPreviewDialog
            open={batchPreviewOpen}
            planId={planId}
            planName={plan.name}
            scope={batchPreviewScope}
            selectedCaseIds={batchPreviewScope === 'SELECTED' ? selected : undefined}
            onClose={() => setBatchPreviewOpen(false)}
            onConfirmed={(newBatch) => {
                setBatchPreviewOpen(false);
                setActiveBatchId(newBatch.batchId);
                setLatestBatch(newBatch);
                setBatchViewOpen(true);
                void refresh();
            }}
        />
        <AutomationBatchViewDialog
            open={batchViewOpen}
            batchId={activeBatchId}
            planId={planId}
            onClose={() => {
                setBatchViewOpen(false);
                void refresh();
                void loadLatestBatch();
            }}
            onBatchChanged={(batch) => { setActiveBatchId(batch.batchId); setLatestBatch(batch); }}
            onBatchFinished={async () => {
                await refresh();
                await loadLatestBatch();
            }}
        />
        <Dialog open={addOpen} onClose={busy ? undefined : () => setAddOpen(false)} fullWidth maxWidth="sm"><DialogTitle>Add cases to this plan</DialogTitle><DialogContent sx={{ pt: '12px !important' }}>
            <Autocomplete multiple disableCloseOnSelect options={available} value={toAdd} getOptionLabel={tc => `#${tc.id} · ${tc.testName}`} isOptionEqualToValue={(a, b) => a.id === b.id} onChange={(_, value) => setToAdd(value)} renderInput={params => <TextField {...params} label="Project test cases" helperText="New cases are unassigned. Assign them after adding." />} />
        </DialogContent><DialogActions><Button onClick={() => setAddOpen(false)} disabled={busy}>Cancel</Button><Button variant="contained" disabled={busy || !toAdd.length || toAdd.length > 1000} onClick={async () => { if (await perform(() => api.post(`/testPlans/${planId}/cases`, { caseIds: toAdd.map(tc => tc.id) }), 'Cases added. Assign a tester to each new case.')) setAddOpen(false); }}>Add {toAdd.length || ''} cases</Button></DialogActions></Dialog>
        <Dialog open={!!removing} onClose={() => setRemoving(undefined)}><DialogTitle>Remove case from plan?</DialogTitle><DialogContent>{removing?.name} will remain in the test-case library.</DialogContent><DialogActions><Button onClick={() => setRemoving(undefined)}>Cancel</Button><Button color="error" disabled={busy} onClick={async () => { if (removing && await perform(() => api.delete(`/testPlans/${planId}/cases/${removing.id}`), 'Case removed from plan.')) setRemoving(undefined); }}>Remove case</Button></DialogActions></Dialog>
        <Dialog open={closing} onClose={() => setClosing(false)}><DialogTitle>Close this test plan?</DialogTitle><DialogContent>All {plan.total} cases are resolved, including {plan.results.FAIL} failures. Closing makes results and evidence read-only until a manager reopens the plan.</DialogContent><DialogActions><Button onClick={() => setClosing(false)}>Keep open</Button><Button variant="contained" disabled={busy} onClick={async () => { if (await action('close')) setClosing(false); }}>Close plan</Button></DialogActions></Dialog>
    </Box>;
}
