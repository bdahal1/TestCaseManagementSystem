import { useEffect, useRef, useState } from 'react';
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
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
    Link,
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
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
import CancelIcon from '@mui/icons-material/Cancel';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import CompareArrowsIcon from '@mui/icons-material/CompareArrows';
import HistoryIcon from '@mui/icons-material/History';
import api from '../../services/api';
import { type AutomationJob, type AutomationLivePreview as Preview, type JobStatus } from './types';
import AutomationLivePreview from './AutomationLivePreview';
import AutomationCompareDialog from './AutomationCompareDialog';

interface Props {
    open: boolean;
    planId: number;
    caseId: number;
    caseName: string;
    testType: string;
    jobId?: number;
    projectId?: number;
    planName?: string;
    batchId?: number | null;
    onClose: () => void;
    onFinished?: () => Promise<void>;
    onOpenBatch?: (batchId: number) => void;
}

export default function AutomationRunDialog({
    open,
    planId,
    caseId,
    caseName,
    testType,
    jobId,
    projectId,
    planName,
    batchId,
    onClose,
    onFinished,
    onOpenBatch,
}: Props) {
    const [job, setJob] = useState<AutomationJob | null>(null);
    const [running, setRunning] = useState(false);
    const [cancelling, setCancelling] = useState(false);
    const [error, setError] = useState('');
    const pollIntervalRef = useRef<ReturnType<typeof setTimeout> | null>(null);
    const [preview, setPreview] = useState<Preview | null>(null);
    const [previewError, setPreviewError] = useState(false);
    const previewSequence = useRef(0);
    const [compareOpen, setCompareOpen] = useState(false);

    const isHistorical = jobId != null;

    const onFinishedRef = useRef(onFinished);
    onFinishedRef.current = onFinished;

    const version = useRef(0);
    const cleanupPolling = () => {
        version.current++;
        if (pollIntervalRef.current) {
            clearTimeout(pollIntervalRef.current);
            pollIntervalRef.current = null;
        }
    };

    useEffect(() => {
        let active = true;
        cleanupPolling();
        const requestVersion = version.current;
        setJob(null); setError(''); setRunning(false); setCancelling(false);
        setPreview(null); setPreviewError(false); previewSequence.current = 0;
        setCompareOpen(false);

        if (open) {
            const path = jobId != null ? `/automation/jobs/${jobId}` : `/automation/jobs/plan/${planId}/case/${caseId}/latest`;
            api.get<AutomationJob>(path).then((res) => {
                if (!active || version.current !== requestVersion || !res.data) return;
                setJob(res.data);
                if (['QUEUED', 'RUNNING'].includes(res.data.status)) {
                    setRunning(true);
                    startPolling(res.data.jobId, requestVersion);
                } else {
                    void loadPreview(res.data.jobId, requestVersion);
                }
            }).catch(() => { if (active && version.current === requestVersion) setError('Could not load this automation run.'); });
        }
        return () => { active = false; cleanupPolling(); };
    }, [open, planId, caseId, jobId]);

    const effectivePlanId = job?.planId ?? planId;
    const effectiveBatchId = (job as any)?.batchId ?? batchId;

    const openEvidence = async (id: number) => {
        try {
            const { data } = await api.get(`/testPlans/${effectivePlanId}/evidence/${id}`, { responseType: 'blob' });
            const url = URL.createObjectURL(data);
            const link = document.createElement('a');
            link.href = url; link.download = `automation-job-${job?.jobId}-evidence.png`; link.click();
            URL.revokeObjectURL(url);
        } catch { setError('Could not download evidence for this run.'); }
    };

    const pollJobStatus = async (pollId: number, requestVersion = version.current): Promise<boolean> => {
        try {
            const { data } = await api.get<AutomationJob>(`/automation/jobs/${pollId}`);
            if (version.current !== requestVersion) return false;
            setJob(data);
            setError(previous => previous === 'Could not refresh this automation run.' ? '' : previous);

            if (data.status !== 'QUEUED' && data.status !== 'CANCELLED') await loadPreview(pollId, requestVersion);
            if (version.current !== requestVersion) return false;

            if (['COMPLETED', 'FAILED', 'INFRASTRUCTURE_ERROR', 'CANCELLED'].includes(data.status)) {
                cleanupPolling();
                setRunning(false);
                setCancelling(false);
                const finished = onFinishedRef.current;
                if (finished) void finished().catch(() => {});
                return false;
            }
            return true;
        } catch {
            if (version.current === requestVersion) setError('Could not refresh this automation run.');
            return version.current === requestVersion;
        }
    };

    const loadPreview = async (id: number, requestVersion: number) => {
        try {
            const response = await api.get<Preview>(`/automation/jobs/${id}/preview?afterSequence=${previewSequence.current}`);
            if (version.current !== requestVersion) return;
            if (response.data && response.data.jobId === id && response.data.sequence > previewSequence.current) {
                setPreview(response.data);
                previewSequence.current = response.data.sequence;
            }
            setPreviewError(false);
        } catch {
            if (version.current === requestVersion) setPreviewError(true);
        }
    };

    const startPolling = (id: number, requestVersion: number) => {
        const tick = async () => {
            if (version.current !== requestVersion) return;
            if (await pollJobStatus(id, requestVersion) && version.current === requestVersion) {
                pollIntervalRef.current = setTimeout(() => { void tick(); }, 1500);
            }
        };
        void tick();
    };

    const handleStartRun = async () => {
        if (isHistorical) return;
        setRunning(true);
        setError('');
        setJob(null);
        setPreview(null); setPreviewError(false); previewSequence.current = 0;
        cleanupPolling();
        const requestVersion = version.current;

        try {
            const { data } = await api.post<{ jobId: number }>(`/automation/jobs/plan/${planId}/case/${caseId}/run`);
            if (version.current !== requestVersion) return;
            const newJobId = data.jobId;
            startPolling(newJobId, requestVersion);
        } catch (e: any) {
            if (version.current !== requestVersion) return;
            setRunning(false);
            setError(e?.response?.data?.message || 'Failed to schedule automation run.');
        }
    };

    const handleCancelRun = async () => {
        if (!job || isHistorical) return;
        setCancelling(true);
        const requestVersion = version.current;
        try {
            await api.post(`/automation/jobs/${job.jobId}/cancel`);
            await pollJobStatus(job.jobId, requestVersion);
        } catch (e: any) {
            if (version.current !== requestVersion) return;
            setError(e?.response?.data?.message || 'Failed to cancel job');
            setCancelling(false);
        }
    };

    const statusColor = (status: JobStatus): 'default' | 'primary' | 'success' | 'error' | 'warning' => {
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

    return (
        <>
            <Dialog open={open} onClose={running && !isHistorical ? undefined : onClose} fullWidth maxWidth="md">
                <DialogTitle>
                    <Stack direction="row" justifyContent="space-between" alignItems="center">
                        <Box>
                            <Stack direction="row" gap={1} alignItems="center">
                                <Typography variant="h6">
                                    {isHistorical ? `Automation Run Details #${jobId}` : 'Browser Automation Runner'}
                                </Typography>
                                {isHistorical && (
                                    <Chip size="small" icon={<HistoryIcon />} label="Historical (Read-Only)" variant="outlined" />
                                )}
                            </Stack>
                            <Typography variant="body2" color="text.secondary">
                                Case #{caseId} · {caseName} ({testType})
                            </Typography>
                            {/* Associated Plan and Batch links */}
                            <Stack direction="row" gap={1.5} alignItems="center" sx={{ mt: 0.5 }}>
                                {effectivePlanId && (
                                    <Typography variant="caption" color="text.secondary">
                                        Plan: {planName ? (
                                            <Link href={`/dashboard/project/${projectId || ''}/test-plans/${effectivePlanId}`} underline="hover">
                                                {planName}
                                            </Link>
                                        ) : (
                                            `#${effectivePlanId}`
                                        )}
                                    </Typography>
                                )}
                                {effectiveBatchId && (
                                    <Typography variant="caption" color="text.secondary">
                                        · Batch:{' '}
                                        {onOpenBatch ? (
                                            <Button
                                                size="small"
                                                variant="text"
                                                sx={{ p: 0, minWidth: 'auto', fontSize: '0.75rem', textTransform: 'none' }}
                                                onClick={() => onOpenBatch(effectiveBatchId)}
                                            >
                                                #{effectiveBatchId}
                                            </Button>
                                        ) : (
                                            `#${effectiveBatchId}`
                                        )}
                                    </Typography>
                                )}
                            </Stack>
                        </Box>
                        <IconButton aria-label="Close runner" onClick={onClose} disabled={running && !isHistorical}>
                            <CloseIcon />
                        </IconButton>
                    </Stack>
                </DialogTitle>
                <DialogContent dividers>
                    {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}

                    <Paper variant="outlined" sx={{ p: 2.5, mb: 2.5, backgroundColor: 'background.paper', borderRadius: 2 }}>
                        <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'center' }} gap={2}>
                            <Box>
                                <Typography variant="subtitle2" color="text.secondary">
                                    Execution State
                                </Typography>
                                <Stack direction="row" gap={1} alignItems="center" sx={{ mt: 0.5 }}>
                                    {job ? (
                                        <Chip
                                            label={job.status}
                                            color={statusColor(job.status)}
                                            size="medium"
                                            icon={
                                                job.status === 'COMPLETED' ? <CheckCircleOutlineIcon /> :
                                                job.status === 'FAILED' ? <ErrorOutlineIcon /> :
                                                job.status === 'RUNNING' ? <CircularProgress size={16} color="inherit" /> :
                                                undefined
                                            }
                                        />
                                    ) : (
                                        <Chip label="Ready to run" variant="outlined" />
                                    )}
                                    {job?.finishedAt && (
                                        <Typography variant="caption" color="text.secondary">
                                            Finished {new Date(job.finishedAt).toLocaleTimeString()}
                                        </Typography>
                                    )}
                                </Stack>
                            </Box>

                            <Stack direction="row" gap={1} alignItems="center">
                                {/* Compare with previous attempt button */}
                                {job && effectivePlanId && projectId && (
                                    <Button
                                        variant="outlined"
                                        startIcon={<CompareArrowsIcon />}
                                        onClick={() => setCompareOpen(true)}
                                    >
                                        Compare Attempt
                                    </Button>
                                )}

                                {!isHistorical && running && (
                                    <Button
                                        variant="outlined"
                                        color="error"
                                        startIcon={<CancelIcon />}
                                        disabled={cancelling}
                                        onClick={() => void handleCancelRun()}
                                    >
                                        {cancelling ? 'Cancelling...' : 'Cancel Run'}
                                    </Button>
                                )}
                                {!isHistorical && (
                                    <Button
                                        variant="contained"
                                        startIcon={running ? <CircularProgress size={18} color="inherit" /> : <PlayArrowIcon />}
                                        disabled={running}
                                        onClick={() => void handleStartRun()}
                                    >
                                        {running ? 'Running...' : job ? 'Run Again' : 'Run Test Now'}
                                    </Button>
                                )}
                            </Stack>
                        </Stack>

                        {running && <LinearProgress sx={{ mt: 2, borderRadius: 1 }} />}

                        {job?.errorMessage && (
                            <Alert severity={job.status === 'INFRASTRUCTURE_ERROR' ? 'warning' : 'error'} sx={{ mt: 2 }}>
                                <Typography fontWeight={600}>
                                    {job.status === 'INFRASTRUCTURE_ERROR' ? 'Infrastructure Failure:' : 'Test Failure:'}
                                </Typography>
                                <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', mt: 0.5 }}>
                                    {job.errorMessage}
                                </Typography>
                            </Alert>
                        )}
                    </Paper>

                    {job && (running || preview) && <AutomationLivePreview preview={preview} status={job.status} error={previewError} />}

                    {/* Evidence Section */}
                    {job?.failureEvidenceId ? (
                        <Box sx={{ mb: 2 }}>
                            <Button variant="outlined" size="small" onClick={() => void openEvidence(job.failureEvidenceId!)}>
                                Download failure evidence
                            </Button>
                        </Box>
                    ) : job && job.status === 'FAILED' ? (
                        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 2 }}>
                            No screenshot evidence was attached to this run.
                        </Typography>
                    ) : null}

                    {/* Step Results Section */}
                    {job?.stepResults && job.stepResults.length > 0 ? (
                        <Box sx={{ mb: 3 }}>
                            <Typography variant="subtitle1" fontWeight={700} sx={{ mb: 1 }}>
                                Step-Level Outcomes ({job.stepResults.filter((s) => s.passed).length}/{job.stepResults.length} passed)
                            </Typography>
                            <TableContainer component={Paper} variant="outlined">
                                <Table size="small">
                                    <TableHead>
                                        <TableRow>
                                            <TableCell sx={{ width: 40 }}>#</TableCell>
                                            <TableCell>Step Description</TableCell>
                                            <TableCell sx={{ width: 100 }}>Status</TableCell>
                                            <TableCell sx={{ width: 100 }}>Duration</TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {job.stepResults.map((s, idx) => (
                                            <TableRow key={idx} sx={{ backgroundColor: s.passed ? 'inherit' : 'rgba(211, 47, 47, 0.04)' }}>
                                                <TableCell>{s.stepOrder}</TableCell>
                                                <TableCell>
                                                    <Typography variant="body2" fontWeight={600}>
                                                        {s.description}
                                                    </Typography>
                                                    {s.errorMessage && (
                                                        <Typography variant="caption" color="error" display="block" sx={{ mt: 0.5, whiteSpace: 'pre-wrap' }}>
                                                            {s.errorMessage}
                                                        </Typography>
                                                    )}
                                                </TableCell>
                                                <TableCell>
                                                    <Chip
                                                        size="small"
                                                        label={s.passed ? 'PASS' : 'FAIL'}
                                                        color={s.passed ? 'success' : 'error'}
                                                    />
                                                </TableCell>
                                                <TableCell>{s.durationMs}ms</TableCell>
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            </TableContainer>
                        </Box>
                    ) : job && ['COMPLETED', 'FAILED', 'INFRASTRUCTURE_ERROR', 'CANCELLED'].includes(job.status) ? (
                        <Box sx={{ mb: 2, p: 2, border: '1px dashed', borderColor: 'divider', borderRadius: 1 }}>
                            <Typography variant="body2" color="text.secondary" align="center">
                                No step-level outcomes were recorded for this attempt.
                            </Typography>
                        </Box>
                    ) : null}

                    {/* Diagnostics Section */}
                    {job?.diagnosticLogs ? (
                        <Accordion variant="outlined">
                            <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                                <Typography variant="subtitle2" fontWeight={600}>
                                    Diagnostic Execution Logs
                                </Typography>
                            </AccordionSummary>
                            <AccordionDetails>
                                <Box
                                    component="pre"
                                    sx={{
                                        m: 0,
                                        p: 2,
                                        backgroundColor: '#1e1e1e',
                                        color: '#d4d4d4',
                                        borderRadius: 1.5,
                                        fontSize: '0.8rem',
                                        fontFamily: 'monospace',
                                        maxHeight: 250,
                                        overflowY: 'auto',
                                        whiteSpace: 'pre-wrap',
                                    }}
                                >
                                    {job.diagnosticLogs}
                                </Box>
                            </AccordionDetails>
                        </Accordion>
                    ) : job && ['COMPLETED', 'FAILED', 'INFRASTRUCTURE_ERROR', 'CANCELLED'].includes(job.status) ? (
                        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
                            No diagnostic execution logs recorded for this attempt.
                        </Typography>
                    ) : null}
                </DialogContent>
                <DialogActions>
                    <Button onClick={onClose} disabled={running && !isHistorical}>
                        Close
                    </Button>
                </DialogActions>
            </Dialog>

            {/* Compare attempts modal */}
            {job && effectivePlanId && projectId && (
                <AutomationCompareDialog
                    open={compareOpen}
                    projectId={projectId}
                    planId={effectivePlanId}
                    caseId={caseId}
                    currentJobId={job.jobId}
                    caseName={caseName}
                    onClose={() => setCompareOpen(false)}
                />
            )}
        </>
    );
}
