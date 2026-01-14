import React, { useEffect, useState } from 'react';
import {
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Paper,
    TextField,
    Button,
    IconButton,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    Snackbar,
    Alert,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import SearchIcon from '@mui/icons-material/Search';
import AssignmentIcon from '@mui/icons-material/Assignment';
import { InputAdornment } from '@mui/material';
import { Box } from "@mui/system";
import api from "../../services/api";
import { AxiosError } from "axios";

interface Project {
    id: number;
    projectName: string;
    projectInitials: string;
}

const ProjectComponent: React.FC = () => {
    const [projects, setProjects] = useState<Project[]>([]);
    const [filteredProjects, setFilteredProjects] = useState<Project[]>([]);
    const [searchQuery, setSearchQuery] = useState('');
    const [open, setOpen] = useState(false);
    const [isEdit, setIsEdit] = useState(false);
    const [selectedProject, setSelectedProject] = useState<Project | null>(null);
    const [projectName, setProjectName] = useState('');
    const [projectInitials, setProjectInitials] = useState('');
    const [alert, setAlert] = useState<{
        open: boolean;
        message: string;
        severity: 'success' | 'error' | 'warning' | 'info';
    }>({
        open: false,
        message: '',
        severity: 'success',
    });

    const API_URL = '/project';

    const fetchProjects = async () => {
        try {
            const response = await api.get(API_URL);
            setProjects(response.data.projects);
            setFilteredProjects(response.data.projects);
        } catch (error) {
            console.error('Error fetching Projects:', error);
        }
    };

    useEffect(() => {
        fetchProjects().then();
    }, []);

    useEffect(() => {
        const lowerCaseQuery = searchQuery.toLowerCase();
        const filtered = projects.filter(proj =>
            proj.projectName.toLowerCase().includes(lowerCaseQuery) ||
            proj.projectInitials.toLowerCase().includes(lowerCaseQuery)
        );
        setFilteredProjects(filtered);
    }, [searchQuery, projects]);

    const handleOpenAdd = () => {
        setIsEdit(false);
        setSelectedProject(null);
        setProjectName('');
        setProjectInitials('');
        setOpen(true);
    };

    const handleOpenEdit = (project: Project) => {
        setIsEdit(true);
        setSelectedProject(project);
        setProjectName(project.projectName);
        setProjectInitials(project.projectInitials);
        setOpen(true);
    };

    const handleClose = () => {
        setOpen(false);
        setProjectName('');
        setProjectInitials('');
        setSelectedProject(null);
    };

    const handleSubmit = async () => {
        if (!projectName.trim()) {
            setAlert({ open: true, message: 'Project name cannot be empty.', severity: 'error' });
            return;
        }
        try {
            if (isEdit && selectedProject) {
                await api.put(
                    `${API_URL}/${selectedProject.id}`,
                    { projectName, projectInitials }
                );
                setAlert({ open: true, message: 'Project updated successfully!', severity: 'success' });
            } else {
                await api.post(
                    API_URL,
                    { projectName, projectInitials }
                );
                setAlert({ open: true, message: 'Project added successfully!', severity: 'success' });
            }
            fetchProjects();
            handleClose();
        } catch (err) {
            const error = err as AxiosError<any>;
            console.error('Error saving Project:', error);
            if (error.response?.status === 409) {
                setAlert({ open: true, message: 'Project with this name already exists.', severity: 'error' });
            } else {
                setAlert({ open: true, message: 'Error saving Project.', severity: 'error' });
            }

        }
    };

    const deleteProject = async (projectId: number) => {
        try {
            await api.delete(`${API_URL}/${projectId}`);
            setAlert({ open: true, message: 'Project deleted successfully!', severity: 'success' });
            fetchProjects();
        } catch (error) {
            console.error('Error deleting Project:', error);
            setAlert({ open: true, message: 'Error deleting Project.', severity: 'error' });
        }
    };

    return (
        <Box sx={{ padding: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
                <TextField
                    placeholder="Search projects..."
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
                    onClick={handleOpenAdd}
                    startIcon={<AssignmentIcon />}
                    sx={{ borderRadius: '10px', px: 3 }}
                >
                    Add Project
                </Button>
            </Box>

            <TableContainer component={Paper}>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell>Name</TableCell>
                            <TableCell>Initials</TableCell>
                            <TableCell>Actions</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {filteredProjects.map((project) => (
                            <TableRow key={project.id} hover>
                                <TableCell>{project.projectName}</TableCell>
                                <TableCell>{project.projectInitials}</TableCell>
                                <TableCell>
                                    <IconButton size="small" onClick={() => handleOpenEdit(project)}>
                                        <EditIcon fontSize="small" color="primary" />
                                    </IconButton>
                                    <IconButton size="small" onClick={() => deleteProject(project.id)}>
                                        <DeleteIcon fontSize="small" color="error" />
                                    </IconButton>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>

            {/* Add/Edit Project Dialog */}
            <Dialog
                open={open}
                onClose={handleClose}
                fullWidth
                maxWidth="xs"
                PaperProps={{ sx: { borderRadius: '20px', p: 1 } }}
            >
                <DialogTitle sx={{ fontWeight: 700, fontSize: '1.25rem', pb: 1 }}>
                    {isEdit ? 'Edit Project' : 'New Project'}
                </DialogTitle>
                <DialogContent sx={{ p: 3, pt: 1 }}>
                    <Box component="form" sx={{ display: 'flex', flexDirection: 'column', gap: 2.5, mt: 1 }}>
                        <TextField
                            label="Project Name"
                            value={projectName}
                            onChange={(e) => setProjectName(e.target.value)}
                            fullWidth
                            variant="outlined"
                        />
                        <TextField
                            label="Project Initials"
                            value={projectInitials}
                            onChange={(e) => setProjectInitials(e.target.value)}
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

            {/* Alert Snackbar */}
            <Snackbar
                open={alert.open}
                autoHideDuration={3000}
                onClose={() => setAlert({ ...alert, open: false })}
                anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
            >
                <Alert
                    onClose={() => setAlert({ ...alert, open: false })}
                    severity={alert.severity as 'success' | 'error' | 'warning' | 'info'}
                    variant="filled"
                    sx={{ width: '100%', borderRadius: 3 }}
                >
                    {alert.message}
                </Alert>
            </Snackbar>
        </Box>
    );
};

export default ProjectComponent;
