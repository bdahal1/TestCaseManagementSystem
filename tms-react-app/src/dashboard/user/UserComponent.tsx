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
    Switch,
    MenuItem,
    Select,
    FormControl,
    InputLabel, Dialog, DialogTitle, DialogContent, DialogActions, SelectChangeEvent, Alert, Snackbar,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import Box from "@mui/material/Box";

// User type interface
interface User {
    id: number;
    firstName: string;
    lastName: string;
    userName: string;
    password: string;
    isActive: boolean;
    roleSet: { roleId: number; roleName: string }[];
    projectsSet: { id: number; projectName: string }[];
    department: { depId: number; depName: string };
}

interface Role {
    roleId: number;
    roleName: string;
}

interface Department {
    depId: number;
    depName: string;
}

interface Project {
    id: number;
    projectName: string;
}

const API_URL_BASE = "http://localhost:8080/dhtcms/api/v1";
const UserComponent: React.FC = () => {
    const [users, setUsers] = useState<User[]>([]);
    const [open, setOpen] = useState(false);
    const [isEdit, setIsEdit] = useState(false);
    const [selectedUser, setSelectedUser] = useState<User | null>(null);
    const [formData, setFormData] = useState<{
        firstName: string;
        lastName: string;
        userName: string;
        password: string;
        isActive: boolean;
        roleId: string;
        departmentId: string;
        projectsSet: Project[];
    }>({
        firstName: '',
        lastName: '',
        userName: '',
        password: '',
        isActive: true,
        roleId: '',
        departmentId: '',
        projectsSet: [],
    });
    const [roles, setRoles] = useState<Role[]>([]);
    const [departments, setDepartments] = useState<Department[]>([]);
    const [projectSet, setProjectSet] = useState<Project[]>([]);
    const [alert, setAlert] = useState<{
        open: boolean;
        message: string;
        severity: 'success' | 'error' | 'warning' | 'info';
    }>({
        open: false,
        message: '',
        severity: 'success',
    });
    const loggedInUserId = Number(localStorage.getItem('userId'));
    const isEditingLoggedInUser = selectedUser?.id === loggedInUserId;

    // Fetch users from the API
    const fetchUsers = async () => {
        try {
            const response = await axios.get(`${API_URL_BASE}/users`, {
                headers: {Authorization: `Bearer ` + localStorage.getItem('authToken')},
            });
            setUsers(response.data.users);
        } catch (error) {
            console.error('Error fetching users:', error);
        }
    };

    const fetchRolesAndDepartments = async () => {
        try {
            const rolesResponse = await axios.get(`${API_URL_BASE}/roles`, {
                headers: {Authorization: `Bearer ` + localStorage.getItem('authToken')},
            });
            setRoles(rolesResponse.data.roles);
        } catch (error) {
            console.error("Failed to fetch roles:", error);
        }

        try {
            const departmentsResponse = await axios.get(`${API_URL_BASE}/department`, {
                headers: {Authorization: `Bearer ` + localStorage.getItem('authToken')},
            });
            setDepartments(departmentsResponse.data.departments);
        } catch (error) {
            console.error("Failed to fetch departments:", error);
        }
    };

    // Fetch projects from the API
    const fetchProjects = async () => {
        try {
            const response = await axios.get(`${API_URL_BASE}/project`, {
                headers: {Authorization: `Bearer ` + localStorage.getItem('authToken')},
            });
            const data = response.data.projects;  // Assuming response.data is the array you want to transform

            // Keep only the id and projectName attributes
            const transformedData = data.map((project: { id: number, projectName: string }) => ({
                id: project.id,
                projectName: project.projectName,
            }));
            setProjectSet(transformedData);
        } catch (error) {
            console.error('Error fetching projects:', error);
        }
    };

    useEffect(() => {
        fetchUsers().then();
        fetchRolesAndDepartments().then();
        fetchProjects().then();
    }, []);

    // Handle form input change
    const handleChange = (
        e: React.ChangeEvent<{ name?: string; value: unknown }> | SelectChangeEvent
    ) => {
        const {name, value} = e.target;
        if (name) {
            setFormData({...formData, [name]: value});
        }
    };

    // Handle open dialog
    const handleOpen = (user: User | null = null) => {
        if (user) {
            setIsEdit(true);
            setSelectedUser(user);
            setFormData({
                firstName: user.firstName,
                lastName: user.lastName,
                userName: user.userName,
                password: user.password,
                isActive: user.isActive,
                roleId: user.roleSet.length > 0 ? user.roleSet[0].roleId.toString() : '',
                departmentId: user.department?.depId.toString() || '',
                projectsSet: user.projectsSet.length > 0 ? user.projectsSet : [],
            });
        } else {
            setIsEdit(false);
            setFormData({
                firstName: '',
                lastName: '',
                userName: '',
                password: '',
                isActive: true,
                roleId: '',
                departmentId: '',
                projectsSet: []
            });
        }
        setOpen(true);
    };

    // Handle close dialog
    const handleClose = () => {
        setOpen(false);
        setSelectedUser(null);
    };

    // Handle form submit
    const handleSubmit = async () => {
        try {
            const payload = {
                ...formData,
                roleId: parseInt(formData.roleId),
                departmentId: parseInt(formData.departmentId),
            };
            if (isEdit && selectedUser) {
                await axios.put(
                    `${API_URL_BASE}/users/${selectedUser.id}`,
                    payload,
                    {
                        headers: {Authorization: `Bearer ` + localStorage.getItem('authToken')},
                    }
                );
                setAlert({open: true, message: 'User Updated Successfully!', severity: 'success'});
            } else {
                await axios.post(
                    `${API_URL_BASE}/users`,
                    payload,
                    {
                        headers: {Authorization: `Bearer ` + localStorage.getItem('authToken')},
                    }
                );
                setAlert({open: true, message: 'User Added Successfully!', severity: 'success'});
            }
            fetchUsers().then();
            handleClose();
        } catch (error) {
            console.error('Error saving user:', error);
            setAlert({open: true, message: 'Error saving user!', severity: 'error'});
        }
    };

    // Handle delete user
    const handleDelete = async (id: number) => {
        if (window.confirm('Are you sure you want to delete this user?')) {
            try {
                await axios.delete(`${API_URL_BASE}/users/${id}`, {
                    headers: {Authorization: `Bearer ` + localStorage.getItem('authToken')},
                });
                fetchUsers().then();
                setAlert({open: true, message: 'User Deleted Successfully!', severity: 'success'});
            } catch (error) {
                console.error('Error deleting user:', error);
                setAlert({open: true, message: 'Error deleting user!', severity: 'error'});
            }
        }
    };

    // Handle toggle active status
    const toggleActiveStatus = async (user: User) => {
        try {
            await axios.put(
                `${API_URL_BASE}/users/${user.id}`,
                {...user, isActive: !user.isActive},
                {
                    headers: {Authorization: `Bearer ` + localStorage.getItem('authToken')},
                }
            );
            fetchUsers().then();
            setAlert({open: true, message: 'User active status changed!', severity: 'success'});
        } catch (error) {
            console.error('Error updating active status:', error);
        }
    };

    return (
        <div>
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
            <br/>
            <Button variant="outlined" color="primary" onClick={() => handleOpen()}>
                + Add User
            </Button>

            {/* Users Table */}
            <TableContainer component={Paper} style={{marginTop: 20}}>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell>First Name</TableCell>
                            <TableCell>Last Name</TableCell>
                            <TableCell>Username</TableCell>
                            <TableCell>Role</TableCell>
                            <TableCell>Department</TableCell>
                            <TableCell>Projects</TableCell>
                            <TableCell>Active</TableCell>
                            <TableCell>Actions</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {users.map((user) => (
                            <TableRow key={user.id}>
                                <TableCell>{user.firstName}</TableCell>
                                <TableCell>{user.lastName}</TableCell>
                                <TableCell>{user.userName}</TableCell>
                                <TableCell>{user.roleSet.map((role) => role.roleName).join(', ')}</TableCell>
                                <TableCell>{user.department?.depName || 'N/A'}</TableCell>
                                <TableCell>{user.projectsSet.map((project) => project.projectName).join(', ')}</TableCell>
                                <TableCell>
                                    <Switch
                                        checked={user.isActive}
                                        onChange={() => toggleActiveStatus(user)}
                                        color="primary"
                                        disabled={user.id === loggedInUserId}
                                    />
                                </TableCell>
                                <TableCell>
                                    <IconButton color="primary" onClick={() => handleOpen(user)}>
                                        <EditIcon/>
                                    </IconButton>
                                    {user.id !== loggedInUserId && (
                                        <IconButton color="secondary" onClick={() => handleDelete(user.id)}>
                                            <DeleteIcon/>
                                        </IconButton>
                                    )}
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>

            {/* Add/Edit Dialog */}
            <Dialog open={open} onClose={handleClose} fullWidth maxWidth="sm">
                <DialogTitle>{isEdit ? 'Edit User' : 'Add User'}</DialogTitle>
                <DialogContent dividers sx={{p: 3, display: 'flex', flexDirection: 'column', gap: 2}}>
                    <TextField
                        name="firstName"
                        label="First Name"
                        value={formData.firstName}
                        onChange={handleChange}
                        fullWidth
                    />
                    <TextField
                        name="lastName"
                        label="Last Name"
                        value={formData.lastName}
                        onChange={handleChange}
                        fullWidth
                    />
                    <TextField
                        name="userName"
                        label="Username"
                        value={formData.userName}
                        onChange={handleChange}
                        fullWidth
                    />
                    {!isEdit && (
                        <TextField
                            name="password"
                            label="Password"
                            type="password"
                            value={formData.password}
                            onChange={handleChange}
                            fullWidth
                        />
                    )}

                    <FormControl fullWidth>
                        <InputLabel>Role</InputLabel>
                        <Select name="roleId" value={formData.roleId} onChange={handleChange} label="Role" variant="outlined" disabled={isEditingLoggedInUser}>
                            {roles.map((role) => (
                                <MenuItem key={role.roleId} value={role.roleId}>
                                    {role.roleName}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>

                    <FormControl fullWidth>
                        <InputLabel>Department</InputLabel>
                        <Select
                            name="departmentId"
                            value={formData.departmentId}
                            onChange={handleChange}
                            label="Department"
                            variant="outlined"
                        >
                            {departments.map((dep) => (
                                <MenuItem key={dep.depId} value={dep.depId}>
                                    {dep.depName}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>

                    <div>
                        <strong style={{display: 'block', marginBottom: 8}}>Assign Projects:</strong>
                        <Box sx={{display: 'flex', flexWrap: 'wrap', gap: 1}}>
                            {projectSet.map((project) => (
                                <label key={project.id} style={{display: 'flex', alignItems: 'center', gap: 4}}>
                                    <input
                                        type="checkbox"
                                        checked={formData.projectsSet.some((p) => p.id === project.id)}
                                        onChange={(e) => {
                                            setFormData((prev) => {
                                                const updated = e.target.checked
                                                    ? [...prev.projectsSet, project]
                                                    : prev.projectsSet.filter((p) => p.id !== project.id);
                                                return {...prev, projectsSet: updated};
                                            });
                                        }}
                                    />
                                    {project.projectName}
                                </label>
                            ))}
                        </Box>
                    </div>
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
        </div>
    );
};

export default UserComponent;
