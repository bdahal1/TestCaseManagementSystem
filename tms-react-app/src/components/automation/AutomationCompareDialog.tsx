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
    Divider,
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
    TableRow,
    Typography,
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import CompareArrowsIcon from '@mui/icons-material/CompareArrows';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import api from '../../services/api';
import { type AttemptSummary, type CompareAttemptResponse } from './types';

interface Props {
    open: boolean;
    projectId: number;
    planId: number;
    caseId: number;
    currentJobId: number;
    caseName: string;
    onClose: () => void;
}

export default function AutomationCompareDialog({
    open,
    projectId,
    planId,
    caseId,
    currentJobId,
    caseName,
    onClose,
}: Props) {
    const [attempts, setAttempts] = useState<AttemptSummary[]>([]);
    const [selectedJobIdB, setSelectedJobIdB] = useState<number | ''>('');
    const [loadingAttempts, setLoadingAttempts] = useState(false);
    const [loadingComparison, setLoadingComparison] = useState(false);
    const [comparison, setComparison] = useState<CompareAttemptResponse | null>(null);
    const [error, setError] = useState('');

    const attemptsVersion = useRef(0);
    const comparisonVersion = useRef(0);

    useEffect(() => {
        if (!open || !projectId || !planId || !caseId) {
            setAttempts([]);
            setSelectedJobIdB('');
            setComparison(null);
            setError('');
            return;
        }

        let active = true;
        attemptsVersion.current++;
        const currentVersion = attemptsVersion.current;
        setLoadingAttempts(true);
        setError('');

        api.get<AttemptSummary[]>(`/automation/history/project/${projectId}/case/${caseId}/plan/${planId}/attempts`)
            .then((res) => {
                if (!active || attemptsVersion.current !== currentVersion) return;
                const list = res.data || [];
                setAttempts(list);

                // Find candidate for comparison: previous attempt before currentJobId, or another attempt
                const currentIdx = list.findIndex(a => a.jobId === currentJobId);
                let defaultCompareId: number | '' = '';
                if (currentIdx > 0) {
                    defaultCompareId = list[currentIdx - 1].jobId;
                } else if (list.length > 1) {
                    const other = list.find(a => a.jobId !== currentJobId);
                    if (other) defaultCompareId = other.jobId;
                }
                setSelectedJobIdB(defaultCompareId);
            })
            .catch(() => {
                if (active && attemptsVersion.current === currentVersion) {
                    setError('Could not load attempts for this case.');
                }
            })
            .finally(() => {
                if (active && attemptsVersion.current === currentVersion) {
                    setLoadingAttempts(false);
                }
            });

        return () => {
            active = false;
        };
    }, [open, projectId, planId, caseId, currentJobId]);

    useEffect(() => {
        if (!open || !selectedJobIdB || !currentJobId || selectedJobIdB === currentJobId) {
            setComparison(null);
            return;
        }

        let active = true;
        comparisonVersion.current++;
        const currentVersion = comparisonVersion.current;
        setLoadingComparison(true);
        setError('');

        api.get<CompareAttemptResponse>(`/automation/history/project/${projectId}/compare?jobIdA=${currentJobId}&jobIdB=${selectedJobIdB}`)
            .then((res) => {
                if (!active || comparisonVersion.current !== currentVersion) return;
                setComparison(res.data);
            })
            .catch((e: any) => {
                if (!active || comparisonVersion.current !== currentVersion) return;
                setError(e?.response?.data?.message || 'Could not compare attempts.');
            })
            .finally(() => {
                if (!active || comparisonVersion.current !== currentVersion) return;
                setLoadingComparison(false);
            });

        return () => {
            active = false;
        };
    }, [open, projectId, currentJobId, selectedJobIdB]);

    const statusChipColor = (status?: string): 'default' | 'primary' | 'success' | 'error' | 'warning' => {
        switch (status) {
            case 'COMPLETED':
                return 'success';
            case 'FAILED':
                return 'error';
            case 'INFRASTRUCTURE_ERROR':
                return 'warning';
            case 'RUNNING':
                return 'primary';
            default:
                return 'default';
        }
    };

    const currentAttemptInfo = attempts.find(a => a.jobId === currentJobId);
    const comparedAttemptInfo = attempts.find(a => a.jobId === selectedJobIdB);

    return (
        <Dialog open={open} onClose={onClose} fullWidth maxWidth="lg">
            <DialogTitle>
                <Stack direction="row" justifyContent="space-between" alignItems="center">
                    <Stack direction="row" gap={1.5} alignItems="center">
                        <CompareArrowsIcon color="primary" />
                        <Box>
                            <Typography variant="h6">Compare Automation Attempts</Typography>
                            <Typography variant="body2" color="text.secondary">
                                Case #{caseId} · {caseName}
                            </Typography>
                        </Box>
                    </Stack>
                    <IconButton aria-label="Close comparison" onClick={onClose}>
                        <CloseIcon />
                    </IconButton>
                </Stack>
            </DialogTitle>
            <DialogContent dividers>
                {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}

                {loadingAttempts ? (
                    <Box sx={{ p: 4, textAlign: 'center' }}>
                        <CircularProgress size={32} />
                        <Typography sx={{ mt: 1 }} color="text.secondary">Loading attempt history...</Typography>
                    </Box>
                ) : attempts.length <= 1 ? (
                    <Alert severity="info" sx={{ my: 2 }}>
                        This case only has one recorded attempt in this plan ({currentAttemptInfo ? `Attempt #${currentAttemptInfo.attemptNumber}` : 'Current attempt'}). Multiple attempts within the same plan are needed to perform a comparison.
                    </Alert>
                ) : (
                    <>
                        <Paper variant="outlined" sx={{ p: 2, mb: 3, backgroundColor: 'background.default' }}>
                            <Stack direction={{ xs: 'column', md: 'row' }} gap={3} alignItems={{ md: 'center' }} justifyContent="space-between">
                                {/* Left: Current Attempt */}
                                <Box sx={{ flex: 1 }}>
                                    <Typography variant="caption" color="text.secondary" fontWeight={600} display="block">
                                        CURRENT RUN (Attempt #{currentAttemptInfo?.attemptNumber ?? 'Current'})
                                    </Typography>
                                    <Stack direction="row" gap={1} alignItems="center" sx={{ mt: 0.5 }}>
                                        <Typography fontWeight={700}>Job #{currentJobId}</Typography>
                                        {currentAttemptInfo && (
                                            <Chip
                                                size="small"
                                                label={currentAttemptInfo.status}
                                                color={statusChipColor(currentAttemptInfo.status)}
                                            />
                                        )}
                                        {comparison?.durationMsA != null && (
                                            <Typography variant="body2" color="text.secondary">
                                                {comparison.durationMsA}ms
                                            </Typography>
                                        )}
                                    </Stack>
                                    {currentAttemptInfo?.createdAt && (
                                        <Typography variant="caption" color="text.secondary" display="block">
                                            {new Date(currentAttemptInfo.createdAt).toLocaleString()}
                                        </Typography>
                                    )}
                                </Box>

                                <Divider orientation="vertical" flexItem sx={{ display: { xs: 'none', md: 'block' } }} />

                                {/* Center / Right: Compare With selector */}
                                <Box sx={{ flex: 1 }}>
                                    <FormControl fullWidth size="small">
                                        <InputLabel id="compare-attempt-label">Compare With Previous Attempt</InputLabel>
                                        <Select
                                            labelId="compare-attempt-label"
                                            label="Compare With Previous Attempt"
                                            value={selectedJobIdB}
                                            onChange={(e) => setSelectedJobIdB(Number(e.target.value))}
                                        >
                                            {attempts
                                                .filter(a => a.jobId !== currentJobId)
                                                .map(a => (
                                                    <MenuItem key={a.jobId} value={a.jobId}>
                                                        Attempt #{a.attemptNumber} · Job #{a.jobId} ({a.status}) — {new Date(a.createdAt).toLocaleTimeString()}
                                                    </MenuItem>
                                                ))}
                                        </Select>
                                    </FormControl>
                                    {comparedAttemptInfo && (
                                        <Stack direction="row" gap={1} alignItems="center" sx={{ mt: 0.75 }}>
                                            <Chip
                                                size="small"
                                                label={comparedAttemptInfo.status}
                                                color={statusChipColor(comparedAttemptInfo.status)}
                                            />
                                            {comparison?.durationMsB != null && (
                                                <Typography variant="body2" color="text.secondary">
                                                    {comparison.durationMsB}ms
                                                </Typography>
                                            )}
                                        </Stack>
                                    )}
                                </Box>
                            </Stack>
                        </Paper>

                        {comparison?.hasChangedSteps && (
                            <Alert
                                severity="warning"
                                icon={<WarningAmberIcon />}
                                sx={{ mb: 2.5 }}
                            >
                                <Typography fontWeight={600} variant="body2">
                                    Step definitions differ between these two attempts.
                                </Typography>
                                <Typography variant="caption" display="block" sx={{ mt: 0.25 }}>
                                    Steps are matched by action definition rather than sequential position. Added, removed, or edited steps are labeled below.
                                </Typography>
                            </Alert>
                        )}

                        {loadingComparison ? (
                            <Box sx={{ p: 4, textAlign: 'center' }}>
                                <CircularProgress size={28} />
                                <Typography sx={{ mt: 1 }} color="text.secondary">Comparing attempt results...</Typography>
                            </Box>
                        ) : comparison && comparison.steps.length > 0 ? (
                            <TableContainer component={Paper} variant="outlined">
                                <Table size="small" aria-label="Step comparison table">
                                    <TableHead>
                                        <TableRow sx={{ backgroundColor: 'action.hover' }}>
                                            <TableCell sx={{ width: 40 }}>#</TableCell>
                                            <TableCell sx={{ minWidth: 240 }}>Step Description</TableCell>
                                            <TableCell sx={{ width: 140 }}>Current (Job #{currentJobId})</TableCell>
                                            <TableCell sx={{ width: 140 }}>Previous (Job #{selectedJobIdB})</TableCell>
                                            <TableCell sx={{ width: 120 }}>Diff State</TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {comparison.steps.map((s, idx) => (
                                            <TableRow
                                                    key={idx}
                                                    sx={{
                                                        backgroundColor: s.onlyInA || s.onlyInB
                                                            ? 'rgba(255, 152, 0, 0.05)'
                                                            : s.passedA !== s.passedB
                                                            ? 'rgba(33, 150, 243, 0.04)'
                                                            : 'inherit',
                                                    }}
                                                >
                                                    <TableCell>{s.stepOrder}</TableCell>
                                                    <TableCell>
                                                        <Typography variant="body2" fontWeight={600}>
                                                            {s.description}
                                                        </Typography>
                                                    </TableCell>
                                                    {/* Attempt A */}
                                                    <TableCell>
                                                        {s.onlyInB ? (
                                                            <Typography variant="caption" color="text.secondary">—</Typography>
                                                        ) : (
                                                            <Box>
                                                                <Chip
                                                                    size="small"
                                                                    label={s.passedA == null ? 'PENDING' : s.passedA ? 'PASS' : 'FAIL'}
                                                                    color={s.passedA == null ? 'default' : s.passedA ? 'success' : 'error'}
                                                                />
                                                                {s.durationMsA != null && (
                                                                    <Typography variant="caption" color="text.secondary" display="block">
                                                                        {s.durationMsA}ms
                                                                    </Typography>
                                                                )}
                                                                {s.errorMessageA && (
                                                                    <Typography variant="caption" color="error" display="block" sx={{ whiteSpace: 'pre-wrap', mt: 0.5 }}>
                                                                        {s.errorMessageA}
                                                                    </Typography>
                                                                )}
                                                            </Box>
                                                        )}
                                                    </TableCell>
                                                    {/* Attempt B */}
                                                    <TableCell>
                                                        {s.onlyInA ? (
                                                            <Typography variant="caption" color="text.secondary">—</Typography>
                                                        ) : (
                                                            <Box>
                                                                <Chip
                                                                    size="small"
                                                                    label={s.passedB == null ? 'PENDING' : s.passedB ? 'PASS' : 'FAIL'}
                                                                    color={s.passedB == null ? 'default' : s.passedB ? 'success' : 'error'}
                                                                />
                                                                {s.durationMsB != null && (
                                                                    <Typography variant="caption" color="text.secondary" display="block">
                                                                        {s.durationMsB}ms
                                                                    </Typography>
                                                                )}
                                                                {s.errorMessageB && (
                                                                    <Typography variant="caption" color="error" display="block" sx={{ whiteSpace: 'pre-wrap', mt: 0.5 }}>
                                                                        {s.errorMessageB}
                                                                    </Typography>
                                                                )}
                                                            </Box>
                                                        )}
                                                    </TableCell>
                                                    {/* Diff Badge */}
                                                    <TableCell>
                                                        {s.onlyInA ? (
                                                            <Chip size="small" variant="outlined" color="warning" label="Only in current" />
                                                        ) : s.onlyInB ? (
                                                            <Chip size="small" variant="outlined" color="warning" label="Only in previous" />
                                                        ) : s.passedA !== s.passedB ? (
                                                            <Chip
                                                                size="small"
                                                                variant="outlined"
                                                                color={s.passedA ? 'success' : 'error'}
                                                                label={s.passedA ? 'Fixed' : 'Regressed'}
                                                            />
                                                        ) : (
                                                            <Typography variant="caption" color="text.secondary">Unchanged</Typography>
                                                        )}
                                                    </TableCell>
                                                </TableRow>
                                            ))}
                                    </TableBody>
                                </Table>
                            </TableContainer>
                        ) : comparison ? (
                            <Box sx={{ p: 4, textAlign: 'center' }}>
                                <Typography color="text.secondary">No step-level definitions available to compare.</Typography>
                            </Box>
                        ) : null}
                    </>
                )}
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>Close</Button>
            </DialogActions>
        </Dialog>
    );
}
