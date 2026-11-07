import React, { lazy, Suspense, useCallback, useEffect, useRef, useState } from "react";
import {
    Alert,
    Autocomplete,
    Button,
    CircularProgress,
    Chip,
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
import { errorMessage } from "../testPlans/types";
import type { Person } from "../testPlans/types";
import CaseDetailsFields from './CaseDetailsFields';
import { detailsDraft, detailsPayload, detailLabel, priorityColor, priorities, type CaseDetails } from './caseDetails';
import AutomationStepBuilder from "../automation/AutomationStepBuilder";
import ProjectLocatorsDialog from "../automation/ProjectLocatorsDialog";
import AutomationImportDialog from "../automation/AutomationImportDialog";
import DownloadIcon from "@mui/icons-material/Download";
import FileUploadIcon from "@mui/icons-material/FileUpload";
import SettingsSuggestIcon from "@mui/icons-material/SettingsSuggest";
import type { AutomationStepModel } from "../automation/types";

const GherkinEditor = lazy(() => import("../gherkin/GherkinEditor"));

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
    testStepOrder?: number;
}

interface TestCase extends CaseDetails {
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
    const [caseDetails, setCaseDetails] = useState(() => detailsDraft());
    const [owners, setOwners] = useState<Person[]>([]);
    const [ownersError, setOwnersError] = useState('');
    const [priorityFilter, setPriorityFilter] = useState('all');

    // Form state
    const [formData, setFormData] = useState({
        testName: "",
        testCaseId: 0,
        projectId: 0,
        testType: "" as TestTypes | ""
    });

    const [steps, setSteps] = useState<TestStep[]>([]);
    const [gherkin, setGherkin] = useState('');
    const [stepSuggestions, setStepSuggestions] = useState<string[]>([]);
    const [saving, setSaving] = useState(false);
    const [saveError, setSaveError] = useState('');
    const [stepsLoading, setStepsLoading] = useState(false);
    const drafts = useRef<Partial<Record<TestTypes, { steps: TestStep[]; gherkin: string }>>>({});
    const [locatorsDialogOpen, setLocatorsDialogOpen] = useState(false);
    const [importDialogOpen, setImportDialogOpen] = useState(false);

    const getAutomationStepModels = (stepsList: TestStep[]): AutomationStepModel[] => {
        return stepsList.map((s, idx) => {
            if (s.testStepData) {
                try {
                    const parsed = JSON.parse(s.testStepData);
                    if (parsed && parsed.template) {
                        return { ...parsed, order: idx + 1 };
                    }
                } catch (ignored) {}
            }
            return {
                order: idx + 1,
                keyword: idx === 0 ? 'Given' : idx === 1 ? 'When' : 'Then',
                template: 'OPEN_PAGE',
                locatorValue: '',
                textValue: s.testStepDesc || '',
                stateValue: 'VISIBLE'
            };
        });
    };

    const formatStepDescription = (m: AutomationStepModel, isCucumber: boolean): string => {
        const kw = m.keyword || 'When';
        const loc = m.locatorValue || '';
        const val = m.textValue || '';
        const state = (m.stateValue || 'VISIBLE').toLowerCase();

        if (isCucumber) {
            switch (m.template) {
                case 'OPEN_PAGE': return `${kw} open page "${val}"`;
                case 'ENTER_TEXT': return `${kw} enter text "${val}" into "${loc}"`;
                case 'CLICK_ELEMENT': return `${kw} click element "${loc}"`;
                case 'VERIFY_TEXT_EQUALS': return `${kw} verify text of "${loc}" equals "${val}"`;
                case 'VERIFY_TEXT_CONTAINS': return `${kw} verify text of "${loc}" contains "${val}"`;
                case 'VERIFY_ELEMENT_STATE': return `${kw} verify element "${loc}" is ${state}`;
            }
        } else {
            switch (m.template) {
                case 'OPEN_PAGE': return `Open page "${val}"`;
                case 'ENTER_TEXT': return `Enter text "${val}" into "${loc}"`;
                case 'CLICK_ELEMENT': return `Click element "${loc}"`;
                case 'VERIFY_TEXT_EQUALS': return `Verify text of "${loc}" equals "${val}"`;
                case 'VERIFY_TEXT_CONTAINS': return `Verify text of "${loc}" contains "${val}"`;
                case 'VERIFY_ELEMENT_STATE': return `Verify element "${loc}" is ${state}`;
            }
        }
        return '';
    };

