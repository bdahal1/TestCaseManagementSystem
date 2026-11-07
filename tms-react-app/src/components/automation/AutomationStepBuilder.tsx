import { useEffect, useState } from 'react';
import {
    Autocomplete,
    Box,
    Button,
    Chip,
    IconButton,
    MenuItem,
    Paper,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    TextField,
    Tooltip,
    Typography,
} from '@mui/material';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import AddIcon from '@mui/icons-material/Add';
import SettingsSuggestIcon from '@mui/icons-material/SettingsSuggest';
import {
    type AutomationStepModel,
    type AutomationTemplate,
    type ElementState,
    type ProjectLocator,
    CUCUMBER_KEYWORDS,
    TEMPLATE_LABELS,
} from './types';
import ProjectLocatorsDialog from './ProjectLocatorsDialog';
import api from '../../services/api';

interface Props {
    mode: 'CUCUMBER_AUTOMATION' | 'KEYWORD_DRIVEN';
    steps: AutomationStepModel[];
    projectId: number;
    scenarioTitle: string;
    onChange: (steps: AutomationStepModel[]) => void;
    disabled?: boolean;
}

export default function AutomationStepBuilder({
    mode,
    steps,
    projectId,
    scenarioTitle,
    onChange,
    disabled = false,
}: Props) {
    const [projectLocators, setProjectLocators] = useState<ProjectLocator[]>([]);
    const [locatorsDialogOpen, setLocatorsDialogOpen] = useState(false);

    useEffect(() => {
        if (projectId) {
            api.get<ProjectLocator[]>(`/automation/locators/project/${projectId}`)
                .then((res) => setProjectLocators(res.data))
                .catch(() => {});
        }
    }, [projectId]);

    const handleStepChange = (index: number, patch: Partial<AutomationStepModel>) => {
        const next = [...steps];
        next[index] = { ...next[index], ...patch };
        onChange(next);
    };

    const handleAddStep = () => {
        const defaultKeyword = steps.length === 0 ? 'Given' : steps.length === 1 ? 'When' : 'Then';
        const newStep: AutomationStepModel = {
            order: steps.length + 1,
            keyword: defaultKeyword,
            template: 'CLICK_ELEMENT',
            locatorValue: '',
            textValue: '',
            stateValue: 'VISIBLE',
        };
        onChange([...steps, newStep]);
    };

    const handleRemoveStep = (index: number) => {
        const next = steps.filter((_, i) => i !== index).map((s, i) => ({ ...s, order: i + 1 }));
        onChange(next);
    };

    const handleMoveStep = (index: number, direction: 'UP' | 'DOWN') => {
        const targetIndex = direction === 'UP' ? index - 1 : index + 1;
        if (targetIndex < 0 || targetIndex >= steps.length) return;
        const next = [...steps];
        const temp = next[index];
        next[index] = next[targetIndex];
        next[targetIndex] = temp;
        onChange(next.map((s, i) => ({ ...s, order: i + 1 })));
    };

    // Helper to format Gherkin line
    const formatGherkinLine = (step: AutomationStepModel): string => {
        const kw = step.keyword || 'When';
        const loc = step.locatorValue || '[locator]';
        const val = step.textValue || '[value]';
        const state = (step.stateValue || 'VISIBLE').toLowerCase();

        switch (step.template) {
            case 'OPEN_PAGE':
                return `${kw} open page "${val}"`;
            case 'ENTER_TEXT':
                return `${kw} enter text "${val}" into "${loc}"`;
            case 'CLICK_ELEMENT':
                return `${kw} click element "${loc}"`;
            case 'VERIFY_TEXT_EQUALS':
                return `${kw} verify text of "${loc}" equals "${val}"`;
            case 'VERIFY_TEXT_CONTAINS':
                return `${kw} verify text of "${loc}" contains "${val}"`;
            case 'VERIFY_ELEMENT_STATE':
                return `${kw} verify element "${loc}" is ${state}`;
            default:
                return '';
        }
    };

    const locatorOptions = projectLocators.map((l) => l.locatorName);

    return (
        <Box sx={{ mt: 1 }}>
            <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
                <Typography variant="subtitle1" fontWeight={700}>
                    {mode === 'CUCUMBER_AUTOMATION' ? 'Cucumber Automation Steps' : 'Keyword Driven Step Table'}
                </Typography>
                <Button
                    size="small"
                    variant="outlined"
                    startIcon={<SettingsSuggestIcon />}
                    onClick={() => setLocatorsDialogOpen(true)}
                >
                    Manage Reusable Locators ({projectLocators.length})
                </Button>
            </Stack>

            {mode === 'CUCUMBER_AUTOMATION' ? (
                // Cucumber Automation Form View
                <Stack gap={2}>
                    {steps.map((step, index) => (
                        <Paper
                            key={index}
                            variant="outlined"
                            sx={{ p: 2, borderRadius: 2, backgroundColor: 'background.paper' }}
                        >
                            <Stack direction={{ xs: 'column', md: 'row' }} gap={1.5} alignItems={{ md: 'center' }}>
                                <Chip label={`#${index + 1}`} size="small" sx={{ width: 40 }} />

                                {/* Keyword selector */}
                                <TextField
                                    select
                                    size="small"
                                    label="Keyword"
                                    value={step.keyword || 'When'}
                                    disabled={disabled}
                                    onChange={(e) => handleStepChange(index, { keyword: e.target.value })}
                                    sx={{ minWidth: 100 }}
                                >
                                    {CUCUMBER_KEYWORDS.map((kw) => (
                                        <MenuItem key={kw} value={kw}>
                                            {kw}
                                        </MenuItem>
                                    ))}
                                </TextField>

                                {/* Template selector */}
                                <TextField
                                    select
                                    size="small"
                                    label="Template"
                                    value={step.template}
                                    disabled={disabled}
                                    onChange={(e) =>
                                        handleStepChange(index, {
                                            template: e.target.value as AutomationTemplate,
                                        })
                                    }
                                    sx={{ minWidth: 200 }}
                                >
                                    {(Object.keys(TEMPLATE_LABELS) as AutomationTemplate[]).map((tmpl) => (
                                        <MenuItem key={tmpl} value={tmpl}>
                                            {TEMPLATE_LABELS[tmpl]}
                                        </MenuItem>
                                    ))}
                                </TextField>

                                {/* Dynamic inputs based on template */}
                                {step.template === 'OPEN_PAGE' && (
                                    <TextField
                                        size="small"
                                        label="Page URL"
                                        value={step.textValue || ''}
                                        disabled={disabled}
                                        onChange={(e) => handleStepChange(index, { textValue: e.target.value })}
                                        placeholder="http://localhost:8080/demo/login or ${baseUrl}/login"
                                        sx={{ flex: 2 }}
                                    />
                                )}

                                {step.template === 'CLICK_ELEMENT' && (
                                    <Autocomplete
                                        freeSolo
                                        size="small"
                                        options={locatorOptions}
                                        value={step.locatorValue || ''}
                                        disabled={disabled}
                                        onInputChange={(_, val) => handleStepChange(index, { locatorValue: val })}
                                        renderInput={(params) => (
                                            <TextField
                                                {...params}
                                                label="Target Locator"
                                                placeholder="Login.Submit or #login-btn"
                                            />
                                        )}
                                        sx={{ flex: 2 }}
                                    />
                                )}

                                {step.template === 'ENTER_TEXT' && (
                                    <>
                                        <Autocomplete
                                            freeSolo
                                            size="small"
                                            options={locatorOptions}
                                            value={step.locatorValue || ''}
                                            disabled={disabled}
                                            onInputChange={(_, val) => handleStepChange(index, { locatorValue: val })}
                                            renderInput={(params) => (
                                                <TextField
                                                    {...params}
                                                    label="Target Locator"
                                                    placeholder="Login.Username or #user"
                                                />
                                            )}
                                            sx={{ flex: 1.5 }}
                                        />
                                        <TextField
                                            size="small"
                                            label="Text to Enter"
                                            value={step.textValue || ''}
                                            disabled={disabled}
                                            onChange={(e) => handleStepChange(index, { textValue: e.target.value })}
                                            placeholder="admin or ${password}"
                                            sx={{ flex: 1 }}
                                        />
                                    </>
                                )}

                                {(step.template === 'VERIFY_TEXT_EQUALS' || step.template === 'VERIFY_TEXT_CONTAINS') && (
                                    <>
                                        <Autocomplete
                                            freeSolo
                                            size="small"
                                            options={locatorOptions}
                                            value={step.locatorValue || ''}
                                            disabled={disabled}
                                            onInputChange={(_, val) => handleStepChange(index, { locatorValue: val })}
                                            renderInput={(params) => (
                                                <TextField
                                                    {...params}
                                                    label="Target Locator"
                                                    placeholder="Login.Heading or #welcome"
                                                />
                                            )}
                                            sx={{ flex: 1.5 }}
                                        />
                                        <TextField
                                            size="small"
                                            label="Expected Text"
                                            value={step.textValue || ''}
                                            disabled={disabled}
                                            onChange={(e) => handleStepChange(index, { textValue: e.target.value })}
                                            placeholder="Welcome, admin!"
                                            sx={{ flex: 1 }}
                                        />
                                    </>
                                )}

                                {step.template === 'VERIFY_ELEMENT_STATE' && (
                                    <>
                                        <Autocomplete
                                            freeSolo
                                            size="small"
                                            options={locatorOptions}
                                            value={step.locatorValue || ''}
                                            disabled={disabled}
                                            onInputChange={(_, val) => handleStepChange(index, { locatorValue: val })}
                                            renderInput={(params) => (
                                                <TextField
                                                    {...params}
                                                    label="Target Locator"
                                                    placeholder="#welcome or Login.Alert"
                                                />
                                            )}
                                            sx={{ flex: 1.5 }}
                                        />
                                        <TextField
                                            select
                                            size="small"
                                            label="Expected State"
                                            value={step.stateValue || 'VISIBLE'}
                                            disabled={disabled}
                                            onChange={(e) =>
                                                handleStepChange(index, {
                                                    stateValue: e.target.value as ElementState,
                                                })
                                            }
                                            sx={{ minWidth: 120 }}
                                        >
                                            <MenuItem value="VISIBLE">Visible</MenuItem>
                                            <MenuItem value="HIDDEN">Hidden</MenuItem>
                                        </TextField>
                                    </>
                                )}

                                {/* Action Buttons */}
                                <Stack direction="row" spacing={0.5} sx={{ ml: 'auto' }}>
                                    <Tooltip title="Move up">
                                        <span>
                                            <IconButton
                                                size="small"
                                                disabled={disabled || index === 0}
                                                onClick={() => handleMoveStep(index, 'UP')}
                                            >
                                                <ArrowUpwardIcon fontSize="small" />
                                            </IconButton>
                                        </span>
                                    </Tooltip>
                                    <Tooltip title="Move down">
                                        <span>
                                            <IconButton
                                                size="small"
                                                disabled={disabled || index === steps.length - 1}
                                                onClick={() => handleMoveStep(index, 'DOWN')}
                                            >
                                                <ArrowDownwardIcon fontSize="small" />
                                            </IconButton>
                                        </span>
                                    </Tooltip>
                                    <Tooltip title="Delete step">
                                        <span>
                                            <IconButton
                                                size="small"
                                                color="error"
                                                disabled={disabled}
                                                onClick={() => handleRemoveStep(index)}
                                            >
                                                <DeleteOutlineIcon fontSize="small" />
                                            </IconButton>
                                        </span>
                                    </Tooltip>
                                </Stack>
                            </Stack>
                        </Paper>
                    ))}
                </Stack>
            ) : (
                // Keyword Driven Table View
                <TableContainer component={Paper} variant="outlined">
                    <Table size="small">
                        <TableHead>
                            <TableRow>
                                <TableCell sx={{ width: 40 }}>#</TableCell>
                                <TableCell sx={{ width: 180 }}>Action / Template</TableCell>
                                <TableCell sx={{ minWidth: 200 }}>Target Locator</TableCell>
                                <TableCell sx={{ minWidth: 180 }}>Value / Parameter</TableCell>
                                <TableCell align="right" sx={{ width: 120 }}>Actions</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {steps.map((step, index) => (
                                <TableRow key={index}>
                                    <TableCell>{index + 1}</TableCell>
                                    <TableCell>
                                        <TextField
                                            select
                                            size="small"
                                            value={step.template}
                                            disabled={disabled}
                                            onChange={(e) =>
                                                handleStepChange(index, {
                                                    template: e.target.value as AutomationTemplate,
                                                })
                                            }
                                            fullWidth
                                        >
                                            {(Object.keys(TEMPLATE_LABELS) as AutomationTemplate[]).map((tmpl) => (
                                                <MenuItem key={tmpl} value={tmpl}>
                                                    {TEMPLATE_LABELS[tmpl]}
                                                </MenuItem>
                                            ))}
                                        </TextField>
                                    </TableCell>
                                    <TableCell>
                                        {step.template !== 'OPEN_PAGE' ? (
                                            <Autocomplete
                                                freeSolo
                                                size="small"
                                                options={locatorOptions}
                                                value={step.locatorValue || ''}
                                                disabled={disabled}
                                                onInputChange={(_, val) => handleStepChange(index, { locatorValue: val })}
                                                renderInput={(params) => (
                                                    <TextField
                                                        {...params}
                                                        placeholder="Login.Submit or #btn"
                                                    />
                                                )}
                                            />
                                        ) : (
                                            <Typography variant="body2" color="text.secondary">
                                                (Browser window)
                                            </Typography>
                                        )}
                                    </TableCell>
                                    <TableCell>
                                        {step.template === 'OPEN_PAGE' && (
                                            <TextField
                                                size="small"
                                                value={step.textValue || ''}
                                                disabled={disabled}
                                                onChange={(e) => handleStepChange(index, { textValue: e.target.value })}
                                                placeholder="http://localhost:8080/demo/login"
                                                fullWidth
                                            />
                                        )}
                                        {step.template === 'ENTER_TEXT' && (
                                            <TextField
                                                size="small"
                                                value={step.textValue || ''}
                                                disabled={disabled}
                                                onChange={(e) => handleStepChange(index, { textValue: e.target.value })}
                                                placeholder="Text value or ${var}"
                                                fullWidth
                                            />
                                        )}
                                        {step.template === 'CLICK_ELEMENT' && (
                                            <Typography variant="body2" color="text.secondary">—</Typography>
                                        )}
                                        {(step.template === 'VERIFY_TEXT_EQUALS' || step.template === 'VERIFY_TEXT_CONTAINS') && (
                                            <TextField
                                                size="small"
                                                value={step.textValue || ''}
                                                disabled={disabled}
                                                onChange={(e) => handleStepChange(index, { textValue: e.target.value })}
                                                placeholder="Expected text"
                                                fullWidth
                                            />
                                        )}
                                        {step.template === 'VERIFY_ELEMENT_STATE' && (
                                            <TextField
                                                select
                                                size="small"
                                                value={step.stateValue || 'VISIBLE'}
                                                disabled={disabled}
                                                onChange={(e) =>
                                                    handleStepChange(index, {
                                                        stateValue: e.target.value as ElementState,
                                                    })
                                                }
                                                fullWidth
                                            >
                                                <MenuItem value="VISIBLE">Visible</MenuItem>
                                                <MenuItem value="HIDDEN">Hidden</MenuItem>
                                            </TextField>
                                        )}
                                    </TableCell>
                                    <TableCell align="right">
                                        <IconButton
                                            size="small"
                                            disabled={disabled || index === 0}
                                            onClick={() => handleMoveStep(index, 'UP')}
                                        >
                                            <ArrowUpwardIcon fontSize="small" />
                                        </IconButton>
                                        <IconButton
                                            size="small"
                                            disabled={disabled || index === steps.length - 1}
                                            onClick={() => handleMoveStep(index, 'DOWN')}
                                        >
                                            <ArrowDownwardIcon fontSize="small" />
                                        </IconButton>
                                        <IconButton
                                            size="small"
                                            color="error"
                                            disabled={disabled}
                                            onClick={() => handleRemoveStep(index)}
                                        >
                                            <DeleteOutlineIcon fontSize="small" />
                                        </IconButton>
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </TableContainer>
            )}

            <Button
                variant="outlined"
                startIcon={<AddIcon />}
                onClick={handleAddStep}
                disabled={disabled}
                sx={{ mt: 2 }}
            >
                Add Step
            </Button>

            {/* Generated Gherkin Preview for Cucumber Automation */}
            {mode === 'CUCUMBER_AUTOMATION' && (
                <Paper variant="outlined" sx={{ mt: 3, p: 2, borderRadius: 2, backgroundColor: '#fcfcfc' }}>
                    <Typography variant="subtitle2" fontWeight={700} sx={{ mb: 1, color: 'text.secondary' }}>
                        Generated Gherkin Preview
                    </Typography>
                    <Box
                        component="pre"
                        sx={{
                            m: 0,
                            p: 2,
                            backgroundColor: '#282c34',
                            color: '#abb2bf',
                            borderRadius: 1.5,
                            fontFamily: 'monospace',
                            fontSize: '0.85rem',
                            overflowX: 'auto',
                            lineHeight: 1.5,
                        }}
                    >
                        <span style={{ color: '#e06c75', fontWeight: 'bold' }}>Feature:</span>{' '}
                        {scenarioTitle || 'Scenario Name'}
                        {'\n\n'}
                        {'  '}
                        <span style={{ color: '#61afef', fontWeight: 'bold' }}>Scenario:</span>{' '}
                        {scenarioTitle || 'Scenario Name'}
                        {'\n'}
                        {steps.map((s, idx) => (
                            <div key={idx}>
                                {'    '}
                                <span style={{ color: '#e5c07b', fontWeight: 'bold' }}>
                                    {s.keyword || 'When'}
                                </span>{' '}
                                <span style={{ color: '#98c379' }}>
                                    {formatGherkinLine(s).substring((s.keyword || 'When').length + 1)}
                                </span>
                            </div>
                        ))}
                        {steps.length === 0 && (
                            <span style={{ color: '#5c6370', fontStyle: 'italic' }}>
                                {'    '}# Add steps above to generate scenario
                            </span>
                        )}
                    </Box>
                </Paper>
            )}

            <ProjectLocatorsDialog
                open={locatorsDialogOpen}
                projectId={projectId}
                onClose={() => setLocatorsDialogOpen(false)}
                onLocatorsChanged={(updated) => setProjectLocators(updated)}
            />
        </Box>
    );
}
