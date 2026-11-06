import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Alert,
    Box,
    Button,
    Chip,
    IconButton,
    MenuItem,
    Stack,
    TextField,
    Typography,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import { automationStates, detailLabel, priorities, priorityColor, type DetailsDraft } from './caseDetails';
import type { Person } from '../testPlans/types';

interface Props {
    value: DetailsDraft;
    onChange: (next: DetailsDraft) => void;
    owners: Person[];
    ownersError?: string;
    disabled?: boolean;
}
export default function CaseDetailsFields({ value, onChange, owners, ownersError, disabled }: Props) {
    const update = <K extends keyof DetailsDraft>(key: K, next: DetailsDraft[K]) => onChange({ ...value, [key]: next });
    return (
        <Accordion
            disableGutters
            elevation={0}
            sx={{
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: '12px !important',
                '&:before': { display: 'none' },
            }}
        >
            <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                <Stack direction="row" gap={1} alignItems="center" flexWrap="wrap">
                    <Typography fontWeight={700}>Case details</Typography>
                    <Chip
                        size="small"
                        label={detailLabel(value.priority)}
                        color={priorityColor(value.priority)}
                        variant="outlined"
                    />
                    <Typography variant="caption" color="text.secondary">
                        Ownership, preconditions, effort, and custom fields
                    </Typography>
                </Stack>
            </AccordionSummary>
            <AccordionDetails>
                <Stack gap={2}>
                    {ownersError && <Alert severity="warning">{ownersError}</Alert>}
                    <Box
                        sx={{
                            display: 'grid',
                            gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', lg: 'repeat(4, 1fr)' },
                            gap: 2,
                        }}
                    >
                        <TextField
                            select
                            label="Priority"
                            value={value.priority}
                            onChange={(e) => update('priority', e.target.value as DetailsDraft['priority'])}
                            disabled={disabled}
                        >
                            {priorities.map((p) => (
                                <MenuItem key={p} value={p}>
                                    {detailLabel(p)}
                                </MenuItem>
                            ))}
                        </TextField>
                        <TextField
                            select
                            label="Automation status"
                            value={value.automationStatus}
                            onChange={(e) =>
                                update('automationStatus', e.target.value as DetailsDraft['automationStatus'])
                            }
                            disabled={disabled}
                        >
                            {automationStates.map((s) => (
                                <MenuItem key={s} value={s}>
                                    {detailLabel(s)}
                                </MenuItem>
                            ))}
                        </TextField>
                        <TextField
                            select
                            label="Case owner"
                            value={value.ownerId}
                            onChange={(e) => update('ownerId', Number(e.target.value))}
                            disabled={disabled}
                            helperText="Separate from the tester assigned to a run."
                        >
                            <MenuItem value={0}>Unassigned</MenuItem>
                            {value.ownerId > 0 && !owners.some((o) => o.id === value.ownerId) && (
                                <MenuItem value={value.ownerId}>Current owner #{value.ownerId}</MenuItem>
                            )}
                            {owners.map((owner) => (
                                <MenuItem key={owner.id} value={owner.id}>
                                    {owner.name}
                                </MenuItem>
                            ))}
                        </TextField>
                        <TextField
                            label="Estimated duration (minutes)"
                            type="number"
                            value={value.estimatedMinutes}
                            onChange={(e) => update('estimatedMinutes', e.target.value)}
                            disabled={disabled}
                            inputProps={{ min: 1, max: 10080 }}
                            helperText="Per execution. Leave blank if unknown."
                        />
                    </Box>
                    <TextField
                        label="Component"
                        value={value.component}
                        onChange={(e) => update('component', e.target.value)}
                        disabled={disabled}
                        inputProps={{ maxLength: 100 }}
                        placeholder="For example, Authentication or Checkout"
                    />
                    <TextField
                        label="Description"
                        value={value.description}
                        onChange={(e) => update('description', e.target.value)}
                        disabled={disabled}
                        multiline
                        minRows={2}
                        inputProps={{ maxLength: 10000 }}
                        placeholder="What does this case verify, and why?"
                    />
                    <TextField
                        label="Preconditions"
                        value={value.preconditions}
                        onChange={(e) => update('preconditions', e.target.value)}
                        disabled={disabled}
                        multiline
                        minRows={2}
                        inputProps={{ maxLength: 10000 }}
                        placeholder="Accounts, permissions, application state, or setup required before testing"
                    />
                    <Stack direction="row" justifyContent="space-between" alignItems="center">
                        <Typography fontWeight={600}>Custom fields</Typography>
                        <Button
                            size="small"
                            disabled={disabled || value.customFields.length >= 20}
                            onClick={() => update('customFields', [...value.customFields, { name: '', value: '' }])}
                        >
                            Add custom field
                        </Button>
                    </Stack>
                    {value.customFields.map((field, index) => (
                        <Stack key={index} direction={{ xs: 'column', sm: 'row' }} gap={1}>
                            <TextField
                                size="small"
                                label={`Field ${index + 1} name`}
                                value={field.name}
                                disabled={disabled}
                                inputProps={{ maxLength: 80 }}
                                onChange={(e) =>
                                    update(
                                        'customFields',
                                        value.customFields.map((f, n) =>
                                            n === index ? { ...f, name: e.target.value } : f,
                                        ),
                                    )
                                }
                            />
                            <TextField
                                size="small"
                                fullWidth
                                label={`Field ${index + 1} value`}
                                value={field.value}
                                disabled={disabled}
                                inputProps={{ maxLength: 1000 }}
                                onChange={(e) =>
                                    update(
                                        'customFields',
                                        value.customFields.map((f, n) =>
                                            n === index ? { ...f, value: e.target.value } : f,
                                        ),
                                    )
                                }
                            />
                            <IconButton
                                aria-label={`Remove custom field ${index + 1}`}
                                disabled={disabled}
                                onClick={() =>
                                    update(
                                        'customFields',
                                        value.customFields.filter((_, n) => n !== index),
                                    )
                                }
                            >
                                <DeleteOutlineIcon />
                            </IconButton>
                        </Stack>
                    ))}
                </Stack>
            </AccordionDetails>
        </Accordion>
    );
}
