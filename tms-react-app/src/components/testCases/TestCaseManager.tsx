import React, { useCallback, useEffect, useState } from "react";
import {
    Alert,
    Autocomplete,
    Button,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    FormControl,
    IconButton,
    InputLabel,
    Link,
    MenuItem,
    Paper,
    Select,
    Snackbar,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    TextField,
    createFilterOptions
} from "@mui/material";
import { Box } from "@mui/system";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import SearchIcon from '@mui/icons-material/Search';
import AddIcon from '@mui/icons-material/Add';
import { InputAdornment } from '@mui/material';
import { TestTypes } from "../../types/TestCase.ts";
import api from "../../services/api";
import { useAuth } from "../../context/AuthContext";

// Constants
const API_URLS = {
    TESTCASE: "/testCase",
    PROJECT: "/project",
    TAGS: "/tags",
    TEST_STEPS: "/testSteps"
};

const TEST_TYPE_OPTIONS = [
    { label: "Manual", value: TestTypes.MANUAL },
    { label: "Cucumber Manual", value: TestTypes.CUCUMBER_MANUAL },
    { label: "Cucumber Automation", value: TestTypes.CUCUMBER_AUTOMATION },
    { label: "Keyword Driven", value: TestTypes.KEYWORD_DRIVEN }
];

// Interfaces
interface Project {
    id: number;
    projectName: string;
}

const filter = createFilterOptions<TagsSet>();

