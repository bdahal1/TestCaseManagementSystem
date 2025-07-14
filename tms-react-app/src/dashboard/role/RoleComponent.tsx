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
import {Box} from "@mui/system";

interface Role {
    roleId: number;
    roleName: string;
}

const RoleComponent: React.FC = () => {
    const [roles, setRoles] = useState<Role[]>([]);
    const [open, setOpen] = useState(false);
    const [isEdit, setIsEdit] = useState(false);
    const [selectedRole, setSelectedRole] = useState<Role | null>(null);
    const [roleName, setRoleName] = useState('');
    const [alert, setAlert] = useState<{
        open: boolean;
        message: string;
        severity: 'success' | 'error' | 'warning' | 'info';
    }>({
        open: false,
        message: '',
        severity: 'success',
    });

    const API_URL = 'http://localhost:8080/dhtcms/api/v1/roles';

    const fetchRoles = async () => {
        try {
            const response = await axios.get(API_URL, {
                headers: {Authorization: `Bearer ` + localStorage.getItem('authToken')},
            });
            setRoles(response.data.roles);
        } catch (error) {
            console.error('Error fetching roles:', error);
        }
    };

    useEffect(() => {
        fetchRoles();
    }, []);

    const handleOpenAdd = () => {
        setIsEdit(false);
        setSelectedRole(null);
        setRoleName('');
        setOpen(true);
    };

    const handleOpenEdit = (role: Role) => {
        setIsEdit(true);
        setSelectedRole(role);
        setRoleName(role.roleName);
        setOpen(true);
    };

    const handleClose = () => {
        setOpen(false);
        setRoleName('');
        setSelectedRole(null);
    };

    const handleSubmit = async () => {
        if (!roleName.trim()) {
            setAlert({open: true, message: 'Role name cannot be empty.', severity: 'error'});
            return;
        }
        try {
            if (isEdit && selectedRole) {
                // Update role
                await axios.put(
                    `${API_URL}/${selectedRole.roleId}`,
                    {roleName},
                    {headers: {Authorization: `Bearer ` + localStorage.getItem('authToken')}}
                );
                setRoles(
                    roles.map((role) =>
                        role.roleId === selectedRole.roleId ? {...role, roleName} : role
                    )
                );
                setAlert({open: true, message: 'Role updated successfully!', severity: 'success'});
            } else {
                // Add new role
                const response = await axios.post(
                    API_URL,
                    {roleName},
                    {headers: {Authorization: `Bearer ` + localStorage.getItem('authToken')}}
                );
                const newRole: Role = response.data;
                setRoles([...roles, newRole]);
                setAlert({open: true, message: 'Role added successfully!', severity: 'success'});
            }
            handleClose();
        } catch (error) {
            console.error('Error saving role:', error);
            setAlert({open: true, message: 'Error saving role.', severity: 'error'});
        }
    };

    const deleteRole = async (roleId: number) => {
        try {
            await axios.delete(`${API_URL}/${roleId}`, {
                headers: {Authorization: `Bearer ` + localStorage.getItem('authToken')},
            });
            setRoles(roles.filter((role) => role.roleId !== roleId));
            setAlert({open: true, message: 'Role deleted successfully!', severity: 'success'});
        } catch (error) {
            console.error('Error deleting role:', error);
            setAlert({open: true, message: 'Error deleting role.', severity: 'error'});
        }
    };

    return (
        <Box sx={{padding: 2}}>
            <Button variant="outlined" color="primary" onClick={handleOpenAdd} sx={{mb: 2}}>
                + Add Role
            </Button>

            <TableContainer component={Paper}>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell>Name</TableCell>
                            <TableCell>Actions</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {roles.map((role) => (
                            <TableRow key={role.roleId}>
                                <TableCell>{role.roleName}</TableCell>
                                <TableCell>
                                    <IconButton color="primary" onClick={() => handleOpenEdit(role)}>
                                        <EditIcon/>
                                    </IconButton>
                                    <IconButton color="secondary" onClick={() => deleteRole(role.roleId)}>
                                        <DeleteIcon/>
                                    </IconButton>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>

            {/* Add/Edit Role Dialog */}
            <Dialog open={open} onClose={handleClose} fullWidth maxWidth="sm">
                <DialogTitle>{isEdit ? 'Edit Role' : 'Add Role'}</DialogTitle>
                <DialogContent dividers sx={{p: 3, display: 'flex', flexDirection: 'column', gap: 2}}>
                    <TextField
                        label="Role Name"
                        value={roleName}
                        onChange={(e) => setRoleName(e.target.value)}
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
        </Box>
    );
};

export default RoleComponent;
