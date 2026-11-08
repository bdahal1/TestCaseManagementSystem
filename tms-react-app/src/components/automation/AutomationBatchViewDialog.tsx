import { useEffect, useRef, useState } from 'react';
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    IconButton,
    LinearProgress,
    Paper,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Typography,
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
import CancelIcon from '@mui/icons-material/Cancel';
import ReplayIcon from '@mui/icons-material/Replay';
import VisibilityIcon from '@mui/icons-material/Visibility';
import api from '../../services/api';
import { type BatchDetailResponse, type BatchStatus, type JobStatus } from './types';
import AutomationRunDialog from './AutomationRunDialog';

interface Props {
    open: boolean;
    batchId: number | null;
    planId: number;
    onClose: () => void;
    onBatchFinished?: () => Promise<void>;
    onBatchChanged?: (batch: BatchDetailResponse) => void;
}

export default function AutomationBatchViewDialog({
    open,
    batchId,
    planId,
    onClose,
    onBatchFinished,
    onBatchChanged,
}: Props) {
    const [batch, setBatch] = useState<BatchDetailResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [cancelling, setCancelling] = useState(false);
    const [retrying, setRetrying] = useState(false);
    const [error, setError] = useState('');
    const [detailJob, setDetailJob] = useState<{ jobId: number; caseId: number; caseName: string; testType: string } | null>(null);

    const pollIntervalRef = useRef<ReturnType<typeof setTimeout> | null>(null);
    const version = useRef(0);
    const onFinishedRef = useRef(onBatchFinished);
    const onChangedRef = useRef(onBatchChanged);
    onFinishedRef.current = onBatchFinished;
    onChangedRef.current = onBatchChanged;

    const cleanupPolling = () => {
        version.current++;
        if (pollIntervalRef.current) clearTimeout(pollIntervalRef.current);
        pollIntervalRef.current = null;
    };

    const acceptBatch = (data: BatchDetailResponse, requestVersion: number) => {
        if (version.current !== requestVersion) return;
        setBatch(data);
        if (data.isFinished) {
            cleanupPolling();
            const finished = onFinishedRef.current;
            if (finished) void finished().catch(() => {});
        } else {
            pollIntervalRef.current = setTimeout(() => { void fetchBatchStatus(data.batchId, requestVersion); }, 1500);
        }
    };

    const fetchBatchStatus = async (id: number, requestVersion = version.current) => {
        try {
            const { data } = await api.get<BatchDetailResponse>(`/automation/batches/${id}`);
            if (version.current !== requestVersion) return;
            setError('');
            setLoading(false);
            acceptBatch(data, requestVersion);
        } catch (e: any) {
            if (version.current !== requestVersion) return;
            setLoading(false);
            setError(e?.response?.data?.message || 'Could not load batch details.');
            pollIntervalRef.current = setTimeout(() => { void fetchBatchStatus(id, requestVersion); }, 1500);
        }
    };

    useEffect(() => {
        cleanupPolling();
        setBatch(null);
        setDetailJob(null);
        setCancelling(false);
        setRetrying(false);
        setError('');
        setLoading(open && !!batchId);
        if (open && batchId) void fetchBatchStatus(batchId, version.current);
        return cleanupPolling;
    }, [open, batchId]);

    const handleCancelBatch = async () => {
        if (!batch || cancelling) return;
        cleanupPolling();
        const requestVersion = version.current;
        setCancelling(true); setError('');
        try {
            const { data } = await api.post<BatchDetailResponse>(`/automation/batches/${batch.batchId}/cancel`);
            if (version.current !== requestVersion) return;
            setCancelling(false);
            acceptBatch(data, requestVersion);
        } catch (e: any) {
            if (version.current !== requestVersion) return;
            setCancelling(false);
            setError(e?.response?.data?.message || 'Failed to cancel batch.');
            pollIntervalRef.current = setTimeout(() => { void fetchBatchStatus(batch.batchId, requestVersion); }, 1500);
        }
    };

    const handleRetryFailed = async () => {
        if (!batch || retrying) return;
        cleanupPolling();
        const requestVersion = version.current;
        setRetrying(true); setError('');
        try {
            const { data } = await api.post<BatchDetailResponse>(`/automation/batches/${batch.batchId}/retry-failed`);
            if (version.current !== requestVersion) return;
            setRetrying(false);
            acceptBatch(data, requestVersion);
            onChangedRef.current?.(data);
        } catch (e: any) {
            if (version.current !== requestVersion) return;
            setRetrying(false);
            setError(e?.response?.data?.message || 'Failed to retry failed cases.');
        }
    };

    const handleCancelJob = async (jobId: number) => {
        if (!batch) return;
        cleanupPolling();
        const requestVersion = version.current;
        try {
            await api.post(`/automation/jobs/${jobId}/cancel`);
        } catch (e: any) {
            if (version.current === requestVersion) setError(e?.response?.data?.message || 'Failed to cancel job.');
        }
        if (version.current === requestVersion) void fetchBatchStatus(batch.batchId, requestVersion);
    };

    const batchStatusColor = (status: BatchStatus): 'default' | 'primary' | 'success' | 'error' | 'warning' => {
        switch (status) {
            case 'QUEUED':
                return 'default';
            case 'RUNNING':
                return 'primary';
            case 'COMPLETED':
                return 'success';
            case 'FAILED':
                return 'error';
            case 'CANCELLED':
                return 'warning';
            default:
                return 'default';
        }
    };

    const batchStatusLabel = (status: BatchStatus): string => {
        switch (status) {
            case 'QUEUED':
                return 'Queued';
            case 'RUNNING':
                return 'Running';
            case 'COMPLETED':
                return 'All Passed';
            case 'FAILED':
                return 'Completed with Failures';
            case 'CANCELLED':
                return 'Cancelled';
            default:
                return status;
        }
    };

    const jobStatusColor = (status: JobStatus): 'default' | 'primary' | 'success' | 'error' | 'warning' => {
        switch (status) {
            case 'QUEUED':
                return 'default';
            case 'RUNNING':
                return 'primary';
            case 'COMPLETED':
                return 'success';
            case 'FAILED':
                return 'error';
            case 'INFRASTRUCTURE_ERROR':
                return 'warning';
            case 'CANCELLED':
                return 'default';
            default:
                return 'default';
        }
    };

    const hasFailuresOrCancellations =
        batch != null &&
        batch.isFinished &&
        batch.canRetryFailed === true;

    return (
        <>
            <Dialog open={open} onClose={onClose} fullWidth maxWidth="lg">
                <DialogTitle>
                    <Stack direction="row" justifyContent="space-between" alignItems="center">
                        <Box>
                            <Typography variant="h6" fontWeight={700}>
                                Batch Automation Run #{batch?.batchId ?? batchId}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                {batch?.planName} · Type: {batch?.batchType} · Triggered by {batch?.createdBy || 'User'}
                            </Typography>
                        </Box>
                        <IconButton aria-label="Close batch view" onClick={onClose}>
                            <CloseIcon />
                        </IconButton>
                    </Stack>
                </DialogTitle>
                <DialogContent dividers>
                    {loading && (
                        <Box sx={{ p: 6, textAlign: 'center' }}>
                            <CircularProgress />
                            <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
                                Loading batch details...
                            </Typography>
                        </Box>
                    )}

                    {error && (
                        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
                            {error}
                        </Alert>
                    )}

                    {!loading && batch && (
                        <>
                            <Paper variant="outlined" sx={{ p: 2.5, mb: 3, backgroundColor: 'background.default', borderRadius: 2 }}>
                                <Stack direction={{ xs: 'column', md: 'row' }} justifyContent="space-between" alignItems={{ md: 'center' }} gap={2}>
                                    <Box>
                                        <Typography variant="caption" color="text.secondary">
                                            Overall Batch State
                                        </Typography>
                                        <Stack direction="row" gap={1.5} alignItems="center" sx={{ mt: 0.5 }}>
                                            <Chip
                                                label={batchStatusLabel(batch.status)}
                                                color={batchStatusColor(batch.status)}
                                                size="medium"
                                                icon={
                                                    batch.status === 'COMPLETED' ? <CheckCircleOutlineIcon /> :
                                                    batch.status === 'FAILED' ? <ErrorOutlineIcon /> :
                                                    batch.status === 'RUNNING' ? <CircularProgress size={16} color="inherit" /> :
                                                    batch.status === 'CANCELLED' ? <CancelIcon /> :
                                                    undefined
                                                }
                                            />
                                            <Typography variant="body2" fontWeight={600}>
                                                {batch.percentComplete}% Complete ({batch.totalCases - (batch.counts.queued + batch.counts.running)} of {batch.totalCases} resolved)
                                            </Typography>
                                        </Stack>
                                    </Box>

                                    <Stack direction="row" gap={1} flexWrap="wrap" alignItems="center">
                                        {!batch.isFinished && batch.canCancel && (
                                            <Button
                                                variant="outlined"
                                                color="error"
                                                startIcon={cancelling ? <CircularProgress size={16} color="inherit" /> : <CancelIcon />}
                                                disabled={cancelling}
                                                onClick={() => void handleCancelBatch()}
                                            >
                                                {cancelling ? 'Cancelling...' : 'Cancel Batch'}
                                            </Button>
                                        )}
                                        {hasFailuresOrCancellations && (
                                            <Button
                                                variant="contained"
                                                color="primary"
                                                startIcon={retrying ? <CircularProgress size={16} color="inherit" /> : <ReplayIcon />}
                                                disabled={retrying}
                                                onClick={() => void handleRetryFailed()}
                                            >
                                                {retrying ? 'Retrying...' : 'Run Failed Cases Again'}
                                            </Button>
                                        )}
                                    </Stack>
                                </Stack>

                                <LinearProgress
                                    variant="determinate"
                                    value={batch.percentComplete}
                                    sx={{ mt: 2.5, mb: 2, height: 8, borderRadius: 4 }}
                                />

                                <Stack direction="row" gap={1} flexWrap="wrap">
                                    <Chip size="small" variant="outlined" label={`${batch.counts.queued} Queued`} />
                                    <Chip size="small" color={batch.counts.running > 0 ? 'primary' : 'default'} variant="outlined" label={`${batch.counts.running} Running`} />
                                    <Chip size="small" color="success" variant="outlined" label={`${batch.counts.passed} Passed`} />
                                    <Chip size="small" color={batch.counts.failed > 0 ? 'error' : 'default'} variant="outlined" label={`${batch.counts.failed} Failed`} />
                                    {batch.counts.infrastructureError > 0 && (
                                        <Chip size="small" color="warning" variant="outlined" label={`${batch.counts.infrastructureError} Infra Error`} />
                                    )}
                                    {batch.counts.cancelled > 0 && (
                                        <Chip size="small" variant="outlined" label={`${batch.counts.cancelled} Cancelled`} />
                                    )}
                                </Stack>
                            </Paper>

                            {batch.status === 'FAILED' && (
                                <Alert severity="error" sx={{ mb: 2 }}>
                                    Batch completed with failures. One or more test cases encountered assertion failures or errors. Inspect the failed cases below for diagnostic details.
                                </Alert>
                            )}

                            {batch.status === 'COMPLETED' && (
                                <Alert severity="success" sx={{ mb: 2 }}>
                                    All {batch.totalCases} automated test cases in this batch passed successfully!
                                </Alert>
                            )}

                            <TableContainer component={Paper} variant="outlined" sx={{ maxHeight: 420 }}>
                                <Table size="small" stickyHeader aria-label="Batch job results">
                                    <TableHead>
                                        <TableRow>
                                            <TableCell sx={{ width: 60 }}>#ID</TableCell>
                                            <TableCell sx={{ width: 220 }}>Test Case</TableCell>
                                            <TableCell sx={{ width: 140 }}>Type</TableCell>
                                            <TableCell sx={{ width: 120 }}>Status</TableCell>
                                            <TableCell sx={{ width: 90 }}>Duration</TableCell>
                                            <TableCell>Summary / Error</TableCell>
                                            <TableCell align="right" sx={{ width: 150 }}>Actions</TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {batch.jobs.map((job) => (
                                            <TableRow key={job.jobId} hover>
                                                <TableCell>#{job.caseId}</TableCell>
                                                <TableCell>
                                                    <Typography variant="body2" fontWeight={600}>
                                                        {job.caseName}
                                                    </Typography>
                                                    <Typography variant="caption" color="text.secondary">
                                                        Job #{job.jobId} {job.assignedWorkerId ? `· ${job.assignedWorkerId}` : ''}
                                                    </Typography>
                                                </TableCell>
                                                <TableCell>
                                                    <Chip size="small" label={job.testType} variant="outlined" />
                                                </TableCell>
                                                <TableCell>
                                                    <Chip
                                                        size="small"
                                                        label={job.status}
                                                        color={jobStatusColor(job.status)}
                                                        icon={
                                                            job.status === 'COMPLETED' ? <CheckCircleOutlineIcon /> :
                                                            job.status === 'FAILED' ? <ErrorOutlineIcon /> :
                                                            job.status === 'RUNNING' ? <CircularProgress size={12} color="inherit" /> :
                                                            undefined
                                                        }
                                                    />
                                                </TableCell>
                                                <TableCell>
                                                    {job.durationMs != null ? `${(job.durationMs / 1000).toFixed(1)}s` : '—'}
                                                </TableCell>
                                                <TableCell>
                                                    {job.errorMessage ? (
                                                        <Typography variant="caption" color="error.main" sx={{ display: 'block', maxHeight: 48, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                                                            {job.errorMessage}
                                                        </Typography>
                                                    ) : job.stepSummary ? (
                                                        <Typography variant="body2" color="text.secondary">
                                                            {job.stepSummary}
                                                        </Typography>
                                                    ) : (
                                                        <Typography variant="caption" color="text.secondary">
                                                            {job.status === 'QUEUED' ? 'Waiting for available worker...' : job.status === 'RUNNING' ? 'Executing browser steps...' : '—'}
                                                        </Typography>
                                                    )}
                                                </TableCell>
                                                <TableCell align="right">
                                                    <Stack direction="row" gap={0.5} justifyContent="flex-end">
                                                        {job.canCancel && (
                                                            <Button
                                                                size="small"
                                                                color="error"
                                                                onClick={() => void handleCancelJob(job.jobId)}
                                                            >
                                                                Cancel
                                                            </Button>
                                                        )}
                                                        <IconButton
                                                            size="small"
                                                            title="View case execution details"
                                                            onClick={() => setDetailJob({ jobId: job.jobId, caseId: job.caseId, caseName: job.caseName, testType: job.testType })}
                                                        >
                                                            <VisibilityIcon fontSize="small" />
                                                        </IconButton>
                                                    </Stack>
                                                </TableCell>
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            </TableContainer>
                        </>
                    )}
                </DialogContent>
                <DialogActions sx={{ p: 2 }}>
                    <Button onClick={onClose}>
                        Close
                    </Button>
                </DialogActions>
            </Dialog>

            {detailJob && (
                <AutomationRunDialog
                    open={!!detailJob}
                    planId={planId}
                    caseId={detailJob.caseId}
                    jobId={detailJob.jobId}
                    caseName={detailJob.caseName}
                    testType={detailJob.testType}
                    onClose={() => setDetailJob(null)}
                    onFinished={async () => {
                        if (batch) { cleanupPolling(); void fetchBatchStatus(batch.batchId, version.current); }
                    }}
                />
            )}
        </>
    );
}
