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
    Autocomplete, ListItemButton
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import axios from 'axios';

interface TestFolder {
    id: number;
    folderName: string;
}

interface TestCase {
    id: number;
    testName: string;
}

interface TestFolderComponentProps {
    projId: number;
}

const API_URL_TEST_FOLDER = 'http://localhost:8080/dhtcms/api/v1/testFolders';
const API_URL_TESTCASE = 'http://localhost:8080/dhtcms/api/v1/testCase';

const TestFolderComponent: React.FC<TestFolderComponentProps> = ({projId}) => {
    const [testFolders, setTestFolders] = useState<TestFolder[]>([]);
    const [selectedFolder, setSelectedFolder] = useState<TestFolder | null>(null);
    const [testCases, setTestCases] = useState<TestCase[]>([]);
    const [openDialog, setOpenDialog] = useState(false);
    const [folderName, setFolderName] = useState('');
    const [isEdit, setIsEdit] = useState(false);
    const [alert, setAlert] = useState({
        open: false,
        message: '',
        severity: 'success' as 'success' | 'error' | 'warning' | 'info'
    });
    const [openAddTestCaseDialog, setOpenAddTestCaseDialog] = useState(false);
    const [allTestCases, setAllTestCases] = useState<TestCase[]>([]);
    const [selectedTestCases, setSelectedTestCases] = useState<TestCase[]>([]);
    const [hoveredRow, setHoveredRow] = useState<number | null>(null);
    const [selectedForDeletion] = useState<number[]>([]);

    const fetchTestFolders = async () => {
        try {
            const response = await axios.get(`${API_URL_TEST_FOLDER}/project/${projId}`, {
                headers: {Authorization: `Bearer ${localStorage.getItem('authToken')}`}
            });
            setTestFolders(response.data.testFolders || []);
            if (response.data.testFolders?.length) {
                setSelectedFolder(response.data.testFolders[0]);
            }
        } catch (error) {
            console.error('Error fetching test folders:', error);
        }
    };

    const fetchAllFolderTestCases = async (folderId: number) => {
        try {
            const response = await axios.get(`${API_URL_TEST_FOLDER}/id/${folderId}`, {
                headers: {Authorization: `Bearer ${localStorage.getItem('authToken')}`}
            });
            const sortedCases = (response.data.testCases || []).sort((a: TestCase, b: TestCase) => a.testName.localeCompare(b.testName));
            setTestCases(sortedCases);
        } catch (error) {
            console.error('Error fetching test cases:', error);
        }
    };

    const fetchAllUnassignedTestCases = async () => {
        try {
            const response = await axios.get(`${API_URL_TESTCASE}/unassignedFolder?projectId=${projId}`, {
                headers: {Authorization: `Bearer ${localStorage.getItem('authToken')}`}
            });
            setAllTestCases(response.data.testCase);
        } catch (err) {
            console.error("Failed to fetch test cases.");
        }
    };


    const handleAddSelectedTestCases = async () => {
        if (!selectedFolder || !selectedTestCases.length) return;
        try {
            await axios.post(
                `${API_URL_TEST_FOLDER}/${selectedFolder.id}/addCases`,
                {testCaseIds: selectedTestCases.map(tc => tc.id)},
                {headers: {Authorization: `Bearer ${localStorage.getItem('authToken')}`}}
            );
            fetchAllFolderTestCases(selectedFolder.id).then();
            fetchAllUnassignedTestCases().then();
            setOpenAddTestCaseDialog(false);
            setSelectedTestCases([]);
            setAlert({open: true, message: 'Test cases added to folder.', severity: 'success'});
        } catch (error) {
            console.error('Error adding test cases:', error);
            setAlert({open: true, message: 'Failed to add test cases.', severity: 'error'});
        }
    };

    const handleRemoveTestsFromList = async (testCaseId: number) => {
        if (!selectedFolder?.id) return;
        try {
            await axios.post(
                `${API_URL_TEST_FOLDER}/${selectedFolder.id}/removeCases`,
                {testCaseIds: [testCaseId]},
                {headers: {Authorization: `Bearer ${localStorage.getItem('authToken')}`}}
            );
            setTestCases(prev => prev.filter(tc => tc.id !== testCaseId));
            fetchAllUnassignedTestCases().then();
            setAlert({open: true, message: 'Test case deleted.', severity: 'success'});
        } catch (error) {
            console.error('Error deleting test case:', error);
            setAlert({open: true, message: 'Failed to delete test case.', severity: 'error'});
        }
    };
    useEffect(() => {
        if (projId) {
            fetchTestFolders().then();
        }
    }, [projId]);

    useEffect(() => {
        if (selectedFolder) {
            fetchAllUnassignedTestCases().then();
            fetchAllFolderTestCases(selectedFolder.id).then();
        }
    }, [selectedFolder]);

    const handleOpenAdd = () => {
        setIsEdit(false);
        setFolderName('');
        setSelectedFolder(null);
        setOpenDialog(true);
    };

    const handleOpenEdit = (folder: TestFolder) => {
        setIsEdit(true);
        setFolderName(folder.folderName);
        setSelectedFolder(folder);
        setOpenDialog(true);
    };

    const handleClose = () => {
        setOpenDialog(false);
        setFolderName('');
        setSelectedFolder(null);
    };

    const handleSubmit = async () => {
        if (!folderName.trim()) {
            setAlert({open: true, message: 'Folder name cannot be empty.', severity: 'error'});
            return;
        }

        try {
            if (isEdit && selectedFolder) {
                const response = await axios.put(
                    `${API_URL_TEST_FOLDER}/${selectedFolder.id}`,
                    {folderName, projectId: projId},
                    {headers: {Authorization: `Bearer ${localStorage.getItem('authToken')}`}}
                );
                setTestFolders((prev) =>
                    prev.map((f) => (f.id === selectedFolder.id ? response.data : f))
                );
                setAlert({open: true, message: 'Test folder updated.', severity: 'success'});
            } else {
                const response = await axios.post(
                    API_URL_TEST_FOLDER,
                    {folderName, projectId: projId},
                    {headers: {Authorization: `Bearer ${localStorage.getItem('authToken')}`}}
                );
                setTestFolders((prev) => [...prev, response.data]);
                setAlert({open: true, message: 'Test folder created.', severity: 'success'});
            }
            handleClose();
        } catch (error) {
            console.error('Error saving folder:', error);
            setAlert({open: true, message: 'Error saving folder.', severity: 'error'});
        }
    };

    const deleteTestFolder = async (folderId: number) => {
        try {
            await axios.delete(`${API_URL_TEST_FOLDER}/${folderId}`, {
                headers: {Authorization: `Bearer ${localStorage.getItem('authToken')}`}
            });
            const updatedFolders = testFolders.filter((f) => f.id !== folderId);
            setTestFolders(updatedFolders);
            if (selectedFolder?.id === folderId && updatedFolders.length) {
                setSelectedFolder(updatedFolders[0]);
            } else if (!updatedFolders.length) {
                setSelectedFolder(null);
                setTestCases([]);
            }
            setAlert({open: true, message: 'Test folder deleted.', severity: 'success'});
        } catch (error) {
            console.error('Error deleting folder:', error);
            setAlert({open: true, message: 'Failed to delete folder.', severity: 'error'});
        }
    };

    return (
        <Box display="flex">
            <Box sx={{width: 240, p: 2}}>
                <Button variant="contained" size="small" onClick={handleOpenAdd} sx={{mt: 1, mb: 2}}>
                    + Add Folder
                </Button>
                <Divider/>
                <List>
                    {testFolders.map((folder) => (
                        <ListItem
                            key={folder.id}
                            disablePadding
                            sx={{
                                '&:hover .action-icons': {opacity: 1},
                                position: 'relative',
                            }}
                        >
                            <ListItemButton
                                selected={selectedFolder?.id === folder.id}
                                onClick={() => setSelectedFolder(folder)}
                                sx={{pr: 6}} // leave space for icons
                            >
                                <ListItemText primary={folder.folderName}/>
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
                                <IconButton size="small" onClick={() => handleOpenEdit(folder)}>
                                    <EditIcon fontSize="small"/>
                                </IconButton>
                                <IconButton size="small" onClick={() => deleteTestFolder(folder.id)}>
                                    <DeleteIcon fontSize="small"/>
                                </IconButton>
                            </Box>
                        </ListItem>
                    ))}
                </List>
            </Box>
            <Box sx={{flexGrow: 1, p: 3}}>
                <Typography variant="h6" gutterBottom>
                    {selectedFolder ? `Test Cases in ${selectedFolder.folderName}` : 'Select a folder to view test cases'}
                </Typography>
                {selectedFolder && (
                    <>
                        <Button variant="outlined" sx={{mb: 2}} onClick={() => setOpenAddTestCaseDialog(true)}>
                           + Add Test Case
                        </Button>
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
                <DialogTitle>{isEdit ? 'Edit Test Folder' : 'Add Test Folder'}</DialogTitle>
                <DialogContent sx={{mt: 2}}>
                    <TextField
                        label="Folder Name"
                        value={folderName}
                        onChange={(e) => setFolderName(e.target.value)}
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

export default TestFolderComponent;