    const handleAutomationStepsChange = (models: AutomationStepModel[]) => {
        const isCucumber = formData.testType === TestTypes.CUCUMBER_AUTOMATION;
        const nextSteps: TestStep[] = models.map((m, idx) => ({
            id: steps[idx]?.id || 0,
            testStepOrder: idx + 1,
            testType: formData.testType as TestTypes,
            testStepData: JSON.stringify(m),
            testStepDesc: formatStepDescription(m, isCucumber),
            testExpectedOutput: m.template === 'VERIFY_TEXT_EQUALS' || m.template === 'VERIFY_TEXT_CONTAINS' ? (m.textValue || '') : (m.stateValue || '')
        }));
        setSteps(nextSteps);
    };

    const handleDownloadAutomationProject = async () => {
        try {
            const runnableCases = testCases.filter(tc =>
                tc.testType === TestTypes.CUCUMBER_AUTOMATION || tc.testType === TestTypes.KEYWORD_DRIVEN
            );
            if (runnableCases.length === 0) {
                showSnackbar("No Cucumber Automation or Keyword Driven cases found to export.");
                return;
            }
            const caseIds = runnableCases.map(c => c.id);
            const response = await api.post(`/automation/projects/${projId}/export`, { caseIds }, { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/zip' }));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `automation-project-${projId}.zip`);
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
            showSnackbar(`Downloaded standalone automation project (${caseIds.length} cases).`);
        } catch (err: any) {
            const msg = err?.response?.data?.message || 'Failed to download automation project';
            showSnackbar(msg);
        }
    };

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

    // Handlers
    const handleOpenDialog = async (testCase: TestCase | null, overrideTestType?: TestTypes) => {
        setSaveError('');
        drafts.current = {};
        if (testCase) {
            setCaseDetails(detailsDraft(testCase));
            const effectiveType = overrideTestType || testCase.testType || TestTypes.MANUAL;
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
                const loaded: TestStep[] = response.data.testSteps ?? [];
                setSteps(loaded);
                setGherkin([...loaded].sort((a, b) => (a.testStepOrder ?? 0) - (b.testStepOrder ?? 0)).map(step => step.testStepDesc).join('\n'));
            } catch (err) {
                setSaveError("Could not load this test definition. Close and reopen it to retry.");
                console.error("Error fetching steps:", err);
            }
        } else {
            setCaseDetails(detailsDraft());
            setFormMode("create");
            setSteps([]);
            setGherkin('');
            setSelectedTags([]);
            setFormData({
                testName: formData.testName,
                testCaseId: 0,
                projectId: projects.length === 1 ? projects[0].id : 0,
                testType: overrideTestType || TestTypes.MANUAL
            });
        }
        setDialogOpen(true);
    };

    const handleTypeChange = async (next: TestTypes) => {
        if (formData.testType) drafts.current[formData.testType] = { steps, gherkin };
        handleFormChange('testType', next); setSaveError('');
        const cached = drafts.current[next];
        if (cached) { setSteps(cached.steps); setGherkin(cached.gherkin); return; }
        setSteps([]); setGherkin('');
        if (!selectedTestCase) return;
        setStepsLoading(true);
        try {
            const { data } = await api.get(`${API_URLS.TEST_STEPS}/testCaseId/${selectedTestCase.id}?type=${next}`);
            const loaded: TestStep[] = data.testSteps ?? [];
            setSteps(loaded); setGherkin([...loaded].sort((a, b) => (a.testStepOrder ?? 0) - (b.testStepOrder ?? 0)).map(step => step.testStepDesc).join('\n'));
        } catch (err) { setSaveError(errorMessage(err, 'Could not load this test definition. Switch back and retry.')); }
        finally { setStepsLoading(false); }
    };

    const handleCloseDialog = () => {
        setDialogOpen(false);
        resetForm();
    };

    const handleFormChange = (field: keyof typeof formData, value: any) => {
        setFormData(prev => ({ ...prev, [field]: value }));
    };



    // ...

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

        if (saving || stepsLoading) return;
        if (formData.testType === TestTypes.CUCUMBER_MANUAL) {
            const { validateGherkin } = await import('../gherkin/gherkinValidation');
            const issue = validateGherkin(gherkin, formData.testName).issues[0];
            if (issue) { setSaveError(`Line ${issue.line}: ${issue.message}`); return; }
        }

        let extraDetails;
        try { extraDetails = detailsPayload(caseDetails); }
        catch (error) { setSaveError((error as Error).message); return; }
        const testPayload = {
            ...extraDetails,
            testName: formData.testName,
            projectId: formData.projectId,
            userId: user?.userId,
            selectedTags: selectedTags.map(tag => tag.id),
            testType: formData.testType,
            ...(formData.testType === TestTypes.CUCUMBER_MANUAL ? { gherkin } : {})
        };

        setSaving(true); setSaveError('');
        try {
            let currentTestCaseId = formData.testCaseId;

            if (formMode === "edit" && selectedTestCase) {
                await api.put(`${API_URLS.TESTCASE}/${selectedTestCase.id}`, testPayload);
                currentTestCaseId = selectedTestCase.id;
            } else {
                const response = await api.post(API_URLS.TESTCASE, testPayload);
                currentTestCaseId = response.data.id;
                setFormData(previous => ({ ...previous, testCaseId: currentTestCaseId }));
                setSelectedTestCase(response.data);
                setFormMode('edit');
            }

            if (steps.length && formData.testType !== TestTypes.CUCUMBER_MANUAL) {
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
            showSnackbar('Test case saved successfully!');
            handleCloseDialog();
        } catch (err) {
            setSaveError(errorMessage(err, 'Could not save the test case. Your draft is still here.'));
            console.error("Error saving test case:", err);
        } finally {
            setSaving(false);
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
        setGherkin(''); setSaveError(''); drafts.current = {};
        setCaseDetails(detailsDraft());
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

    useEffect(() => {
        if (!formData.projectId) return;
        let active = true;
        setOwners([]); setOwnersError('');
        api.get(`/testPlans/project/${formData.projectId}/testers`).then(({ data }) => { if (active) setOwners(Array.isArray(data) ? data : []); })
            .catch(() => { if (active) setOwnersError('Could not load project owners. Reopen the case to retry.'); });
        return () => { active = false; };
    }, [formData.projectId]);

    useEffect(() => {
        if (!dialogOpen || formData.testType !== TestTypes.CUCUMBER_MANUAL || !formData.projectId) return;
        let active = true;
        setStepSuggestions([]);
        api.get('/testCase/gherkin-suggestions', { params: { projectId: formData.projectId } })
            .then(({ data }) => { if (active) setStepSuggestions(Array.isArray(data) ? data : []); })
            .catch(() => { /* Keyword suggestions remain available if project suggestions cannot load. */ });
        return () => { active = false; };
    }, [dialogOpen, formData.testType, formData.projectId]);

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
        if (formData.testType === TestTypes.CUCUMBER_AUTOMATION &&
            steps.length === 0) {
            addStep();
        }
    }, [steps]);

    useEffect(() => {
        const lowerCaseQuery = searchQuery.toLowerCase();
        const filtered = testCases.filter(tc =>
            (priorityFilter === 'all' || (tc.priority ?? 'MEDIUM') === priorityFilter) && (
            tc.testName.toLowerCase().includes(lowerCaseQuery) ||
            (tc.component ?? '').toLowerCase().includes(lowerCaseQuery) ||
            tc.testProjectId.toLowerCase().includes(lowerCaseQuery) ||
            (tc.tagsSet ?? []).some(tag => tag.tagName.toLowerCase().includes(lowerCaseQuery)))
        );
        setFilteredTestCases(filtered);
    }, [searchQuery, testCases, priorityFilter]);

    // Render
    if (!projId) return <div>Please select a project.</div>;
    if (loading) return <CircularProgress />;
    if (error) return <div>{error}</div>;

    return (
        <Box sx={{ padding: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 2, flexWrap: 'wrap', mb: 3 }}>
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
                <TextField select size="small" label="Filter by priority" value={priorityFilter} onChange={e => setPriorityFilter(e.target.value)} sx={{ minWidth: 160 }}><MenuItem value="all">All priorities</MenuItem>{priorities.map(p => <MenuItem key={p} value={p}>{detailLabel(p)}</MenuItem>)}</TextField>
                <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                    <Button
                        variant="outlined"
                        color="secondary"
                        onClick={() => setLocatorsDialogOpen(true)}
                        startIcon={<SettingsSuggestIcon />}
                        sx={{ borderRadius: '10px' }}
                    >
                        Manage Locators
                    </Button>
                    <Button
                        variant="outlined"
                        color="primary"
                        onClick={handleDownloadAutomationProject}
                        startIcon={<DownloadIcon />}
                        sx={{ borderRadius: '10px' }}
                    >
                        Export Automation
                    </Button>
                    <Button
                        variant="outlined"
                        color="primary"
                        onClick={() => setImportDialogOpen(true)}
                        startIcon={<FileUploadIcon />}
                        sx={{ borderRadius: '10px' }}
                        id="import-automation-btn"
                    >
                        Import Automation
                    </Button>
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
            </Box>

            <TableContainer component={Paper}>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell>Test-ID</TableCell>
                            <TableCell>Name</TableCell>
                            <TableCell>Project</TableCell>
                            <TableCell>Priority / Component</TableCell>
                            <TableCell>Automation</TableCell>
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
                                <TableCell><Chip size="small" label={detailLabel(tc.priority ?? 'MEDIUM')} color={priorityColor(tc.priority ?? 'MEDIUM')} variant="outlined" />{tc.component && <Box sx={{ mt: 0.5, fontSize: 12, color: 'text.secondary' }}>{tc.component}</Box>}</TableCell>
                                <TableCell>{detailLabel(tc.automationStatus ?? 'NOT_AUTOMATED')}</TableCell>
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
                onClose={saving ? undefined : handleCloseDialog}
                fullWidth
                maxWidth={formData.testType === TestTypes.CUCUMBER_MANUAL ? "lg" : "md"}
                PaperProps={{ sx: { borderRadius: '20px', p: 1 } }}
            >
                <DialogTitle sx={{ fontWeight: 700, fontSize: '1.5rem', pb: 1 }}>
                    {formMode === "create" ? "New Test Case" : "Edit Test Case"}
                </DialogTitle>
                <DialogContent sx={{ p: 3, pt: 1, display: "flex", flexDirection: "column", gap: 3 }}>
                    {saveError && <Alert severity="error">{saveError}</Alert>}
                    <Box component="form" sx={{ mt: 1, display: 'grid', gridTemplateColumns: formData.testType === TestTypes.CUCUMBER_MANUAL ? { xs: '1fr', sm: '1fr 1fr' } : '1fr', gap: 2 }}>
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
                            <InputLabel id="test-case-type-label">Test Type</InputLabel>
                            <Select labelId="test-case-type-label" id="test-case-type" value={formData.testType} disabled={saving || stepsLoading} onChange={(e) => void handleTypeChange(e.target.value as TestTypes)} label="Test Type" variant="outlined">
                                {TEST_TYPE_OPTIONS.map((type) => (
                                    <MenuItem key={type.value} value={type.value}>
                                        {type.label}
                                    </MenuItem>
                                ))}
                            </Select>
                        </FormControl>
                    </Box>

                    <CaseDetailsFields value={caseDetails} onChange={setCaseDetails} owners={owners} ownersError={ownersError} disabled={saving} />

                    <Box sx={{ mt: 1, p: formData.testType === TestTypes.CUCUMBER_MANUAL ? 0 : 2, backgroundColor: 'background.default', borderRadius: 3 }}>
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
                            <Suspense fallback={<CircularProgress aria-label="Loading Gherkin editor" />}>
                                {stepsLoading ? <CircularProgress aria-label="Loading scenario" /> : <GherkinEditor value={gherkin} onChange={setGherkin} title={formData.testName} suggestions={stepSuggestions} disabled={saving} />}
                            </Suspense>
                        )}

