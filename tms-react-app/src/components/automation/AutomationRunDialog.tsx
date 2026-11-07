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
import api from '../../services/api';
import { type AutomationJob, type JobStatus } from './types';

interface Props {
    open: boolean;
    planId: number;
    caseId: number;
    caseName: string;
    testType: string;
    onClose: () => void;
    onFinished: () => Promise<void>;
}

export default function AutomationRunDialog({
    open,
    planId,
    caseId,
    caseName,
    testType,
    onClose,
    onFinished,
}: Props) {
    const [job, setJob] = useState<AutomationJob | null>(null);
    const [running, setRunning] = useState(false);
    const [cancelling, setCancelling] = useState(false);
    const [error, setError] = useState('');
    const pollIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

    const cleanupPolling = () => {
        if (pollIntervalRef.current) {
            clearInterval(pollIntervalRef.current);
            pollIntervalRef.current = null;
        }
    };

    useEffect(() => {
        if (open) {
            setError('');
            // Check if there is an existing/latest job for this case
            api.get<AutomationJob>(`/automation/jobs/plan/${planId}/case/${caseId}/latest`)
                .then((res) => {
                    if (res.data) setJob(res.data);
                })
                .catch(() => {});
        } else {
            cleanupPolling();
            setJob(null);
            setRunning(false);
            setCancelling(false);
        }
    }, [open, planId, caseId]);

    const pollJobStatus = async (jobId: number) => {
        try {
            const { data } = await api.get<AutomationJob>(`/automation/jobs/${jobId}`);
            setJob(data);

            if (['COMPLETED', 'FAILED', 'INFRASTRUCTURE_ERROR', 'CANCELLED'].includes(data.status)) {
                cleanupPolling();
                setRunning(false);
                setCancelling(false);
                void onFinished();
            }
        } catch (e: any) {
            console.error('Failed to poll automation job status', e);
        }
    };

    const handleStartRun = async () => {
        setRunning(true);
        setError('');
        setJob(null);
        cleanupPolling();

        try {
            const { data } = await api.post<{ jobId: number }>(`/automation/jobs/plan/${planId}/case/${caseId}/run`);
            const jobId = data.jobId;

            // Start polling every 1.5 seconds
            await pollJobStatus(jobId);
            pollIntervalRef.current = setInterval(() => {
                void pollJobStatus(jobId);
            }, 1500);
        } catch (e: any) {
            setRunning(false);
            setError(e?.response?.data?.message || 'Failed to schedule automation run.');
        }
    };

    const handleCancelRun = async () => {
        if (!job) return;
        setCancelling(true);
        try {
            await api.post(`/automation/jobs/${job.jobId}/cancel`);
            await pollJobStatus(job.jobId);
        } catch (e: any) {
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
        <Dialog open={open} onClose={running ? undefined : onClose} fullWidth maxWidth="md">
            <DialogTitle>
                <Stack direction="row" justifyContent="space-between" alignItems="center">
                    <Box>
                        <Typography variant="h6">Browser Automation Runner</Typography>
                        <Typography variant="body2" color="text.secondary">
                            Case #{caseId} · {caseName} ({testType})
                        </Typography>
                    </Box>
                    <IconButton aria-label="Close runner" onClick={onClose} disabled={running}>
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

                        <Stack direction="row" gap={1}>
                            {running && (
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
                            <Button
                                variant="contained"
                                startIcon={running ? <CircularProgress size={18} color="inherit" /> : <PlayArrowIcon />}
                                disabled={running}
                                onClick={() => void handleStartRun()}
                            >
                                {running ? 'Running...' : job ? 'Run Again' : 'Run Test Now'}
                            </Button>
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

                {job?.stepResults && job.stepResults.length > 0 && (
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
                )}

                {job?.diagnosticLogs && (
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
                )}
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} disabled={running}>
                    Close
                </Button>
            </DialogActions>
        </Dialog>
    );
}
