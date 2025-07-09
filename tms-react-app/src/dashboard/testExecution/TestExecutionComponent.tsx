import React, {useEffect, useState} from 'react';
import {
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Divider,
    List,
    ListItem,
    ListItemText,
    TextField,
    Typography,
    Snackbar,
    Alert,
    IconButton,
    Paper,
    Table,
    TableBody,
    TableCell,
    TableRow,
    Autocomplete,
    ListItemButton
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import axios from 'axios';
import AccordionTestResultComponent from './AccordianTestResultComponent.tsx';
import {TestCase, TestExecution} from '../../types/TestCase';

interface TestExecutionComponentProps {
    projId: number;
}

const API_URL_TEST_EXECUTION = 'http://localhost:8080/dhtcms/api/v1/testExecutions';
const API_URL_TESTCASE = 'http://localhost:8080/dhtcms/api/v1/testCase';

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
    const [openExecuteTestsDialog, setOpenExecuteTestsDialog] = useState(false);
    const [allTestCases, setAllTestCases] = useState<TestCase[]>([]);
    const [selectedTestCases, setSelectedTestCases] = useState<TestCase[]>([]);
    const [hoveredRow, setHoveredRow] = useState<number | null>(null);
    const [selectedForDeletion] = useState<number[]>([]);

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
                return 'white';
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
                <Button variant="contained" size="small" onClick={handleOpenAdd} sx={{mt: 1, mb: 2}}>
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
                <Typography variant="h6" gutterBottom>
                    {selectedExecution ? `Test Cases in ${selectedExecution.executionName}` : 'Select a Execution to view test cases'}
                </Typography>
                {selectedExecution && (
                    <>
                        <Box display="flex" mb={2} justifyContent="space-between" alignItems="center">
                            <Button variant="outlined" onClick={() => setOpenAddTestCaseDialog(true)}>
                                Add Test Case
                            </Button>
                            <Button
                                variant="contained"
                                color="success"
                                onClick={async () => {
                                    if (selectedExecution?.id) {
                                        await fetchAllExecutionTestCases(selectedExecution.id);
                                        setOpenExecuteTestsDialog(true);
                                    }
                                }}
                            >
                                Execute Tests
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
                            <Typography><strong>Total:</strong> {testCases.length}</Typography>
                            <Typography color="success.main"><strong>Passed:</strong> {testCases.filter(tc => tc.resultStatus === 'PASS').length}</Typography>
                            <Typography color="error.main"><strong>Failed:</strong> {testCases.filter(tc => tc.resultStatus === 'FAIL').length}</Typography>
                            <Typography color="warning.main"><strong>Skipped:</strong> {testCases.filter(tc => tc.resultStatus === 'SKIPPED').length}</Typography>
                            <Typography color="text.secondary"><strong>Blocked:</strong> {testCases.filter(tc => tc.resultStatus === 'BLOCKED').length}</Typography>
                            <Typography color="text.secondary"><strong>Not Run:</strong> {testCases.filter(tc => tc.resultStatus === 'NOT_RUN' || !tc.resultStatus).length}</Typography>
                        </Box>
                        <Paper>
                            <Table>
                                <TableBody>
                                    {testCases.map((tc) => (
                                        <TableRow
                                            key={tc.id}
                                            onMouseEnter={() => setHoveredRow(tc.id)}
                                            onMouseLeave={() => setHoveredRow(null)}
                                            selected={selectedForDeletion.includes(tc.id)}
                                            sx={{
                                                cursor: 'pointer',
                                                backgroundColor: getRowColor(tc.resultStatus),
                                                '&:hover': {
                                                    backgroundColor: 'rgba(0, 0, 0, 0.04)', // subtle gray hover effect
                                                },
                                            }}
                                        >
                                            <TableCell>{tc.testName}</TableCell>
                                            <TableCell
                                                align="right"
                                                sx={{
                                                    width: '40px',
                                                    padding: '4px 8px', // keep it compact
                                                }}
                                            >
                                                <Box
                                                    sx={{
                                                        height: '24px',
                                                        width: '24px',
                                                        display: 'flex',
                                                        alignItems: 'center',
                                                        justifyContent: 'center',
                                                    }}
                                                >
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
                                                            padding: 0, // remove default IconButton padding
                                                            height: '24px',
                                                            width: '24px',
                                                        }}
                                                    >
                                                        <DeleteIcon fontSize="small" color="error"/>
                                                    </IconButton>
                                                </Box>
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        </Paper>
                    </>
                )}
            </Box>

            <Dialog open={openDialog} onClose={handleClose} fullWidth maxWidth="sm">
                <DialogTitle>{isEdit ? 'Edit Test Execution' : 'Add Test Execution'}</DialogTitle>
                <DialogContent sx={{mt: 2}}>
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
                <DialogContent sx={{mt: 2}}>
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
            <Dialog
                open={openExecuteTestsDialog}
                onClose={() => setOpenExecuteTestsDialog(false)}
                fullWidth
                maxWidth="md"
            >
                <DialogTitle>Execute Tests</DialogTitle>
                <DialogContent dividers>
                    {openExecuteTestsDialog && testCases.map((tc) => (
                        <AccordionTestResultComponent
                            key={tc.id}
                            testCase={tc}
                            executionId={selectedExecution?.id}
                            token={localStorage.getItem('authToken')}
                            onResultUpdate={(resultStatus, resultComment) => {
                                setTestCases(prev =>
                                    prev.map(t =>
                                        t.id === tc.id ? {...t, resultStatus, resultComment} : t
                                    )
                                );
                            }}
                        />
                    ))}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setOpenExecuteTestsDialog(false)}>Close</Button>
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