interface TagsSet {
    id: number | null;
    tagName: string;
    inputValue?: string;
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

const TestCaseComponent: React.FC<TestCaseComponentProps> = ({ projId }) => {
    // State
    const { user } = useAuth();
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
    const [searchQuery, setSearchQuery] = useState('');
    const [filteredTestCases, setFilteredTestCases] = useState<TestCase[]>([]);

    // Form state
    const [formData, setFormData] = useState({
        testName: "",
        testCaseId: 0,
        projectId: 0,
        testType: "" as TestTypes | ""
    });

    const [steps, setSteps] = useState<TestStep[]>([]);

    // Memoized auth headers


    // API Calls
    const fetchTestCases = useCallback(async () => {
        setLoading(true);
        try {
            const response = await api.get(`${API_URLS.TESTCASE}?projectId=${projId}`);
            setTestCases(response.data.testCase ?? []);
            setFilteredTestCases(response.data.testCase ?? []);
            setError(null);
        } catch (err) {
            setError("Failed to fetch test cases.");
            console.error("Error fetching test cases:", err);
        } finally {
            setLoading(false);
        }
    }, [projId]);

    const fetchProjects = useCallback(async () => {
        try {
            const response = await api.get(`${API_URLS.PROJECT}/id/${projId}`);
            setProjects(response.data.projects);
        } catch (err) {
            setError("Failed to fetch projects.");
            console.error("Error fetching projects:", err);
        }
    }, [projId]);

    const fetchTags = useCallback(async () => {
        try {
            const response = await api.get(API_URLS.TAGS);
            setTags(Array.isArray(response.data?.tags) ? response.data.tags : []);
        } catch (err) {
            setError("Failed to fetch tags.");
            console.error("Error fetching tags:", err);
        }
    }, []);

    const syncMissingTags = async (incomingTags: TagsSet[]): Promise<TagsSet[]> => {
        const existingTagNames = new Set(tags.map(tag => tag.tagName.toLowerCase()));
        const createdTags: TagsSet[] = [];

        for (const tag of incomingTags) {
            if (!existingTagNames.has(tag.tagName.toLowerCase())) {
                try {
                    const res = await api.post(API_URLS.TAGS, { tagName: tag.tagName });
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
                const response = await api.get(
                    `${API_URLS.TEST_STEPS}/testCaseId/${testCase.id}?type=${effectiveType}`
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
        setFormData(prev => ({ ...prev, [field]: value }));
    };



    // ...

    // Validators
    const validateGherkinStep = (stepDesc: string): boolean => {
        const gherkinKeywords = ["Given", "When", "Then", "And", "But", "*"];
        const firstWord = stepDesc.trim().split(" ")[0];
        return gherkinKeywords.includes(firstWord);
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

        // Gherkin Validation
        if (formData.testType === TestTypes.CUCUMBER_MANUAL) {
            const invalidSteps = steps.filter(step => !validateGherkinStep(step.testStepDesc));
            if (invalidSteps.length > 0) {
                showSnackbar(`Invalid Gherkin step(s) found. Steps must start with Given, When, Then, And, or But.`);
                return;
            }
        }

        const testPayload = {
            testName: formData.testName,
            projectId: formData.projectId,
            userId: user?.userId,
            selectedTags: selectedTags.map(tag => tag.id),
            testType: formData.testType
        };

        try {
            let currentTestCaseId = formData.testCaseId;

            if (formMode === "edit" && selectedTestCase) {
                await api.put(`${API_URLS.TESTCASE}/${selectedTestCase.id}`, testPayload);
                currentTestCaseId = selectedTestCase.id;
                showSnackbar("Test case updated successfully!");
            } else {
                const response = await api.post(API_URLS.TESTCASE, testPayload);
                currentTestCaseId = response.data.id;
                showSnackbar("Test case created successfully!");
            }

            if (steps.length) {
                const testStepsPayload = steps.map((step, index) => ({
                    testStepDesc: step.testStepDesc.split('\n').map(line => line.trim()).join('\n'),
                    testExpectedOutput: step.testExpectedOutput,
                    testStepData: step.testStepData,
                    testStepOrder: index + 1,
                    testCaseId: currentTestCaseId,
                    userId: user?.userId,
                    stepId: step.id,
                    testType: formData.testType
                }));
                await api.post(API_URLS.TEST_STEPS, testStepsPayload);
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
            await api.delete(`${API_URLS.TESTCASE}/${id}`);
            await fetchTestCases();
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

    useEffect(() => {
        const lowerCaseQuery = searchQuery.toLowerCase();
        const filtered = testCases.filter(tc =>
            tc.testName.toLowerCase().includes(lowerCaseQuery) ||
            tc.testProjectId.toLowerCase().includes(lowerCaseQuery) ||
            tc.tagsSet.some(tag => tag.tagName.toLowerCase().includes(lowerCaseQuery))
        );
        setFilteredTestCases(filtered);
    }, [searchQuery, testCases]);

    // Render
    if (!projId) return <div>Please select a project.</div>;
    if (loading) return <CircularProgress />;
    if (error) return <div>{error}</div>;

    return (
        <Box sx={{ padding: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
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
                    onClick={() => handleOpenDialog(null)}
                    startIcon={<AddIcon />}
                    sx={{ borderRadius: '10px', px: 3 }}
                >
                    Add Test Case
                </Button>
            </Box>

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
                        {filteredTestCases.map((tc) => (
                            <TableRow key={tc.id} hover>
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
                                    <IconButton size="small" onClick={() => handleOpenDialog(tc)}>
                                        <EditIcon fontSize="small" color="primary" />
                                    </IconButton>
                                    <IconButton size="small" onClick={() => deleteTestCase(tc.id)}>
                                        <DeleteIcon fontSize="small" color="error" />
                                    </IconButton>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>

            <Dialog
                open={dialogOpen}
                onClose={handleCloseDialog}
                fullWidth
                maxWidth="md"
                PaperProps={{ sx: { borderRadius: '20px', p: 1 } }}
            >
                <DialogTitle sx={{ fontWeight: 700, fontSize: '1.5rem', pb: 1 }}>
                    {formMode === "create" ? "New Test Case" : "Edit Test Case"}
                </DialogTitle>
                <DialogContent sx={{ p: 3, pt: 1, display: "flex", flexDirection: "column", gap: 3 }}>
                    <Box component="form" sx={{ mt: 1, display: 'flex', flexDirection: 'column', gap: 2 }}>
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
                                slotProps={{ input: { readOnly: true } }}
                                variant="filled"
                            />
                        )}

                        <TextField
                            label="Test Case Title"
                            value={formData.testName}
                            onChange={(e) => handleFormChange("testName", e.target.value)}
                            required
                            fullWidth
                            variant="outlined"
                        />

                        <Autocomplete
                            multiple
                            freeSolo
                            options={tags}
                            renderOption={(props, option) => {
                                const { key, ...otherProps } = props;
                                return (
                                    <li key={key} {...otherProps}>
                                        {option.tagName}
                                    </li>
                                );
                            }}
                            getOptionLabel={(option) => {
                                if (typeof option === "string") return option;
                                if (option.inputValue) return option.inputValue;
                                return option.tagName;
                            }}
                            filterOptions={(options, params) => {
                                const filtered = filter(options, params);
                                const { inputValue } = params;
                                const isExisting = options.some((option) => inputValue.toLowerCase() === option.tagName.toLowerCase());
                                if (inputValue !== "" && !isExisting) {
                                    filtered.push({
                                        inputValue: inputValue,
                                        tagName: `Add "${inputValue}"`,
                                        id: null
                                    } as any);
                                }
                                return filtered;
                            }}
                            value={selectedTags}
                            onChange={async (_, newValue) => {
                                const processedTags = await Promise.all(newValue.map(async (item: any) => {
                                    if (typeof item === 'string') {
                                        // Handle free text input that wasn't selected from "Add..." option
                                        // This creates a tag immediately if user just hits enter on text
                                        try {
                                            const res = await api.post(API_URLS.TAGS, { tagName: item });
                                            // Update local tags list so it shows up next time
                                            setTags(prev => [...prev, res.data]);
                                            return res.data;
                                        } catch (e) {
                                            console.error("Error creating tag:", e);
                                            return { id: null, tagName: item };
                                        }
                                    } else if (item.inputValue) {
                                        // Handle "Add [Value]" selection
                                        try {
                                            const res = await api.post(API_URLS.TAGS, { tagName: item.inputValue });
                                            setTags(prev => [...prev, res.data]);
                                            return res.data;
                                        } catch (e) {
                                            console.error("Error creating tag:", e);
                                            return { id: null, tagName: item.inputValue };
                                        }
                                    } else {
                                        // Existing tag selected
                                        return item;
                                    }
                                }));
                                setSelectedTags(processedTags);
                            }}
                            renderInput={(params) => <TextField {...params} label="Tags" variant="outlined" />}
                            isOptionEqualToValue={(option, value) => {
                                if (option.id && value.id) return option.id === value.id;
                                return option.tagName.toLowerCase() === value.tagName.toLowerCase();
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
                    </Box>

                    <Box sx={{ mt: 1, p: 2, backgroundColor: 'background.default', borderRadius: 3 }}>
                        {formData.testType === TestTypes.MANUAL && (
                            <>
                                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                                    <Box sx={{ fontWeight: "700", color: 'text.secondary' }}>Test Steps</Box>
                                    <Button onClick={addStep} size="small" variant="outlined">+ Add Step</Button>
                                </Box>
                                {steps.map((step, index) => (
                                    <Paper key={index} elevation={0} sx={{
                                        p: 2,
                                        mb: 2,
                                        border: '1px solid',
                                        borderColor: 'divider',
                                        borderRadius: 2,
                                        display: "grid",
                                        gridTemplateColumns: "1fr 1fr 1fr auto",
                                        gap: 2
                                    }}>
                                        <TextField
                                            label={`Step ${index + 1}`}
                                            value={step.testStepDesc}
                                            onChange={(e) => handleStepChange(e.target.value, index, "testStepDesc")}
                                            fullWidth
                                            multiline
                                            minRows={2}
                                            variant="standard"
                                        />
                                        <TextField
                                            label="Data"
                                            value={step.testStepData}
                                            onChange={(e) => handleStepChange(e.target.value, index, "testStepData")}
                                            fullWidth
                                            multiline
                                            minRows={2}
                                            variant="standard"
                                        />
                                        <TextField
                                            label="Expected Output"
                                            value={step.testExpectedOutput}
                                            onChange={(e) => handleStepChange(e.target.value, index, "testExpectedOutput")}
                                            fullWidth
                                            multiline
                                            minRows={2}
                                            variant="standard"
                                        />
                                        <IconButton onClick={() => removeStep(index)} color="error" sx={{ alignSelf: 'start' }}>
                                            <DeleteIcon />
                                        </IconButton>
                                    </Paper>
                                ))}
                                {steps.length === 0 && (
                                    <Box sx={{ textAlign: 'center', py: 4, color: 'text.disabled' }}>
                                        No steps added yet.
                                    </Box>
                                )}
                            </>
                        )}

                        {formData.testType === TestTypes.CUCUMBER_MANUAL && (
                            <>
                                <Box sx={{ display: "grid", gridTemplateColumns: "1fr", gap: 2, mb: 2 }}>
                                    {steps.map((step, index) => (
                                        <Box key={index}>
                                            <TextField
                                                label="Cucumber Gherkin Steps"
                                                placeholder="Given I am on the login page..."
                                                value={step.testStepDesc}
                                                onChange={(e) => handleStepChange(e.target.value, index, "testStepDesc")}
                                                fullWidth
                                                multiline
                                                minRows={3}
                                                variant="outlined"
                                                sx={{ backgroundColor: 'background.paper' }}
                                            />
                                        </Box>
                                    ))}
                                </Box>
                            </>
                        )}

                        {formData.testType === TestTypes.CUCUMBER_AUTOMATION && (
                            <>
                                <Box sx={{ display: "grid", gridTemplateColumns: "1fr", gap: 2, mb: 2 }}>
                                    {steps.map((step, index) => (
                                        <Box key={index}>
                                            <TextField
                                                label="Automation Script ID / Reference"
                                                placeholder="e.g., test_login.feature:12"
                                                value={step.testStepDesc}
                                                onChange={(e) => handleStepChange(e.target.value, index, "testStepDesc")}
                                                fullWidth
                                                multiline
                                                minRows={6}
                                                variant="outlined"
                                                sx={{ backgroundColor: 'background.paper', fontFamily: 'monospace' }}
                                            />
                                        </Box>
                                    ))}
                                </Box>
                            </>
                        )}

                        {formData.testType === TestTypes.KEYWORD_DRIVEN && (
                            <>
                                <Box sx={{ fontWeight: "700", mb: 2, color: 'text.secondary' }}>Keyword Driven Actions</Box>
                                {steps.map((step, index) => (
                                    <Paper key={index} elevation={0} sx={{
                                        p: 2, mb: 2, border: '1px solid', borderColor: 'divider', borderRadius: 2,
                                        display: "grid", gridTemplateColumns: "1fr 1fr 1fr auto", gap: 2
                                    }}>
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
                                            <DeleteIcon />
                                        </IconButton>
                                    </Paper>
                                ))}
                                <Button onClick={addStep} variant="outlined">+ Add Action</Button>
                            </>
                        )}
                    </Box>
                </DialogContent>
                <DialogActions sx={{ p: 3, pt: 1 }}>
                    <Button onClick={handleCloseDialog} variant="text" color="inherit" sx={{ borderRadius: '10px' }}>
                        Cancel
                    </Button>
                    <Button
                        variant="contained"
                        onClick={handleSubmit}
                        color="primary"
                        sx={{ borderRadius: '10px', px: 3 }}
                    >
                        {formMode === "create" ? "Create Case" : "Save Changes"}
                    </Button>
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
