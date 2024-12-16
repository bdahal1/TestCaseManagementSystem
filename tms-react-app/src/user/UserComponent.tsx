import React, { useEffect, useState } from 'react';
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
    const [alert, setAlert] = useState({
        open: false,
        message: '',
        severity: 'success', // 'success', 'error', 'warning', 'info'
    });

    const API_URL = 'http://localhost:8080/dhtcms/api/v1/users';

    // Fetch users from the API
    const fetchUsers = async () => {
        try {
            const response = await axios.get(API_URL, {
                headers: { Authorization: `Bearer ` + localStorage.getItem('authToken') },
            });
            setUsers(response.data.users);
        } catch (error) {
            console.error('Error fetching users:', error);
        }
    };

    const fetchRolesAndDepartments = async () => {
        try {
            const [rolesResponse, departmentsResponse] = await Promise.all([
                axios.get('http://localhost:8080/dhtcms/api/v1/roles', {
                    headers: { Authorization: `Bearer ` + localStorage.getItem('authToken') },
                }),
                axios.get('http://localhost:8080/dhtcms/api/v1/department', {
                    headers: { Authorization: `Bearer ` + localStorage.getItem('authToken') },
                }),
            ]);
            setRoles(rolesResponse.data.roles);
            setDepartments(departmentsResponse.data.departments);
        } catch (error) {
            console.error('Error fetching roles or departments:', error);
        }
    };

    // Fetch projects from the API
    const fetchProjects = async () => {
        try {
            const response = await axios.get('http://localhost:8080/dhtcms/api/v1/project', {
                headers: { Authorization: `Bearer ` + localStorage.getItem('authToken') },
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
        e: React.ChangeEvent<{ name?: string; value: unknown }> | SelectChangeEvent<string>
    ) => {
        const { name, value } = e.target;
        if (name) {
            setFormData({ ...formData, [name]: value });
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
                password:user.password,
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
                    `${API_URL}/${selectedUser.id}`,
                    payload,
                    {
                        headers: { Authorization: `Bearer ` + localStorage.getItem('authToken') },
                    }
                );
                setAlert({ open: true, message: 'User Updated Successfully!', severity: 'success' });
            } else {
                await axios.post(
                    API_URL,
                    payload,
                    {
                        headers: { Authorization: `Bearer ` + localStorage.getItem('authToken') },
                    }
                );
                setAlert({ open: true, message: 'User Added Successfully!', severity: 'success' });
            }
            fetchUsers().then();
            handleClose();
        } catch (error) {
            console.error('Error saving user:', error);
            setAlert({ open: true, message: 'Error saving user!', severity: 'Error' });
        }
    };

    // Handle delete user
    const handleDelete = async (id: number) => {
        if (window.confirm('Are you sure you want to delete this user?')) {
            try {
                await axios.delete(`${API_URL}/${id}`, {
                    headers: { Authorization: `Bearer ` + localStorage.getItem('authToken') },
                });
                fetchUsers().then();
                setAlert({ open: true, message: 'User Deleted Successfully!', severity: 'success' });
            } catch (error) {
                console.error('Error deleting user:', error);
                setAlert({ open: true, message: 'Error deleting user!', severity: 'error' });
            }
        }
    };

    // Handle toggle active status
    const toggleActiveStatus = async (user: User) => {
        try {
            await axios.put(
                `${API_URL}/${user.id}`,
                { ...user, isActive: !user.isActive },
                {
                    headers: { Authorization: `Bearer ` + localStorage.getItem('authToken') },
                }
            );
            fetchUsers().then();
            setAlert({ open: true, message: 'User active status changed!', severity: 'success' });
        } catch (error) {
            console.error('Error updating active status:', error);
        }
    };

    return (
        <div>
            <Snackbar
                open={alert.open}
                autoHideDuration={3000}
                onClose={() => setAlert({ ...alert, open: false })}
                anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
            >
                <Alert onClose={() => setAlert({ ...alert, open: false })} variant="filled">
                    {alert.message}
                </Alert>
            </Snackbar>
            <h1>Users Management</h1>
            <Button variant="contained" color="primary" onClick={() => handleOpen()}>
                Add User
            </Button>

            {/* Users Table */}
            <TableContainer component={Paper} style={{ marginTop: 20 }}>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell>ID</TableCell>
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
                                <TableCell>{user.id}</TableCell>
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
                                    />
                                </TableCell>
                                <TableCell>
                                    <IconButton color="primary" onClick={() => handleOpen(user)}>
                                        <EditIcon />
                                    </IconButton>
                                    <IconButton color="secondary" onClick={() => handleDelete(user.id)}>
                                        <DeleteIcon />
                                    </IconButton>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>

            {/* Add/Edit Dialog */}
            <Dialog open={open} onClose={handleClose} fullWidth maxWidth="sm">
                <DialogTitle>{isEdit ? 'Edit User' : 'Add User'}</DialogTitle>
                <DialogContent>
                    <TextField
                        name="firstName"
                        label="First Name"
                        value={formData.firstName}
                        onChange={handleChange}
                        fullWidth
                        margin="dense"
                    />
                    <TextField
                        name="lastName"
                        label="Last Name"
                        value={formData.lastName}
                        onChange={handleChange}
                        fullWidth
                        margin="dense"
                    />
                    <TextField
                        name="userName"
                        label="Username"
                        value={formData.userName}
                        onChange={handleChange}
                        fullWidth
                        margin="dense"
                    />
                    {isEdit ?formData.password:<TextField
                            name="password"
                            label="Password"
                            value={formData.password}
                            onChange={handleChange}
                            fullWidth
                            margin="dense"
                        />}
                    <FormControl fullWidth margin="dense">
                        <InputLabel>Role</InputLabel>
                        <Select
                            name="roleId"
                            value={formData.roleId}
                            onChange={handleChange}
                        >
                            {roles.map((role: Role) => (
                                <MenuItem key={role.roleId} value={role.roleId}>
                                    {role.roleName}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                    <FormControl fullWidth margin="dense">
                        <InputLabel>Department</InputLabel>
                        <Select
                            name="departmentId"
                            value={formData.departmentId}
                            onChange={handleChange}
                        >
                            {departments.map((department: Department) => (
                                <MenuItem key={department.depId} value={department.depId}>
                                    {department.depName}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                    <div>
                        <p><strong>Assign Projects:</strong></p>
                        {projectSet.map((project) => (
                            <div key={project.id}>
                                <label>
                                    <input
                                        type="checkbox"
                                        checked={
                                            formData.projectsSet.some((p) => p.id === project.id) || false
                                        }
                                        onChange={(e) => {
                                            setFormData((prevData) => {
                                                let updatedProjectsSet = [...prevData.projectsSet];
                                                if (e.target.checked) {
                                                    updatedProjectsSet.push(project);
                                                } else {
                                                    updatedProjectsSet = updatedProjectsSet.filter(p => p.id !== project.id);
                                                }
                                                return { ...prevData, projectsSet: updatedProjectsSet };
                                            });
                                        }}
                                    />
                                    {project.projectName}
                                </label>
                            </div>
                        ))}
                    </div>
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleClose} color="secondary">
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
