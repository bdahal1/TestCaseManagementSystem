import { useCallback, useEffect, useState } from 'react';
import { Alert, Box, Button, Card, CardContent, Chip, CircularProgress, LinearProgress, Pagination, Stack, TextField, ToggleButton, ToggleButtonGroup, Typography } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import PlanEditor from './PlanEditor';
import { type PlanSummary, errorMessage, statusLabel } from './types';

export default function TestPlans({ projectId }: { projectId: number }) {
    const { user } = useAuth();
    const manager = user?.roleList.some(role => ['ROLE_QAManager', 'ROLE_ADMIN'].includes(role));
    const navigate = useNavigate();
    const [plans, setPlans] = useState<PlanSummary[]>([]);
    const [page, setPage] = useState(1);
    const [pages, setPages] = useState(0);
    const [scope, setScope] = useState('all');
    const [query, setQuery] = useState('');
    const [search, setSearch] = useState('');
    useEffect(() => { const timer = setTimeout(() => { setSearch(query); setPage(1); }, 250); return () => clearTimeout(timer); }, [query]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [creating, setCreating] = useState(false);
    const load = useCallback(async () => {
        setLoading(true); setError('');
        try { const { data } = await api.get(`/testPlans/project/${projectId}`, { params: { page: page - 1, q: search, scope } }); setPlans(data.plans); setPages(data.totalPages); }
        catch (e) { setError(errorMessage(e, 'Could not load test plans.')); }
        finally { setLoading(false); }
    }, [projectId, page, search, scope]);
    useEffect(() => { void load(); }, [load]);
    useEffect(() => { setPage(1); }, [projectId]);
    const visible = plans;
    return <Box sx={{ py: 3 }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'center' }} gap={2} sx={{ mb: 3 }}>
            <Box><Typography variant="h4" fontWeight={700}>Test plans & runs</Typography><Typography color="text.secondary">Plan a release, share the work, and follow testing through to completion.</Typography></Box>
            {manager && <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreating(true)}>New test plan</Button>}
        </Stack>
        <Stack direction={{ xs: 'column', sm: 'row' }} gap={2} sx={{ mb: 3 }}>
            <TextField label="Search plans" placeholder="Plan, release, or build" value={query} onChange={e => setQuery(e.target.value)} size="small" sx={{ flex: 1 }} />
            <ToggleButtonGroup exclusive value={scope} size="small" onChange={(_, value: string | null) => { if (value) { setScope(value); setPage(1); } }} aria-label="Plan scope">
                <ToggleButton value="all">All plans</ToggleButton><ToggleButton value="mine">Assigned to me</ToggleButton>
            </ToggleButtonGroup>
        </Stack>
        {error && <Alert severity="error" action={<Button color="inherit" onClick={() => void load()}>Retry</Button>}>{error}</Alert>}
        {loading ? <Box sx={{ p: 6, textAlign: 'center' }}><CircularProgress aria-label="Loading test plans" /></Box> : <>
            {!visible.length && !error && <Card><CardContent sx={{ p: 5, textAlign: 'center' }}><Typography variant="h6">{plans.length ? 'No matching plans' : 'Your next release starts here'}</Typography>
                <Typography color="text.secondary" sx={{ mt: 1 }}>{manager ? 'Create a plan, select test cases, and assign your team.' : 'Your manager can create a test plan and assign cases to you.'}</Typography></CardContent></Card>}
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: 'repeat(2, 1fr)' }, gap: 3 }}>
                {visible.map(plan => {
                    const percentage = plan.total ? Math.round(plan.completed / plan.total * 100) : 0;
                    const overdue = plan.dueDate && plan.dueDate < new Date().toLocaleDateString('en-CA') && plan.status !== 'CLOSED';
                    return <Card key={plan.id} variant="outlined"><CardContent sx={{ p: 3 }}>
                        <Stack direction="row" justifyContent="space-between" alignItems="flex-start" gap={2}><Typography variant="h6" fontWeight={700}>{plan.name}</Typography>
                            <Chip size="small" label={statusLabel(plan.status)} color={plan.status === 'CLOSED' ? 'success' : plan.status === 'DRAFT' ? 'default' : 'primary'} /></Stack>
                        <Typography color="text.secondary" variant="body2" sx={{ mt: 1, minHeight: 22 }}>{[plan.release, plan.build, plan.environment].filter(Boolean).join(' · ') || 'Release details not set'}</Typography>
                        <Stack direction="row" justifyContent="space-between" sx={{ mt: 3, mb: 1 }}><Typography variant="body2">{plan.completed} / {plan.total} resolved</Typography><Typography variant="body2" fontWeight={700}>{percentage}%</Typography></Stack>
                        <LinearProgress variant="determinate" value={percentage} sx={{ height: 7, borderRadius: 4 }} aria-label={`${plan.name} progress`} />
                        <Stack direction="row" gap={1} flexWrap="wrap" sx={{ mt: 2 }}>
                            <Chip size="small" label={`${plan.results.PASS} passed`} color="success" variant="outlined" />
                            <Chip size="small" label={`${plan.results.FAIL} failed`} color="error" variant="outlined" />
                            <Chip size="small" label={`${plan.results.BLOCKED} blocked`} color="warning" variant="outlined" />
                            {plan.unassigned > 0 && <Chip size="small" label={`${plan.unassigned} unassigned`} />}
                            {plan.assignedToMe > 0 && <Chip size="small" label={`${plan.assignedToMe} assigned to you`} color="primary" variant="outlined" />}
                        </Stack>
                        <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mt: 3 }}><Typography variant="caption" color={overdue ? 'error' : 'text.secondary'}>{plan.dueDate ? `${overdue ? 'Overdue · ' : 'Due '}${plan.dueDate}` : 'No deadline'}</Typography>
                            <Button endIcon={<ArrowForwardIcon />} onClick={() => navigate(`/dashboard/project/${projectId}/test-plans/${plan.id}`)}>Open plan</Button></Stack>
                    </CardContent></Card>;
                })}
            </Box>
            {pages > 1 && <Pagination count={pages} page={page} onChange={(_, value) => setPage(value)} sx={{ mt: 3 }} />}
        </>}
        <PlanEditor open={creating} projectId={projectId} onClose={() => setCreating(false)} onSaved={id => { setCreating(false); navigate(`/dashboard/project/${projectId}/test-plans/${id}`); }} />
    </Box>;
}
