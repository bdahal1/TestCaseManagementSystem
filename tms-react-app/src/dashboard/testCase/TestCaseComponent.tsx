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
    Autocomplete, Typography
} from "@mui/material";
import {Box} from "@mui/system";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";

const API_URL_TESTCASE = "http://localhost:8080/dhtcms/api/v1/testCase";
const API_URL_PROJECT = "http://localhost:8080/dhtcms/api/v1/project";
const API_URL_TAGS = "http://localhost:8080/dhtcms/api/v1/tags";
const API_URL_TEST_STEPS = "http://localhost:8080/dhtcms/api/v1/testSteps";

interface Project {
    id: number;
    projectName: string;
}

interface TagsSet {
    id: number | null;
    tagName: string
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
    tagsSet: TagsSet[];
}

interface TestCaseComponentProps {
    projId?: number
}

const TestCaseComponent: React.FC<TestCaseComponentProps> = ({projId}) => {
    if (projId === null) {
        return <div>Please select a project.</div>;
    }

    const [testCases, setTestCases] = useState<TestCase[]>([]);
    const [projects, setProjects] = useState<Project[]>([]);
    const [tags, setTags] = useState<TagsSet[]>([]);
    const [selectedTags, setSelectedTags] = useState<TagsSet[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);
    const [snackbarOpen, setSnackbarOpen] = useState<boolean>(false);
    const [snackbarMessage, setSnackbarMessage] = useState<string>("");
    const [dialogOpen, setDialogOpen] = useState(false);
    const [selectedTestCase, setSelectedTestCase] = useState<TestCase | null>(null);
    const [formMode, setFormMode] = useState("create");

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
            const response = await axios.get(`${API_URL_TESTCASE}?projectId=${projId}`, {
                headers: {Authorization: `Bearer ${localStorage.getItem("authToken")}`},
            });
            setTestCases(response.data.testCase ?? []);
            setError(null);
        } catch {
            setError("Failed to fetch test cases.");
        } finally {
            setLoading(false);
        }
    };

    const fetchProjects = async () => {
        try {
            const response = await axios.get(`${API_URL_PROJECT}/id/${projId}`, {
                headers: {Authorization: `Bearer ${localStorage.getItem("authToken")}`},
            });
            setProjects(response.data.projects);
        } catch {
            setError("Failed to fetch projects.");
        }
    };

    const fetchTags = async () => {
        try {
            const response = await axios.get(API_URL_TAGS, {
                headers: {Authorization: `Bearer ${localStorage.getItem("authToken")}`},
            });
            setTags(Array.isArray(response.data?.tags) ? response.data.tags : []);
        } catch {
            setError("Failed to fetch tags.");
        }
    };

    const syncMissingTags = async (incomingTags: TagsSet[]): Promise<TagsSet[]> => {
        const existingTagNames = new Set(tags.map(tag => tag.tagName.toLowerCase()));
        const createdTags: TagsSet[] = [];

        for (const tag of incomingTags) {
            if (!existingTagNames.has(tag.tagName.toLowerCase())) {
                try {
                    const res = await axios.post(API_URL_TAGS, {tagName: tag.tagName}, {
                        headers: {Authorization: `Bearer ${localStorage.getItem("authToken")}`},
                    });
                    createdTags.push(res.data);
                } catch {
                }
            }
        }

        const updatedTagList = [...tags, ...createdTags];
        const tagMap = new Map<string, TagsSet>();
        updatedTagList.forEach(tag => tagMap.set(tag.tagName.toLowerCase(), tag));
        const deduplicatedTagList = Array.from(tagMap.values());

        setTags(deduplicatedTagList);

        return incomingTags.map(tag => {
            const found = deduplicatedTagList.find(t => t.tagName.toLowerCase() === tag.tagName.toLowerCase());
            return found || tag;
        });
    };

    const handleOpenDialog = async (testCase: TestCase | null) => {
        if (testCase) {
            setFormMode("edit");
            setSelectedTestCase(testCase);
            setTestCaseId(testCase.id);
            setTestName(testCase.testName);
            setProjectId(testCase.projects.id);
            setSelectedTags(testCase.tagsSet);
            try {
                const response = await axios.get(`${API_URL_TEST_STEPS}/testCaseId/${testCase.id}`, {
                    headers: {Authorization: `Bearer ${localStorage.getItem("authToken")}`},
                });
                setSteps(response.data.testSteps);
            } catch {
                setError("Failed to fetch steps.");
            }
        } else {
            setFormMode("create");
            setSteps([]);
            resetForm();
            setSteps([]);
            resetForm();
            if (projects.length === 1) {
                setProjectId(projects[0].id);
            } else {
                setProjectId(0);
            }
        }
        setDialogOpen(true);
    };

    const handleCloseDialog = () => {
        setDialogOpen(false);
        resetForm();
    };

    const handleSubmit = async (event: React.FormEvent) => {
        event.preventDefault();

        if (!testName.trim()) {
            setSnackbarMessage("Test Name is required.");
            setSnackbarOpen(true);
            return;
        }

        if (!projectId) {
            setSnackbarMessage("Project is required.");
            setSnackbarOpen(true);
            return;
        }

        const testPayload = {
            testName,
            projectId,
            userId: localStorage.getItem("userId"),
            selectedTags: selectedTags.map(tag => tag.id),
        };

        let currentTestCaseId = testCaseId;

        try {
            if (formMode === "edit" && selectedTestCase) {
                await axios.put(`${API_URL_TESTCASE}/${selectedTestCase.id}`, testPayload, {
                    headers: {Authorization: `Bearer ${localStorage.getItem("authToken")}`},
                });
                currentTestCaseId = selectedTestCase.id;
                setSnackbarMessage("Test case updated successfully!");
            } else {
                const response = await axios.post(API_URL_TESTCASE, testPayload, {
                    headers: {Authorization: `Bearer ${localStorage.getItem("authToken")}`},
                });
                currentTestCaseId = response.data.id;
                setSnackbarMessage("Test case created successfully!");
            }

            if (steps.length) {
                const testStepsPayload = steps.map((step, index) => ({
                    testStepDesc: step.testStepDesc,
                    testExpectedOutput: step.testExpectedOutput,
                    testStepData: step.testStepData,
                    testStepOrder: index + 1,
                    testCaseId: currentTestCaseId,
                    userId: localStorage.getItem("userId"),
                    stepId: step.id,
                }));
                await axios.post(API_URL_TEST_STEPS, testStepsPayload, {
                    headers: {Authorization: `Bearer ${localStorage.getItem("authToken")}`},
                });
            }

            fetchTestCases().then();
        } catch {
            setSnackbarMessage("Failed to save test case or steps.");
        } finally {
            setSnackbarOpen(true);
            handleCloseDialog();
        }
    };

    const deleteTestCase = async (id: number) => {
        try {
            await axios.delete(`${API_URL_TESTCASE}/${id}`, {
                headers: {Authorization: `Bearer ${localStorage.getItem("authToken")}`},
            });
            setTestCases(testCases.filter(tc => tc.id !== id));
            setSnackbarMessage("Test case deleted successfully!");
        } catch {
            setSnackbarMessage("Failed to delete test case.");
        } finally {
            setSnackbarOpen(true);
        }
    };

    const resetForm = () => {
        setTestName("");
        setSelectedTestCase(null);
        setSelectedTags([]);
    };

    useEffect(() => {
        fetchTestCases().then();
        fetchProjects().then();
        fetchTags().then();
    }, [projId]);

    useEffect(() => {
        if (projects.length === 1) {
            setProjectId(projects[0].id);
        }
    }, [projects]);

    if (loading) return <CircularProgress/>;
    if (error) return <div>{error}</div>;

    return (
        <Box sx={{padding: 2}}>
            <br/>
            <Button variant="outlined" onClick={() => handleOpenDialog(null)} sx={{mb: 2}}>
                + Add Test Case
            </Button>
            <TableContainer component={Paper}>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell>Test-ID</TableCell>
                            <TableCell>Name</TableCell>
                            <TableCell>Project</TableCell>
                            <TableCell>Tags</TableCell>
                            <TableCell>Created By</TableCell>
                            <TableCell>Actions</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {testCases.map((tc) => (
                            <TableRow key={tc.id}>
                                <TableCell><Link component="button"
                                                 onClick={() => handleOpenDialog(tc)}>{tc.testProjectId}</Link>
                                </TableCell>
                                <TableCell><Link component="button"
                                                 onClick={() => handleOpenDialog(tc)}>{tc.testName}</Link></TableCell>
                                <TableCell>{tc.projects.projectName}</TableCell>
                                <TableCell>{tc.tagsSet.map(t => t.tagName).join(", ")}</TableCell>
                                <TableCell>{tc.testCreatedBy}</TableCell>
                                <TableCell>
                                    <IconButton color="primary"
                                                onClick={() => handleOpenDialog(tc)}><EditIcon/></IconButton>
                                    <IconButton color="secondary"
                                                onClick={() => deleteTestCase(tc.id)}><DeleteIcon/></IconButton>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>

            <Dialog open={dialogOpen} onClose={handleCloseDialog} fullWidth maxWidth="md">
                <DialogTitle>{formMode === "create" ? "Add" : "Edit"} Test Case</DialogTitle>
                <DialogContent dividers sx={{display: "flex", flexDirection: "column", gap: 2, p: 3}}>
                    {/* Project Selection */}
                    {projects.length > 1 ? (
                        <FormControl fullWidth>
                            <InputLabel>Project</InputLabel>
                            <Select
                                required
                                value={projectId}
                                onChange={(e) => setProjectId(Number(e.target.value))}
                                label="Project" variant="outlined"
                            >
                                <MenuItem value={0} disabled>Select a project</MenuItem>
                                {projects.map((project) => (
                                    <MenuItem key={project.id} value={project.id}>{project.projectName}</MenuItem>
                                ))}
                            </Select>
                        </FormControl>
                    ) : (
                        <>
                            <input type="hidden" value={projectId}/>
                            <TextField
                                label="Project"
                                value={projects[0]?.projectName || ""}
                                fullWidth
                                slotProps={{input: {readOnly: true}}}
                            />
                        </>
                    )}
                    <TextField
                        label="Test Case Title"
                        value={testName}
                        onChange={(e) => setTestName(e.target.value)}
                        required
                        fullWidth
                    />
                    <Autocomplete
                        multiple
                        freeSolo
                        options={tags}
                        getOptionLabel={(option) => (typeof option === "string" ? option : option.tagName)}
                        value={selectedTags}
                        onChange={async (_, newValue) => {
                            const formattedTags: TagsSet[] = newValue.map((item) =>
                                typeof item === "string" ? {id: null, tagName: item} : item
                            );
                            const resolved = await syncMissingTags(formattedTags);
                            setSelectedTags(resolved);
                        }}
                        renderInput={(params) => <TextField {...params} label="Tags"/>}
                    />

                    <Box sx={{mt: 2}}>
                        <Box sx={{fontWeight: "bold", mb: 1}}>Test Steps</Box>
                        {steps.map((step, index) => (
                            <Box key={index}
                                 sx={{display: "grid", gridTemplateColumns: "1fr 1fr 1fr auto", gap: 2, mb: 2}}>
                                <TextField
                                    label={`Step ${index + 1}`}
                                    value={step.testStepDesc}
                                    onChange={(e) => handleStepChange(e.target.value, index, "testStepDesc")}
                                    fullWidth
                                />
                                <TextField
                                    label="Data"
                                    value={step.testStepData}
                                    onChange={(e) => handleStepChange(e.target.value, index, "testStepData")}
                                    fullWidth
                                />
                                <TextField
                                    label="Expected Output"
                                    value={step.testExpectedOutput}
                                    onChange={(e) => handleStepChange(e.target.value, index, "testExpectedOutput")}
                                    fullWidth
                                />
                                <IconButton onClick={() => removeStep(index)} color="error">
                                    <DeleteIcon/>
                                </IconButton>
                            </Box>
                        ))}
                        <Button onClick={addStep}>+ Add Step</Button>
                    </Box>
                </DialogContent>
                <DialogActions>
                    <Button variant="contained" onClick={handleSubmit}>
                        {formMode === "create" ? "Create" : "Update"}
                    </Button>
                    <Button variant="outlined" onClick={handleCloseDialog}>Cancel</Button>
                </DialogActions>
            </Dialog>

            <Snackbar open={snackbarOpen} autoHideDuration={6000} onClose={() => setSnackbarOpen(false)}>
                <Alert onClose={() => setSnackbarOpen(false)}
                       severity={snackbarMessage.includes("success") ? "success" : "error"}>
                    {snackbarMessage}
                </Alert>
            </Snackbar>
        </Box>
    );
};

export default TestCaseComponent;
