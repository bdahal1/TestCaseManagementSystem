import React, {useEffect, useState} from "react";
import axios from "axios";
import {
    TextField,
    Button,
    MenuItem,
    Select,
    InputLabel,
    FormControl,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Paper,
    CircularProgress,
    Snackbar,
    Alert,
    IconButton,
    Link,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
} from "@mui/material";
import {Box} from "@mui/system";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";

const API_URL_TESTCASE = "http://localhost:8080/dhtcms/api/v1/testCase";
const API_URL_PROJECT = "http://localhost:8080/dhtcms/api/v1/project";
const API_URL_TESTSTEPS = "http://localhost:8080/dhtcms/api/v1/testSteps";

interface Project {
    id: number;
    projectName: string;
}

interface TestCase {
    id: number;
    testName: string;
    testProjectId: string;
    testCreatedBy: string;
    testCreatedDate: string;
    testModifiedBy: string;
    testModifiedDate: string;
    projects: Project;
}

const TestCaseComponent: React.FC = () => {
    const [testCases, setTestCases] = useState<TestCase[]>([]);
    const [projects, setProjects] = useState<Project[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);
    const [snackbarOpen, setSnackbarOpen] = useState<boolean>(false);
    const [snackbarMessage, setSnackbarMessage] = useState<string>("");
    const [dialogOpen, setDialogOpen] = useState(false);
    const [selectedTestCase, setSelectedTestCase] = useState<TestCase | null>(null);
    const [formMode, setFormMode] = useState<"create" | "edit">("create");

    // Form state
    const [testName, setTestName] = useState<string>("");
    const [testCaseId, setTestCaseId] = useState<number>(0);
    const [projectId, setProjectId] = useState<number>(0);
    const [steps, setSteps] = useState<{
        id: number;
        testStepDesc: string;
        testStepData: string,
        testExpectedOutput: string
    }[]>([]);

// Handle adding a new step
    const addStep = () => {
        setSteps([...steps, {id: 0, testStepDesc: "", testStepData: "", testExpectedOutput: ""}]);
    };

// Handle removing a step
    const removeStep = (index: number) => {
        const updatedSteps = steps.filter((_, i) => i !== index);
        setSteps(updatedSteps);
    };

// Handle updating a specific step or action
    const handleStepChange = (value: string, index: number, field: "testStepDesc" | "testStepData" | "testExpectedOutput") => {
        const updatedSteps = [...steps];
        updatedSteps[index][field] = value;
        setSteps(updatedSteps);
    };

    // Fetch all test cases
    const fetchTestCases = async () => {
        setLoading(true);
        try {
            const response = await axios.get(`${API_URL_TESTCASE}`, {
                headers: {Authorization: `Bearer ` + localStorage.getItem("authToken")},
            });
            setTestCases(response.data.testCase);
            setError(null);
        } catch (err) {
            setError("Failed to fetch test cases.");
        } finally {
            setLoading(false);
        }
    };

    // Fetch all projects
    const fetchProjects = async () => {
        try {
            const response = await axios.get(API_URL_PROJECT, {
                headers: {Authorization: `Bearer ` + localStorage.getItem("authToken")},
            });
            setProjects(response.data.projects);
        } catch (err) {
            setError("Failed to fetch projects.");
        }
    };

    const handleOpenDialog = async (testCase: TestCase | null) => {
        if (testCase) {
            setFormMode("edit");
            setSelectedTestCase(testCase);
            setTestCaseId(testCase.id)
            setTestName(testCase.testName);
            setProjectId(testCase.projects.id);
            try {
                const response = await axios.get(`${API_URL_TESTSTEPS}/testCaseId/${testCase.id}`, {
                    headers: {Authorization: `Bearer ` + localStorage.getItem("authToken")},
                });
                setSteps(response.data.testSteps);
            } catch (err) {
                setError("Failed to fetch projects.");
            }
        } else {
            setFormMode("create");
            setSteps([]);
            resetForm();
        }
        setDialogOpen(true);
    };

    const handleCloseDialog = () => {
        setDialogOpen(false);
        resetForm();
    };

    const handleSubmit = async (event: React.FormEvent) => {
        event.preventDefault();

        const testPayload = {
            testName,
            projectId,
            userId: localStorage.getItem("userId"),
        };

        try {
            if (formMode === "edit" && selectedTestCase) {
                await axios.put(`${API_URL_TESTCASE}/${selectedTestCase.id}`, testPayload, {
                    headers: {Authorization: `Bearer ` + localStorage.getItem("authToken")},
                });
                setSnackbarMessage("Test case updated successfully!");
            } else {
                await axios.post(API_URL_TESTCASE, testPayload, {
                    headers: {Authorization: `Bearer ` + localStorage.getItem("authToken")},
                });
                setSnackbarMessage("Test case created successfully!");
            }

            fetchTestCases().then(); // Refresh the list
        } catch (err) {
            setSnackbarMessage("Failed to save test case.");
        } finally {
            setSnackbarOpen(true);
            handleCloseDialog();
        }
        try {
            // Prepare and send steps payload
            const testStepsPayload = steps.map((step, index) => ({
                testStepDesc: step.testStepDesc,
                testExpectedOutput: step.testExpectedOutput,
                testRemarks: step.testStepData, // Map data to remarks or adjust fields
                testStepOrder: index + 1,
                testCaseId,
                userId: localStorage.getItem("userId"),
                stepId: step.id
            }));

            if (testStepsPayload.length !== 0) {
                await axios.post(`${API_URL_TESTSTEPS}`, testStepsPayload, {
                    headers: {Authorization: `Bearer ` + localStorage.getItem("authToken")},
                });
            }

            fetchTestCases().then(); // Refresh the list
        } catch (err) {
            setSnackbarMessage("Failed to save Test steps.");
        } finally {
            setSnackbarOpen(true);
            handleCloseDialog();
        }
    };

    const deleteTestCase = async (id: number) => {
        try {
            await axios.delete(`${API_URL_TESTCASE}/${id}`, {
                headers: {Authorization: `Bearer ` + localStorage.getItem("authToken")},
            });
            setTestCases(testCases.filter((testCase) => testCase.id !== id));
            setSnackbarMessage("Test case deleted successfully!");
        } catch (err) {
            setSnackbarMessage("Failed to delete test case.");
        } finally {
            setSnackbarOpen(true);
        }
    };

    const resetForm = () => {
        setTestName("");
        setProjectId(0);
        setSelectedTestCase(null);
    };

    useEffect(() => {
        fetchTestCases().then();
        fetchProjects().then();
    }, []);

    if (loading) return <CircularProgress/>;
    if (error) return <div>{error}</div>;

    return (
        <Box sx={{padding: 2}}>
            <h1>Test Case Manager</h1>

            <Box>
                <Button
                    variant="contained"
                    color="primary"
                    onClick={() => handleOpenDialog(null)}
                    sx={{marginBottom: 2}}
                >
                    Add Test Case
                </Button>

                <TableContainer component={Paper}>
                    <Table>
                        <TableHead>
                            <TableRow>
                                <TableCell>Test-ID</TableCell>
                                <TableCell>Name</TableCell>
                                <TableCell>Project</TableCell>
                                <TableCell>Created By</TableCell>
                                <TableCell>Actions</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {Array.isArray(testCases) && testCases.map((testCase) => (
                                <TableRow key={testCase.id}>
                                    <TableCell>
                                        <Link component="button" onClick={() => handleOpenDialog(testCase)}>
                                            {testCase.testProjectId}
                                        </Link>
                                    </TableCell>
                                    <TableCell>
                                        <Link component="button" onClick={() => handleOpenDialog(testCase)}>
                                            {testCase.testName}
                                        </Link>
                                    </TableCell>
                                    <TableCell>{testCase.projects.projectName}</TableCell>
                                    <TableCell>{testCase.testCreatedBy}</TableCell>
                                    <TableCell>
                                        <IconButton color="primary" onClick={() => handleOpenDialog(testCase)}>
                                            <EditIcon/>
                                        </IconButton>
                                        <IconButton color="secondary" onClick={() => deleteTestCase(testCase.id)}>
                                            <DeleteIcon/>
                                        </IconButton>
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </TableContainer>
            </Box>

            <Dialog open={dialogOpen} onClose={handleCloseDialog} fullWidth>
                <DialogTitle>{formMode === "create" ? "Add Test Case" : "Edit Test Case"}</DialogTitle>
                <DialogContent>
                    <FormControl fullWidth sx={{marginBottom: 2}}>
                        <InputLabel>Project</InputLabel>
                        <Select
                            value={projectId}
                            onChange={(e) => setProjectId(Number(e.target.value))}
                            label="Project"
                        >
                            <MenuItem value={0} disabled>
                                Select a project
                            </MenuItem>
                            {projects.map((project) => (
                                <MenuItem key={project.id} value={project.id}>
                                    {project.projectName}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>

                    <TextField
                        label="Test Name"
                        value={testName}
                        onChange={(e) => setTestName(e.target.value)}
                        required
                        fullWidth
                    />
                    {Array.isArray(steps) && steps.map((step, index) => (
                        <Box key={index} sx={{display: 'flex', alignItems: 'center', gap: 1, marginBottom: 2}}>
                            <TextField
                                label={`Step ${index + 1}`}
                                value={step.testStepDesc}
                                onChange={(e) => handleStepChange(e.target.value, index, "testStepDesc")}
                                fullWidth
                            />
                            <TextField
                                label={`Data ${index + 1}`}
                                value={step.testStepData}
                                onChange={(e) => handleStepChange(e.target.value, index, "testStepData")}
                                fullWidth
                            />
                            <TextField
                                label={`Action ${index + 1}`}
                                value={step.testExpectedOutput}
                                onChange={(e) => handleStepChange(e.target.value, index, "testExpectedOutput")}
                                fullWidth
                            />
                            <IconButton onClick={() => removeStep(index)} color="error">
                                <DeleteIcon/>
                            </IconButton>
                        </Box>
                    ))}
                    <Button onClick={addStep} variant="contained" color="primary">
                        + Add Step
                    </Button>
                </DialogContent>
                <DialogActions>
                    <Button variant="contained" color="primary" onClick={handleSubmit}>
                        {formMode === "create" ? "Create" : "Update"}
                    </Button>
                    <Button variant="outlined" onClick={handleCloseDialog}>
                        Cancel
                    </Button>
                </DialogActions>
            </Dialog>

            <Snackbar
                open={snackbarOpen}
                autoHideDuration={6000}
                onClose={() => setSnackbarOpen(false)}
            >
                <Alert
                    onClose={() => setSnackbarOpen(false)}
                    severity={snackbarMessage.includes("success") ? "success" : "error"}
                >
                    {snackbarMessage}
                </Alert>
            </Snackbar>
        </Box>
    );
};

export default TestCaseComponent;