                        {(formData.testType === TestTypes.CUCUMBER_AUTOMATION || formData.testType === TestTypes.KEYWORD_DRIVEN) && (
                            <AutomationStepBuilder
                                mode={formData.testType}
                                steps={getAutomationStepModels(steps)}
                                projectId={formData.projectId}
                                scenarioTitle={formData.testName}
                                onChange={handleAutomationStepsChange}
                                disabled={saving || stepsLoading}
                            />
                        )}
                    </Box>
                </DialogContent>
                <DialogActions sx={{ p: 3, pt: 1 }}>
                    <Button disabled={saving} onClick={handleCloseDialog} variant="text" color="inherit" sx={{ borderRadius: '10px' }}>
                        Cancel
                    </Button>
                    <Button
                        variant="contained"
                        onClick={handleSubmit}
                        disabled={saving || stepsLoading}
                        color="primary"
                        sx={{ borderRadius: '10px', px: 3 }}
                    >
                        {saving ? "Saving…" : formMode === "create" ? "Create Case" : "Save Changes"}
                    </Button>
                </DialogActions>
            </Dialog>

            <Snackbar open={snackbarOpen} autoHideDuration={6000} onClose={() => setSnackbarOpen(false)}>
                <Alert onClose={() => setSnackbarOpen(false)}
                    severity={snackbarMessage.includes("success") ? "success" : "error"}>
                    {snackbarMessage}
                </Alert>
            </Snackbar>

            <ProjectLocatorsDialog
                open={locatorsDialogOpen}
                projectId={Number(projId)}
                onClose={() => setLocatorsDialogOpen(false)}
            />

            <AutomationImportDialog
                open={importDialogOpen}
                projectId={Number(projId)}
                onClose={() => setImportDialogOpen(false)}
                onImportSuccess={() => {
                    fetchTestCases();
                    showSnackbar("Automation test cases imported successfully!");
                }}
            />
        </Box>
    );
};

export default TestCaseComponent;
