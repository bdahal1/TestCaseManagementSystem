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
    Box,
    InputAdornment
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import SearchIcon from '@mui/icons-material/Search';
import LabelIcon from '@mui/icons-material/Label';
import api from "../../services/api";

interface Tag {
    id: number;
    tagName: string;
}

const TagManager: React.FC = () => {
    const [tags, setTags] = useState<Tag[]>([]);
    const [filteredTags, setFilteredTags] = useState<Tag[]>([]);
    const [searchQuery, setSearchQuery] = useState('');
    const [open, setOpen] = useState(false);
    const [isEdit, setIsEdit] = useState(false);
    const [selectedTag, setSelectedTag] = useState<Tag | null>(null);
    const [tagName, setTagName] = useState('');
    const [alert, setAlert] = useState<{
        open: boolean;
        message: string;
        severity: 'success' | 'error' | 'warning' | 'info';
    }>({
        open: false,
        message: '',
        severity: 'success',
    });

    const API_URL = '/tags';

    const fetchTags = async () => {
        try {
            const response = await api.get(API_URL);
            // Assuming response structure based on other controllers
            const tagsList = response.data.tags || [];
            setTags(tagsList);
            setFilteredTags(tagsList);
        } catch (error) {
            console.error('Error fetching tags:', error);
            setAlert({ open: true, message: 'Failed to fetch tags', severity: 'error' });
        }
    };

    useEffect(() => {
        fetchTags();
    }, []);

    useEffect(() => {
        const lowerCaseQuery = searchQuery.toLowerCase();
        const filtered = tags.filter(tag =>
            tag.tagName.toLowerCase().includes(lowerCaseQuery)
        );
        setFilteredTags(filtered);
    }, [searchQuery, tags]);

    const handleOpenAdd = () => {
        setIsEdit(false);
        setSelectedTag(null);
        setTagName('');
        setOpen(true);
    };

    const handleOpenEdit = (tag: Tag) => {
        setIsEdit(true);
        setSelectedTag(tag);
        setTagName(tag.tagName);
        setOpen(true);
    };

    const handleClose = () => {
        setOpen(false);
        setTagName('');
        setSelectedTag(null);
    };

    const handleSubmit = async () => {
        if (!tagName.trim()) {
            setAlert({ open: true, message: 'Tag name cannot be empty.', severity: 'error' });
            return;
        }

        try {
            if (isEdit && selectedTag) {
                await api.put(`${API_URL}/${selectedTag.id}`, { tagName });
                setAlert({ open: true, message: 'Tag updated successfully!', severity: 'success' });
            } else {
                await api.post(API_URL, { tagName });
                setAlert({ open: true, message: 'Tag created successfully!', severity: 'success' });
            }
            fetchTags();
            handleClose();
        } catch (error) {
            console.error('Error saving tag:', error);
            setAlert({ open: true, message: 'Error saving tag.', severity: 'error' });
        }
    };

    const handleDelete = async (id: number) => {
        if (window.confirm('Are you sure you want to delete this tag?')) {
            try {
                await api.delete(`${API_URL}/${id}`);
                setAlert({ open: true, message: 'Tag deleted successfully!', severity: 'success' });
                fetchTags();
            } catch (error) {
                console.error('Error deleting tag:', error);
                setAlert({ open: true, message: 'Error deleting tag.', severity: 'error' });
            }
        }
    };

    return (
        <Box sx={{ padding: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
                <TextField
                    placeholder="Search tags..."
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
                <Button variant="contained" color="primary" onClick={handleOpenAdd} startIcon={<LabelIcon />} sx={{ borderRadius: '10px', px: 3 }}>
                    Add Tag
                </Button>
            </Box>

            <TableContainer component={Paper} sx={{ borderRadius: 3, boxShadow: '0 4px 20px rgba(0,0,0,0.05)' }}>
                <Table>
                    <TableHead>
                        <TableRow sx={{ backgroundColor: 'action.hover' }}>
                            <TableCell sx={{ fontWeight: 700 }}>Tag Name</TableCell>
                            <TableCell align="right" sx={{ fontWeight: 700 }}>Actions</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {filteredTags.length > 0 ? (
                            filteredTags.map((tag) => (
                                <TableRow key={tag.id} hover>
                                    <TableCell>{tag.tagName}</TableCell>
                                    <TableCell align="right">
                                        <IconButton size="small" onClick={() => handleOpenEdit(tag)} sx={{ mr: 1 }}>
                                            <EditIcon fontSize="small" color="primary" />
                                        </IconButton>
                                        <IconButton size="small" onClick={() => handleDelete(tag.id)}>
                                            <DeleteIcon fontSize="small" color="error" />
                                        </IconButton>
                                    </TableCell>
                                </TableRow>
                            ))
                        ) : (
                            <TableRow>
                                <TableCell colSpan={2} align="center" sx={{ py: 3, color: 'text.secondary' }}>
                                    No tags found.
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
            </TableContainer>

            <Dialog
                open={open}
                onClose={handleClose}
                fullWidth
                maxWidth="xs"
                PaperProps={{ sx: { borderRadius: '20px', p: 1 } }}
            >
                <DialogTitle sx={{ fontWeight: 700, fontSize: '1.25rem', pb: 1 }}>
                    {isEdit ? 'Edit Tag' : 'New Tag'}
                </DialogTitle>
                <DialogContent sx={{ p: 3, pt: 1 }}>
                    <Box component="form" sx={{ mt: 1 }}>
                        <TextField
                            autoFocus
                            label="Tag Name"
                            value={tagName}
                            onChange={(e) => setTagName(e.target.value)}
                            fullWidth
                            variant="outlined"
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

            <Snackbar
                open={alert.open}
                autoHideDuration={4000}
                onClose={() => setAlert({ ...alert, open: false })}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
            >
                <Alert
                    onClose={() => setAlert({ ...alert, open: false })}
                    severity={alert.severity}
                    variant="filled"
                    sx={{ width: '100%', borderRadius: 3 }}
                >
                    {alert.message}
                </Alert>
            </Snackbar>
        </Box>
    );
};

export default TagManager;
