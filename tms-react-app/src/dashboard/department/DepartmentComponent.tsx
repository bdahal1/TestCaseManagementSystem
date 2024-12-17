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

// Department type interface
interface Department {
    depId: number;
    depName: string;
}

const DepartmentComponent: React.FC = () => {
    const [departments, setDepartments] = useState<Department[]>([]);
    const [newDepartmentName, setNewDepartmentName] = useState<string>('');
    const [editDepartmentId, setEditDepartmentId] = useState<number | null>(null);
    const [editDepartmentName, setEditDepartmentName] = useState<string>('');
    const [alert, setAlert] = useState({
        open: false,
        message: '',
        severity: 'success', // 'success', 'error', 'warning', 'info'
    });


    const API_URL = 'http://localhost:8080/dhtcms/api/v1/department';

    // Fetch Departments from the API
    const fetchDepartments = async () => {
        try {
            const response = await axios.get(API_URL,{ headers: {"Authorization" : `Bearer `+localStorage.getItem("authToken")} });
            setDepartments(response.data.departments);
        } catch (error) {
            console.error('Error fetching Departments:', error);
        }
    };

    // Add a new Department
    const addDepartment = async () => {
        if (!newDepartmentName.trim()) {
            setAlert({ open: true, message: 'Department name cannot be empty.', severity: 'error' });
            return;
        }
        try {
            const response = await axios.post(API_URL, { depName: newDepartmentName },{ headers: {"Authorization" : `Bearer `+localStorage.getItem("authToken")} });
            const newDepartment: Department = response.data;

            if (newDepartment && newDepartment.depId && newDepartment.depName) {
                setDepartments([...departments, newDepartment]); // Add the new Department to the list
                setNewDepartmentName('');
            } else {
                console.error('Invalid Department data received:', response.data);
                setAlert({ open: true, message: 'Failed to add Department: Invalid data received from the server.', severity: 'error' });
            }
            setAlert({ open: true, message: 'Department added successfully!', severity: 'success' });
        } catch (error) {
            console.error('Error adding Department:', error);
            setAlert({ open: true, message: 'Error adding Department.', severity: 'error' });
        }
    };

    // Update an existing Department
    const updateDepartment = async () => {
        if (!editDepartmentName.trim()) {
            setAlert({ open: true, message: 'Department name cannot be empty.', severity: 'error' });
            return;
        }
        try {
            await axios.put(`${API_URL}/${editDepartmentId}`, { depName: editDepartmentName },{ headers: {"Authorization" : `Bearer `+localStorage.getItem("authToken")} });
            setDepartments(
                departments.map((department) =>
                    department.depId === editDepartmentId ? { ...department, depName: editDepartmentName } : department
                )
            );
            setEditDepartmentId(null);
            setEditDepartmentName('');
            setAlert({ open: true, message: 'Department edited successfully!', severity: 'error' });
        } catch (error) {
            console.error('Error updating Department:', error);
            setAlert({ open: true, message: 'Error Updating Department.', severity: 'error' });
        }
    };

    // Delete a Department
    const deleteDepartment = async (departmentId: number) => {
        try {
            await axios.delete(`${API_URL}/${departmentId}`,{ headers: {"Authorization" : `Bearer `+localStorage.getItem("authToken")} });
            setDepartments(departments.filter((department) => department.depId !== departmentId));
            setAlert({ open: true, message: 'Department deleted successfully!', severity: 'success' });
        } catch (error) {
            console.error('Error deleting Department:', error);
            setAlert({ open: true, message: 'Error deleting Department.', severity: 'error' });
        }
    };

    useEffect(() => {
        fetchDepartments().then();
    }, []);

    return (
        <div>
            <h1>Departments Management</h1>

            {/* Add Department Section */}
            <div style={{ marginBottom: '20px' }}>
                <TextField
                    label="New Department Name"
                    value={newDepartmentName}
                    onChange={(e) => setNewDepartmentName(e.target.value)}
                />
                <Button variant="contained" color="primary" onClick={addDepartment} style={{ marginLeft: '10px' }}>
                    Add Department
                </Button>
            </div>

            {/* Edit Department Section */}
            {editDepartmentId && (
                <div style={{ marginBottom: '20px' }}>
                    <TextField
                        label="Edit Department Name"
                        value={editDepartmentName}
                        onChange={(e) => setEditDepartmentName(e.target.value)}
                    />
                    <Button
                        variant="contained"
                        color="secondary"
                        onClick={updateDepartment}
                        style={{ marginLeft: '10px' }}
                    >
                        Update Department
                    </Button>
                </div>
            )}

            {/* Departments Table */}
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
                        {Array.isArray(departments) && departments.map((department) => (
                            <TableRow key={department.depId}>
                                <TableCell>{department.depId}</TableCell>
                                <TableCell>{department.depName}</TableCell>
                                <TableCell>
                                    <IconButton
                                        color="primary"
                                        onClick={() => {
                                            setEditDepartmentId(department.depId);
                                            setEditDepartmentName(department.depName);
                                        }}
                                    >
                                        <EditIcon />
                                    </IconButton>
                                    <IconButton
                                        color="secondary"
                                        onClick={() => deleteDepartment(department.depId)}
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

export default DepartmentComponent;
