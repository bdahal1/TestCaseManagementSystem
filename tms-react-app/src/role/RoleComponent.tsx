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
    IconButton, Snackbar, Alert,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';

// Role type interface
interface Role {
    roleId: number;
    roleName: string;
}

const RoleComponent: React.FC = () => {
    const [roles, setRoles] = useState<Role[]>([]);
    const [newRoleName, setNewRoleName] = useState<string>('');
    const [editRoleId, setEditRoleId] = useState<number | null>(null);
    const [editRoleName, setEditRoleName] = useState<string>('');
    const [alert, setAlert] = useState({
        open: false,
        message: '',
        severity: 'success', // 'success', 'error', 'warning', 'info'
    });


    const API_URL = 'http://localhost:8080/dhtcms/api/v1/roles';

    // Fetch roles from the API
    const fetchRoles = async () => {
        try {
            const response = await axios.get(API_URL,{ headers: {"Authorization" : `Bearer `+localStorage.getItem("authToken")} });
            setRoles(response.data.roles);
        } catch (error) {
            console.error('Error fetching roles:', error);
        }
    };

    // Add a new role
    const addRole = async () => {
        if (!newRoleName.trim()) {
            setAlert({ open: true, message: 'Role name cannot be empty.', severity: 'error' });
            return;
        }
        try {
            const response = await axios.post(API_URL, { roleName: newRoleName },{ headers: {"Authorization" : `Bearer `+localStorage.getItem("authToken")} });
            const newRole: Role = response.data;

            if (newRole && newRole.roleId && newRole.roleName) {
                setRoles([...roles, newRole]); // Add the new role to the list
                setNewRoleName('');
            } else {
                console.error('Invalid role data received:', response.data);
                setAlert({ open: true, message: 'Failed to add role: Invalid data received from the server.', severity: 'error' });
            }
            setAlert({ open: true, message: 'Role added successfully!', severity: 'success' });
        } catch (error) {
            console.error('Error adding role:', error);
            setAlert({ open: true, message: 'Error adding role.', severity: 'error' });
        }
    };

    // Update an existing role
    const updateRole = async () => {
        if (!editRoleName.trim()) {
            setAlert({ open: true, message: 'Role name cannot be empty.', severity: 'error' });
            return;
        }
        try {
            await axios.put(`${API_URL}/${editRoleId}`, { roleName: editRoleName },{ headers: {"Authorization" : `Bearer `+localStorage.getItem("authToken")} });
            setRoles(
                roles.map((role) =>
                    role.roleId === editRoleId ? { ...role, roleName: editRoleName } : role
                )
            );
            setEditRoleId(null);
            setEditRoleName('');
            setAlert({ open: true, message: 'Role edited successfully!', severity: 'error' });
        } catch (error) {
            console.error('Error updating role:', error);
            setAlert({ open: true, message: 'Error Updating role.', severity: 'error' });
        }
    };

    // Delete a role
    const deleteRole = async (roleId: number) => {
        try {
            await axios.delete(`${API_URL}/${roleId}`,{ headers: {"Authorization" : `Bearer `+localStorage.getItem("authToken")} });
            setRoles(roles.filter((role) => role.roleId !== roleId));
            setAlert({ open: true, message: 'Role deleted successfully!', severity: 'success' });
        } catch (error) {
            console.error('Error deleting role:', error);
            setAlert({ open: true, message: 'Error deleting role.', severity: 'error' });
        }
    };

    useEffect(() => {
        fetchRoles().then();
    }, []);

    return (
        <div>
            <h1>Roles Management</h1>

            {/* Add Role Section */}
            <div style={{ marginBottom: '20px' }}>
                <TextField
                    label="New Role Name"
                    value={newRoleName}
                    onChange={(e) => setNewRoleName(e.target.value)}
                />
                <Button variant="contained" color="primary" onClick={addRole} style={{ marginLeft: '10px' }}>
                    Add Role
                </Button>
            </div>

            {/* Edit Role Section */}
            {editRoleId && (
                <div style={{ marginBottom: '20px' }}>
                    <TextField
                        label="Edit Role Name"
                        value={editRoleName}
                        onChange={(e) => setEditRoleName(e.target.value)}
                    />
                    <Button
                        variant="contained"
                        color="secondary"
                        onClick={updateRole}
                        style={{ marginLeft: '10px' }}
                    >
                        Update Role
                    </Button>
                </div>
            )}

            {/* Roles Table */}
            <TableContainer component={Paper}>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell>ID</TableCell>
                            <TableCell>Name</TableCell>
                            <TableCell>Actions</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {Array.isArray(roles) && roles.map((role) => (
                            <TableRow key={role.roleId}>
                                <TableCell>{role.roleId}</TableCell>
                                <TableCell>{role.roleName}</TableCell>
                                <TableCell>
                                    <IconButton
                                        color="primary"
                                        onClick={() => {
                                            setEditRoleId(role.roleId);
                                            setEditRoleName(role.roleName);
                                        }}
                                    >
                                        <EditIcon />
                                    </IconButton>
                                    <IconButton
                                        color="secondary"
                                        onClick={() => deleteRole(role.roleId)}
                                    >
                                        <DeleteIcon />
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
                onClose={() => setAlert({ ...alert, open: false })}
                anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
            >
                <Alert onClose={() => setAlert({ ...alert, open: false })} variant="filled">
                    {alert.message}
                </Alert>
            </Snackbar>
        </div>
    );
};

export default RoleComponent;
