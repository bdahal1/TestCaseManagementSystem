import { useCallback, useEffect, useRef, useState } from 'react';
import {
    Alert,
    Box,
    Button,
    Card,
    CardContent,
    Chip,
    CircularProgress,
    FormControl,
    IconButton,
    InputLabel,
    MenuItem,
    Paper,
    Select,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TablePagination,
    TableRow,
    TextField,
    Tooltip,
    Typography,
} from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';
import FilterAltOffIcon from '@mui/icons-material/FilterAltOff';
import HistoryIcon from '@mui/icons-material/History';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import CancelIcon from '@mui/icons-material/Cancel';
import HourglassEmptyIcon from '@mui/icons-material/HourglassEmpty';
import VisibilityIcon from '@mui/icons-material/Visibility';
import api from '../../services/api';
import { type HistoryJobRow, type HistoryPageResponse, type JobStatus } from './types';
import AutomationRunDialog from './AutomationRunDialog';
import AutomationBatchViewDialog from './AutomationBatchViewDialog';

interface Props {
    projectId: number;
}

interface PlanOption {
    id: number;
    name: string;
}

export default function AutomationHistoryPage({ projectId }: Props) {
    const [history, setHistory] = useState<HistoryPageResponse | null>(null);
    const [plans, setPlans] = useState<PlanOption[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    // Filters
    const [planIdFilter, setPlanIdFilter] = useState<number | ''>('');
    const [statusFilter, setStatusFilter] = useState<string>('');
    const [caseNameFilter, setCaseNameFilter] = useState('');
    const [debouncedCaseName, setDebouncedCaseName] = useState('');
    const [fromDate, setFromDate] = useState('');
    const [toDate, setToDate] = useState('');

    // Pagination
    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(20);

    // Dialog state for viewing exact run details
    const [selectedJob, setSelectedJob] = useState<HistoryJobRow | null>(null);

    // Dialog state for opening batch view
    const [activeBatchId, setActiveBatchId] = useState<number | null>(null);
    const [batchPlanId, setBatchPlanId] = useState<number | null>(null);

    const version = useRef(0);

    // Debounce case name search
    useEffect(() => {
        const timer = setTimeout(() => {
            setDebouncedCaseName(caseNameFilter);
            setPage(0);
        }, 300);
        return () => clearTimeout(timer);
    }, [caseNameFilter]);

    // Load project plans for filter dropdown
    useEffect(() => {
        let active = true;
        api.get(`/testPlans/project/${projectId}`)
            .then((res) => {
                if (!active) return;
                const list = (res.data?.plans || []).map((p: any) => ({
                    id: p.id,
                    name: p.name || p.executionName || `Plan #${p.id}`,
                }));
                setPlans(list);
            })
            .catch(() => {});
        return () => {
            active = false;
        };
    }, [projectId]);

    // Fetch history
    const fetchHistory = useCallback(async () => {
        version.current++;
        const requestVersion = version.current;
        setLoading(true);
        setError('');

        try {
            const params: Record<string, any> = {
                page,
                size: rowsPerPage,
            };
            if (planIdFilter !== '') params.planId = planIdFilter;
            if (statusFilter) params.status = statusFilter;
            if (debouncedCaseName.trim()) params.caseNameContains = debouncedCaseName.trim();
            if (fromDate) params.from = `${fromDate}T00:00:00Z`;
            if (toDate) params.to = `${toDate}T23:59:59Z`;

            const { data } = await api.get<HistoryPageResponse>(`/automation/history/project/${projectId}`, { params });
            if (version.current !== requestVersion) return;
            setHistory(data);
        } catch (e: any) {
            if (version.current !== requestVersion) return;
            setError(e?.response?.data?.message || 'Could not load automation history.');
        } finally {
            if (version.current === requestVersion) {
                setLoading(false);
            }
        }
    }, [projectId, page, rowsPerPage, planIdFilter, statusFilter, debouncedCaseName, fromDate, toDate]);

    useEffect(() => {
        void fetchHistory();
    }, [fetchHistory]);

    const handleResetFilters = () => {
        setPlanIdFilter('');
        setStatusFilter('');
        setCaseNameFilter('');
        setDebouncedCaseName('');
        setFromDate('');
        setToDate('');
        setPage(0);
    };

    const hasActiveFilters = planIdFilter !== '' || statusFilter !== '' || caseNameFilter !== '' || fromDate !== '' || toDate !== '';

    const statusChip = (status: JobStatus) => {
        switch (status) {
            case 'COMPLETED':
                return <Chip size="small" icon={<CheckCircleOutlineIcon />} label="COMPLETED" color="success" />;
            case 'FAILED':
                return <Chip size="small" icon={<ErrorOutlineIcon />} label="FAILED" color="error" />;
            case 'INFRASTRUCTURE_ERROR':
                return <Chip size="small" icon={<WarningAmberIcon />} label="INFRA ERROR" color="warning" />;
            case 'RUNNING':
                return <Chip size="small" icon={<CircularProgress size={12} color="inherit" />} label="RUNNING" color="primary" />;
            case 'QUEUED':
                return <Chip size="small" icon={<HourglassEmptyIcon />} label="QUEUED" color="default" />;
            case 'CANCELLED':
                return <Chip size="small" icon={<CancelIcon />} label="CANCELLED" color="default" />;
            default:
                return <Chip size="small" label={status} />;
        }
    };

    const formatDuration = (ms: number | null) => {
        if (ms == null) return '—';
        if (ms < 1000) return `${ms}ms`;
        const seconds = (ms / 1000).toFixed(1);
        return `${seconds}s`;
    };

    return (
        <Box sx={{ py: 3 }}>
            {/* Header */}
            <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'center' }} gap={2} sx={{ mb: 3 }}>
                <Box>
                    <Stack direction="row" gap={1.5} alignItems="center">
                        <HistoryIcon color="primary" sx={{ fontSize: 32 }} />
                        <Typography variant="h4" fontWeight={700}>
                            Automation History
                        </Typography>
                    </Stack>
                    <Typography color="text.secondary" sx={{ mt: 0.5 }}>
                        Inspect historical execution attempts, investigate failure diagnostics, and compare run outcomes across plans.
                    </Typography>
                </Box>
                <Button
                    variant="outlined"
                    startIcon={<RefreshIcon />}
                    onClick={() => void fetchHistory()}
                    disabled={loading}
                >
                    Refresh
                </Button>
            </Stack>

            {error && (
                <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError('')}>
                    {error}
                </Alert>
            )}

            {/* Filter Toolbar */}
            <Paper variant="outlined" sx={{ p: 2.5, mb: 3, backgroundColor: 'background.paper' }}>
                <Stack direction={{ xs: 'column', sm: 'row' }} gap={2} flexWrap="wrap" alignItems="center">
                    {/* Case Name Filter */}
                    <TextField
                        size="small"
                        label="Case Name"
                        placeholder="Search frozen case name..."
                        value={caseNameFilter}
                        onChange={(e) => setCaseNameFilter(e.target.value)}
                        sx={{ minWidth: 200, flex: 1 }}
                    />

                    {/* Plan Filter */}
                    <FormControl size="small" sx={{ minWidth: 180 }}>
                        <InputLabel id="plan-filter-label">Test Plan</InputLabel>
                        <Select
                            labelId="plan-filter-label"
                            label="Test Plan"
                            value={planIdFilter}
                            onChange={(e) => {
                                const v = String(e.target.value);
                                setPlanIdFilter(v === '' ? '' : Number(v));
                                setPage(0);
                            }}
                        >
                            <MenuItem value="">All Plans</MenuItem>
                            {plans.map((p) => (
                                <MenuItem key={p.id} value={p.id}>
                                    {p.name}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>

                    {/* Status Filter */}
                    <FormControl size="small" sx={{ minWidth: 160 }}>
                        <InputLabel id="status-filter-label">Status</InputLabel>
                        <Select
                            labelId="status-filter-label"
                            label="Status"
                            value={statusFilter}
                            onChange={(e) => {
                                setStatusFilter(e.target.value);
                                setPage(0);
                            }}
                        >
                            <MenuItem value="">All Statuses</MenuItem>
                            <MenuItem value="COMPLETED">Completed</MenuItem>
                            <MenuItem value="FAILED">Failed</MenuItem>
                            <MenuItem value="INFRASTRUCTURE_ERROR">Infrastructure Error</MenuItem>
                            <MenuItem value="RUNNING">Running</MenuItem>
                            <MenuItem value="QUEUED">Queued</MenuItem>
                            <MenuItem value="CANCELLED">Cancelled</MenuItem>
                        </Select>
                    </FormControl>

                    {/* Date From */}
                    <TextField
                        size="small"
                        type="date"
                        label="From Date"
                        InputLabelProps={{ shrink: true }}
                        value={fromDate}
                        onChange={(e) => {
                            setFromDate(e.target.value);
                            setPage(0);
                        }}
                        sx={{ width: 150 }}
                    />

                    {/* Date To */}
                    <TextField
                        size="small"
                        type="date"
                        label="To Date"
                        InputLabelProps={{ shrink: true }}
                        value={toDate}
                        onChange={(e) => {
                            setToDate(e.target.value);
                            setPage(0);
                        }}
                        sx={{ width: 150 }}
                    />

                    {/* Clear Filters */}
                    {hasActiveFilters && (
                        <Button
                            variant="text"
                            color="inherit"
                            startIcon={<FilterAltOffIcon />}
                            onClick={handleResetFilters}
                            size="small"
                        >
                            Clear Filters
                        </Button>
                    )}
                </Stack>
            </Paper>

            {/* Table or Empty State */}
            {loading && !history ? (
                <Box sx={{ p: 6, textAlign: 'center' }}>
                    <CircularProgress aria-label="Loading automation history" />
                    <Typography sx={{ mt: 2 }} color="text.secondary">Loading automation history...</Typography>
                </Box>
            ) : !history?.content.length ? (
                <Card variant="outlined">
                    <CardContent sx={{ p: 6, textAlign: 'center' }}>
                        <HistoryIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 1 }} />
                        <Typography variant="h6">
                            {hasActiveFilters ? 'No runs match your filters' : 'No automation runs recorded yet'}
                        </Typography>
                        <Typography color="text.secondary" sx={{ mt: 1 }}>
                            {hasActiveFilters
                                ? 'Try adjusting your filters or date range.'
                                : 'Execute test cases in an active test plan using "Run automation" or "Run All Automated" to build history.'}
                        </Typography>
                        {hasActiveFilters && (
                            <Button variant="outlined" sx={{ mt: 2 }} onClick={handleResetFilters}>
                                Reset Filters
                            </Button>
                        )}
                    </CardContent>
                </Card>
            ) : (
                <Paper variant="outlined">
                    <TableContainer>
                        <Table aria-label="Automation run history">
                            <TableHead>
                                <TableRow sx={{ backgroundColor: 'action.hover' }}>
                                    <TableCell sx={{ width: 80 }}>Job ID</TableCell>
                                    <TableCell sx={{ minWidth: 220 }}>Test Case</TableCell>
                                    <TableCell sx={{ minWidth: 160 }}>Test Plan</TableCell>
                                    <TableCell sx={{ width: 110 }}>Batch</TableCell>
                                    <TableCell sx={{ width: 120 }}>Status</TableCell>
                                    <TableCell sx={{ width: 150 }}>Queued Time</TableCell>
                                    <TableCell sx={{ width: 150 }}>Finished Time</TableCell>
                                    <TableCell sx={{ width: 90 }}>Duration</TableCell>
                                    <TableCell align="right" sx={{ width: 100 }}>Actions</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {history.content.map((row) => (
                                    <TableRow
                                        key={row.jobId}
                                        hover
                                        sx={{ cursor: 'pointer' }}
                                        onClick={() => setSelectedJob(row)}
                                    >
                                        <TableCell>
                                            <Typography variant="body2" fontWeight={600}>
                                                #{row.jobId}
                                            </Typography>
                                        </TableCell>
                                        <TableCell>
                                            <Typography variant="body2" fontWeight={600}>
                                                {row.caseName}
                                            </Typography>
                                            <Typography variant="caption" color="text.secondary">
                                                Case #{row.caseId} · {row.testType}
                                            </Typography>
                                            {row.errorMessage && (
                                                <Typography
                                                    variant="caption"
                                                    color="error"
                                                    display="block"
                                                    noWrap
                                                    sx={{ maxWidth: 300 }}
                                                >
                                                    {row.errorMessage}
                                                </Typography>
                                            )}
                                        </TableCell>
                                        <TableCell>
                                            <Typography variant="body2">
                                                {row.planName || `#${row.planId}`}
                                            </Typography>
                                        </TableCell>
                                        <TableCell onClick={(e) => e.stopPropagation()}>
                                            {row.batchId ? (
                                                <Button
                                                    size="small"
                                                    variant="outlined"
                                                    sx={{ textTransform: 'none', py: 0.25, px: 1, minWidth: 'auto', fontSize: '0.75rem' }}
                                                    onClick={() => {
                                                        setBatchPlanId(row.planId);
                                                        setActiveBatchId(row.batchId);
                                                    }}
                                                >
                                                    Batch #{row.batchId}
                                                </Button>
                                            ) : (
                                                <Typography variant="caption" color="text.secondary">
                                                    Individual
                                                </Typography>
                                            )}
                                        </TableCell>
                                        <TableCell>{statusChip(row.status)}</TableCell>
                                        <TableCell>
                                            <Typography variant="body2">
                                                {new Date(row.createdAt).toLocaleDateString()}
                                            </Typography>
                                            <Typography variant="caption" color="text.secondary">
                                                {new Date(row.createdAt).toLocaleTimeString()}
                                            </Typography>
                                        </TableCell>
                                        <TableCell>
                                            {row.finishedAt ? (
                                                <>
                                                    <Typography variant="body2">
                                                        {new Date(row.finishedAt).toLocaleDateString()}
                                                    </Typography>
                                                    <Typography variant="caption" color="text.secondary">
                                                        {new Date(row.finishedAt).toLocaleTimeString()}
                                                    </Typography>
                                                </>
                                            ) : (
                                                <Typography variant="caption" color="text.secondary">—</Typography>
                                            )}
                                        </TableCell>
                                        <TableCell>
                                            <Typography variant="body2">
                                                {formatDuration(row.durationMs)}
                                            </Typography>
                                        </TableCell>
                                        <TableCell align="right" onClick={(e) => e.stopPropagation()}>
                                            <Tooltip title="View exact run details & investigation">
                                                <IconButton
                                                    size="small"
                                                    color="primary"
                                                    onClick={() => setSelectedJob(row)}
                                                    aria-label={`View job ${row.jobId}`}
                                                >
                                                    <VisibilityIcon fontSize="small" />
                                                </IconButton>
                                            </Tooltip>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>
                    <TablePagination
                        component="div"
                        count={history.totalElements}
                        page={page}
                        onPageChange={(_, newPage) => setPage(newPage)}
                        rowsPerPage={rowsPerPage}
                        onRowsPerPageChange={(e) => {
                            setRowsPerPage(parseInt(e.target.value, 10));
                            setPage(0);
                        }}
                        rowsPerPageOptions={[10, 20, 50, 100]}
                    />
                </Paper>
            )}

            {/* Run Detail Modal (uses existing AutomationRunDialog with jobId prop) */}
            {selectedJob && (
                <AutomationRunDialog
                    open={Boolean(selectedJob)}
                    jobId={selectedJob.jobId}
                    planId={selectedJob.planId}
                    planName={selectedJob.planName}
                    caseId={selectedJob.caseId}
                    caseName={selectedJob.caseName}
                    testType={selectedJob.testType}
                    projectId={projectId}
                    batchId={selectedJob.batchId}
                    onClose={() => setSelectedJob(null)}
                    onOpenBatch={(bId) => {
                        setBatchPlanId(selectedJob.planId);
                        setActiveBatchId(bId);
                    }}
                />
            )}

            {/* Batch Dialog if clicked from history row */}
            {activeBatchId != null && batchPlanId != null && (
                <AutomationBatchViewDialog
                    open={Boolean(activeBatchId)}
                    batchId={activeBatchId}
                    planId={batchPlanId}
                    onClose={() => {
                        setActiveBatchId(null);
                        setBatchPlanId(null);
                    }}
                />
            )}
        </Box>
    );
}
