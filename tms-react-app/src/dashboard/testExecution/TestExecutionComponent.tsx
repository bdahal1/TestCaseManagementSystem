import React, {useEffect, useState} from 'react';
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
import axios from 'axios';
import {ResultStatus, TestCase, TestExecution, TestTypes} from '../../types/TestCase';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import {alpha} from '@mui/material/styles';

interface TestExecutionComponentProps {
    projId: number;
}

const API_URL_TEST_EXECUTION = '/dhtcms/api/v1/testExecutions';
const API_URL_TESTCASE = '/dhtcms/api/v1/testCase';

const TestExecutionComponent: React.FC<TestExecutionComponentProps> = ({projId}) => {
    const [testExecutions, setTestExecutions] = useState<TestExecution[]>([]);
    const [selectedExecution, setSelectedExecution] = useState<TestExecution | null>(null);
    const [executionName, setExecutionName] = useState('');
    const [openDialog, setOpenDialog] = useState(false);
    const [testCases, setTestCases] = useState<TestCase[]>([]);
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
            const response = await axios.get(`${API_URL_TEST_EXECUTION}/project/${projId}`, {
                headers: {Authorization: `Bearer ${localStorage.getItem('authToken')}`}
            });
            const executions = response.data.testExecutions || [];
            setTestExecutions(executions);
            if (executions.length) setSelectedExecution(executions[0]);
        } catch (error) {
            console.error('Error fetching test Executions:', error);
        }
    };

    const fetchAllExecutionTestCases = async (executionId: number) => {
        try {
            const response = await axios.get(`${API_URL_TEST_EXECUTION}/id/${executionId}`, {
                headers: {Authorization: `Bearer ${localStorage.getItem('authToken')}`}
            });
            const sortedCases = (response.data.testCases || []).sort((a: TestCase, b: TestCase) => a.testName.localeCompare(b.testName));
            setTestCases(sortedCases);
        } catch (error) {
            console.error('Error fetching test cases:', error);
        }
    };

    const fetchAllUnassignedTestCases = async (executionId: number) => {
        try {
            const response = await axios.get(`${API_URL_TESTCASE}/unassignedExecution?projectId=${projId}&&executionId=${executionId}`, {
                headers: {Authorization: `Bearer ${localStorage.getItem('authToken')}`}
            });
            setAllTestCases(response.data.testCase);
        } catch (err) {
            console.error('Failed to fetch test cases.');
        }
    };

    const handleAddSelectedTestCases = async () => {
        if (!selectedExecution || !selectedTestCases.length) return;
        try {
            await axios.post(`${API_URL_TEST_EXECUTION}/${selectedExecution.id}/addCases`,
                {testCaseIds: selectedTestCases.map(tc => tc.id)},
                {headers: {Authorization: `Bearer ${localStorage.getItem('authToken')}`}});
            await Promise.all([
                fetchAllExecutionTestCases(selectedExecution.id),
                fetchAllUnassignedTestCases(selectedExecution.id)
            ]);
            setOpenAddTestCaseDialog(false);
            setSelectedTestCases([]);
            setAlert({open: true, message: 'Test cases added to Execution.', severity: 'success'});
        } catch (error) {
            console.error('Error adding test cases:', error);
            setAlert({open: true, message: 'Failed to add test cases.', severity: 'error'});
        }
    };

    const handleRemoveTestsFromList = async (testCaseId: number) => {
        if (!selectedExecution?.id) return;
        try {
            await axios.post(`${API_URL_TEST_EXECUTION}/${selectedExecution.id}/removeCases`,
                {testCaseIds: [testCaseId]},
                {headers: {Authorization: `Bearer ${localStorage.getItem('authToken')}`}});
            setTestCases(prev => prev.filter(tc => tc.id !== testCaseId));
            await fetchAllUnassignedTestCases(selectedExecution.id);
            setAlert({open: true, message: 'Test case deleted.', severity: 'success'});
        } catch (error) {
            console.error('Error deleting test case:', error);
            setAlert({open: true, message: 'Failed to delete test case.', severity: 'error'});
        }
    };
    const handleSaveResult = async (
        testCaseId: number,
        resultStatus: ResultStatus | null | undefined,
        resultComment: string
    ) => {
        try {
            await axios.post(
                `${API_URL_TEST_EXECUTION}/results`,
                {
                    executionId: selectedExecution?.id,
                    testCaseId,
                    resultStatus,
                    resultComment
                },
                {
                    headers: {
                        Authorization: `Bearer ${localStorage.getItem('authToken')}`
                    }
                }
            );

            setTestCases(prev =>
                prev.map(tc =>
                    tc.id === testCaseId ? {...tc, resultStatus, resultComment} : tc
                )
            );
            setAlert({open: true, message: 'Result saved', severity: 'success'});
        } catch (error) {
            console.error('Failed to save result:', error);
            setAlert({open: true, message: 'Failed to save result', severity: 'error'});
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
            setAlert({open: true, message: 'Execution name cannot be empty.', severity: 'error'});
            return;
        }
        try {
            if (isEdit && selectedExecution) {
                const response = await axios.put(`${API_URL_TEST_EXECUTION}/${selectedExecution.id}`,
                    {executionName, projectId: projId},
                    {headers: {Authorization: `Bearer ${localStorage.getItem('authToken')}`}});
                setTestExecutions(prev => prev.map(f => f.id === selectedExecution.id ? response.data : f));
                setAlert({open: true, message: 'Test Execution updated.', severity: 'success'});
            } else {
                const response = await axios.post(API_URL_TEST_EXECUTION,
                    {executionName, projectId: projId},
                    {headers: {Authorization: `Bearer ${localStorage.getItem('authToken')}`}});
                setTestExecutions(prev => [...prev, response.data]);
                setAlert({open: true, message: 'Test Execution created.', severity: 'success'});
            }
            handleDialogClose();
        } catch (error) {
            console.error('Error saving Execution:', error);
            setAlert({open: true, message: 'Error saving Execution.', severity: 'error'});
        }
    };

    const deleteTestExecution = async (executionId: number) => {
        try {
            await axios.delete(`${API_URL_TEST_EXECUTION}/${executionId}`, {
                headers: {Authorization: `Bearer ${localStorage.getItem('authToken')}`}
            });
            const updatedExecutions = testExecutions.filter(f => f.id !== executionId);
            setTestExecutions(updatedExecutions);
            if (selectedExecution?.id === executionId && updatedExecutions.length) {
                setSelectedExecution(updatedExecutions[0]);
            } else if (!updatedExecutions.length) {
                setSelectedExecution(null);
                setTestCases([]);
            }
            setAlert({open: true, message: 'Test Execution deleted.', severity: 'success'});
        } catch (error) {
            console.error('Error deleting Execution:', error);
            setAlert({open: true, message: 'Failed to delete Execution.', severity: 'error'});
        }
    };

    return (
        <Box display="flex">
            <Box sx={{width: 240, p: 2}}>
                <Button variant="outlined" size="small" onClick={handleOpenAdd} sx={{mt: 1, mb: 2}}>
                    + Add Execution
                </Button>
                <Divider/>
                <List>
                    {testExecutions.map((execution) => (
                        <ListItem
                            key={execution.id}
                            disablePadding
                            sx={{
                                '&:hover .action-icons': {opacity: 1},
                                position: 'relative',
                            }}
                        >
                            <ListItemButton
                                selected={selectedExecution?.id === execution.id}
                                onClick={() => setSelectedExecution(execution)}
                                sx={{pr: 6}} // leave space for icons
                            >
                                <ListItemText primary={execution.executionName}/>
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
                                    <EditIcon fontSize="small"/>
                                </IconButton>
                                <IconButton size="small" onClick={() => deleteTestExecution(execution.id)}>
                                    <DeleteIcon fontSize="small"/>
                                </IconButton>
                            </Box>
                        </ListItem>
                    ))}
                </List>
            </Box>
            <Box sx={{flexGrow: 1, p: 3}}>
                <Typography variant="subtitle1" gutterBottom>
                    {selectedExecution ? `Test Cases in ${selectedExecution.executionName}` : 'Select a Execution to view test cases'}
                </Typography>
                {selectedExecution && (
                    <>
                        <Box display="flex" mb={2} justifyContent="space-between" alignItems="center">
                            <Button variant="outlined" sx={{mb: 2}} onClick={() => setOpenAddTestCaseDialog(true)}>
                                + Add Test Case
                            </Button>
                        </Box>
                        {/* Stats Section */}
                        <Box
                            display="flex"
                            gap={3}
                            flexWrap="wrap"
                            mb={3}
                            sx={{backgroundColor: '#f5f5f5', p: 2, borderRadius: 1}}
                        >
                            <Typography><strong>Total:</strong> {testCases.length}</Typography>
                            <Typography
                                color="success.main"><strong>Passed:</strong> {testCases.filter(tc => tc.resultStatus === 'PASS').length}
                            </Typography>
                            <Typography
                                color="error.main"><strong>Failed:</strong> {testCases.filter(tc => tc.resultStatus === 'FAIL').length}
                            </Typography>
                            <Typography
                                color="warning.main"><strong>Skipped:</strong> {testCases.filter(tc => tc.resultStatus === 'SKIPPED').length}
                            </Typography>
                            <Typography
                                color="text.secondary"><strong>Blocked:</strong> {testCases.filter(tc => tc.resultStatus === 'BLOCKED').length}
                            </Typography>
                            <Typography color="text.secondary"><strong>Not
                                Run:</strong> {testCases.filter(tc => tc.resultStatus === 'NOT_RUN' || !tc.resultStatus).length}
                            </Typography>
                        </Box>
                        <Paper>
                            <Table>
                                <TableBody>
                                    {testCases.map((tc) => (
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
                                                        {expandedRowId === tc.id ? <ExpandLessIcon/> :
                                                            <ExpandMoreIcon/>}
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
                                                        <DeleteIcon fontSize="small" color="error"/>
                                                    </IconButton>
                                                </TableCell>
                                            </TableRow>
                                            <TableRow>
                                                <TableCell colSpan={2} sx={{paddingBottom: 0, paddingTop: 0}}>
                                                    <Collapse in={expandedRowId === tc.id} timeout="auto" unmountOnExit>
                                                        <Box margin={2}>
                                                            {/* Test Steps Table */}
                                                            {tc.testSteps?.length > 0 && (
                                                                <Paper variant="outlined" sx={{mb: 2}}>
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
                                                                                            dangerouslySetInnerHTML={{__html: formatText(step.testStepDesc)}}
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
                                                                                                    fullWidth/>}
                                                                sx={{mb: 2, width: '300px'}}
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

            <Dialog open={openDialog} onClose={handleClose} fullWidth maxWidth="sm">
                <DialogTitle>{isEdit ? 'Edit Test Execution' : 'Add Test Execution'}</DialogTitle>
                <DialogContent dividers sx={{p: 3, display: 'flex', flexDirection: 'column', gap: 2}}>
                    <TextField
                        label="Execution Name"
                        value={executionName}
                        onChange={(e) => setExecutionName(e.target.value)}
                        fullWidth
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleClose}>Cancel</Button>
                    <Button onClick={handleSubmit} variant="contained">
                        {isEdit ? 'Update' : 'Create'}
                    </Button>
                </DialogActions>
            </Dialog>

            <Dialog open={openAddTestCaseDialog} onClose={() => setOpenAddTestCaseDialog(false)} fullWidth
                    maxWidth="sm">
                <DialogTitle>Select Test Cases to Add</DialogTitle>
                <DialogContent dividers sx={{p: 3, display: 'flex', flexDirection: 'column', gap: 2}}>
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
                                label="Test Cases"
                                placeholder="Choose test cases"
                            />
                        )}
                    />

                    {/* Add All Test Cases Button */}
                    <Button
                        sx={{mt: 2}}
                        onClick={() => setSelectedTestCases(allTestCases)}
                        variant="outlined"
                        fullWidth
                    >
                        Add All Test Cases
                    </Button>
                </DialogContent>

                <DialogActions>
                    <Button onClick={() => setOpenAddTestCaseDialog(false)}>Cancel</Button>
                    <Button onClick={handleAddSelectedTestCases} variant="contained">Add</Button>
                </DialogActions>
            </Dialog>


            <Snackbar
                open={alert.open}
                autoHideDuration={3000}
                onClose={() => setAlert({...alert, open: false})}
                anchorOrigin={{vertical: 'top', horizontal: 'center'}}
            >
                <Alert
                    onClose={() => setAlert({...alert, open: false})}
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