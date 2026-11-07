import { useEffect, useState } from 'react';
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
    MenuItem,
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
    TextField,
    Typography,
} from '@mui/material';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import EditIcon from '@mui/icons-material/Edit';
import AddIcon from '@mui/icons-material/Add';
import api from '../../services/api';
import { type LocatorType, type ProjectLocator, type ProjectVariable } from './types';

interface Props {
    open: boolean;
    projectId: number;
    onClose: () => void;
    onLocatorsChanged?: (locators: ProjectLocator[]) => void;
}

export default function ProjectLocatorsDialog({ open, projectId, onClose, onLocatorsChanged }: Props) {
    const [tab, setTab] = useState<0 | 1>(0);
    const [locators, setLocators] = useState<ProjectLocator[]>([]);
    const [variables, setVariables] = useState<ProjectVariable[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [notice, setNotice] = useState('');

    // Locator form state
    const [editingLocId, setEditingLocId] = useState<number | null>(null);
    const [locName, setLocName] = useState('');
    const [locType, setLocType] = useState<LocatorType>('CSS');
    const [locValue, setLocValue] = useState('');
    const [locDesc, setLocDesc] = useState('');

    // Variable form state
    const [varKey, setVarKey] = useState('');
    const [varValue, setVarValue] = useState('');
    const [isSecret, setIsSecret] = useState(false);
    const [varDesc, setVarDesc] = useState('');

    const loadData = async () => {
        if (!projectId) return;
        setLoading(true);
        setError('');
        try {
            const [locRes, varRes] = await Promise.all([
                api.get<ProjectLocator[]>(`/automation/locators/project/${projectId}`),
                api.get<ProjectVariable[]>(`/automation/variables/project/${projectId}`),
            ]);
            setLocators(locRes.data);
            setVariables(varRes.data);
            if (onLocatorsChanged) onLocatorsChanged(locRes.data);
        } catch (e: any) {
            setError(e?.response?.data?.message || 'Failed to load project automation settings');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (open) {
            void loadData();
            resetLocForm();
            resetVarForm();
        }
    }, [open, projectId]);

    const resetLocForm = () => {
        setEditingLocId(null);
        setLocName('');
        setLocType('CSS');
        setLocValue('');
        setLocDesc('');
    };

    const resetVarForm = () => {
        setVarKey('');
        setVarValue('');
        setIsSecret(false);
        setVarDesc('');
    };

    const handleSaveLocator = async () => {
        if (!locName.trim() || !locValue.trim()) {
            setError('Locator name and value are required.');
            return;
        }
        setError('');
        setNotice('');
        try {
            if (editingLocId) {
                await api.put(`/automation/locators/${editingLocId}`, {
                    locatorName: locName.trim(),
                    locatorType: locType,
                    locatorValue: locValue.trim(),
                    description: locDesc.trim() || null,
                });
                setNotice('Locator updated.');
            } else {
                await api.post(`/automation/locators/project/${projectId}`, {
                    locatorName: locName.trim(),
                    locatorType: locType,
                    locatorValue: locValue.trim(),
                    description: locDesc.trim() || null,
                });
                setNotice('Locator created.');
            }
            resetLocForm();
            await loadData();
        } catch (e: any) {
            setError(e?.response?.data?.message || 'Could not save locator');
        }
    };

    const handleDeleteLocator = async (id: number) => {
        setError('');
        setNotice('');
        try {
            await api.delete(`/automation/locators/${id}`);
            setNotice('Locator deleted.');
            await loadData();
        } catch (e: any) {
            setError(e?.response?.data?.message || 'Could not delete locator');
        }
    };

    const handleEditLocator = (loc: ProjectLocator) => {
        setEditingLocId(loc.id || null);
        setLocName(loc.locatorName);
        setLocType(loc.locatorType);
        setLocValue(loc.locatorValue);
        setLocDesc(loc.description || '');
    };

    const handleSaveVariable = async () => {
        if (!varKey.trim()) {
            setError('Variable key is required.');
            return;
        }
        setError('');
        setNotice('');
        try {
            await api.post(`/automation/variables/project/${projectId}`, {
                variableKey: varKey.trim(),
                variableValue: varValue,
                isSecret,
                description: varDesc.trim() || null,
            });
            setNotice('Variable saved.');
            resetVarForm();
            await loadData();
        } catch (e: any) {
            setError(e?.response?.data?.message || 'Could not save variable');
        }
    };

    const handleDeleteVariable = async (id: number) => {
        setError('');
        setNotice('');
        try {
            await api.delete(`/automation/variables/${id}`);
            setNotice('Variable deleted.');
            await loadData();
        } catch (e: any) {
            setError(e?.response?.data?.message || 'Could not delete variable');
        }
    };

    return (
        <Dialog open={open} onClose={onClose} fullWidth maxWidth="md">
            <DialogTitle>Project Automation Locators & Variables</DialogTitle>
            <DialogContent dividers>
                {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}
                {notice && <Alert severity="success" sx={{ mb: 2 }} onClose={() => setNotice('')}>{notice}</Alert>}

                <Tabs value={tab} onChange={(_, val) => setTab(val)} sx={{ mb: 2 }}>
                    <Tab label={`Reusable Locators (${locators.length})`} />
                    <Tab label={`Variables & Secrets (${variables.length})`} />
                </Tabs>

                {loading ? (
                    <Box sx={{ p: 4, textAlign: 'center' }}><CircularProgress /></Box>
                ) : tab === 0 ? (
                    <Stack gap={2}>
                        <Paper variant="outlined" sx={{ p: 2 }}>
                            <Typography variant="subtitle2" fontWeight={600} sx={{ mb: 1.5 }}>
                                {editingLocId ? 'Edit Locator' : 'Add Reusable Named Element'}
                            </Typography>
                            <Stack direction={{ xs: 'column', sm: 'row' }} gap={2} sx={{ mb: 1.5 }}>
                                <TextField
                                    size="small"
                                    label="Name (e.g. Login.Username)"
                                    value={locName}
                                    onChange={(e) => setLocName(e.target.value)}
                                    sx={{ flex: 1 }}
                                />
                                <TextField
                                    select
                                    size="small"
                                    label="Type"
                                    value={locType}
                                    onChange={(e) => setLocType(e.target.value as LocatorType)}
                                    sx={{ minWidth: 130 }}
                                >
                                    <MenuItem value="CSS">CSS</MenuItem>
                                    <MenuItem value="XPATH">XPath</MenuItem>
                                    <MenuItem value="ID">ID</MenuItem>
                                    <MenuItem value="TEST_ID">Test ID</MenuItem>
                                    <MenuItem value="ROLE_NAME">Role & Name</MenuItem>
                                </TextField>
                                <TextField
                                    size="small"
                                    label="Selector / Value"
                                    value={locValue}
                                    onChange={(e) => setLocValue(e.target.value)}
                                    sx={{ flex: 1.5 }}
                                    placeholder={locType === 'ROLE_NAME' ? "button[name='Sign In']" : '#username'}
                                />
                            </Stack>
                            <TextField
                                size="small"
                                label="Description (optional)"
                                value={locDesc}
                                onChange={(e) => setLocDesc(e.target.value)}
                                fullWidth
                                sx={{ mb: 1.5 }}
                            />
                            <Stack direction="row" gap={1} justifyContent="flex-end">
                                {editingLocId && <Button onClick={resetLocForm}>Cancel</Button>}
                                <Button
                                    variant="contained"
                                    startIcon={editingLocId ? <EditIcon /> : <AddIcon />}
                                    onClick={() => void handleSaveLocator()}
                                >
                                    {editingLocId ? 'Update Locator' : 'Add Locator'}
                                </Button>
                            </Stack>
                        </Paper>

                        <TableContainer component={Paper} variant="outlined">
                            <Table size="small">
                                <TableHead>
                                    <TableRow>
                                        <TableCell>Named Element</TableCell>
                                        <TableCell>Type</TableCell>
                                        <TableCell>Selector / Value</TableCell>
                                        <TableCell>Description</TableCell>
                                        <TableCell align="right">Actions</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {locators.map((loc) => (
                                        <TableRow key={loc.id}>
                                            <TableCell sx={{ fontWeight: 600 }}>{loc.locatorName}</TableCell>
                                            <TableCell><Chip size="small" label={loc.locatorType} variant="outlined" /></TableCell>
                                            <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>{loc.locatorValue}</TableCell>
                                            <TableCell color="text.secondary">{loc.description || '—'}</TableCell>
                                            <TableCell align="right">
                                                <IconButton size="small" aria-label="Edit locator" onClick={() => handleEditLocator(loc)}>
                                                    <EditIcon fontSize="small" />
                                                </IconButton>
                                                <IconButton size="small" aria-label="Delete locator" onClick={() => loc.id && void handleDeleteLocator(loc.id)}>
                                                    <DeleteOutlineIcon fontSize="small" />
                                                </IconButton>
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                    {locators.length === 0 && (
                                        <TableRow>
                                            <TableCell colSpan={5} sx={{ textAlign: 'center', py: 3, color: 'text.secondary' }}>
                                                No named locators created yet. Add elements like Login.Username to reuse across test cases.
                                            </TableCell>
                                        </TableRow>
                                    )}
                                </TableBody>
                            </Table>
                        </TableContainer>
                    </Stack>
                ) : (
                    <Stack gap={2}>
                        <Paper variant="outlined" sx={{ p: 2 }}>
                            <Typography variant="subtitle2" fontWeight={600} sx={{ mb: 1.5 }}>
                                Add or Update Variable
                            </Typography>
                            <Stack direction={{ xs: 'column', sm: 'row' }} gap={2} sx={{ mb: 1.5 }}>
                                <TextField
                                    size="small"
                                    label="Key (e.g. baseUrl, username)"
                                    value={varKey}
                                    onChange={(e) => setVarKey(e.target.value)}
                                    sx={{ flex: 1 }}
                                />
                                <TextField
                                    size="small"
                                    type={isSecret ? "password" : "text"}
                                    label={isSecret ? "Secret Value" : "Value"}
                                    value={varValue}
                                    onChange={(e) => setVarValue(e.target.value)}
                                    sx={{ flex: 1.5 }}
                                    placeholder={isSecret ? "Enter secret token / password" : "http://localhost:8080"}
                                />
                                <TextField
                                    select
                                    size="small"
                                    label="Type"
                                    value={isSecret ? "SECRET" : "PLAIN"}
                                    onChange={(e) => setIsSecret(e.target.value === "SECRET")}
                                    sx={{ minWidth: 120 }}
                                >
                                    <MenuItem value="PLAIN">Plain Text</MenuItem>
                                    <MenuItem value="SECRET">Secret</MenuItem>
                                </TextField>
                            </Stack>
                            <TextField
                                size="small"
                                label="Description (optional)"
                                value={varDesc}
                                onChange={(e) => setVarDesc(e.target.value)}
                                fullWidth
                                sx={{ mb: 1.5 }}
                            />
                            <Stack direction="row" gap={1} justifyContent="flex-end">
                                <Button variant="contained" startIcon={<AddIcon />} onClick={() => void handleSaveVariable()}>
                                    Save Variable
                                </Button>
                            </Stack>
                        </Paper>

                        <TableContainer component={Paper} variant="outlined">
                            <Table size="small">
                                <TableHead>
                                    <TableRow>
                                        <TableCell>Variable Key</TableCell>
                                        <TableCell>Type</TableCell>
                                        <TableCell>Value</TableCell>
                                        <TableCell>Description</TableCell>
                                        <TableCell align="right">Actions</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {variables.map((v) => (
                                        <TableRow key={v.id}>
                                            <TableCell sx={{ fontWeight: 600, fontFamily: 'monospace' }}>${`{${v.variableKey}}`}</TableCell>
                                            <TableCell>
                                                <Chip size="small" label={v.isSecret ? "Secret" : "Plain"} color={v.isSecret ? "warning" : "default"} variant="outlined" />
                                            </TableCell>
                                            <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>{v.variableValue}</TableCell>
                                            <TableCell color="text.secondary">{v.description || '—'}</TableCell>
                                            <TableCell align="right">
                                                <IconButton size="small" aria-label="Delete variable" onClick={() => v.id && void handleDeleteVariable(v.id)}>
                                                    <DeleteOutlineIcon fontSize="small" />
                                                </IconButton>
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                    {variables.length === 0 && (
                                        <TableRow>
                                            <TableCell colSpan={5} sx={{ textAlign: 'center', py: 3, color: 'text.secondary' }}>
                                                No variables configured. You can define variables like baseUrl or credentials to reference with ${`{variableName}`}.
                                            </TableCell>
                                        </TableRow>
                                    )}
                                </TableBody>
                            </Table>
                        </TableContainer>
                    </Stack>
                )}
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>Done</Button>
            </DialogActions>
        </Dialog>
    );
}
