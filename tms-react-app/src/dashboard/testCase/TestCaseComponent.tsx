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
    Autocomplete
} from "@mui/material";
import {Box} from "@mui/system";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";

const API_URL_TESTCASE = "/dhtcms/api/v1/testCase";
const API_URL_PROJECT = "/dhtcms/api/v1/project";
const API_URL_TAGS = "/dhtcms/api/v1/tags";
const API_URL_TEST_STEPS = "/dhtcms/api/v1/testSteps";

interface Project {
    id: number;
    projectName: string;
}

interface TagsSet {
    id: number | null;
    tagName: string
}

enum TestTypes {
    MANUAL = "MANUAL",
    CUCUMBER_MANUAL = "CUCUMBER_MANUAL",
    CUCUMBER_AUTOMATION = "CUCUMBER_AUTOMATION",
    KEYWORD_DRIVEN = "KEYWORD_DRIVEN"
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
    testType?: TestTypes;
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
    const [testType, setTestType] = useState<TestTypes | "">(formMode === "edit" ? selectedTestCase?.testType ?? "" : "");
    // Form state
    const [testName, setTestName] = useState<string>("");
    const [testCaseId, setTestCaseId] = useState<number>(0);
    const [projectId, setProjectId] = useState<number>(0);
    const [steps, setSteps] = useState<{
        id: number
        testStepDesc: string
        testStepData: string
        testExpectedOutput: string
        testType: TestTypes
    }[]>([]);
    const testTypeOptions = [
        {label: "Manual", value: TestTypes.MANUAL},
        {label: "Cucumber Manual", value: TestTypes.CUCUMBER_MANUAL},
        {label: "Cucumber Automation", value: TestTypes.CUCUMBER_AUTOMATION},
        {label: "Keyword Driven", value: TestTypes.KEYWORD_DRIVEN}
    ];
// Handle adding a new step
    const addStep = () => {
        console.log("Adding a step...");
        setSteps([...steps, {
            id: 0,
            testStepDesc: "",
            testStepData: "",
            testExpectedOutput: "",
            testType: TestTypes.MANUAL
        }]);
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

    const handleOpenDialog = async (testCase: TestCase | null, overrideTestType?: TestTypes) => {
        if (testCase) {
            setFormMode("edit");
            setSelectedTestCase(testCase);
            setTestCaseId(testCase.id);
            setTestName(testCase.testName);
            setProjectId(testCase.projects.id);
            setSelectedTags(testCase.tagsSet);
            const effectiveType = overrideTestType || testCase.testType;
            if (effectiveType) {
                setTestType(effectiveType);
            }
            try {
                const response = await axios.get(`${API_URL_TEST_STEPS}/testCaseId/${testCase.id}?type=${effectiveType}`, {
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
            testType: testType
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
                    testType: testType
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

    useEffect(() => {
        if ((testType === TestTypes.CUCUMBER_MANUAL || testType === TestTypes.CUCUMBER_AUTOMATION) && steps.length === 0) {
            setSteps([{
                id: 0,
                testStepDesc: "",
                testStepData: "",
                testExpectedOutput: "",
                testType: TestTypes.MANUAL
            }]);
        }
    }, [steps]);

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
                    <FormControl fullWidth required>
                        <InputLabel>Test Type</InputLabel>
                        <Select
                            value={testType}
                            onChange={(e) => {
                                const selectedValue = e.target.value as TestTypes;
                                console.log("Selected Test Type:", selectedValue);
                                setTestType(selectedValue);
                                handleOpenDialog(selectedTestCase ?? null, selectedValue).then();
                            }}
                            label="Test Type"
                            variant="outlined"
                        >
                            {testTypeOptions.map((type) => (
                                <MenuItem key={type.value} value={type.value}>
                                    {type.label}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                    <Box sx={{mt: 2}}>
                        {testType === TestTypes.MANUAL && (
                            <>
                                <Box sx={{fontWeight: "bold", mb: 1}}>Test Steps</Box>
                                {steps.map((step, index) => (
                                    <Box key={index}
                                         sx={{display: "grid", gridTemplateColumns: "1fr 1fr 1fr auto", gap: 2, mb: 2}}>
                                        <TextField
                                            label={`Step ${index + 1}`}
                                            value={step.testStepDesc}
                                            onChange={(e) => handleStepChange(e.target.value, index, "testStepDesc")}
                                            fullWidth
                                            multiline
                                            minRows={2}
                                        />
                                        <TextField
                                            label="Data"
                                            value={step.testStepData}
                                            onChange={(e) => handleStepChange(e.target.value, index, "testStepData")}
                                            fullWidth
                                            multiline
                                            minRows={2}
                                        />
                                        <TextField
                                            label="Expected Output"
                                            value={step.testExpectedOutput}
                                            onChange={(e) => handleStepChange(e.target.value, index, "testExpectedOutput")}
                                            fullWidth
                                            multiline
                                            minRows={2}
                                        />
                                        <IconButton onClick={() => removeStep(index)} color="error">
                                            <DeleteIcon/>
                                        </IconButton>
                                    </Box>
                                ))}
                                <Button onClick={addStep}>+ Add Step</Button>

                            </>
                        )}
                        {testType === TestTypes.CUCUMBER_MANUAL && (
                            <>
                                <Box sx={{display: "grid", gridTemplateColumns: "1fr", gap: 2, mb: 2}}>
                                    {steps.map((step, index) => (
                                        <Box key={index} sx={{display: "grid"}}>
                                            <TextField
                                                key={index}
                                                label="Cucumber Steps"
                                                placeholder="Given I am on the login page..."
                                                value={step.testStepDesc}
                                                onChange={(e) =>
                                                    handleStepChange(e.target.value, index, "testStepDesc")
                                                }
                                                fullWidth
                                                multiline
                                                minRows={3}
                                            />
                                        </Box>
                                    ))}
                                </Box>
                            </>
                        )}
                        {testType === TestTypes.CUCUMBER_AUTOMATION && (
                            <>
                                <Box sx={{display: "grid", gridTemplateColumns: "1fr", gap: 2, mb: 2}}>
                                    {steps.map((step, index) => (
                                        <Box key={index}>
                                            <TextField
                                                label="Automation Script ID or Reference"
                                                placeholder="e.g., test_login.feature:12"
                                                value={step.testStepDesc}
                                                onChange={(e) =>
                                                    handleStepChange(e.target.value, index, "testStepDesc")
                                                }
                                                fullWidth
                                                multiline
                                                minRows={6}
                                            />
                                        </Box>
                                    ))}
                                </Box>
                            </>
                        )}

                        {testType === TestTypes.KEYWORD_DRIVEN && (
                            <>
                                <Box sx={{fontWeight: "bold", mb: 1}}>Keyword Driven</Box>
                                {steps.map((step, index) => (
                                    <Box key={index}
                                         sx={{display: "grid", gridTemplateColumns: "1fr 1fr 1fr auto", gap: 2, mb: 2}}>
                                        <TextField
                                            label={`Action ${index + 1}`}
                                            value={step.testStepDesc}
                                            onChange={(e) => handleStepChange(e.target.value, index, "testStepDesc")}
                                            fullWidth
                                            multiline
                                            minRows={2}
                                        />
                                        <TextField
                                            label="Target"
                                            value={step.testStepData}
                                            onChange={(e) => handleStepChange(e.target.value, index, "testStepData")}
                                            fullWidth
                                            multiline
                                            minRows={2}
                                        />
                                        <TextField
                                            label="Value"
                                            value={step.testExpectedOutput}
                                            onChange={(e) => handleStepChange(e.target.value, index, "testExpectedOutput")}
                                            fullWidth
                                            multiline
                                            minRows={2}
                                        />
                                        <IconButton onClick={() => removeStep(index)} color="error">
                                            <DeleteIcon/>
                                        </IconButton>
                                    </Box>
                                ))}
                                <Button onClick={addStep}>+ Add Step</Button>
                            </>
                        )}
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
