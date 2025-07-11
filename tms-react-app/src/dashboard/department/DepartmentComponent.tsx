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
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    Snackbar,
    Alert,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';

interface Department {
    depId: number;
    depName: string;
}

const DepartmentComponent: React.FC = () => {
    const [departments, setDepartments] = useState<Department[]>([]);
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

    const API_URL = 'http://localhost:8080/dhtcms/api/v1/department';

    const fetchDepartments = async () => {
        try {
            const response = await axios.get(API_URL, {
                headers: { Authorization: `Bearer ${localStorage.getItem('authToken')}` },
            });
            setDepartments(response.data.departments);
        } catch (error) {
            console.error('Error fetching Departments:', error);
        }
    };

    useEffect(() => {
        fetchDepartments().then();
    }, []);

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
                await axios.put(
                    `${API_URL}/${selectedDepartment.depId}`,
                    { depName },
                    { headers: { Authorization: `Bearer ${localStorage.getItem('authToken')}` } }
                );
                setDepartments(
                    departments.map((dep) =>
                        dep.depId === selectedDepartment.depId ? { ...dep, depName } : dep
                    )
                );
                setAlert({ open: true, message: 'Department updated successfully!', severity: 'success' });
            } else {
                const response = await axios.post(
                    API_URL,
                    { depName },
                    { headers: { Authorization: `Bearer ${localStorage.getItem('authToken')}` } }
                );
                const newDepartment: Department = response.data;
                setDepartments([...departments, newDepartment]);
                setAlert({ open: true, message: 'Department added successfully!', severity: 'success' });
            }
            handleClose();
        } catch (error) {
            console.error('Error saving Department:', error);
            setAlert({ open: true, message: 'Error saving Department.', severity: 'error' });
        }
    };

    const deleteDepartment = async (depId: number) => {
        try {
            await axios.delete(`${API_URL}/${depId}`, {
                headers: { Authorization: `Bearer ${localStorage.getItem('authToken')}` },
            });
            setDepartments(departments.filter((dep) => dep.depId !== depId));
            setAlert({ open: true, message: 'Department deleted successfully!', severity: 'success' });
        } catch (error) {
            console.error('Error deleting Department:', error);
            setAlert({ open: true, message: 'Error deleting Department.', severity: 'error' });
        }
    };

    return (
        <div>
            <br/>
            <Button variant="outlined" color="primary" onClick={handleOpenAdd} sx={{ mb: 2 }}>
                + Add Department
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
                        {departments.map((department) => (
                            <TableRow key={department.depId}>
                                <TableCell>{department.depName}</TableCell>
                                <TableCell>
                                    <IconButton color="primary" onClick={() => handleOpenEdit(department)}>
                                        <EditIcon />
                                    </IconButton>
                                    <IconButton color="secondary" onClick={() => deleteDepartment(department.depId)}>
                                        <DeleteIcon />
                                    </IconButton>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>

            {/* Add/Edit Department Dialog */}
            <Dialog open={open} onClose={handleClose} fullWidth maxWidth="sm">
                <DialogTitle>{isEdit ? 'Edit Department' : 'Add Department'}</DialogTitle>
                <DialogContent dividers sx={{p: 3, display: 'flex', flexDirection: 'column', gap: 2}}>
                    <TextField
                        label="Department Name"
                        value={depName}
                        onChange={(e) => setDepName(e.target.value)}
                        fullWidth
                        autoFocus
                    />
                </DialogContent>
                <DialogActions sx={{ p: 2 }}>
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
        </div>
    );
};

export default DepartmentComponent;
