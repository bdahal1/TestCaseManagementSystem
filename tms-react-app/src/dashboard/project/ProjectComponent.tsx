import React, {useEffect, useState} from 'react';
import axios from 'axios';
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

interface Project {
    id: number;
    projectName: string;
    projectInitials: string;
}

const ProjectComponent: React.FC = () => {
    const [projects, setProjects] = useState<Project[]>([]);
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

    const API_URL = 'http://localhost:8080/dhtcms/api/v1/project';

    const fetchProjects = async () => {
        try {
            const response = await axios.get(API_URL, {
                headers: {Authorization: `Bearer ` + localStorage.getItem('authToken')},
            });
            setProjects(response.data.projects);
        } catch (error) {
            console.error('Error fetching Projects:', error);
        }
    };

    useEffect(() => {
        fetchProjects().then();
    }, []);

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
            setAlert({open: true, message: 'Project name cannot be empty.', severity: 'error'});
            return;
        }
        try {
            if (isEdit && selectedProject) {
                await axios.put(
                    `${API_URL}/${selectedProject.id}`,
                    {projectName, projectInitials},
                    {headers: {Authorization: `Bearer ` + localStorage.getItem('authToken')}}
                );
                setProjects(
                    projects.map((proj) =>
                        proj.id === selectedProject.id ? {...proj, projectName, projectInitials} : proj
                    )
                );
                setAlert({open: true, message: 'Project updated successfully!', severity: 'success'});
            } else {
                const response = await axios.post(
                    API_URL,
                    {projectName, projectInitials},
                    {headers: {Authorization: `Bearer ` + localStorage.getItem('authToken')}}
                );
                const newProject: Project = response.data;
                setProjects([...projects, newProject]);
                setAlert({open: true, message: 'Project added successfully!', severity: 'success'});
            }
            handleClose();
        } catch (error) {
            console.error('Error saving Project:', error);
            setAlert({open: true, message: 'Error saving Project.', severity: 'error'});
        }
    };

    const deleteProject = async (projectId: number) => {
        try {
            await axios.delete(`${API_URL}/${projectId}`, {
                headers: {Authorization: `Bearer ` + localStorage.getItem('authToken')},
            });
            setProjects(projects.filter((proj) => proj.id !== projectId));
            setAlert({open: true, message: 'Project deleted successfully!', severity: 'success'});
        } catch (error) {
            console.error('Error deleting Project:', error);
            setAlert({open: true, message: 'Error deleting Project.', severity: 'error'});
        }
    };

    return (
        <div>
            <h1>Projects Management</h1>

            <Button variant="contained" color="primary" onClick={handleOpenAdd} sx={{mb: 2}}>
                Add Project
            </Button>

            <TableContainer component={Paper}>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell>S.N.</TableCell>
                            <TableCell>Name</TableCell>
                            <TableCell>Initials</TableCell>
                            <TableCell>Actions</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {projects.map((project, index) => (
                            <TableRow key={project.id}>
                                <TableCell>{index + 1}</TableCell>
                                <TableCell>{project.projectName}</TableCell>
                                <TableCell>{project.projectInitials}</TableCell>
                                <TableCell>
                                    <IconButton color="primary" onClick={() => handleOpenEdit(project)}>
                                        <EditIcon/>
                                    </IconButton>
                                    <IconButton color="secondary" onClick={() => deleteProject(project.id)}>
                                        <DeleteIcon/>
                                    </IconButton>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>

            {/* Add/Edit Project Dialog */}
            <Dialog open={open} onClose={handleClose} fullWidth maxWidth="sm">
                <DialogTitle sx={{bgcolor: '#1976d2', color: '#fff', py: 2}}>
                    {isEdit ? 'Edit Project' : 'Add Project'}
                </DialogTitle>
                <DialogContent dividers sx={{p: 3, display: 'flex', flexDirection: 'column', gap: 2}}>
                    <TextField
                        label="Project Name"
                        value={projectName}
                        onChange={(e) => setProjectName(e.target.value)}
                        fullWidth
                    />
                    <TextField
                        label="Project Initials"
                        value={projectInitials}
                        onChange={(e) => setProjectInitials(e.target.value)}
                        fullWidth
                    />
                </DialogContent>
                <DialogActions sx={{p: 2}}>
                    <Button onClick={handleClose} variant="outlined">
                        Cancel
                    </Button>
                    <Button onClick={handleSubmit} variant="contained" color="primary">
                        {isEdit ? 'Update' : 'Add'}
                    </Button>
                </DialogActions>
            </Dialog>

            {/* Alert Snackbar */}
            <Snackbar
                open={alert.open}
                autoHideDuration={3000}
                onClose={() => setAlert({...alert, open: false})}
                anchorOrigin={{vertical: 'top', horizontal: 'center'}}
            >
                <Alert
                    onClose={() => setAlert({...alert, open: false})}
                    severity={alert.severity as 'success' | 'error' | 'warning' | 'info'}
                    variant="filled"
                >
                    {alert.message}
                </Alert>
            </Snackbar>
        </div>
    );
};

export default ProjectComponent;
