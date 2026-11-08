import { Alert, Box, Chip, LinearProgress, Paper, Stack, Typography } from '@mui/material';
import { type AutomationLivePreview as Preview, type JobStatus } from './types';

export default function AutomationLivePreview({ preview, status, error }: {
    preview: Preview | null;
    status: JobStatus;
    error: boolean;
}) {
    const active = status === 'RUNNING' || status === 'QUEUED';
    const steps = preview?.progress.steps || [];
    const completed = steps.filter(step => step.status === 'PASSED' || step.status === 'FAILED').length;
    const current = preview && steps[preview.progress.currentStepIndex];
    return <Paper variant="outlined" sx={{ p: 2, mb: 2.5 }}>
        <Stack direction="row" justifyContent="space-between" gap={1} alignItems="center" sx={{ mb: 1 }}>
            <Typography variant="subtitle1" fontWeight={700}>Browser preview</Typography>
            <Chip size="small" label={active ? 'Step-by-step preview' : 'Last preview'} color={active ? 'primary' : 'default'} />
        </Stack>
        <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 1 }}>
            Images refresh after each step. Form fields are masked. Other application content may be visible.
        </Typography>
        {error && active && <Alert severity="warning" sx={{ mb: 1 }}>Preview temporarily unavailable. The test continues running.</Alert>}
        {preview?.screenshot ? <Box component="img" src={`data:image/jpeg;base64,${preview.screenshot}`}
            alt="Browser preview for this automation run"
            sx={{ display: 'block', width: '100%', maxHeight: 400, objectFit: 'contain', bgcolor: '#161b22', borderRadius: 1 }} /> :
            <Box sx={{ p: 3, bgcolor: 'action.hover', borderRadius: 1 }}>
                <Typography color="text.secondary">
                    {status === 'QUEUED' ? 'Waiting for a worker to start this test.' : active ?
                        'Waiting for the first browser image. Preview requires an updated worker.' : 'No browser preview was captured.'}
                </Typography>
            </Box>}
        {preview &&
            <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.5 }}>
                Last update {new Date(preview.updatedAt).toLocaleTimeString()}
            </Typography>}
        {preview && active && <>
            <Typography fontWeight={600} sx={{ mt: 1.5 }}>
                {active && current?.status === 'RUNNING' ? `Running step ${preview.progress.currentStepIndex + 1} of ${steps.length}` : `${completed} of ${steps.length} steps reported`}
            </Typography>
            {active && current?.status === 'RUNNING' && <Typography variant="body2" sx={{ overflowWrap: 'anywhere' }}>{current.description}</Typography>}
            <LinearProgress variant="determinate" value={steps.length ? completed / steps.length * 100 : 0} sx={{ my: 1 }} />
            <Stack gap={0.75} aria-label="Live step progress">
                {steps.map((step, index) => {
                    const state = step.status;
                    return <Stack key={index} direction="row" gap={1} alignItems="flex-start"
                        sx={{ p: 1, borderRadius: 1, bgcolor: state === 'RUNNING' ? 'action.selected' : 'transparent' }}>
                        <Typography variant="body2" color="text.secondary">{index + 1}.</Typography>
                        <Box sx={{ flex: 1, minWidth: 0 }}>
                            <Typography variant="body2" sx={{ overflowWrap: 'anywhere' }}>{step.description}</Typography>
                            {step.errorMessage && <Typography variant="caption" color="error" sx={{ whiteSpace: 'pre-wrap' }}>{step.errorMessage}</Typography>}
                        </Box>
                        <Chip size="small" label={state} color={state === 'PASSED' ? 'success' : state === 'FAILED' ? 'error' : state === 'RUNNING' ? 'primary' : 'default'} />
                    </Stack>;
                })}
            </Stack>
        </>}
    </Paper>;
}
