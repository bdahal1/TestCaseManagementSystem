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
    FormControl,
    InputLabel,
    MenuItem,
    Select,
    Switch,
    Box,
    SelectChangeEvent,
    Typography
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import SearchIcon from '@mui/icons-material/Search';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import { InputAdornment } from '@mui/material';
import api from "../../services/api";

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

const API_URL_BASE = "";
const UserComponent: React.FC = () => {
    const [users, setUsers] = useState<User[]>([]);
    const [filteredUsers, setFilteredUsers] = useState<User[]>([]);
    const [searchQuery, setSearchQuery] = useState('');
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
            const response = await api.get(`${API_URL_BASE}/users`);
            setUsers(response.data.users);
            setFilteredUsers(response.data.users);
        } catch (error) {
            console.error('Error fetching users:', error);
        }
    };

    const fetchRolesAndDepartments = async () => {
        try {
            const rolesResponse = await api.get(`${API_URL_BASE}/roles`);
            setRoles(rolesResponse.data.roles);
        } catch (error) {
            console.error("Failed to fetch roles:", error);
        }

        try {
            const departmentsResponse = await api.get(`${API_URL_BASE}/department`);
            setDepartments(departmentsResponse.data.departments);
        } catch (error) {
            console.error("Failed to fetch departments:", error);
        }
    };

    // Fetch projects from the API
    const fetchProjects = async () => {
        try {
            const response = await api.get(`${API_URL_BASE}/project`);
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

    useEffect(() => {
        const lowerCaseQuery = searchQuery.toLowerCase();
        const filtered = users.filter((user) =>
            user.firstName.toLowerCase().includes(lowerCaseQuery) ||
            user.lastName.toLowerCase().includes(lowerCaseQuery) ||
            user.userName.toLowerCase().includes(lowerCaseQuery)
        );
        setFilteredUsers(filtered);
    }, [searchQuery, users]);

    // Handle form input change
    const handleChange = (
        e: React.ChangeEvent<{ name?: string; value: unknown }> | SelectChangeEvent
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
                await api.put(
                    `${API_URL_BASE}/users/${selectedUser.id}`,
                    payload,
                    {
                        headers: { Authorization: `Bearer ` + localStorage.getItem('authToken') },
                    }
                );
                setAlert({ open: true, message: 'User Updated Successfully!', severity: 'success' });
            } else {
                await api.post(
                    `${API_URL_BASE}/users`,
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
            setAlert({ open: true, message: 'Error saving user!', severity: 'error' });
        }
    };

    // Handle delete user
    const handleDelete = async (id: number) => {
        if (window.confirm('Are you sure you want to delete this user?')) {
            try {
                await api.delete(`${API_URL_BASE}/users/${id}`);
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
            await api.put(
                `${API_URL_BASE}/users/${user.id}`,
                { ...user, isActive: !user.isActive }
            );
            fetchUsers().then();
            setAlert({ open: true, message: 'User active status changed!', severity: 'success' });
        } catch (error) {
            console.error('Error updating active status:', error);
        }
    };

    return (
        <Box sx={{ padding: 2 }}>
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
                >
                    {alert.message}
                </Alert>
            </Snackbar>


            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
                <TextField
                    placeholder="Search users..."
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
                    onClick={() => handleOpen()}
                    startIcon={<PersonAddIcon />}
                    sx={{ borderRadius: '10px', px: 3 }}
                >
                    Add User
                </Button>
            </Box>

            {/* Users Table */}
            <TableContainer component={Paper} style={{ marginTop: 20 }}>
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
                        {filteredUsers.map((user) => (
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
                                    <IconButton size="small" onClick={() => handleOpen(user)}>
                                        <EditIcon fontSize="small" color="primary" />
                                    </IconButton>
                                    {user.id !== loggedInUserId && (
                                        <IconButton size="small" onClick={() => handleDelete(user.id)}>
                                            <DeleteIcon fontSize="small" color="error" />
                                        </IconButton>
                                    )}
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>

            {/* Add/Edit Dialog */}
            <Dialog
                open={open}
                onClose={handleClose}
                fullWidth
                maxWidth="sm"
                PaperProps={{
                    sx: { borderRadius: '20px', p: 1 }
                }}
            >
                <DialogTitle sx={{
                    fontWeight: 700,
                    fontSize: '1.5rem',
                    color: 'text.primary',
                    pb: 1
                }}>
                    {isEdit ? 'Edit User' : 'New User'}
                </DialogTitle>
                <DialogContent sx={{ p: 3 }}>
                    <Box component="form" sx={{ display: 'flex', flexDirection: 'column', gap: 2.5, mt: 1 }}>
                        <Box sx={{ display: 'flex', gap: 2 }}>
                            <TextField
                                name="firstName"
                                label="First Name"
                                value={formData.firstName}
                                onChange={handleChange}
                                fullWidth
                                variant="outlined"
                            />
                            <TextField
                                name="lastName"
                                label="Last Name"
                                value={formData.lastName}
                                onChange={handleChange}
                                fullWidth
                                variant="outlined"
                            />
                        </Box>

                        <TextField
                            name="userName"
                            label="Username"
                            value={formData.userName}
                            onChange={handleChange}
                            fullWidth
                            variant="outlined"
                        />

                        {!isEdit && (
                            <TextField
                                name="password"
                                label="Password"
                                type="password"
                                value={formData.password}
                                onChange={handleChange}
                                fullWidth
                                variant="outlined"
                            />
                        )}

                        <FormControl fullWidth>
                            <InputLabel>Role</InputLabel>
                            <Select
                                name="roleId"
                                value={formData.roleId}
                                onChange={handleChange}
                                label="Role"
                                disabled={isEditingLoggedInUser}
                            >
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
                            >
                                {departments.map((dep) => (
                                    <MenuItem key={dep.depId} value={dep.depId}>
                                        {dep.depName}
                                    </MenuItem>
                                ))}
                            </Select>
                        </FormControl>

                        <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, backgroundColor: 'background.default' }}>
                            <Typography variant="subtitle2" fontWeight="600" mb={1.5}>
                                Assign Projects
                            </Typography>
                            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                                {projectSet.map((project) => (
                                    <Box
                                        key={project.id}
                                        sx={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            gap: 1,
                                            px: 1.5,
                                            py: 0.5,
                                            borderRadius: '20px',
                                            backgroundColor: formData.projectsSet.some((p) => p.id === project.id)
                                                ? 'primary.light'
                                                : 'white',
                                            color: formData.projectsSet.some((p) => p.id === project.id)
                                                ? 'white'
                                                : 'text.secondary',
                                            cursor: 'pointer',
                                            border: '1px solid',
                                            borderColor: formData.projectsSet.some((p) => p.id === project.id)
                                                ? 'primary.main'
                                                : 'divider',
                                            transition: 'all 0.2s'
                                        }}
                                        onClick={() => {
                                            const isSelected = formData.projectsSet.some((p) => p.id === project.id);
                                            setFormData((prev) => {
                                                const updated = !isSelected
                                                    ? [...prev.projectsSet, project]
                                                    : prev.projectsSet.filter((p) => p.id !== project.id);
                                                return { ...prev, projectsSet: updated };
                                            });
                                        }}
                                    >
                                        <Typography variant="body2" fontWeight={500}>
                                            {project.projectName}
                                        </Typography>
                                    </Box>
                                ))}
                            </Box>
                        </Paper>
                    </Box>
                </DialogContent>
                <DialogActions sx={{ p: 3, pt: 1 }}>
                    <Button onClick={handleClose} variant="text" color="inherit" sx={{ borderRadius: '10px' }}>
                        Cancel
                    </Button>
                    <Button
                        onClick={handleSubmit}
                        variant="contained"
                        color="primary"
                        sx={{ px: 4, borderRadius: '10px' }}
                    >
                        {isEdit ? 'Save Changes' : 'Create User'}
                    </Button>
                </DialogActions>
            </Dialog>
        </Box >
    );
};

export default UserComponent;
