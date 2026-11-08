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
    Typography,
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import BlockIcon from '@mui/icons-material/Block';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
import HourglassEmptyIcon from '@mui/icons-material/HourglassEmpty';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import api from '../../services/api';
import { type BatchPreviewResponse, type BatchDetailResponse } from './types';

interface Props {
    open: boolean;
    planId: number;
    planName: string;
    scope: 'ALL_ELIGIBLE' | 'SELECTED';
    selectedCaseIds?: number[];
    onClose: () => void;
    onConfirmed: (batch: BatchDetailResponse) => void;
}

export default function AutomationBatchPreviewDialog({
    open,
    planId,
    planName,
    scope,
    selectedCaseIds,
    onClose,
    onConfirmed,
}: Props) {
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState('');
    const [preview, setPreview] = useState<BatchPreviewResponse | null>(null);
    const [tabIndex, setTabIndex] = useState(0);

    const version = useRef(0);
    useEffect(() => {
        const requestVersion = ++version.current;
        setSubmitting(false);
        if (open) {
            setLoading(true);
            setError('');
            setPreview(null);
            setTabIndex(0);

            api.post<BatchPreviewResponse>(`/automation/batches/plan/${planId}/preview`, {
                scope,
                caseIds: scope === 'SELECTED' ? selectedCaseIds : undefined,
            })
                .then((res) => {
                    if (version.current === requestVersion) setPreview(res.data);
                })
                .catch((e: any) => {
                    if (version.current === requestVersion) setError(e?.response?.data?.message || 'Failed to analyze batch automation eligibility.');
                })
                .finally(() => {
                    if (version.current === requestVersion) setLoading(false);
                });
        }
        return () => { version.current++; };
    }, [open, planId, scope, selectedCaseIds]);

    const handleConfirmRun = async () => {
        if (!preview?.canExecute || !preview.readyCases.length || submitting) return;
        const requestVersion = version.current;
        setSubmitting(true);
        setError('');

        try {
            const readyIds = preview.readyCases.map((c) => c.caseId);
            const { data } = await api.post<BatchDetailResponse>(`/automation/batches/plan/${planId}/create`, {
                scope,
                caseIds: readyIds,
                batchType: scope,
            });
            if (version.current === requestVersion) onConfirmed(data);
        } catch (e: any) {
            if (version.current === requestVersion) setError(e?.response?.data?.message || 'Failed to queue batch automation execution.');
        } finally {
            if (version.current === requestVersion) setSubmitting(false);
        }
    };

    const totalExcluded =
        (preview?.manualSkippedCases.length || 0) +
        (preview?.invalidConfigCases.length || 0) +
        (preview?.alreadyPendingCases.length || 0) +
        (preview?.unauthorizedCases.length || 0);

    return (
        <Dialog open={open} onClose={submitting ? undefined : onClose} fullWidth maxWidth="md">
            <DialogTitle>
                <Stack direction="row" justifyContent="space-between" alignItems="center">
                    <Box>
                        <Typography variant="h6" fontWeight={700}>
                            Batch Automation Run Preview
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                            Plan: {planName} · Scope: {scope === 'ALL_ELIGIBLE' ? 'All eligible automated cases' : `${selectedCaseIds?.length ?? 0} selected cases`}
                        </Typography>
                    </Box>
                    <IconButton aria-label="Close preview" onClick={onClose} disabled={submitting}>
                        <CloseIcon />
                    </IconButton>
                </Stack>
            </DialogTitle>
            <DialogContent dividers>
                {loading && (
                    <Box sx={{ p: 6, textAlign: 'center' }}>
                        <CircularProgress />
                        <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
                            Evaluating automation configurations, assignments, and queues...
                        </Typography>
                    </Box>
                )}

                {error && (
                    <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
                        {error}
                    </Alert>
                )}

                {!loading && preview && (
                    <>
                        {preview.planStatus !== 'IN_PROGRESS' && (
                            <Alert severity="warning" sx={{ mb: 2 }}>
                                This test plan is currently <strong>{preview.planStatus}</strong>. Automation batches can only be executed in active (IN_PROGRESS) plans.
                            </Alert>
                        )}

                        {preview.readyCount > (preview.maxBatchSize ?? 100) && <Alert severity="warning" sx={{ mb: 2 }}>
                            Select at most {preview.maxBatchSize ?? 100} ready cases for one batch.
                        </Alert>}
                        <Paper variant="outlined" sx={{ p: 2, mb: 3, backgroundColor: 'background.default' }}>
                            <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1.5 }}>
                                Eligibility Breakdown ({preview.totalInScope} cases evaluated)
                            </Typography>
                            <Stack direction="row" gap={1} flexWrap="wrap">
                                <Chip
                                    icon={<CheckCircleOutlineIcon />}
                                    color="success"
                                    label={`${preview.readyCount} Ready to Run`}
                                    variant={preview.readyCount > 0 ? 'filled' : 'outlined'}
                                />
                                {preview.manualSkippedCases.length > 0 && (
                                    <Chip
                                        icon={<BlockIcon />}
                                        color="info"
                                        label={`${preview.manualSkippedCases.length} Manual Skipped`}
                                        variant="outlined"
                                    />
                                )}
                                {preview.invalidConfigCases.length > 0 && (
                                    <Chip
                                        icon={<ErrorOutlineIcon />}
                                        color="error"
                                        label={`${preview.invalidConfigCases.length} Invalid Config`}
                                        variant="outlined"
                                    />
                                )}
                                {preview.alreadyPendingCases.length > 0 && (
                                    <Chip
                                        icon={<HourglassEmptyIcon />}
                                        color="warning"
                                        label={`${preview.alreadyPendingCases.length} Already Pending`}
                                        variant="outlined"
                                    />
                                )}
                                {preview.unauthorizedCases.length > 0 && (
                                    <Chip
                                        icon={<LockOutlinedIcon />}
                                        color="default"
                                        label={`${preview.unauthorizedCases.length} Assignment Restricted`}
                                        variant="outlined"
                                    />
                                )}
                            </Stack>
                        </Paper>

                        <Tabs
                            value={tabIndex}
                            onChange={(_, val) => setTabIndex(val)}
                            sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}
                        >
                            <Tab label={`Ready Cases (${preview.readyCount})`} id="batch-preview-ready-tab" />
                            <Tab label={`Excluded Cases (${totalExcluded})`} id="batch-preview-excluded-tab" />
                        </Tabs>

                        {tabIndex === 0 && (
                            <>
                                {preview.readyCases.length === 0 ? (
                                    <Typography color="text.secondary" sx={{ p: 3, textAlign: 'center' }}>
                                        No automated cases in this scope are ready to run. Check excluded cases for details.
                                    </Typography>
                                ) : (
                                    <TableContainer component={Paper} variant="outlined" sx={{ maxHeight: 320 }}>
                                        <Table size="small" stickyHeader>
                                            <TableHead>
                                                <TableRow>
                                                    <TableCell sx={{ width: 60 }}>#ID</TableCell>
                                                    <TableCell>Test Case</TableCell>
                                                    <TableCell sx={{ width: 160 }}>Type</TableCell>
                                                    <TableCell sx={{ width: 150 }}>Assignee</TableCell>
                                                    <TableCell align="right" sx={{ width: 80 }}>Steps</TableCell>
                                                </TableRow>
                                            </TableHead>
                                            <TableBody>
                                                {preview.readyCases.map((tc) => (
                                                    <TableRow key={tc.caseId} hover>
                                                        <TableCell>#{tc.caseId}</TableCell>
                                                        <TableCell>
                                                            <Typography fontWeight={600} variant="body2">
                                                                {tc.caseName}
                                                            </Typography>
                                                        </TableCell>
                                                        <TableCell>
                                                            <Chip size="small" label={tc.testType} variant="outlined" />
                                                        </TableCell>
                                                        <TableCell>
                                                            <Typography variant="body2">{tc.assignee}</Typography>
                                                        </TableCell>
                                                        <TableCell align="right">{tc.stepCount}</TableCell>
                                                    </TableRow>
                                                ))}
                                            </TableBody>
                                        </Table>
                                    </TableContainer>
                                )}
                            </>
                        )}

                        {tabIndex === 1 && (
                            <>
                                {totalExcluded === 0 ? (
                                    <Typography color="text.secondary" sx={{ p: 3, textAlign: 'center' }}>
                                        No cases in this scope were excluded. All cases are eligible!
                                    </Typography>
                                ) : (
                                    <TableContainer component={Paper} variant="outlined" sx={{ maxHeight: 320 }}>
                                        <Table size="small" stickyHeader>
                                            <TableHead>
                                                <TableRow>
                                                    <TableCell sx={{ width: 60 }}>#ID</TableCell>
                                                    <TableCell sx={{ width: 200 }}>Test Case</TableCell>
                                                    <TableCell sx={{ width: 140 }}>Exclusion Category</TableCell>
                                                    <TableCell>Reason for Exclusion</TableCell>
                                                </TableRow>
                                            </TableHead>
                                            <TableBody>
                                                {preview.invalidConfigCases.map((tc) => (
                                                    <TableRow key={`inv-${tc.caseId}`} hover>
                                                        <TableCell>#{tc.caseId}</TableCell>
                                                        <TableCell>
                                                            <Typography variant="body2" fontWeight={600}>
                                                                {tc.caseName}
                                                            </Typography>
                                                            <Typography variant="caption" color="text.secondary">
                                                                {tc.testType}
                                                            </Typography>
                                                        </TableCell>
                                                        <TableCell>
                                                            <Chip size="small" color="error" label="Invalid Config" />
                                                        </TableCell>
                                                        <TableCell>
                                                            <Typography variant="body2" color="error.main">
                                                                {tc.reason}
                                                            </Typography>
                                                        </TableCell>
                                                    </TableRow>
                                                ))}

                                                {preview.alreadyPendingCases.map((tc) => (
                                                    <TableRow key={`pen-${tc.caseId}`} hover>
                                                        <TableCell>#{tc.caseId}</TableCell>
                                                        <TableCell>
                                                            <Typography variant="body2" fontWeight={600}>
                                                                {tc.caseName}
                                                            </Typography>
                                                            <Typography variant="caption" color="text.secondary">
                                                                {tc.testType}
                                                            </Typography>
                                                        </TableCell>
                                                        <TableCell>
                                                            <Chip size="small" color="warning" label="Already Pending" />
                                                        </TableCell>
                                                        <TableCell>
                                                            <Typography variant="body2">{tc.reason}</Typography>
                                                        </TableCell>
                                                    </TableRow>
                                                ))}

                                                {preview.unauthorizedCases.map((tc) => (
                                                    <TableRow key={`unauth-${tc.caseId}`} hover>
                                                        <TableCell>#{tc.caseId}</TableCell>
                                                        <TableCell>
                                                            <Typography variant="body2" fontWeight={600}>
                                                                {tc.caseName}
                                                            </Typography>
                                                            <Typography variant="caption" color="text.secondary">
                                                                {tc.testType}
                                                            </Typography>
                                                        </TableCell>
                                                        <TableCell>
                                                            <Chip size="small" color="default" label="Assignment Restricted" />
                                                        </TableCell>
                                                        <TableCell>
                                                            <Typography variant="body2">{tc.reason}</Typography>
                                                        </TableCell>
                                                    </TableRow>
                                                ))}

                                                {preview.manualSkippedCases.map((tc) => (
                                                    <TableRow key={`man-${tc.caseId}`} hover>
                                                        <TableCell>#{tc.caseId}</TableCell>
                                                        <TableCell>
                                                            <Typography variant="body2" fontWeight={600}>
                                                                {tc.caseName}
                                                            </Typography>
                                                            <Typography variant="caption" color="text.secondary">
                                                                {tc.testType}
                                                            </Typography>
                                                        </TableCell>
                                                        <TableCell>
                                                            <Chip size="small" color="info" label="Manual Skipped" />
                                                        </TableCell>
                                                        <TableCell>
                                                            <Typography variant="body2" color="text.secondary">
                                                                {tc.reason}
                                                            </Typography>
                                                        </TableCell>
                                                    </TableRow>
                                                ))}
                                            </TableBody>
                                        </Table>
                                    </TableContainer>
                                )}
                            </>
                        )}

                        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 2 }}>
                            Note: Confirming will atomically queue only the {preview.readyCount} eligible automated cases. Excluded cases will not be modified or scheduled.
                        </Typography>
                    </>
                )}
            </DialogContent>
            <DialogActions sx={{ p: 2, gap: 1 }}>
                <Button onClick={onClose} disabled={submitting}>
                    Cancel
                </Button>
                <Button
                    variant="contained"
                    color="success"
                    startIcon={submitting ? <CircularProgress size={18} color="inherit" /> : <PlayArrowIcon />}
                    disabled={loading || submitting || !preview?.canExecute || !preview?.readyCount}
                    onClick={() => void handleConfirmRun()}
                >
                    {submitting ? 'Queueing Batch...' : `Confirm & Run ${preview?.readyCount || ''} Cases`}
                </Button>
            </DialogActions>
        </Dialog>
    );
}
