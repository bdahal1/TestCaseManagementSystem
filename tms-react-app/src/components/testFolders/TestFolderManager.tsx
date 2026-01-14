import React, { useEffect, useState } from 'react';
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
    ListItemButton,
    InputAdornment
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import CreateNewFolderIcon from '@mui/icons-material/CreateNewFolder';
import SearchIcon from '@mui/icons-material/Search';
import AddIcon from '@mui/icons-material/Add';
import api from "../../services/api";

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

const API_URL_TEST_FOLDER = '/testFolders';
const API_URL_TESTCASE = '/testCase';

const TestFolderComponent: React.FC<TestFolderComponentProps> = ({ projId }) => {
    const [testFolders, setTestFolders] = useState<TestFolder[]>([]);
    const [selectedFolder, setSelectedFolder] = useState<TestFolder | null>(null);
    const [testCases, setTestCases] = useState<TestCase[]>([]);
    const [searchQuery, setSearchQuery] = useState('');
    const [filteredTestCases, setFilteredTestCases] = useState<TestCase[]>([]);
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
    const [searchFolderQuery, setSearchFolderQuery] = useState('');

    const filteredFolders = testFolders.filter(folder =>
        folder.folderName.toLowerCase().includes(searchFolderQuery.toLowerCase())
    );

    const fetchTestFolders = async () => {
        try {
            const response = await api.get(`${API_URL_TEST_FOLDER}/project/${projId}`);
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
            const response = await api.get(`${API_URL_TEST_FOLDER}/id/${folderId}`);
            const sortedCases = (response.data.testCases || []).sort((a: TestCase, b: TestCase) => a.testName.localeCompare(b.testName));
            setTestCases(sortedCases);
            setFilteredTestCases(sortedCases);
        } catch (error) {
            console.error('Error fetching test cases:', error);
        }
    };

    const fetchAllUnassignedTestCases = async () => {
        try {
            const response = await api.get(`${API_URL_TESTCASE}/unassignedFolder?projectId=${projId}`);
            setAllTestCases(response.data.testCase);
        } catch (err) {
            console.error("Failed to fetch test cases.");
        }
    };


    const handleAddSelectedTestCases = async () => {
        if (!selectedFolder || !selectedTestCases.length) return;
        try {
            await api.post(
                `${API_URL_TEST_FOLDER}/${selectedFolder.id}/addCases`,
                { testCaseIds: selectedTestCases.map(tc => tc.id) }
            );
            fetchAllFolderTestCases(selectedFolder.id).then();
            fetchAllUnassignedTestCases().then();
            setOpenAddTestCaseDialog(false);
            setSelectedTestCases([]);
            setAlert({ open: true, message: 'Test cases added to folder.', severity: 'success' });
        } catch (error) {
            console.error('Error adding test cases:', error);
            setAlert({ open: true, message: 'Failed to add test cases.', severity: 'error' });
        }
    };

    const handleRemoveTestsFromList = async (testCaseId: number) => {
        if (!selectedFolder?.id) return;
        try {
            await api.post(
                `${API_URL_TEST_FOLDER}/${selectedFolder.id}/removeCases`,
                { testCaseIds: [testCaseId] }
            );
            setTestCases(prev => prev.filter(tc => tc.id !== testCaseId));
            fetchAllUnassignedTestCases().then();
            setAlert({ open: true, message: 'Test case deleted.', severity: 'success' });
        } catch (error) {
            console.error('Error deleting test case:', error);
            setAlert({ open: true, message: 'Failed to delete test case.', severity: 'error' });
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

    useEffect(() => {
        const lowerCaseQuery = searchQuery.toLowerCase();
        const filtered = testCases.filter(tc =>
            tc.testName.toLowerCase().includes(lowerCaseQuery)
        );
        setFilteredTestCases(filtered);
    }, [searchQuery, testCases]);

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
            setAlert({ open: true, message: 'Folder name cannot be empty.', severity: 'error' });
            return;
        }

        try {
            if (isEdit && selectedFolder) {
                await api.put(
                    `${API_URL_TEST_FOLDER}/${selectedFolder.id}`,
                    { folderName, projectId: projId }
                );
                setAlert({ open: true, message: 'Test folder updated.', severity: 'success' });
            } else {
                await api.post(
                    API_URL_TEST_FOLDER,
                    { folderName, projectId: projId }
                );
                setAlert({ open: true, message: 'Test folder created.', severity: 'success' });
            }
            fetchTestFolders().then();
            handleClose();
        } catch (error) {
            console.error('Error saving folder:', error);
            setAlert({ open: true, message: 'Error saving folder.', severity: 'error' });
        }
    };

    const deleteTestFolder = async (folderId: number) => {
        try {
            await api.delete(`${API_URL_TEST_FOLDER}/${folderId}`);
            fetchTestFolders().then();
            if (selectedFolder?.id === folderId) {
                setSelectedFolder(null);
                setTestCases([]);
            }
            setAlert({ open: true, message: 'Test folder deleted.', severity: 'success' });
        } catch (error) {
            console.error('Error deleting folder:', error);
            setAlert({ open: true, message: 'Failed to delete folder.', severity: 'error' });
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
                    startIcon={<CreateNewFolderIcon />}
                >
                    Add Folder
                </Button>
                <TextField
                    placeholder="Search folders..."
                    size="small"
                    fullWidth
                    variant="outlined"
                    value={searchFolderQuery}
                    onChange={(e) => setSearchFolderQuery(e.target.value)}
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
                    {filteredFolders.map((folder) => (
                        <ListItem
                            key={folder.id}
                            disablePadding
                            sx={{
                                '&:hover .action-icons': { opacity: 1 },
                                position: 'relative',
                            }}
                        >
                            <ListItemButton
                                selected={selectedFolder?.id === folder.id}
                                onClick={() => setSelectedFolder(folder)}
                                sx={{ pr: 6, borderRadius: 1 }} // leave space for icons
                            >
                                <ListItemText
                                    primary={folder.folderName}
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
                                <IconButton size="small" onClick={() => handleOpenEdit(folder)}>
                                    <EditIcon fontSize="small" color="primary" />
                                </IconButton>
                                <IconButton size="small" onClick={() => deleteTestFolder(folder.id)}>
                                    <DeleteIcon fontSize="small" color="error" />
                                </IconButton>
                            </Box>
                        </ListItem>
                    ))}
                </List>
            </Box>
            <Box sx={{ flexGrow: 1, p: 3 }}>
                <Typography variant="h6" fontWeight="bold" gutterBottom>
                    {selectedFolder ? `${selectedFolder.folderName}` : 'Select a folder to view test cases'}
                </Typography>
                {selectedFolder && (
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
                        <Paper>
                            <Table>
                                <TableBody>
                                    {filteredTestCases.map((tc) => (
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
                                                        <DeleteIcon fontSize="small" color="error" />
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

            {/* Add/Edit Folder Dialog */}
            <Dialog
                open={openDialog}
                onClose={handleClose}
                fullWidth
                maxWidth="xs"
                PaperProps={{ sx: { borderRadius: '20px', p: 1 } }}
            >
                <DialogTitle sx={{ fontWeight: 700, fontSize: '1.25rem', pb: 1 }}>
                    {isEdit ? 'Edit Test Folder' : 'New Test Folder'}
                </DialogTitle>
                <DialogContent sx={{ p: 3, pt: 1 }}>
                    <Box component="form" sx={{ mt: 1 }}>
                        <TextField
                            label="Folder Name"
                            value={folderName}
                            onChange={(e) => setFolderName(e.target.value)}
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

export default TestFolderComponent;
