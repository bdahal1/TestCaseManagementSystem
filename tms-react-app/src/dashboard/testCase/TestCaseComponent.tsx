import React, {useEffect, useState, useCallback} from "react";
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
import {TestTypes} from "../../types/TestCase.ts";

// Constants
const API_URLS = {
    TESTCASE: "/dhtcms/api/v1/testCase",
    PROJECT: "/dhtcms/api/v1/project",
    TAGS: "/dhtcms/api/v1/tags",
    TEST_STEPS: "/dhtcms/api/v1/testSteps"
};

const TEST_TYPE_OPTIONS = [
    {label: "Manual", value: TestTypes.MANUAL},
    {label: "Cucumber Manual", value: TestTypes.CUCUMBER_MANUAL},
    {label: "Cucumber Automation", value: TestTypes.CUCUMBER_AUTOMATION},
    {label: "Keyword Driven", value: TestTypes.KEYWORD_DRIVEN}
];

// Interfaces
interface Project {
    id: number;
    projectName: string;
}

interface TagsSet {
    id: number | null;
    tagName: string;
}

interface TestStep {
    id: number;
    testStepDesc: string;
    testStepData: string;
    testExpectedOutput: string;
    testType: TestTypes;
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
    projId?: number;
}

const TestCaseComponent: React.FC<TestCaseComponentProps> = ({projId}) => {
    // State
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
    const [formMode, setFormMode] = useState<"create" | "edit">("create");

    // Form state
    const [formData, setFormData] = useState({
        testName: "",
        testCaseId: 0,
        projectId: 0,
        testType: "" as TestTypes | ""
    });

    const [steps, setSteps] = useState<TestStep[]>([]);

    // Memoized auth headers
    const getAuthHeaders = useCallback(() => ({
        headers: {Authorization: `Bearer ${localStorage.getItem("authToken")}`}
    }), []);

    // API Calls
    const fetchTestCases = useCallback(async () => {
        setLoading(true);
        try {
            const response = await axios.get(`${API_URLS.TESTCASE}?projectId=${projId}`, getAuthHeaders());
            setTestCases(response.data.testCase ?? []);
            setError(null);
        } catch (err) {
            setError("Failed to fetch test cases.");
            console.error("Error fetching test cases:", err);
        } finally {
            setLoading(false);
        }
    }, [projId, getAuthHeaders]);

    const fetchProjects = useCallback(async () => {
        try {
            const response = await axios.get(`${API_URLS.PROJECT}/id/${projId}`, getAuthHeaders());
            setProjects(response.data.projects);
        } catch (err) {
            setError("Failed to fetch projects.");
            console.error("Error fetching projects:", err);
        }
    }, [projId, getAuthHeaders]);

    const fetchTags = useCallback(async () => {
        try {
            const response = await axios.get(API_URLS.TAGS, getAuthHeaders());
            setTags(Array.isArray(response.data?.tags) ? response.data.tags : []);
        } catch (err) {
            setError("Failed to fetch tags.");
            console.error("Error fetching tags:", err);
        }
    }, [getAuthHeaders]);

    const syncMissingTags = async (incomingTags: TagsSet[]): Promise<TagsSet[]> => {
        const existingTagNames = new Set(tags.map(tag => tag.tagName.toLowerCase()));
        const createdTags: TagsSet[] = [];

        for (const tag of incomingTags) {
            if (!existingTagNames.has(tag.tagName.toLowerCase())) {
                try {
                    const res = await axios.post(API_URLS.TAGS, {tagName: tag.tagName}, getAuthHeaders());
                    createdTags.push(res.data);
                } catch (err) {
                    console.error("Error creating tag:", err);
                }
            }
        }

        const updatedTagList = [...tags, ...createdTags];
        const tagMap = new Map<string, TagsSet>();
        updatedTagList.forEach(tag => tagMap.set(tag.tagName.toLowerCase(), tag));
        const deduplicatedTagList = Array.from(tagMap.values());
        setTags(deduplicatedTagList);
        return incomingTags.map(tag => {
            const found = Array.from(tagMap.values())
                .find(t => t.tagName.toLowerCase() === tag.tagName.toLowerCase());
            return found || tag;
        });
    };

    // Handlers
    const handleOpenDialog = async (testCase: TestCase | null, overrideTestType?: TestTypes) => {
        if (testCase) {
            const effectiveType = overrideTestType || testCase.testType;
            setFormMode("edit");
            setSelectedTestCase(testCase);
            setFormData({
                testName: testCase.testName,
                testCaseId: testCase.id,
                projectId: testCase.projects.id,
                testType: effectiveType || ""
            });
            setSelectedTags(testCase.tagsSet);

            try {
                const response = await axios.get(
                    `${API_URLS.TEST_STEPS}/testCaseId/${testCase.id}?type=${effectiveType}`,
                    getAuthHeaders()
                );
                setSteps(response.data.testSteps);
            } catch (err) {
                setError("Failed to fetch steps.");
                console.error("Error fetching steps:", err);
            }
        } else {
            setFormMode("create");
            setSteps([]);
            setFormData({
                testName: formData.testName,
                testCaseId: 0,
                projectId: projects.length === 1 ? projects[0].id : 0,
                testType: overrideTestType || TestTypes.MANUAL
            });
        }
        setDialogOpen(true);
    };

    const handleCloseDialog = () => {
        setDialogOpen(false);
        resetForm();
    };

    const handleFormChange = (field: keyof typeof formData, value: any) => {
        setFormData(prev => ({...prev, [field]: value}));
    };

    const handleSubmit = async (event: React.FormEvent) => {
        event.preventDefault();

        if (!formData.testName.trim()) {
            showSnackbar("Test Name is required.");
            return;
        }

        if (!formData.projectId) {
            showSnackbar("Project is required.");
            return;
        }

        if (!formData.testType) {
            showSnackbar("Test Type is required.");
            return;
        }

        const testPayload = {
            testName: formData.testName,
            projectId: formData.projectId,
            userId: localStorage.getItem("userId"),
            selectedTags: selectedTags.map(tag => tag.id),
            testType: formData.testType
        };

        try {
            let currentTestCaseId = formData.testCaseId;

            if (formMode === "edit" && selectedTestCase) {
                await axios.put(`${API_URLS.TESTCASE}/${selectedTestCase.id}`, testPayload, getAuthHeaders());
                currentTestCaseId = selectedTestCase.id;
                showSnackbar("Test case updated successfully!");
            } else {
                const response = await axios.post(API_URLS.TESTCASE, testPayload, getAuthHeaders());
                currentTestCaseId = response.data.id;
                showSnackbar("Test case created successfully!");
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
                    testType: formData.testType
                }));
                await axios.post(API_URLS.TEST_STEPS, testStepsPayload, getAuthHeaders());
            }

            await fetchTestCases();
        } catch (err) {
            showSnackbar("Failed to save test case or steps.");
            console.error("Error saving test case:", err);
        } finally {
            handleCloseDialog();
        }
    };

    const deleteTestCase = async (id: number) => {
        try {
            await axios.delete(`${API_URLS.TESTCASE}/${id}`, getAuthHeaders());
            setTestCases(testCases.filter(tc => tc.id !== id));
            showSnackbar("Test case deleted successfully!");
        } catch (err) {
            showSnackbar("Failed to delete test case.");
            console.error("Error deleting test case:", err);
        }
    };

    const resetForm = () => {
        setFormData({
            testName: "",
            testCaseId: 0,
            projectId: projects.length === 1 ? projects[0].id : 0,
            testType: ""
        });
        setSelectedTags([]);
        setSteps([]);
        setSelectedTestCase(null);
    };

    const showSnackbar = (message: string) => {
        setSnackbarMessage(message);
        setSnackbarOpen(true);
    };

    // Step handlers
    const addStep = () => {
        setSteps([...steps, {
            id: 0,
            testStepDesc: "",
            testStepData: "",
            testExpectedOutput: "",
            testType: TestTypes.MANUAL
        }]);
    };

    const removeStep = (index: number) => {
        setSteps(steps.filter((_, i) => i !== index));
    };

    const handleStepChange = (
        value: string,
        index: number,
        field: "testStepDesc" | "testStepData" | "testExpectedOutput"
    ) => {
        const updatedSteps = [...steps];
        updatedSteps[index][field] = value;
        setSteps(updatedSteps);
    };

    // Effects
    useEffect(() => {
        if (projId) {
            Promise.all([fetchTestCases(), fetchProjects(), fetchTags()])
                .catch(err => console.error("Initialization error:", err));
        }
    }, [projId, fetchTestCases, fetchProjects, fetchTags]);

    useEffect(() => {
        if (projects.length === 1) {
            handleFormChange("projectId", projects[0].id);
        }
    }, [projects]);

    useEffect(() => {
        if ((formData.testType === TestTypes.CUCUMBER_MANUAL ||
                formData.testType === TestTypes.CUCUMBER_AUTOMATION) &&
            steps.length === 0) {
            addStep();
        }
    }, [steps]);

    // Render
    if (!projId) return <div>Please select a project.</div>;
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
                                <TableCell>
                                    <Link component="button" onClick={() => handleOpenDialog(tc)}>
                                        {tc.testProjectId}
                                    </Link>
                                </TableCell>
                                <TableCell>
                                    <Link component="button" onClick={() => handleOpenDialog(tc)}>
                                        {tc.testName}
                                    </Link>
                                </TableCell>
                                <TableCell>{tc.projects.projectName}</TableCell>
                                <TableCell>{tc.tagsSet.map(t => t.tagName).join(", ")}</TableCell>
                                <TableCell>{tc.testCreatedBy}</TableCell>
                                <TableCell>
                                    <IconButton color="primary" onClick={() => handleOpenDialog(tc)}>
                                        <EditIcon/>
                                    </IconButton>
                                    <IconButton color="secondary" onClick={() => deleteTestCase(tc.id)}>
                                        <DeleteIcon/>
                                    </IconButton>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>

            <Dialog open={dialogOpen} onClose={handleCloseDialog} fullWidth maxWidth="md">
                <DialogTitle>{formMode === "create" ? "Add" : "Edit"} Test Case</DialogTitle>
                <DialogContent dividers sx={{display: "flex", flexDirection: "column", gap: 2, p: 3}}>
                    {projects.length > 1 ? (
                        <FormControl fullWidth>
                            <InputLabel>Project</InputLabel>
                            <Select
                                required
                                value={formData.projectId}
                                onChange={(e) => handleFormChange("projectId", Number(e.target.value))}
                                label="Project" variant="outlined"
                            >
                                <MenuItem value={0} disabled>Select a project</MenuItem>
                                {projects.map((project) => (
                                    <MenuItem key={project.id} value={project.id}>{project.projectName}</MenuItem>
                                ))}
                            </Select>
                        </FormControl>
                    ) : (
                        <TextField
                            label="Project"
                            value={projects[0]?.projectName || ""}
                            fullWidth
                            slotProps={{input: {readOnly: true}}}
                        />
                    )}

                    <TextField
                        label="Test Case Title"
                        value={formData.testName}
                        onChange={(e) => handleFormChange("testName", e.target.value)}
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
                        isOptionEqualToValue={(option, value) => {
                            // Compare tags by ID if available, otherwise by name (case insensitive)
                            if (option.id && value.id) {
                                return option.id === value.id;
                            }
                            return option.tagName.toLowerCase() === value.tagName.toLowerCase();
                        }}
                        filterOptions={(options, params) => {
                            const filtered = options.filter(option =>
                                option.tagName.toLowerCase().includes(params.inputValue.toLowerCase())
                            );

                            // Show the newly entered tag that's not in options yet
                            if (params.inputValue !== '' &&
                                !filtered.some(option =>
                                    option.tagName.toLowerCase() === params.inputValue.toLowerCase())
                            ) {
                                filtered.push({
                                    id: null,
                                    tagName: params.inputValue
                                });
                            }

                            return filtered;
                        }}
                    />

                    <FormControl fullWidth required>
                        <InputLabel>Test Type</InputLabel>
                        <Select value={formData.testType} onChange={(e) => {
                            const selectedValue = e.target.value as TestTypes;
                            handleFormChange("testType", selectedValue);
                            handleOpenDialog(selectedTestCase, selectedValue).then();
                        }} label="Test Type" variant="outlined">
                            {TEST_TYPE_OPTIONS.map((type) => (
                                <MenuItem key={type.value} value={type.value}>
                                    {type.label}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>

                    <Box sx={{mt: 2}}>
                        {formData.testType === TestTypes.MANUAL && (
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

                        {formData.testType === TestTypes.CUCUMBER_MANUAL && (
                            <>
                                <Box sx={{display: "grid", gridTemplateColumns: "1fr", gap: 2, mb: 2}}>
                                    {steps.map((step, index) => (
                                        <Box key={index}>
                                            <TextField
                                                label="Cucumber Steps"
                                                placeholder="Given I am on the login page..."
                                                value={step.testStepDesc}
                                                onChange={(e) => handleStepChange(e.target.value, index, "testStepDesc")}
                                                fullWidth
                                                multiline
                                                minRows={3}
                                            />
                                        </Box>
                                    ))}
                                </Box>
                            </>
                        )}

                        {formData.testType === TestTypes.CUCUMBER_AUTOMATION && (
                            <>
                                <Box sx={{display: "grid", gridTemplateColumns: "1fr", gap: 2, mb: 2}}>
                                    {steps.map((step, index) => (
                                        <Box key={index}>
                                            <TextField
                                                label="Automation Script ID or Reference"
                                                placeholder="e.g., test_login.feature:12"
                                                value={step.testStepDesc}
                                                onChange={(e) => handleStepChange(e.target.value, index, "testStepDesc")}
                                                fullWidth
                                                multiline
                                                minRows={6}
                                            />
                                        </Box>
                                    ))}
                                </Box>
                            </>
                        )}

                        {formData.testType === TestTypes.KEYWORD_DRIVEN && (
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
                <Alert onClose={() => setSnackbarOpen(false)} severity={snackbarMessage.includes("success") ? "success" : "error"}>
                    {snackbarMessage}
                </Alert>
            </Snackbar>
        </Box>
    );
};

export default TestCaseComponent;
