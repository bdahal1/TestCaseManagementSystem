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
    IconButton, Snackbar, Alert,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';

// Project type interface
interface Project {
    id: number;
    projectName: string;
    projectInitials: string;
}

const ProjectComponent: React.FC = () => {
    const [projects, setProjects] = useState<Project[]>([]);
    const [newProjectName, setNewProjectName] = useState<string>('');
    const [newProjectInitials, setNewProjectInitials] = useState<string>('');
    const [editProjectId, setEditProjectId] = useState<number | null>(null);
    const [editProjectName, setEditProjectName] = useState<string>('');
    const [editProjectInitials, setEditProjectInitials] = useState<string>('');
    const [alert, setAlert] = useState({
        open: false,
        message: '',
        severity: 'success', // 'success', 'error', 'warning', 'info'
    });


    const API_URL = 'http://localhost:8080/dhtcms/api/v1/project';

    // Fetch Projects from the API
    const fetchProjects = async () => {
        try {
            const response = await axios.get(API_URL, {headers: {"Authorization": `Bearer ` + localStorage.getItem("authToken")}});
            setProjects(response.data.projects);
        } catch (error) {
            console.error('Error fetching Projects:', error);
        }
    };

    // Add a new Project
    const addProject = async () => {
        if (!newProjectName.trim()) {
            setAlert({open: true, message: 'Project name cannot be empty.', severity: 'error'});
            return;
        }
        try {
            const response = await axios.post(API_URL, {projectName: newProjectName,projectInitials: newProjectInitials}, {headers: {"Authorization": `Bearer ` + localStorage.getItem("authToken")}});
            const newProject: Project = response.data;

            if (newProject && newProject.id && newProject.projectName) {
                setProjects([...projects, newProject]); // Add the new Project to the list
                setNewProjectName('');
                setNewProjectInitials('');
            } else {
                console.error('Invalid Project data received:', response.data);
                setAlert({
                    open: true,
                    message: 'Failed to add Project: Invalid data received from the server.',
                    severity: 'error'
                });
            }
            setAlert({open: true, message: 'Project added successfully!', severity: 'success'});
        } catch (error) {
            console.error('Error adding Project:', error);
            setAlert({open: true, message: 'Error adding Project.', severity: 'error'});
        }
    };

    // Update an existing Project
    const updateProject = async () => {
        if (!editProjectName.trim()) {
            setAlert({open: true, message: 'Project name cannot be empty.', severity: 'error'});
            return;
        }
        try {
            await axios.put(`${API_URL}/${editProjectId}`, {projectName: editProjectName, projectInitials: editProjectInitials}, {headers: {"Authorization": `Bearer ` + localStorage.getItem("authToken")}});
            setProjects(
                projects.map((project) =>
                    project.id === editProjectId ? {...project, projectName: editProjectName, projectInitials: editProjectInitials} : project
                )
            );
            setEditProjectId(null);
            setEditProjectName('');
            setEditProjectInitials('');
            setAlert({open: true, message: 'Project edited successfully!', severity: 'error'});
        } catch (error) {
            console.error('Error updating Project:', error);
            setAlert({open: true, message: 'Error Updating Project.', severity: 'error'});
        }
    };

    // Delete a Project
    const deleteProject = async (projectId: number) => {
        try {
            await axios.delete(`${API_URL}/${projectId}`, {headers: {"Authorization": `Bearer ` + localStorage.getItem("authToken")}});
            setProjects(projects.filter((project) => project.id !== projectId));
            setAlert({open: true, message: 'Project deleted successfully!', severity: 'success'});
        } catch (error) {
            console.error('Error deleting Project:', error);
            setAlert({open: true, message: 'Error deleting Project.', severity: 'error'});
        }
    };

    useEffect(() => {
        fetchProjects().then();
    }, []);

    return (
        <div>
            <h1>Projects Management</h1>

            {/* Add Project Section */}
            <div style={{marginBottom: '20px'}}>
                <TextField
                    label="New Project Name"
                    value={newProjectName}
                    onChange={(e) => setNewProjectName(e.target.value)}
                />
                <TextField
                    label="New Project Initials"
                    value={newProjectInitials}
                    onChange={(e) => setNewProjectInitials(e.target.value)}
                />
                <Button variant="contained" color="primary" onClick={addProject} style={{marginLeft: '10px'}}>
                    Add Project
                </Button>
            </div>

            {/* Edit Project Section */}
            {editProjectId && (
                <div style={{marginBottom: '20px'}}>
                    <TextField
                        label="Edit Project Name"
                        value={editProjectName}
                        onChange={(e) => setEditProjectName(e.target.value)}
                    />
                    <TextField
                        label="Edit Project Initials"
                        value={editProjectInitials}
                        onChange={(e) => setEditProjectInitials(e.target.value)}
                    />
                    <Button
                        variant="contained"
                        color="secondary"
                        onClick={updateProject}
                        style={{marginLeft: '10px'}}
                    >
                        Update Project
                    </Button>
                </div>
            )}

            {/* Projects Table */}
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
                        {Array.isArray(projects) && projects.map((project, index) => (
                            <TableRow key={project.id}>
                                <TableCell>{index + 1}</TableCell>
                                <TableCell>{project.projectName}</TableCell>
                                <TableCell>{project.projectInitials}</TableCell>
                                <TableCell>
                                    <IconButton
                                        color="primary"
                                        onClick={() => {
                                            setEditProjectId(project.id);
                                            setEditProjectName(project.projectName);
                                            setEditProjectInitials(project.projectInitials);
                                        }}
                                    >
                                        <EditIcon/>
                                    </IconButton>
                                    <IconButton
                                        color="secondary"
                                        onClick={() => deleteProject(project.id)}
                                    >
                                        <DeleteIcon/>
                                    </IconButton>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
            {/* Alert Snackbar */}
            <Snackbar
                open={alert.open}
                autoHideDuration={3000}
                onClose={() => setAlert({...alert, open: false})}
                anchorOrigin={{vertical: 'top', horizontal: 'center'}}
            >
                <Alert onClose={() => setAlert({...alert, open: false})} variant="filled">
                    {alert.message}
                </Alert>
            </Snackbar>
        </div>
    );
};

export default ProjectComponent;
