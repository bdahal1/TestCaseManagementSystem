import React, { useEffect, useState } from 'react';
import {
    Alert,
    Autocomplete,
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
    FormControlLabel,
    IconButton,
    MenuItem,
    Paper,
    Radio,
    RadioGroup,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    TextField,
    Typography,
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import SaveIcon from '@mui/icons-material/Save';
import api from '../../services/api';
import { inferImportLocatorType, validateImportPreview } from './importValidation';
import {
    type AutomationStepModel,
    type AutomationTemplate,
    type ElementState,
    type ImportAction,
    type ImportPreviewResponse,
    type ImportSaveRequest,
    type ImportSaveResponse,
    type ImportStepPreview,
    type OptionSelectBy,
    type ProjectLocator,
    type UrlComparison,
    CUCUMBER_KEYWORDS,
    SUPPORTED_KEY_OPTIONS,
    TEMPLATE_LABELS,
} from './types';

interface Props {
    open: boolean;
    projectId: number;
    onClose: () => void;
    onImportSuccess: (savedCases: ImportSaveResponse['savedCases']) => void;
}

export default function AutomationImportDialog({
    open,
    projectId,
    onClose,
    onImportSuccess,
}: Props) {
    const [step, setStep] = useState<'UPLOAD' | 'PREVIEW' | 'SUCCESS'>('UPLOAD');
    const [file, setFile] = useState<File | null>(null);
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [previewData, setPreviewData] = useState<ImportPreviewResponse | null>(null);
    const [projectLocators, setProjectLocators] = useState<ProjectLocator[]>([]);
    const [savedResult, setSavedResult] = useState<ImportSaveResponse | null>(null);

    // Fetch project locators for autocomplete
    useEffect(() => {
        if (open && projectId) {
            api.get<ProjectLocator[]>(`/automation/locators/project/${projectId}`)
                .then((res) => {
                    setProjectLocators(res.data);
                    setPreviewData((current) => current ? validateImportPreview(current, res.data) : current);
                })
                .catch(() => {});
        }
    }, [open, projectId]);

    const handleReset = () => {
        setStep('UPLOAD');
        setFile(null);
        setError(null);
        setPreviewData(null);
        setSavedResult(null);
    };

    const handleDialogClose = () => {
        handleReset();
        onClose();
    };

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setError(null);
        if (e.target.files && e.target.files.length > 0) {
            const selected = e.target.files[0];
            const lowerName = selected.name.toLowerCase();
            if (!lowerName.endsWith('.feature') && !lowerName.endsWith('.json')) {
                setError('Invalid file type. Please upload a Cucumber .feature file or a Keyword Driven .json file.');
                return;
            }
            if (selected.size > 2 * 1024 * 1024) {
                setError('File size exceeds the 2MB limit.');
                return;
            }
            setFile(selected);
        }
    };

    const handleUploadPreview = async () => {
        if (!file) {
            setError('Please choose a file to upload.');
            return;
        }

        setLoading(true);
        setError(null);

        try {
            const formData = new FormData();
            formData.append('file', file);

            const res = await api.post<ImportPreviewResponse>(
                `/automation/projects/${projectId}/import/preview`,
                formData,
                {
                    headers: { 'Content-Type': 'multipart/form-data' },
                }
            );

            setPreviewData(res.data);
            setStep('PREVIEW');
        } catch (err: any) {
            const msg =
                err.response?.data?.message ||
                err.response?.data?.description ||
                err.message ||
                'Failed to parse and preview import file.';
            setError(msg);
        } finally {
            setLoading(false);
        }
    };

    // Update case properties (name, conflict action)
    const handleCaseNameChange = (caseIndex: number, newName: string) => {
        if (!previewData) return;
        const updatedCases = [...previewData.cases];
        updatedCases[caseIndex] = { ...updatedCases[caseIndex], name: newName };
        setPreviewData(validateImportPreview({ ...previewData, cases: updatedCases }, projectLocators));
    };

    const handleCaseActionChange = (caseIndex: number, newAction: ImportAction) => {
        if (!previewData) return;
        const updatedCases = [...previewData.cases];
        updatedCases[caseIndex] = { ...updatedCases[caseIndex], action: newAction };
        setPreviewData(validateImportPreview({ ...previewData, cases: updatedCases }, projectLocators));
    };

    // Update step properties
    const handleStepPatch = (caseIndex: number, stepIndex: number, patch: Partial<ImportStepPreview>) => {
        if (!previewData) return;
        const updatedCases = [...previewData.cases];
        const updatedSteps = [...updatedCases[caseIndex].steps];
        const curStep = updatedSteps[stepIndex];
        const nextStep: ImportStepPreview = { ...curStep, ...patch };
        if ('locatorValue' in patch) nextStep.locatorType = inferImportLocatorType(patch.locatorValue, projectLocators);
        if (patch.template === 'VERIFY_ELEMENT_STATE' && !nextStep.stateValue) nextStep.stateValue = 'VISIBLE';
        if (patch.template === 'SELECT_OPTION' && !nextStep.selectBy) nextStep.selectBy = 'LABEL';
        if (patch.template === 'SET_CHECKBOX' && nextStep.checkedValue === undefined) nextStep.checkedValue = true;
        if (patch.template === 'PRESS_KEY' && !nextStep.keyValue) nextStep.keyValue = 'Enter';
        if (patch.template === 'VERIFY_URL' && !nextStep.urlComparison) nextStep.urlComparison = 'EQUALS';
        if (patch.template === 'VERIFY_ATTRIBUTE' && !nextStep.attributeName) nextStep.attributeName = 'id';
        updatedSteps[stepIndex] = nextStep;
        updatedCases[caseIndex] = { ...updatedCases[caseIndex], steps: updatedSteps };
        setPreviewData(validateImportPreview({ ...previewData, cases: updatedCases }, projectLocators));
    };

    const handleSaveImport = async () => {
        if (!previewData) return;

        const checked = validateImportPreview(previewData, projectLocators);
        setPreviewData(checked);
        if (!checked.canSave) {
            setError('Correct the highlighted cases and steps before saving.');
            return;
        }

        setSaving(true);
        setError(null);

        const request: ImportSaveRequest = {
            cases: checked.cases.map((c) => ({
                action: c.action,
                targetCaseId: c.action === 'REPLACE_EXISTING' ? c.existingCaseId : undefined,
                caseName: c.name,
                testType: c.testType as 'CUCUMBER_AUTOMATION' | 'KEYWORD_DRIVEN',
                steps: c.steps.map(
                    (s): AutomationStepModel => ({
                        order: s.order,
                        keyword: s.keyword,
                        template: s.template as AutomationTemplate,
                        locatorType: s.locatorType,
                        locatorValue: s.locatorValue,
                        textValue: s.textValue,
                        stateValue: s.stateValue,
                        selectBy: s.selectBy,
                        checkedValue: s.checkedValue,
                        keyValue: s.keyValue,
                        urlComparison: s.urlComparison,
                        attributeName: s.attributeName,
                    })
                ),
            })),
        };

        try {
            const res = await api.post<ImportSaveResponse>(
                `/automation/projects/${projectId}/import/save`,
                request
            );
            setSavedResult(res.data);
            setStep('SUCCESS');
            onImportSuccess(res.data.savedCases);
        } catch (err: any) {
            const msg =
                err.response?.data?.message ||
                err.response?.data?.description ||
                err.message ||
                'Failed to save test cases into library.';
            setError(msg);
        } finally {
            setSaving(false);
        }
    };

    return (
        <Dialog
            open={open}
            onClose={handleDialogClose}
            maxWidth="lg"
            fullWidth
            PaperProps={{
                id: 'automation-import-dialog',
                sx: { borderRadius: '14px', minHeight: '520px' },
            }}
        >
            <DialogTitle sx={{ m: 0, p: 2, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Stack direction="row" spacing={1} alignItems="center">
                    <UploadFileIcon color="primary" />
                    <Typography variant="h6" fontWeight="bold">
                        Import Automation Test Cases
                    </Typography>
                </Stack>
                <IconButton onClick={handleDialogClose} size="small" id="close-import-dialog-btn">
                    <CloseIcon />
                </IconButton>
            </DialogTitle>

            <Divider />

            <DialogContent sx={{ p: 3 }}>
                {error && (
                    <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
                        {error}
                    </Alert>
                )}

                {/* Step 1: Upload */}
                {step === 'UPLOAD' && (
                    <Box sx={{ py: 3, textAlign: 'center' }}>
                        <Paper
                            variant="outlined"
                            sx={{
                                p: 5,
                                borderStyle: 'dashed',
                                borderWidth: '2px',
                                borderColor: 'primary.light',
                                borderRadius: '12px',
                                bgcolor: '#fafafa',
                                maxWidth: '640px',
                                mx: 'auto',
                            }}
                        >
                            <UploadFileIcon sx={{ fontSize: 64, color: 'primary.main', mb: 2 }} />
                            <Typography variant="h6" fontWeight="bold" gutterBottom>
                                Select Automation Definition File
                            </Typography>
                            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                                Supports Cucumber <strong>.feature</strong> files and Keyword Driven <strong>.json</strong> export files (Max 2MB).
                                <br />
                                Multiple scenarios, Background steps, and variable references are supported.
                            </Typography>

                            <input
                                accept=".feature,.json"
                                style={{ display: 'none' }}
                                id="import-file-input"
                                type="file"
                                onChange={handleFileChange}
                            />
                            <label htmlFor="import-file-input">
                                <Button variant="outlined" component="span" startIcon={<UploadFileIcon />}>
                                    Browse File
                                </Button>
                            </label>

                            {file && (
                                <Box sx={{ mt: 2, p: 1, bgcolor: '#e8f0fe', borderRadius: '8px', display: 'inline-block' }}>
                                    <Typography variant="body2" fontWeight="medium" color="primary.dark">
                                        Selected: {file.name} ({(file.size / 1024).toFixed(1)} KB)
                                    </Typography>
                                </Box>
                            )}
                        </Paper>
                    </Box>
                )}

                {/* Step 2: Review Preview */}
                {step === 'PREVIEW' && previewData && (
                    <Box>
                        {/* Summary Bar */}
                        <Paper
                            elevation={0}
                            sx={{
                                p: 2,
                                mb: 3,
                                bgcolor: '#f4f6f8',
                                borderRadius: '10px',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'space-between',
                                flexWrap: 'wrap',
                                gap: 2,
                            }}
                        >
                            <Stack direction="row" spacing={2} alignItems="center">
                                <Typography variant="subtitle1" fontWeight="bold">
                                    File: {previewData.fileName}
                                </Typography>
                                <Chip
                                    label={previewData.fileType === 'FEATURE' ? 'Cucumber Feature' : 'Keyword JSON'}
                                    color="primary"
                                    size="small"
                                />
                                <Chip label={`${previewData.totalCases} Case(s)`} variant="outlined" size="small" />
                                <Chip label={`${previewData.totalSteps} Total Steps`} variant="outlined" size="small" />
                            </Stack>

                            {previewData.hasErrors ? (
                                <Chip
                                    icon={<WarningAmberIcon />}
                                    label="Mapping or syntax issues detected"
                                    color="warning"
                                    size="small"
                                />
                            ) : (
                                <Chip
                                    icon={<CheckCircleOutlineIcon />}
                                    label="Ready to save"
                                    color="success"
                                    size="small"
                                />
                            )}
                        </Paper>

                        {/* Global Errors */}
                        {previewData.globalErrors.length > 0 && (
                            <Alert severity="error" sx={{ mb: 3 }}>
                                <Typography variant="subtitle2" fontWeight="bold">
                                    File syntax errors:
                                </Typography>
                                <ul>
                                    {previewData.globalErrors.map((ge, idx) => (
                                        <li key={idx}>{ge}</li>
                                    ))}
                                </ul>
                            </Alert>
                        )}

                        {/* Cases List */}
                        <Stack spacing={3}>
                            {previewData.cases.map((c, caseIdx) => (
                                <Paper
                                    key={caseIdx}
                                    variant="outlined"
                                    sx={{ p: 2.5, borderRadius: '10px', borderColor: c.errorCount > 0 ? 'warning.main' : 'divider' }}
                                >
                                    {/* Case Header */}
                                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2, flexWrap: 'wrap', gap: 1 }}>
                                        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ flexGrow: 1, maxWidth: '600px' }}>
                                            <TextField
                                                size="small"
                                                label="Test Case Name"
                                                value={c.name}
                                                onChange={(e) => handleCaseNameChange(caseIdx, e.target.value)}
                                                fullWidth
                                                id={`case-name-input-${caseIdx}`}
                                            />
                                            <Chip
                                                label={c.testType === 'CUCUMBER_AUTOMATION' ? 'Cucumber Automation' : c.testType === 'KEYWORD_DRIVEN' ? 'Keyword Driven' : 'Unsupported type'}
                                                color="info"
                                                size="small"
                                            />
                                        </Stack>

                                        <Stack direction="row" spacing={1}>
                                            {c.unmappedCount > 0 && (
                                                <Chip
                                                    icon={<WarningAmberIcon />}
                                                    label={`${c.unmappedCount} Unmapped Step(s)`}
                                                    color="warning"
                                                    size="small"
                                                />
                                            )}
                                        </Stack>
                                    </Box>

                                    {c.validationError && <Alert severity="error" sx={{ mb: 2 }}>{c.validationError}</Alert>}

                                    {/* Unsupported Syntax Warning */}
                                    {c.unsupportedSyntax && (
                                        <Alert severity="error" sx={{ mb: 2 }}>
                                            {c.unsupportedSyntax}
                                        </Alert>
                                    )}

                                    {/* Conflict Resolution */}
                                    {c.conflict && (
                                        <Alert
                                            severity="warning"
                                            sx={{ mb: 2 }}
                                            icon={<ErrorOutlineIcon />}
                                        >
                                            <Typography variant="body2" fontWeight="bold" gutterBottom>
                                                Name Conflict: A test case named "{c.existingCaseName}" already exists in this project ({c.existingCaseProjectId}).
                                            </Typography>
                                            <FormControl component="fieldset" size="small">
                                                <RadioGroup
                                                    row
                                                    value={c.action}
                                                    onChange={(e) => handleCaseActionChange(caseIdx, e.target.value as ImportAction)}
                                                >
                                                    <FormControlLabel
                                                        value="CREATE_NEW"
                                                        control={<Radio size="small" id={`conflict-create-new-${caseIdx}`} />}
                                                        label="Create as new test case"
                                                    />
                                                    <FormControlLabel
                                                        value="REPLACE_EXISTING"
                                                        control={<Radio size="small" id={`conflict-replace-${caseIdx}`} />}
                                                        label={`Replace existing case (${c.existingCaseProjectId})`}
                                                    />
                                                </RadioGroup>
                                            </FormControl>
                                        </Alert>
                                    )}

                                    {/* Steps Table */}
                                    <TableContainer component={Box} sx={{ border: '1px solid #e0e0e0', borderRadius: '8px' }}>
                                        <Table size="small">
                                            <TableHead sx={{ bgcolor: '#fafafa' }}>
                                                <TableRow>
                                                    <TableCell sx={{ width: '80px', fontWeight: 'bold' }}># (Line)</TableCell>
                                                    {c.testType === 'CUCUMBER_AUTOMATION' && (
                                                        <TableCell sx={{ width: '100px', fontWeight: 'bold' }}>Keyword</TableCell>
                                                    )}
                                                    <TableCell sx={{ fontWeight: 'bold' }}>Step Text</TableCell>
                                                    <TableCell sx={{ width: '220px', fontWeight: 'bold' }}>Mapped Template</TableCell>
                                                    <TableCell sx={{ width: '200px', fontWeight: 'bold' }}>Locator</TableCell>
                                                    <TableCell sx={{ width: '200px', fontWeight: 'bold' }}>Value / State</TableCell>
                                                    <TableCell sx={{ width: '100px', fontWeight: 'bold' }}>Status</TableCell>
                                                </TableRow>
                                            </TableHead>
                                            <TableBody>
                                                {c.steps.map((st, stepIdx) => (
                                                    <TableRow
                                                        key={stepIdx}
                                                        hover
                                                        sx={{
                                                            bgcolor: !st.template || st.unsupportedSyntax ? '#fff8e1' : undefined,
                                                        }}
                                                    >
                                                        {/* Order & Line */}
                                                        <TableCell>
                                                            #{st.order} {st.sourceLine ? `(L${st.sourceLine})` : ''}
                                                        </TableCell>

                                                        {/* Keyword (Cucumber only) */}
                                                        {c.testType === 'CUCUMBER_AUTOMATION' && (
                                                            <TableCell>
                                                                <TextField
                                                                    select
                                                                    size="small"
                                                                    value={st.keyword || 'When'}
                                                                    onChange={(e) =>
                                                                        handleStepPatch(caseIdx, stepIdx, {
                                                                            keyword: e.target.value,
                                                                        })
                                                                    }
                                                                    sx={{ minWidth: 90 }}
                                                                >
                                                                    {CUCUMBER_KEYWORDS.map((kw) => (
                                                                        <MenuItem key={kw} value={kw}>
                                                                            {kw}
                                                                        </MenuItem>
                                                                    ))}
                                                                </TextField>
                                                            </TableCell>
                                                        )}

                                                        {/* Raw Step Text (plain text, plain monospace) */}
                                                        <TableCell>
                                                            <Typography
                                                                variant="body2"
                                                                sx={{
                                                                    fontFamily: 'monospace',
                                                                    fontSize: '0.85rem',
                                                                    whiteSpace: 'pre-wrap',
                                                                    wordBreak: 'break-word',
                                                                }}
                                                            >
                                                                {st.rawText}
                                                            </Typography>
                                                            {st.unsupportedSyntax && (
                                                                <Typography variant="caption" color="error.main" display="block">
                                                                    {st.unsupportedSyntax}
                                                                </Typography>
                                                            )}
                                                            {st.validationError && !st.unsupportedSyntax && (
                                                                <Typography variant="caption" color="warning.dark" display="block">
                                                                    {st.validationError}
                                                                </Typography>
                                                            )}
                                                        </TableCell>

                                                        {/* Template Selector */}
                                                        <TableCell>
                                                            <TextField
                                                                select
                                                                size="small"
                                                                fullWidth
                                                                value={st.template || ''}
                                                                onChange={(e) =>
                                                                    handleStepPatch(caseIdx, stepIdx, {
                                                                        template: (e.target.value as AutomationTemplate) || undefined,
                                                                    })
                                                                }
                                                                error={!st.template}
                                                                helperText={!st.template ? 'Map to template' : undefined}
                                                                id={`step-template-select-${caseIdx}-${stepIdx}`}
                                                            >
                                                                <MenuItem value="">
                                                                    <em>-- Select Template --</em>
                                                                </MenuItem>
                                                                {Object.entries(TEMPLATE_LABELS).map(([tmpl, label]) => (
                                                                    <MenuItem key={tmpl} value={tmpl}>
                                                                        {label}
                                                                    </MenuItem>
                                                                ))}
                                                            </TextField>
                                                        </TableCell>

                                                        {/* Locator */}
                                                        <TableCell>
                                                            {st.template && st.template !== 'OPEN_PAGE' && st.template !== 'VERIFY_URL' ? (
                                                                <Autocomplete
                                                                    freeSolo
                                                                    size="small"
                                                                    options={projectLocators.map((l) => l.locatorName)}
                                                                    value={st.locatorValue || ''}
                                                                    onInputChange={(_, val) =>
                                                                        handleStepPatch(caseIdx, stepIdx, {
                                                                            locatorValue: val,
                                                                        })
                                                                    }
                                                                    renderInput={(params) => (
                                                                        <TextField
                                                                            {...params}
                                                                            placeholder="Locator / Named"
                                                                            id={`step-locator-input-${caseIdx}-${stepIdx}`}
                                                                        />
                                                                    )}
                                                                />
                                                            ) : (
                                                                <Typography variant="caption" color="text.disabled">
                                                                    {st.template === 'VERIFY_URL' ? '(Browser URL)' : 'N/A'}
                                                                </Typography>
                                                            )}
                                                        </TableCell>

                                                        {/* Value or State */}
                                                        <TableCell>
                                                            {st.template === 'VERIFY_ELEMENT_STATE' ? (
                                                                <TextField
                                                                    select
                                                                    size="small"
                                                                    fullWidth
                                                                    value={st.stateValue || ''}
                                                                    onChange={(e) =>
                                                                        handleStepPatch(caseIdx, stepIdx, {
                                                                            stateValue: e.target.value as ElementState,
                                                                        })
                                                                    }
                                                                >
                                                                    <MenuItem value=""><em>Select state</em></MenuItem>
                                                                    <MenuItem value="VISIBLE">Visible</MenuItem>
                                                                    <MenuItem value="HIDDEN">Hidden</MenuItem>
                                                                </TextField>
                                                            ) : st.template === 'SELECT_OPTION' ? (
                                                                <Stack direction="row" spacing={0.5}>
                                                                    <TextField
                                                                        select
                                                                        size="small"
                                                                        value={st.selectBy || 'LABEL'}
                                                                        onChange={(e) =>
                                                                            handleStepPatch(caseIdx, stepIdx, {
                                                                                selectBy: e.target.value as OptionSelectBy,
                                                                            })
                                                                        }
                                                                        sx={{ minWidth: 80 }}
                                                                    >
                                                                        <MenuItem value="LABEL">Label</MenuItem>
                                                                        <MenuItem value="VALUE">Value</MenuItem>
                                                                    </TextField>
                                                                    <TextField
                                                                        size="small"
                                                                        fullWidth
                                                                        placeholder="Option"
                                                                        value={st.textValue || ''}
                                                                        onChange={(e) =>
                                                                            handleStepPatch(caseIdx, stepIdx, {
                                                                                textValue: e.target.value,
                                                                            })
                                                                        }
                                                                    />
                                                                </Stack>
                                                            ) : st.template === 'SET_CHECKBOX' ? (
                                                                <TextField
                                                                    select
                                                                    size="small"
                                                                    fullWidth
                                                                    label="Checkbox state"
                                                                    value={st.checkedValue == null ? '' : String(st.checkedValue)}
                                                                    onChange={(e) =>
                                                                        handleStepPatch(caseIdx, stepIdx, {
                                                                            checkedValue: e.target.value === 'true',
                                                                        })
                                                                    }
                                                                >
                                                                    <MenuItem value=""><em>Select checkbox state</em></MenuItem>
                                                                    <MenuItem value="true">Checked</MenuItem>
                                                                    <MenuItem value="false">Unchecked</MenuItem>
                                                                </TextField>
                                                            ) : st.template === 'PRESS_KEY' ? (
                                                                <Autocomplete
                                                                    freeSolo
                                                                    size="small"
                                                                    options={SUPPORTED_KEY_OPTIONS.map((k) => k)}
                                                                    value={st.keyValue ?? ''}
                                                                    onInputChange={(_, val) =>
                                                                        handleStepPatch(caseIdx, stepIdx, {
                                                                            keyValue: val,
                                                                        })
                                                                    }
                                                                    renderInput={(params) => (
                                                                        <TextField
                                                                            {...params}
                                                                            placeholder="Key"
                                                                        />
                                                                    )}
                                                                    fullWidth
                                                                />
                                                            ) : st.template === 'VERIFY_URL' ? (
                                                                <Stack direction="row" spacing={0.5}>
                                                                    <TextField
                                                                        select
                                                                        size="small"
                                                                        value={st.urlComparison || 'EQUALS'}
                                                                        onChange={(e) =>
                                                                            handleStepPatch(caseIdx, stepIdx, {
                                                                                urlComparison: e.target.value as UrlComparison,
                                                                            })
                                                                        }
                                                                        sx={{ minWidth: 90 }}
                                                                    >
                                                                        <MenuItem value="EQUALS">Equals</MenuItem>
                                                                        <MenuItem value="CONTAINS">Contains</MenuItem>
                                                                    </TextField>
                                                                    <TextField
                                                                        size="small"
                                                                        fullWidth
                                                                        placeholder="Expected URL"
                                                                        value={st.textValue || ''}
                                                                        onChange={(e) =>
                                                                            handleStepPatch(caseIdx, stepIdx, {
                                                                                textValue: e.target.value,
                                                                            })
                                                                        }
                                                                    />
                                                                </Stack>
                                                            ) : st.template === 'VERIFY_ATTRIBUTE' ? (
                                                                <Stack direction="row" spacing={0.5}>
                                                                    <TextField
                                                                        size="small"
                                                                        placeholder="Attribute"
                                                                        value={st.attributeName || ''}
                                                                        onChange={(e) =>
                                                                            handleStepPatch(caseIdx, stepIdx, {
                                                                                attributeName: e.target.value,
                                                                            })
                                                                        }
                                                                        sx={{ flex: 1 }}
                                                                    />
                                                                    <TextField
                                                                        size="small"
                                                                        placeholder="Expected"
                                                                        value={st.textValue ?? ''}
                                                                        onChange={(e) =>
                                                                            handleStepPatch(caseIdx, stepIdx, {
                                                                                textValue: e.target.value,
                                                                            })
                                                                        }
                                                                        sx={{ flex: 1 }}
                                                                    />
                                                                </Stack>
                                                            ) : st.template && st.template !== 'CLICK_ELEMENT' ? (
                                                                <TextField
                                                                    size="small"
                                                                    fullWidth
                                                                    placeholder={
                                                                        st.template === 'OPEN_PAGE'
                                                                            ? 'URL (e.g. https://...)'
                                                                            : 'Text value or ${var}'
                                                                    }
                                                                    value={st.textValue || ''}
                                                                    onChange={(e) =>
                                                                        handleStepPatch(caseIdx, stepIdx, {
                                                                            textValue: e.target.value,
                                                                        })
                                                                    }
                                                                    id={`step-value-input-${caseIdx}-${stepIdx}`}
                                                                />
                                                            ) : (
                                                                <Typography variant="caption" color="text.disabled">
                                                                    N/A
                                                                </Typography>
                                                            )}
                                                        </TableCell>

                                                        {/* Status */}
                                                        <TableCell>
                                                            {st.template && !st.unsupportedSyntax && !st.validationError ? (
                                                                <Chip label="Mapped" color="success" size="small" variant="outlined" />
                                                            ) : (
                                                                <Chip label="Attention" color="warning" size="small" />
                                                            )}
                                                        </TableCell>
                                                    </TableRow>
                                                ))}
                                            </TableBody>
                                        </Table>
                                    </TableContainer>
                                </Paper>
                            ))}
                        </Stack>
                    </Box>
                )}

                {/* Step 3: Success */}
                {step === 'SUCCESS' && savedResult && (
                    <Box sx={{ py: 4, textAlign: 'center' }}>
                        <CheckCircleOutlineIcon sx={{ fontSize: 72, color: 'success.main', mb: 2 }} />
                        <Typography variant="h5" fontWeight="bold" gutterBottom>
                            Import Completed Successfully!
                        </Typography>
                        <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
                            Saved <strong>{savedResult.createdCount + savedResult.replacedCount}</strong> test case(s) into the library
                            ({savedResult.createdCount} created, {savedResult.replacedCount} replaced).
                        </Typography>

                        <Paper variant="outlined" sx={{ maxWidth: '600px', mx: 'auto', p: 2, borderRadius: '10px', textAlign: 'left' }}>
                            <Typography variant="subtitle2" fontWeight="bold" gutterBottom>
                                Summary of Saved Cases:
                            </Typography>
                            <Stack spacing={1}>
                                {savedResult.savedCases.map((sc) => (
                                    <Box
                                        key={sc.id}
                                        sx={{
                                            p: 1.5,
                                            borderRadius: '6px',
                                            bgcolor: '#fafafa',
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'space-between',
                                        }}
                                    >
                                        <Typography variant="body2" fontWeight="medium">
                                            [{sc.testProjectId}] {sc.name}
                                        </Typography>
                                        <Chip
                                            label={sc.action}
                                            color={sc.action === 'REPLACED' ? 'secondary' : 'primary'}
                                            size="small"
                                        />
                                    </Box>
                                ))}
                            </Stack>
                        </Paper>
                    </Box>
                )}
            </DialogContent>

            <Divider />

            <DialogActions sx={{ p: 2, px: 3, justifyContent: 'space-between' }}>
                {step === 'UPLOAD' && (
                    <>
                        <Button onClick={handleDialogClose} color="inherit">
                            Cancel
                        </Button>
                        <Button
                            variant="contained"
                            onClick={handleUploadPreview}
                            disabled={!file || loading}
                            startIcon={loading ? <CircularProgress size={18} color="inherit" /> : <UploadFileIcon />}
                            id="preview-import-btn"
                        >
                            {loading ? 'Analyzing...' : 'Analyze & Preview'}
                        </Button>
                    </>
                )}

                {step === 'PREVIEW' && (
                    <>
                        <Button onClick={() => setStep('UPLOAD')} startIcon={<ArrowBackIcon />}>
                            Back to Upload
                        </Button>
                        <Button
                            variant="contained"
                            color="primary"
                            onClick={handleSaveImport}
                            disabled={saving || !previewData?.canSave}
                            startIcon={saving ? <CircularProgress size={18} color="inherit" /> : <SaveIcon />}
                            id="save-import-btn"
                        >
                            {saving ? 'Saving...' : 'Save to Test Case Library'}
                        </Button>
                    </>
                )}

                {step === 'SUCCESS' && (
                    <Box sx={{ width: '100%', display: 'flex', justifyContent: 'flex-end' }}>
                        <Button variant="contained" onClick={handleDialogClose} id="finish-import-btn">
                            Done
                        </Button>
                    </Box>
                )}
            </DialogActions>
        </Dialog>
    );
}
