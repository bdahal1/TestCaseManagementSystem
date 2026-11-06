import { useEffect, useMemo, useState, type ReactNode } from 'react';
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    LinearProgress,
    Link,
    MenuItem,
    Paper,
    Stack,
    Tab,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Tabs,
    TextField,
    Tooltip,
    Typography,
} from '@mui/material';
import DownloadOutlinedIcon from '@mui/icons-material/DownloadOutlined';
import RefreshIcon from '@mui/icons-material/Refresh';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import { Link as RouterLink } from 'react-router-dom';
import api from '../../services/api';
import { detailLabel, priorities, priorityColor, automationStates } from '../testCases/caseDetails';
import { errorMessage, resultColor, resultLabel, statusLabel } from '../testPlans/types';
import {
    downloadCsv,
    outcomeColors,
    outcomes,
    percent,
    utcDate,
    type ProjectReport,
    type ResultCounts,
} from './reportTypes';

function Panel({ title, subtitle, children }: { title: string; subtitle?: string; children: ReactNode }) {
    return (
        <Paper variant="outlined" sx={{ p: { xs: 2, md: 3 }, minWidth: 0, boxShadow: 'none' }}>
            <Typography variant="h6">{title}</Typography>
            {subtitle && (
                <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5, mb: 2 }}>
                    {subtitle}
                </Typography>
            )}
            {children}
        </Paper>
    );
}
function Stat({ label, value, note }: { label: string; value: string | number; note: string }) {
    return (
        <Paper variant="outlined" sx={{ p: 2.5, boxShadow: 'none' }}>
            <Typography variant="body2" color="text.secondary">
                {label}
            </Typography>
            <Typography sx={{ fontSize: 32, fontWeight: 750, my: 0.5 }}>{value}</Typography>
            <Typography variant="caption" color="text.secondary">
                {note}
            </Typography>
        </Paper>
    );
}
function ResultBar({ counts, label }: { counts: ResultCounts; label: string }) {
    const total = Object.values(counts).reduce((a, b) => a + b, 0);
    return (
        <Stack gap={1.5}>
            <Box
                role="img"
                aria-label={`${label}: ${outcomes.map((s) => `${counts[s]} ${resultLabel(s)}`).join(', ')}`}
                sx={{ height: 22, borderRadius: 1.5, overflow: 'hidden', display: 'flex', bgcolor: '#eef2f7' }}
            >
                {total > 0 &&
                    outcomes
                        .filter((s) => counts[s] > 0)
                        .map((s) => (
                            <Tooltip key={s} title={`${resultLabel(s)}: ${counts[s]}`}>
                                <Box sx={{ width: `${(100 * counts[s]) / total}%`, bgcolor: outcomeColors[s] }} />
                            </Tooltip>
                        ))}
            </Box>
            <Stack direction="row" gap={1.5} flexWrap="wrap">
                {outcomes.map((s) => (
                    <Typography key={s} variant="caption">
                        <Box
                            component="span"
                            sx={{
                                display: 'inline-block',
                                width: 8,
                                height: 8,
                                borderRadius: '50%',
                                mr: 0.6,
                                bgcolor: outcomeColors[s],
                            }}
                        />
                        {counts[s]} {resultLabel(s)}
                    </Typography>
                ))}
            </Stack>
        </Stack>
    );
}
function Distribution({ rows }: { rows: { name: string; count: number; color: string }[] }) {
    const total = rows.reduce((n, row) => n + row.count, 0);
    return (
        <Stack gap={1.5}>
            {rows.map((row) => (
                <Box key={row.name}>
                    <Stack direction="row" justifyContent="space-between">
                        <Typography variant="body2">{row.name}</Typography>
                        <Typography variant="body2" fontWeight={600}>
                            {row.count}
                        </Typography>
                    </Stack>
                    <LinearProgress
                        variant="determinate"
                        value={total ? (row.count * 100) / total : 0}
                        aria-label={row.name}
                        sx={{
                            height: 6,
                            borderRadius: 1,
                            mt: 0.5,
                            bgcolor: '#eef2f7',
                            '& .MuiLinearProgress-bar': { bgcolor: row.color },
                        }}
                    />
                </Box>
            ))}
        </Stack>
    );
}
const day = (date: Date) => date.toISOString().slice(0, 10);
export default function ProjectReports({ projectId }: { projectId: number }) {
    const [report, setReport] = useState<ProjectReport | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [revision, setRevision] = useState(0);
    const [environment, setEnvironment] = useState('');
    const [release, setRelease] = useState('');
    const [from, setFrom] = useState(() => day(new Date(Date.now() - 29 * 86400000)));
    const [to, setTo] = useState(() => day(new Date()));
    const [tab, setTab] = useState(0);
    const [query, setQuery] = useState('');
    const [result, setResult] = useState('all');
    const [priority, setPriority] = useState('all');
    const [page, setPage] = useState(0);
    useEffect(() => {
        let active = true;
        setLoading(true);
        setError('');
        api.get(`/projectReports/${projectId}`, {
            params: { environment, release, from: from || undefined, to: to || undefined },
        })
            .then(({ data }) => {
                if (active) {
                    setReport(data);
                    setPage(0);
                }
            })
            .catch((e) => {
                if (active) {
                    setError(errorMessage(e, 'Could not load project reports.'));
                    setReport(null);
                }
            })
            .finally(() => {
                if (active) setLoading(false);
            });
        return () => {
            active = false;
        };
    }, [projectId, environment, release, from, to, revision]);
    const filteredCases = useMemo(
        () =>
            (report?.cases ?? []).filter(
                (tc) =>
                    (result === 'all' || tc.result === result) &&
                    (priority === 'all' || tc.priority === priority) &&
                    [tc.name, tc.key, tc.component, tc.owner].some((value) =>
                        value?.toLowerCase().includes(query.toLowerCase()),
                    ),
            ),
        [report, result, priority, query],
    );
    const exportCases = () =>
        downloadCsv(`test-cases-project-${projectId}.csv`, [
            [
                'Case ID',
                'Name',
                'Priority',
                'Component',
                'Owner',
                'Automation status',
                'Estimated minutes',
                'Latest outcome',
                'Plan',
                'Recorded at (UTC)',
                'Library differs from run snapshot',
                'Linked defect',
            ],
            ...filteredCases.map((tc) => [
                tc.key,
                tc.name,
                tc.priority,
                tc.component,
                tc.owner,
                tc.automationStatus,
                tc.estimatedMinutes,
                tc.result,
                tc.planName,
                tc.recordedAt,
                tc.definitionChanged,
                tc.defectUrl,
            ]),
        ]);
    const exportPlans = () =>
        report &&
        downloadCsv(`test-plans-project-${projectId}.csv`, [
            [
                'Plan',
                'Release',
                'Build',
                'Environment',
                'Status',
                'Case executions',
                'Resolved',
                'Pass rate (%)',
                'Unassigned',
                ...outcomes,
            ],
            ...report.plans.map((plan) => [
                plan.name,
                plan.release,
                plan.build,
                plan.environment,
                plan.status,
                plan.total,
                plan.completed,
                plan.passRate,
                plan.unassigned,
                ...outcomes.map((s) => plan.results[s]),
            ]),
        ]);
    const exportActivity = () =>
        report &&
        downloadCsv(`result-activity-${report.from}-${report.to}.csv`, [
            ['Date (UTC)', ...outcomes],
            ...report.activity.map((d) => [d.date, ...outcomes.map((s) => d.results[s])]),
        ]);
    const maxActivity = Math.max(
        1,
        ...(report?.activity ?? []).map((d) => Object.values(d.results).reduce((a, b) => a + b, 0)),
    );
    const root = `/dashboard/project/${projectId}`;
    return (
        <Stack gap={3}>
            <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" gap={2}>
                <Box>
                    <Typography variant="overline" color="primary">
                        Project insights
                    </Typography>
                    <Typography variant="h4" fontWeight={750}>
                        Reports
                    </Typography>
                    <Typography color="text.secondary" sx={{ mt: 0.5 }}>
                        Understand test coverage, release results, and the work still ahead.
                    </Typography>
                </Box>
                <Button startIcon={<RefreshIcon />} onClick={() => setRevision((n) => n + 1)} disabled={loading}>
                    Refresh
                </Button>
            </Stack>
            <Paper variant="outlined" sx={{ p: 2, boxShadow: 'none' }}>
                <Stack direction={{ xs: 'column', md: 'row' }} gap={2}>
                    <TextField
                        select
                        fullWidth
                        label="Environment"
                        value={environment}
                        onChange={(e) => setEnvironment(e.target.value)}
                        slotProps={{ select: { displayEmpty: true }, inputLabel: { shrink: true } }}
                    >
                        <MenuItem value="">All environments</MenuItem>
                        {[...new Set([...(report?.environments ?? []), ...(environment ? [environment] : [])])].map(
                            (s) => (
                                <MenuItem key={s} value={s}>
                                    {s}
                                </MenuItem>
                            ),
                        )}
                    </TextField>
                    <TextField
                        select
                        fullWidth
                        label="Release"
                        value={release}
                        onChange={(e) => setRelease(e.target.value)}
                        slotProps={{ select: { displayEmpty: true }, inputLabel: { shrink: true } }}
                    >
                        <MenuItem value="">All releases</MenuItem>
                        {[...new Set([...(report?.releases ?? []), ...(release ? [release] : [])])].map((s) => (
                            <MenuItem key={s} value={s}>
                                {s}
                            </MenuItem>
                        ))}
                    </TextField>
                    <TextField
                        fullWidth
                        label="Activity from (UTC)"
                        type="date"
                        value={from}
                        onChange={(e) => setFrom(e.target.value)}
                        slotProps={{ inputLabel: { shrink: true } }}
                    />
                    <TextField
                        fullWidth
                        label="Activity to (UTC)"
                        type="date"
                        value={to}
                        onChange={(e) => setTo(e.target.value)}
                        slotProps={{ inputLabel: { shrink: true } }}
                    />
                </Stack>
                <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
                    Environment and release filter plans, results, and workload. Dates filter the activity chart only.
                    Library details always cover the current project.
                </Typography>
            </Paper>
            {error && (
                <Alert severity="error" action={<Button onClick={() => setRevision((n) => n + 1)}>Retry</Button>}>
                    {error}
                </Alert>
            )}
            {loading ? (
                <Box sx={{ textAlign: 'center', p: 5 }}>
                    <CircularProgress aria-label="Loading reports" />
                </Box>
            ) : (
                report && (
                    <>
                        {!report.library.total && (
                            <Alert severity="info">
                                Your project has no test cases yet. Add cases and record results to populate these
                                reports.
                            </Alert>
                        )}
                        <Box
                            sx={{
                                display: 'grid',
                                gridTemplateColumns: { xs: '1fr 1fr', lg: 'repeat(4, 1fr)' },
                                gap: 2,
                            }}
                        >
                            <Stat
                                label="Library cases"
                                value={report.library.total}
                                note={`${report.library.unowned} without an owner`}
                            />
                            <Stat
                                label="Case executions"
                                value={report.execution.total}
                                note={`${report.execution.activePlans} active · ${report.execution.closedPlans} closed plans`}
                            />
                            <Stat
                                label="Pass rate"
                                value={percent(report.execution.passRate)}
                                note="Passed ÷ (passed + failed)"
                            />
                            <Stat
                                label="Linked defects"
                                value={report.execution.linkedDefects}
                                note="Distinct bug URLs; issue status is not tracked"
                            />
                        </Box>
                        <Tabs
                            value={tab}
                            onChange={(_, value: number) => {
                                setTab(value);
                                setPage(0);
                            }}
                            variant="scrollable"
                            aria-label="Report sections"
                        >
                            <Tab label="Overview" />
                            <Tab label="Case results" />
                            <Tab label="Plan comparison" />
                            <Tab label="Workload" />
                        </Tabs>
                        {tab === 0 && (
                            <>
                                <Box
                                    sx={{
                                        display: 'grid',
                                        gridTemplateColumns: { xs: '1fr', lg: '1.5fr 1fr' },
                                        gap: 2,
                                    }}
                                >
                                    <Panel
                                        title="Execution outcomes"
                                        subtitle="Current results across active and closed plans. A case in two plans counts as two executions."
                                    >
                                        <ResultBar counts={report.execution.results} label="Execution outcomes" />
                                        <Stack direction="row" gap={4} sx={{ mt: 3 }}>
                                            <Box>
                                                <Typography variant="h5">
                                                    {percent(report.execution.completionRate)}
                                                </Typography>
                                                <Typography variant="caption" color="text.secondary">
                                                    Resolved: pass, fail, or skipped
                                                </Typography>
                                            </Box>
                                            <Box>
                                                <Typography variant="h5">
                                                    {report.execution.testedCases} / {report.library.total}
                                                </Typography>
                                                <Typography variant="caption" color="text.secondary">
                                                    Library cases with a recorded outcome in scope
                                                </Typography>
                                            </Box>
                                        </Stack>
                                        <Typography
                                            variant="caption"
                                            color="text.secondary"
                                            sx={{ display: 'block', mt: 2 }}
                                        >
                                            {report.execution.draftPlans} draft plans excluded. A passing result does
                                            not establish requirements coverage.
                                        </Typography>
                                    </Panel>
                                    <Panel
                                        title="Automation readiness"
                                        subtitle="Declared status across the current library."
                                    >
                                        <Distribution
                                            rows={automationStates.map((state, i) => ({
                                                name: detailLabel(state),
                                                count: report.library.automation[state],
                                                color: ['#94a3b8', '#6366f1', '#059669', '#cbd5e1'][i],
                                            }))}
                                        />
                                    </Panel>
                                </Box>
                                <Panel
                                    title="Result activity"
                                    subtitle={`${report.from} to ${report.to} · UTC. Counts saved attempts, including reruns and result resets. It is not a unique-case count.`}
                                >
                                    <Box sx={{ overflowX: 'auto' }}>
                                        <Box
                                            role="img"
                                            aria-label="Daily result activity; exact values are available in the activity table below"
                                            sx={{
                                                display: 'flex',
                                                alignItems: 'end',
                                                height: 150,
                                                gap: 0.6,
                                                minWidth: Math.max(280, report.activity.length * 14),
                                                borderBottom: '1px solid #e2e8f0',
                                                pt: 2,
                                            }}
                                        >
                                            {report.activity.map((d) => (
                                                <Tooltip
                                                    key={d.date}
                                                    title={`${d.date}: ${outcomes.map((s) => `${d.results[s]} ${resultLabel(s)}`).join(', ')}`}
                                                >
                                                    <Box
                                                        sx={{
                                                            height: '100%',
                                                            flex: 1,
                                                            display: 'flex',
                                                            flexDirection: 'column',
                                                            justifyContent: 'end',
                                                            minWidth: 8,
                                                        }}
                                                    >
                                                        {outcomes.map(
                                                            (s) =>
                                                                d.results[s] > 0 && (
                                                                    <Box
                                                                        key={s}
                                                                        sx={{
                                                                            height: `${(d.results[s] * 100) / maxActivity}%`,
                                                                            bgcolor: outcomeColors[s],
                                                                            minHeight: 2,
                                                                        }}
                                                                    />
                                                                ),
                                                        )}
                                                    </Box>
                                                </Tooltip>
                                            ))}
                                        </Box>
                                    </Box>
                                    <Stack direction="row" justifyContent="space-between" sx={{ mt: 0.5 }}>
                                        <Typography variant="caption">{report.from}</Typography>
                                        <Typography variant="caption">{report.to}</Typography>
                                    </Stack>
                                    <Accordion
                                        elevation={0}
                                        disableGutters
                                        sx={{ mt: 1, '&:before': { display: 'none' } }}
                                    >
                                        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                                            View daily activity table
                                        </AccordionSummary>
                                        <AccordionDetails>
                                            <Button startIcon={<DownloadOutlinedIcon />} onClick={exportActivity}>
                                                Export activity CSV
                                            </Button>
                                            <TableContainer sx={{ maxHeight: 320 }}>
                                                <Table size="small" stickyHeader aria-label="Daily result activity">
                                                    <TableHead>
                                                        <TableRow>
                                                            <TableCell>Date (UTC)</TableCell>
                                                            {outcomes.map((s) => (
                                                                <TableCell key={s}>{resultLabel(s)}</TableCell>
                                                            ))}
                                                        </TableRow>
                                                    </TableHead>
                                                    <TableBody>
                                                        {report.activity.map((d) => (
                                                            <TableRow key={d.date}>
                                                                <TableCell>{d.date}</TableCell>
                                                                {outcomes.map((s) => (
                                                                    <TableCell key={s}>{d.results[s]}</TableCell>
                                                                ))}
                                                            </TableRow>
                                                        ))}
                                                    </TableBody>
                                                </Table>
                                            </TableContainer>
                                        </AccordionDetails>
                                    </Accordion>
                                </Panel>
                                <Panel
                                    title="Library priorities"
                                    subtitle="Current classification across all cases in this project."
                                >
                                    <Distribution
                                        rows={priorities.map((p, i) => ({
                                            name: detailLabel(p),
                                            count: report.library.priorities[p],
                                            color: ['#dc2626', '#d97706', '#6366f1', '#94a3b8'][i],
                                        }))}
                                    />
                                </Panel>
                            </>
                        )}
                        {tab === 1 && (
                            <Panel
                                title="Latest case results"
                                subtitle="Most recent saved outcome per case across selected active and closed plans. Historical outcomes remain visible after a case is edited."
                            >
                                <Stack direction={{ xs: 'column', md: 'row' }} gap={2} sx={{ mb: 2 }}>
                                    <TextField
                                        size="small"
                                        label="Search cases, components, or owners"
                                        value={query}
                                        onChange={(e) => {
                                            setQuery(e.target.value);
                                            setPage(0);
                                        }}
                                        sx={{ flex: 1 }}
                                    />
                                    <TextField
                                        select
                                        size="small"
                                        label="Outcome"
                                        value={result}
                                        onChange={(e) => {
                                            setResult(e.target.value);
                                            setPage(0);
                                        }}
                                        sx={{ minWidth: 150 }}
                                    >
                                        <MenuItem value="all">All outcomes</MenuItem>
                                        {outcomes.map((s) => (
                                            <MenuItem key={s} value={s}>
                                                {resultLabel(s)}
                                            </MenuItem>
                                        ))}
                                    </TextField>
                                    <TextField
                                        select
                                        size="small"
                                        label="Priority"
                                        value={priority}
                                        onChange={(e) => {
                                            setPriority(e.target.value);
                                            setPage(0);
                                        }}
                                        sx={{ minWidth: 150 }}
                                    >
                                        <MenuItem value="all">All priorities</MenuItem>
                                        {priorities.map((p) => (
                                            <MenuItem key={p} value={p}>
                                                {detailLabel(p)}
                                            </MenuItem>
                                        ))}
                                    </TextField>
                                    <Button
                                        startIcon={<DownloadOutlinedIcon />}
                                        disabled={!filteredCases.length}
                                        onClick={exportCases}
                                    >
                                        Export CSV
                                    </Button>
                                </Stack>
                                <TableContainer>
                                    <Table aria-label="Latest case results">
                                        <TableHead>
                                            <TableRow>
                                                {[
                                                    'Case',
                                                    'Priority / Component',
                                                    'Owner / Automation',
                                                    'Latest outcome',
                                                    'Plan / Recorded at',
                                                ].map((label) => (
                                                    <TableCell key={label}>{label}</TableCell>
                                                ))}
                                            </TableRow>
                                        </TableHead>
                                        <TableBody>
                                            {filteredCases.slice(page * 25, (page + 1) * 25).map((tc) => (
                                                <TableRow key={tc.id}>
                                                    <TableCell>
                                                        <Typography variant="caption" color="text.secondary">
                                                            {tc.key}
                                                        </Typography>
                                                        <Typography variant="body2" fontWeight={600}>
                                                            {tc.name}
                                                        </Typography>
                                                        {tc.definitionChanged && (
                                                            <Chip
                                                                size="small"
                                                                label="Library changed"
                                                                color="warning"
                                                                variant="outlined"
                                                                sx={{ mt: 0.5 }}
                                                            />
                                                        )}
                                                    </TableCell>
                                                    <TableCell>
                                                        <Chip
                                                            size="small"
                                                            label={detailLabel(tc.priority)}
                                                            color={priorityColor(tc.priority)}
                                                            variant="outlined"
                                                        />
                                                        <Typography variant="caption" display="block">
                                                            {tc.component || 'No component'}
                                                        </Typography>
                                                    </TableCell>
                                                    <TableCell>
                                                        {tc.owner}
                                                        <Typography
                                                            variant="caption"
                                                            display="block"
                                                            color="text.secondary"
                                                        >
                                                            {detailLabel(tc.automationStatus)}
                                                        </Typography>
                                                    </TableCell>
                                                    <TableCell>
                                                        <Chip
                                                            size="small"
                                                            color={resultColor(tc.result)}
                                                            label={resultLabel(tc.result)}
                                                        />
                                                    </TableCell>
                                                    <TableCell>
                                                        {tc.planId && (
                                                            <Link
                                                                component={RouterLink}
                                                                to={`${root}/test-plans/${tc.planId}`}
                                                            >
                                                                {tc.planName}
                                                            </Link>
                                                        )}
                                                        <Typography
                                                            variant="caption"
                                                            display="block"
                                                            color="text.secondary"
                                                        >
                                                            {utcDate(tc.recordedAt)}
                                                        </Typography>
                                                    </TableCell>
                                                </TableRow>
                                            ))}
                                            {!filteredCases.length && (
                                                <TableRow>
                                                    <TableCell colSpan={5}>No cases match these filters.</TableCell>
                                                </TableRow>
                                            )}
                                        </TableBody>
                                    </Table>
                                </TableContainer>
                                <Stack
                                    direction="row"
                                    justifyContent="space-between"
                                    alignItems="center"
                                    sx={{ mt: 2 }}
                                >
                                    <Typography variant="caption">
                                        {filteredCases.length} cases · exports include every filtered row
                                    </Typography>
                                    <Box>
                                        <Button disabled={page === 0} onClick={() => setPage((n) => n - 1)}>
                                            Previous
                                        </Button>
                                        <Button
                                            disabled={(page + 1) * 25 >= filteredCases.length}
                                            onClick={() => setPage((n) => n + 1)}
                                        >
                                            Next
                                        </Button>
                                    </Box>
                                </Stack>
                            </Panel>
                        )}
                        {tab === 2 && (
                            <Panel
                                title="Plan comparison"
                                subtitle="Compare releases and environments. Draft plans appear here for planning but are excluded from the overview’s execution totals."
                            >
                                <Button
                                    startIcon={<DownloadOutlinedIcon />}
                                    disabled={!report.plans.length}
                                    onClick={exportPlans}
                                    sx={{ mb: 2 }}
                                >
                                    Export plans CSV
                                </Button>
                                <TableContainer>
                                    <Table aria-label="Plan comparison">
                                        <TableHead>
                                            <TableRow>
                                                {[
                                                    'Plan',
                                                    'Release / Environment',
                                                    'Status',
                                                    'Resolved',
                                                    'Pass rate',
                                                    'Results',
                                                    'Unassigned',
                                                ].map((label) => (
                                                    <TableCell key={label}>{label}</TableCell>
                                                ))}
                                            </TableRow>
                                        </TableHead>
                                        <TableBody>
                                            {report.plans.map((plan) => (
                                                <TableRow key={plan.id}>
                                                    <TableCell>
                                                        <Link
                                                            component={RouterLink}
                                                            to={`${root}/test-plans/${plan.id}`}
                                                        >
                                                            {plan.name}
                                                        </Link>
                                                        <Typography variant="caption" display="block">
                                                            {plan.build}
                                                        </Typography>
                                                    </TableCell>
                                                    <TableCell>
                                                        {plan.release || '—'}
                                                        <Typography variant="caption" display="block">
                                                            {plan.environment || 'No environment'}
                                                        </Typography>
                                                    </TableCell>
                                                    <TableCell>
                                                        <Chip size="small" label={statusLabel(plan.status)} />
                                                    </TableCell>
                                                    <TableCell>
                                                        {plan.completed} / {plan.total}
                                                    </TableCell>
                                                    <TableCell>{percent(plan.passRate)}</TableCell>
                                                    <TableCell sx={{ minWidth: 210 }}>
                                                        <ResultBar label={plan.name} counts={plan.results} />
                                                    </TableCell>
                                                    <TableCell>{plan.unassigned}</TableCell>
                                                </TableRow>
                                            ))}
                                            {!report.plans.length && (
                                                <TableRow>
                                                    <TableCell colSpan={7}>
                                                        No plans match the selected environment and release.
                                                    </TableCell>
                                                </TableRow>
                                            )}
                                        </TableBody>
                                    </Table>
                                </TableContainer>
                            </Panel>
                        )}
                        {tab === 3 && (
                            <Panel
                                title="Active testing workload"
                                subtitle="Assignments in active plans only. Remaining effort uses the estimate saved with each run; it is not measured execution time."
                            >
                                <TableContainer>
                                    <Table aria-label="Active testing workload">
                                        <TableHead>
                                            <TableRow>
                                                {[
                                                    'Tester',
                                                    'Assigned',
                                                    'Remaining',
                                                    'Blocked',
                                                    'Estimated remaining effort',
                                                    'Missing estimates',
                                                ].map((label) => (
                                                    <TableCell key={label}>{label}</TableCell>
                                                ))}
                                            </TableRow>
                                        </TableHead>
                                        <TableBody>
                                            {report.workload.map((row, i) => (
                                                <TableRow key={i}>
                                                    <TableCell>{row.tester}</TableCell>
                                                    <TableCell>{row.total}</TableCell>
                                                    <TableCell>{row.remaining}</TableCell>
                                                    <TableCell>{row.blocked}</TableCell>
                                                    <TableCell>{row.estimatedRemainingMinutes} min</TableCell>
                                                    <TableCell>{row.missingEstimates}</TableCell>
                                                </TableRow>
                                            ))}
                                            {!report.workload.length && (
                                                <TableRow>
                                                    <TableCell colSpan={6}>
                                                        No active plan assignments in this scope.
                                                    </TableCell>
                                                </TableRow>
                                            )}
                                        </TableBody>
                                    </Table>
                                </TableContainer>
                            </Panel>
                        )}
                        <Typography variant="caption" color="text.secondary">
                            Generated {utcDate(report.generatedAt)}. Refresh to include new results. Requirements
                            coverage and actual time tracking are not yet available.
                        </Typography>
                    </>
                )
            )}
        </Stack>
    );
}
