import React, { useEffect, useState } from 'react';
import {
    Alert,
    Autocomplete,
    Box,
    Button,
    Collapse,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Divider,
    IconButton,
    List,
    ListItem,
    ListItemButton,
    ListItemText,
    Paper,
    Snackbar,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableRow,
    TextField,
    Typography
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import { ResultStatus, TestCase, TestExecution, TestTypes } from '../../types/TestCase';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import { alpha } from '@mui/material/styles';
import SearchIcon from '@mui/icons-material/Search';
import AddIcon from '@mui/icons-material/Add';
import PlayCircleFilledWhiteIcon from '@mui/icons-material/PlayCircleFilledWhite';
import { InputAdornment } from '@mui/material';
import api from "../../services/api";

interface TestExecutionComponentProps {
    projId: number;
}

const API_URL_TEST_EXECUTION = '/testExecutions';
const API_URL_TESTCASE = '/testCase';

const TestExecutionComponent: React.FC<TestExecutionComponentProps> = ({ projId }) => {
    const [testExecutions, setTestExecutions] = useState<TestExecution[]>([]);
    const [selectedExecution, setSelectedExecution] = useState<TestExecution | null>(null);
    const [executionName, setExecutionName] = useState('');
    const [openDialog, setOpenDialog] = useState(false);
    const [testCases, setTestCases] = useState<TestCase[]>([]);
    const [searchQuery, setSearchQuery] = useState('');
    const [filteredTestCases, setFilteredTestCases] = useState<TestCase[]>([]);
    const [alert, setAlert] = useState({
        open: false,
        message: '',
        severity: 'success' as 'success' | 'error' | 'warning' | 'info'
    });
    const [isEdit, setIsEdit] = useState(false);
    const [openAddTestCaseDialog, setOpenAddTestCaseDialog] = useState(false);
    const [allTestCases, setAllTestCases] = useState<TestCase[]>([]);
    const [selectedTestCases, setSelectedTestCases] = useState<TestCase[]>([]);

    const [hoveredRow, setHoveredRow] = useState<number | null>(null);
    const [searchExecutionQuery, setSearchExecutionQuery] = useState('');

    const filteredExecutions = testExecutions.filter(ex =>
        ex.executionName.toLowerCase().includes(searchExecutionQuery.toLowerCase())
    );
    const [expandedRowId, setExpandedRowId] = useState<number | null>(null);
    const toggleExpand = (id: number) => {
        setExpandedRowId(prev => (prev === id ? null : id));
    };
    const keywordColors: Record<string, string> = {
        Given: "green",
        When: "blue",
        Then: "orange",
        And: "purple",
        But: "red"
    };

    const formatText = (text: string) => {
        return text.split('\n').map(line => {
            const match = line.match(/^(Given|When|Then|And|But)\b/);
            if (match) {
                const keyword = match[1] as keyof typeof keywordColors;
                const coloredKeyword = `<span style="color:${keywordColors[keyword]}; font-weight: bold;">${keyword}</span>`;
                return line.replace(keyword, coloredKeyword);
            }
            return line;
        }).join('<br/>');
    };


    const getRowColor = (status?: string | null) => {
        switch (status) {
            case 'PASS':
                return '#e8f5e9';
            case 'FAIL':
                return '#ffebee';
            case 'BLOCKED':
                return '#f5f5f5';
            case 'SKIPPED':
                return '#fff3e0';
            default:
                return '#ffffff';
        }
    };

    const fetchTestExecutions = async () => {
        try {
            const response = await api.get(`${API_URL_TEST_EXECUTION}/project/${projId}`);
            const executions = response.data.testExecutions || [];
            setTestExecutions(executions);
            if (executions.length) setSelectedExecution(executions[0]);
        } catch (error) {
            console.error('Error fetching test Executions:', error);
        }
    };

    const fetchAllExecutionTestCases = async (executionId: number) => {
        try {
            const response = await api.get(`${API_URL_TEST_EXECUTION}/id/${executionId}`);
            const sortedCases = (response.data.testCases || []).sort((a: TestCase, b: TestCase) => a.testName.localeCompare(b.testName));
            setTestCases(sortedCases);
            setFilteredTestCases(sortedCases);
        } catch (error) {
            console.error('Error fetching test cases:', error);
        }
    };

    const fetchAllUnassignedTestCases = async (executionId: number) => {
        try {
            const response = await api.get(`${API_URL_TESTCASE}/unassignedExecution?projectId=${projId}&&executionId=${executionId}`);
            setAllTestCases(response.data.testCase);
        } catch (err) {
            console.error('Failed to fetch test cases.');
        }
    };

    const handleAddSelectedTestCases = async () => {
        if (!selectedExecution || !selectedTestCases.length) return;
        try {
            await api.post(`${API_URL_TEST_EXECUTION}/${selectedExecution.id}/addCases`,
                { testCaseIds: selectedTestCases.map(tc => tc.id) });
            await Promise.all([
                fetchAllExecutionTestCases(selectedExecution.id),
                fetchAllUnassignedTestCases(selectedExecution.id)
            ]);
            setOpenAddTestCaseDialog(false);
            setSelectedTestCases([]);
            setAlert({ open: true, message: 'Test cases added to Execution.', severity: 'success' });
        } catch (error) {
            console.error('Error adding test cases:', error);
            setAlert({ open: true, message: 'Failed to add test cases.', severity: 'error' });
        }
    };

    const handleRemoveTestsFromList = async (testCaseId: number) => {
        if (!selectedExecution?.id) return;
        try {
            await api.post(`${API_URL_TEST_EXECUTION}/${selectedExecution.id}/removeCases`,
                { testCaseIds: [testCaseId] });
            setTestCases(prev => prev.filter(tc => tc.id !== testCaseId));
            await fetchAllUnassignedTestCases(selectedExecution.id);
            setAlert({ open: true, message: 'Test case deleted.', severity: 'success' });
        } catch (error) {
            console.error('Error deleting test case:', error);
            setAlert({ open: true, message: 'Failed to delete test case.', severity: 'error' });
        }
    };
    const handleSaveResult = async (
        testCaseId: number,
        resultStatus: ResultStatus | null | undefined,
        resultComment: string
    ) => {
        try {
            await api.post(
                `${API_URL_TEST_EXECUTION}/results`,
                {
                    executionId: selectedExecution?.id,
                    testCaseId,
                    resultStatus,
                    resultComment
                }
            );

            setTestCases(prev =>
                prev.map(tc =>
                    tc.id === testCaseId ? { ...tc, resultStatus, resultComment } : tc
                )
            );
            setAlert({ open: true, message: 'Result saved', severity: 'success' });
        } catch (error) {
            console.error('Failed to save result:', error);
            setAlert({ open: true, message: 'Failed to save result', severity: 'error' });
        }
    };

    useEffect(() => {
        if (projId) fetchTestExecutions().then();
    }, [projId]);

    useEffect(() => {
        if (selectedExecution) {
            fetchAllUnassignedTestCases(selectedExecution.id).then();
            fetchAllExecutionTestCases(selectedExecution.id).then();
        }
    }, [selectedExecution]);

    useEffect(() => {
        const lowerCaseQuery = searchQuery.toLowerCase();
        const filtered = testCases.filter(tc =>
            tc.testName.toLowerCase().includes(lowerCaseQuery)
        );
        setFilteredTestCases(filtered);
    }, [searchQuery, testCases]);

    const handleDialogClose = () => {
        setOpenDialog(false);
        setExecutionName('');
        setSelectedExecution(null);
    };
    const handleOpenAdd = () => {
        setIsEdit(false);
        setExecutionName('');
        setSelectedExecution(null);
        setOpenDialog(true);
    };

    const handleOpenEdit = (execution: TestExecution) => {
        setIsEdit(true);
        setExecutionName(execution.executionName);
        setSelectedExecution(execution);
        setOpenDialog(true);
    };
    const handleClose = () => {
        setOpenDialog(false);
        setExecutionName('');
        setSelectedExecution(null);
    };

    const handleSubmit = async () => {
        if (!executionName.trim()) {
            setAlert({ open: true, message: 'Execution name cannot be empty.', severity: 'error' });
            return;
        }
        try {
            if (isEdit && selectedExecution) {
                await api.put(`${API_URL_TEST_EXECUTION}/${selectedExecution.id}`,
                    { executionName, projectId: projId });
                setAlert({ open: true, message: 'Test Execution updated.', severity: 'success' });
            } else {
                await api.post(API_URL_TEST_EXECUTION,
                    { executionName, projectId: projId });
                setAlert({ open: true, message: 'Test Execution created.', severity: 'success' });
            }
            fetchTestExecutions().then();
            handleDialogClose();
        } catch (error) {
            console.error('Error saving Execution:', error);
            setAlert({ open: true, message: 'Error saving Execution.', severity: 'error' });
        }
    };

    const deleteTestExecution = async (executionId: number) => {
        try {
            await api.delete(`${API_URL_TEST_EXECUTION}/${executionId}`);
            fetchTestExecutions().then();
            if (selectedExecution?.id === executionId) {
                setSelectedExecution(null);
                setTestCases([]);
            }
            setAlert({ open: true, message: 'Test Execution deleted.', severity: 'success' });
        } catch (error) {
            console.error('Error deleting Execution:', error);
            setAlert({ open: true, message: 'Failed to delete Execution.', severity: 'error' });
        }
    };

    return (
        <Box display="flex">
            <Box sx={{ width: 240, p: 2 }}>
                <Button
                    variant="contained"
                    size="small"
                    onClick={handleOpenAdd}
                    sx={{ mt: 1, mb: 1, borderRadius: '10px', width: '100%' }}
                    startIcon={<PlayCircleFilledWhiteIcon />}
                >
                    Add Execution
                </Button>
                <TextField
                    placeholder="Search executions..."
                    size="small"
                    fullWidth
                    variant="outlined"
                    value={searchExecutionQuery}
                    onChange={(e) => setSearchExecutionQuery(e.target.value)}
                    sx={{ mb: 1 }}
                    InputProps={{
                        startAdornment: (
                            <InputAdornment position="start">
                                <SearchIcon fontSize="small" color="action" />
                            </InputAdornment>
                        ),
                    }}
                />
                <Divider />
                <List>
                    {filteredExecutions.map((execution) => (
                        <ListItem
                            key={execution.id}
                            disablePadding
                            sx={{
                                '&:hover .action-icons': { opacity: 1 },
                                position: 'relative',
                            }}
                        >
                            <ListItemButton
                                selected={selectedExecution?.id === execution.id}
                                onClick={() => setSelectedExecution(execution)}
                                sx={{ pr: 6, borderRadius: 1 }} // leave space for icons
                            >
                                <ListItemText
                                    primary={execution.executionName}
                                    primaryTypographyProps={{ fontSize: '0.9rem', fontWeight: 500 }}
                                />
                            </ListItemButton>

                            <Box
                                className="action-icons"
                                sx={{
                                    position: 'absolute',
                                    right: 8,
                                    top: '50%',
                                    transform: 'translateY(-50%)',
                                    display: 'flex',
                                    gap: 1,
                                    opacity: 0,
                                    transition: 'opacity 0.2s',
                                }}
                            >
                                <IconButton size="small" onClick={() => handleOpenEdit(execution)}>
                                    <EditIcon fontSize="small" color="primary" />
                                </IconButton>
                                <IconButton size="small" onClick={() => deleteTestExecution(execution.id)}>
                                    <DeleteIcon fontSize="small" color="error" />
                                </IconButton>
                            </Box>
                        </ListItem>
                    ))}
                </List>
            </Box>
            <Box sx={{ flexGrow: 1, p: 3 }}>
                <Typography variant="h6" fontWeight="bold" gutterBottom>
                    {selectedExecution ? `${selectedExecution.executionName}` : 'Select a Execution to view test cases'}
                </Typography>
                {selectedExecution && (
                    <>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                            <TextField
                                placeholder="Search test cases..."
                                variant="outlined"
                                size="small"
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                                sx={{ width: 300 }}
                                InputProps={{
                                    startAdornment: (
                                        <InputAdornment position="start">
                                            <SearchIcon color="action" />
                                        </InputAdornment>
                                    ),
                                }}
                            />
                            <Button
                                variant="contained"
                                color="primary"
                                onClick={() => setOpenAddTestCaseDialog(true)}
                                startIcon={<AddIcon />}
                                sx={{ borderRadius: '10px', px: 3 }}
                            >
                                Add Test Case
                            </Button>
                        </Box>
                        {/* Stats Section */}
                        <Box
                            display="flex"
                            gap={3}
                            flexWrap="wrap"
                            mb={3}
                            sx={{ backgroundColor: '#f5f5f5', p: 2, borderRadius: 1 }}
                        >
                            <Typography><strong>Total:</strong> {filteredTestCases.length}</Typography>
                            <Typography
                                color="success.main"><strong>Passed:</strong> {filteredTestCases.filter(tc => tc.resultStatus === 'PASS').length}
                            </Typography>
                            <Typography
                                color="error.main"><strong>Failed:</strong> {filteredTestCases.filter(tc => tc.resultStatus === 'FAIL').length}
                            </Typography>
                            <Typography
                                color="warning.main"><strong>Skipped:</strong> {filteredTestCases.filter(tc => tc.resultStatus === 'SKIPPED').length}
                            </Typography>
                            <Typography
                                color="text.secondary"><strong>Blocked:</strong> {filteredTestCases.filter(tc => tc.resultStatus === 'BLOCKED').length}
                            </Typography>
                            <Typography color="text.secondary"><strong>Not
                                Run:</strong> {filteredTestCases.filter(tc => tc.resultStatus === 'NOT_RUN' || !tc.resultStatus).length}
                            </Typography>
                        </Box>
                        <Paper>
                            <Table>
                                <TableBody>
                                    {filteredTestCases.map((tc) => (
                                        <React.Fragment key={tc.id}>
                                            <TableRow
                                                onMouseEnter={() => setHoveredRow(tc.id)}
                                                onMouseLeave={() => setHoveredRow(null)}
                                                sx={{
                                                    cursor: 'pointer',
                                                    backgroundColor: getRowColor(tc.resultStatus),
                                                    '&:hover': {
                                                        backgroundColor: alpha(getRowColor(tc.resultStatus), 0.8),
                                                    },
                                                }}
                                            >
                                                <TableCell onClick={() => toggleExpand(tc.id)}>
                                                    {tc.testName}
                                                </TableCell>
                                                <TableCell align="right">
                                                    <IconButton onClick={() => toggleExpand(tc.id)} size="small">
                                                        {expandedRowId === tc.id ? <ExpandLessIcon /> :
                                                            <ExpandMoreIcon />}
                                                    </IconButton>
                                                    <IconButton
                                                        size="small"
                                                        onClick={() => {
                                                            if (window.confirm('Are you sure you want to delete this test case?')) {
                                                                handleRemoveTestsFromList(tc.id).then();
                                                            }
                                                        }}
                                                        sx={{
                                                            opacity: hoveredRow === tc.id ? 1 : 0,
                                                            transition: 'opacity 0.2s ease-in-out',
                                                        }}
                                                    >
                                                        <DeleteIcon fontSize="small" color="error" />
                                                    </IconButton>
                                                </TableCell>
                                            </TableRow>
                                            <TableRow>
                                                <TableCell colSpan={2} sx={{ paddingBottom: 0, paddingTop: 0 }}>
                                                    <Collapse in={expandedRowId === tc.id} timeout="auto" unmountOnExit>
                                                        <Box margin={2}>
                                                            {/* Test Steps Table */}
                                                            {tc.testSteps?.length > 0 && (
                                                                <Paper variant="outlined" sx={{ mb: 2 }}>
                                                                    <Box p={2}>
                                                                        <Typography variant="subtitle1" gutterBottom>
                                                                            Test Steps
                                                                        </Typography>
                                                                        {(tc.testType === TestTypes.MANUAL || tc.testType === TestTypes.KEYWORD_DRIVEN) && (
                                                                            <Table size="small">
                                                                                <TableHead>
                                                                                    <TableRow>
                                                                                        <TableCell><strong>#</strong></TableCell>
                                                                                        <TableCell><strong>Description</strong></TableCell>
                                                                                        <TableCell><strong>Data</strong></TableCell>
                                                                                        <TableCell><strong>Expected
                                                                                            Output</strong></TableCell>
                                                                                    </TableRow>
                                                                                </TableHead>
                                                                                <TableBody>
                                                                                    {tc.testSteps
                                                                                        .sort((a, b) => a.stepOrder - b.stepOrder)
                                                                                        .map((step, idx) => (
                                                                                            <TableRow key={idx}>
                                                                                                <TableCell>{idx + 1}</TableCell>
                                                                                                <TableCell>{step.testStepDesc}</TableCell>
                                                                                                <TableCell>{step.testStepData}</TableCell>
                                                                                                <TableCell>{step.testExpectedOutput}</TableCell>
                                                                                            </TableRow>
                                                                                        ))}
                                                                                </TableBody>
                                                                            </Table>
                                                                        )}
                                                                        {(tc.testType === TestTypes.CUCUMBER_MANUAL || tc.testType === TestTypes.CUCUMBER_AUTOMATION) && (
                                                                            <>
                                                                                {tc.testSteps
                                                                                    .sort((a, b) => a.stepOrder - b.stepOrder)
                                                                                    .map((step) => (
                                                                                        <div
                                                                                            dangerouslySetInnerHTML={{ __html: formatText(step.testStepDesc) }}
                                                                                            style={{
                                                                                                padding: "8px",
                                                                                                minHeight: "150px",
                                                                                                whiteSpace: "pre-wrap",
                                                                                                borderRadius: "4px"
                                                                                            }}
                                                                                        />
                                                                                    ))}
                                                                            </>
                                                                        )}
                                                                    </Box>
                                                                </Paper>
                                                            )}

                                                            {/* Status Selector */}
                                                            <Autocomplete<ResultStatus, false, false, false>
                                                                value={tc.resultStatus ?? 'NOT_RUN'}
                                                                onChange={(_, newStatus: ResultStatus | null) => {
                                                                    const updatedStatus: ResultStatus = newStatus ?? 'NOT_RUN';
                                                                    handleSaveResult(tc.id, updatedStatus, tc.resultComment ?? '').then();
                                                                }}
                                                                options={['PASS', 'FAIL', 'SKIPPED', 'BLOCKED', 'NOT_RUN']}
                                                                renderInput={(params) => <TextField {...params}
                                                                    label="Status"
                                                                    fullWidth />}
                                                                sx={{ mb: 2, width: '300px' }}
                                                            />

                                                            {/* Comment Box */}
                                                            <TextField
                                                                label="Comment"
                                                                defaultValue={tc.resultComment ?? ''}
                                                                fullWidth
                                                                multiline
                                                                rows={3}
                                                                onBlur={(e) => {
                                                                    handleSaveResult(tc.id, tc.resultStatus ?? 'NOT_RUN', e.target.value);
                                                                }}
                                                            />
                                                        </Box>
                                                    </Collapse>
                                                </TableCell>
                                            </TableRow>
                                        </React.Fragment>
                                    ))}
                                </TableBody>
                            </Table>
                        </Paper>

                    </>
                )}
            </Box>

            {/* Add/Edit Execution Dialog */}
            <Dialog
                open={openDialog}
                onClose={handleClose}
                fullWidth
                maxWidth="xs"
                PaperProps={{ sx: { borderRadius: '20px', p: 1 } }}
            >
                <DialogTitle sx={{ fontWeight: 700, fontSize: '1.25rem', pb: 1 }}>
                    {isEdit ? 'Edit Execution' : 'New Execution'}
                </DialogTitle>
                <DialogContent sx={{ p: 3, pt: 1 }}>
                    <Box component="form" sx={{ mt: 1 }}>
                        <TextField
                            label="Execution Run Name"
                            value={executionName}
                            onChange={(e) => setExecutionName(e.target.value)}
                            fullWidth
                            variant="outlined"
                        />
                    </Box>
                </DialogContent>
                <DialogActions sx={{ p: 3, pt: 1 }}>
                    <Button onClick={handleClose} variant="text" color="inherit" sx={{ borderRadius: '10px' }}>
                        Cancel
                    </Button>
                    <Button onClick={handleSubmit} variant="contained" color="primary" sx={{ borderRadius: '10px', px: 3 }}>
                        {isEdit ? 'Update' : 'Create'}
                    </Button>
                </DialogActions>
            </Dialog>

            {/* Add Test Cases Dialog */}
            <Dialog
                open={openAddTestCaseDialog}
                onClose={() => setOpenAddTestCaseDialog(false)}
                fullWidth
                maxWidth="sm"
                PaperProps={{ sx: { borderRadius: '20px', p: 1 } }}
            >
                <DialogTitle sx={{ fontWeight: 700, fontSize: '1.25rem', pb: 1 }}>
                    Add Test Cases
                </DialogTitle>
                <DialogContent sx={{ p: 3, pt: 1 }}>
                    <Box sx={{ mt: 2 }}>
                        <Autocomplete
                            multiple
                            options={allTestCases}
                            getOptionLabel={(option) => option.testName}
                            value={selectedTestCases}
                            onChange={(_, newValue) => setSelectedTestCases(newValue)}
                            renderInput={(params) => (
                                <TextField
                                    {...params}
                                    variant="outlined"
                                    label="Select Test Cases"
                                    placeholder="Search test cases..."
                                />
                            )}
                        />
                        <Button
                            sx={{ mt: 2, borderRadius: 2 }}
                            onClick={() => setSelectedTestCases(allTestCases)}
                            variant="outlined"
                            fullWidth
                            color="inherit"
                        >
                            Select All Available
                        </Button>
                    </Box>
                </DialogContent>

                <DialogActions sx={{ p: 3, pt: 1 }}>
                    <Button onClick={() => setOpenAddTestCaseDialog(false)} variant="text" color="inherit" sx={{ borderRadius: '10px' }}>
                        Cancel
                    </Button>
                    <Button onClick={handleAddSelectedTestCases} variant="contained" color="primary" sx={{ borderRadius: '10px', px: 3 }}>
                        Add Selected
                    </Button>
                </DialogActions>
            </Dialog>


            <Snackbar
                open={alert.open}
                autoHideDuration={3000}
                onClose={() => setAlert({ ...alert, open: false })}
                anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
            >
                <Alert
                    onClose={() => setAlert({ ...alert, open: false })}
                    severity={alert.severity}
                    variant="filled"
                >
                    {alert.message}
                </Alert>
            </Snackbar>
        </Box>
    );
};

export default TestExecutionComponent;