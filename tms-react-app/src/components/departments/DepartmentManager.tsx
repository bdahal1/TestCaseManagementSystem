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
import BusinessIcon from '@mui/icons-material/Business';
import { InputAdornment } from '@mui/material';
import { Box } from "@mui/system";
import api from "../../services/api";

interface Department {
    depId: number;
    depName: string;
}

const DepartmentComponent: React.FC = () => {
    const [departments, setDepartments] = useState<Department[]>([]);
    const [filteredDepartments, setFilteredDepartments] = useState<Department[]>([]);
    const [searchQuery, setSearchQuery] = useState('');
    const [open, setOpen] = useState(false);
    const [isEdit, setIsEdit] = useState(false);
    const [selectedDepartment, setSelectedDepartment] = useState<Department | null>(null);
    const [depName, setDepName] = useState('');
    const [alert, setAlert] = useState<{
        open: boolean;
        message: string;
        severity: 'success' | 'error' | 'warning' | 'info';
    }>({
        open: false,
        message: '',
        severity: 'success',
    });

    const API_URL = '/department';

    const fetchDepartments = async () => {
        try {
            const response = await api.get(API_URL);
            setDepartments(response.data.departments);
            setFilteredDepartments(response.data.departments);
        } catch (error) {
            console.error('Error fetching Departments:', error);
        }
    };

    useEffect(() => {
        fetchDepartments().then();
    }, []);

    useEffect(() => {
        const lowerCaseQuery = searchQuery.toLowerCase();
        const filtered = departments.filter(dep =>
            dep.depName.toLowerCase().includes(lowerCaseQuery)
        );
        setFilteredDepartments(filtered);
    }, [searchQuery, departments]);

    const handleOpenAdd = () => {
        setIsEdit(false);
        setSelectedDepartment(null);
        setDepName('');
        setOpen(true);
    };

    const handleOpenEdit = (department: Department) => {
        setIsEdit(true);
        setSelectedDepartment(department);
        setDepName(department.depName);
        setOpen(true);
    };

    const handleClose = () => {
        setOpen(false);
        setDepName('');
        setSelectedDepartment(null);
    };

    const handleSubmit = async () => {
        if (!depName.trim()) {
            setAlert({ open: true, message: 'Department name cannot be empty.', severity: 'error' });
            return;
        }
        try {
            if (isEdit && selectedDepartment) {
                await api.put(
                    `${API_URL}/${selectedDepartment.depId}`,
                    { depName },
                    { headers: { Authorization: `Bearer ${localStorage.getItem('authToken')}` } }
                );
                setAlert({ open: true, message: 'Department updated successfully!', severity: 'success' });
            } else {
                await api.post(
                    API_URL,
                    { depName },
                    { headers: { Authorization: `Bearer ${localStorage.getItem('authToken')}` } }
                );
                setAlert({ open: true, message: 'Department added successfully!', severity: 'success' });
            }
            fetchDepartments();
            handleClose();
        } catch (error) {
            console.error('Error saving Department:', error);
            setAlert({ open: true, message: 'Error saving Department.', severity: 'error' });
        }
    };

    const deleteDepartment = async (depId: number) => {
        try {
            await api.delete(`${API_URL}/${depId}`);
            setAlert({ open: true, message: 'Department deleted successfully!', severity: 'success' });
            fetchDepartments();
        } catch (error) {
            console.error('Error deleting Department:', error);
            setAlert({ open: true, message: 'Error deleting Department.', severity: 'error' });
        }
    };

    return (
        <Box sx={{ padding: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
                <TextField
                    placeholder="Search departments..."
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
                    startIcon={<BusinessIcon />}
                    sx={{ borderRadius: '10px', px: 3 }}
                >
                    Add Department
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
                        {filteredDepartments.map((department) => (
                            <TableRow key={department.depId}>
                                <TableCell>{department.depName}</TableCell>
                                <TableCell>
                                    <IconButton size="small" onClick={() => handleOpenEdit(department)}>
                                        <EditIcon fontSize="small" color="primary" />
                                    </IconButton>
                                    <IconButton size="small" onClick={() => deleteDepartment(department.depId)}>
                                        <DeleteIcon fontSize="small" color="error" />
                                    </IconButton>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>

            {/* Add/Edit Department Dialog */}
            <Dialog
                open={open}
                onClose={handleClose}
                fullWidth
                maxWidth="xs"
                PaperProps={{ sx: { borderRadius: '20px', p: 1 } }}
            >
                <DialogTitle sx={{ fontWeight: 700, fontSize: '1.25rem', pb: 1 }}>
                    {isEdit ? 'Edit Department' : 'New Department'}
                </DialogTitle>
                <DialogContent sx={{ p: 3, pt: 1 }}>
                    <Box component="form" sx={{ mt: 1 }}>
                        <TextField
                            label="Department Name"
                            value={depName}
                            onChange={(e) => setDepName(e.target.value)}
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

export default DepartmentComponent;
