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
import SecurityIcon from '@mui/icons-material/Security';
import { InputAdornment } from '@mui/material';
import { Box } from "@mui/system";
import api from "../../services/api";

interface Role {
    roleId: number;
    roleName: string;
}

const RoleComponent: React.FC = () => {
    const [roles, setRoles] = useState<Role[]>([]);
    const [filteredRoles, setFilteredRoles] = useState<Role[]>([]);
    const [searchQuery, setSearchQuery] = useState('');
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

    const API_URL = '/roles';

    const fetchRoles = async () => {
        try {
            const response = await api.get(API_URL);
            setRoles(response.data.roles);
            setFilteredRoles(response.data.roles);
        } catch (error) {
            console.error('Error fetching roles:', error);
        }
    };

    useEffect(() => {
        fetchRoles();
    }, []);

    useEffect(() => {
        const lowerCaseQuery = searchQuery.toLowerCase();
        const filtered = roles.filter(role =>
            role.roleName.toLowerCase().includes(lowerCaseQuery)
        );
        setFilteredRoles(filtered);
    }, [searchQuery, roles]);

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
            setAlert({ open: true, message: 'Role name cannot be empty.', severity: 'error' });
            return;
        }
        try {
            if (isEdit && selectedRole) {
                // Update role
                await api.put(
                    `${API_URL}/${selectedRole.roleId}`,
                    { roleName }
                );
                setAlert({ open: true, message: 'Role updated successfully!', severity: 'success' });
            } else {
                // Add new role
                await api.post(
                    API_URL,
                    { roleName }
                );
                setAlert({ open: true, message: 'Role added successfully!', severity: 'success' });
            }
            fetchRoles();
            handleClose();
        } catch (error) {
            console.error('Error saving role:', error);
            setAlert({ open: true, message: 'Error saving role.', severity: 'error' });
        }
    };

    const deleteRole = async (roleId: number) => {
        try {
            await api.delete(`${API_URL}/${roleId}`);
            setAlert({ open: true, message: 'Role deleted successfully!', severity: 'success' });
            fetchRoles();
        } catch (error) {
            console.error('Error deleting role:', error);
            setAlert({ open: true, message: 'Error deleting role.', severity: 'error' });
        }
    };

    return (
        <Box sx={{ padding: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
                <TextField
                    placeholder="Search roles..."
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
                    startIcon={<SecurityIcon />}
                    sx={{ borderRadius: '10px', px: 3 }}
                >
                    Add Role
                </Button>
            </Box>

            <TableContainer component={Paper}>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell>Name</TableCell>
                            <TableCell>Actions</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {filteredRoles.map((role) => (
                            <TableRow key={role.roleId} hover>
                                <TableCell>{role.roleName}</TableCell>
                                <TableCell>
                                    <IconButton size="small" onClick={() => handleOpenEdit(role)}>
                                        <EditIcon fontSize="small" color="primary" />
                                    </IconButton>
                                    <IconButton size="small" onClick={() => deleteRole(role.roleId)}>
                                        <DeleteIcon fontSize="small" color="error" />
                                    </IconButton>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>

            {/* Add/Edit Role Dialog */}
            <Dialog
                open={open}
                onClose={handleClose}
                fullWidth
                maxWidth="xs"
                PaperProps={{ sx: { borderRadius: '20px', p: 1 } }}
            >
                <DialogTitle sx={{ fontWeight: 700, fontSize: '1.25rem', pb: 1 }}>
                    {isEdit ? 'Edit Role' : 'New Role'}
                </DialogTitle>
                <DialogContent sx={{ p: 3, pt: 1 }}>
                    <Box component="form" sx={{ mt: 1 }}>
                        <TextField
                            label="Role Name"
                            value={roleName}
                            onChange={(e) => setRoleName(e.target.value)}
                            fullWidth
                            variant="outlined"
                            autoFocus
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

export default RoleComponent;
