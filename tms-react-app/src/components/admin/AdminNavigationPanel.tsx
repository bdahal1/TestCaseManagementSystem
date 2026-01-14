import React from 'react';
import {
    Box,
    Divider,
    List,
    ListItem,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    Typography,
    Paper,
    useTheme,
    alpha
} from '@mui/material';
import SettingsIcon from "@mui/icons-material/Settings";
import PeopleIcon from '@mui/icons-material/People';
import SecurityIcon from '@mui/icons-material/Security';
import BusinessIcon from '@mui/icons-material/Business';
import AssignmentIcon from '@mui/icons-material/Assignment';
import LabelIcon from '@mui/icons-material/Label';

interface AdminNavigationPanelProps {
    onSelect: (view: string) => void;
    currentView: string;
}

const ICON_MAP: Record<string, React.ReactElement> = {
    Users: <PeopleIcon />,
    Roles: <SecurityIcon />,
    Department: <BusinessIcon />,
    Project: <AssignmentIcon />,
    Tags: <LabelIcon />,
};

const AdminNavigationPanel: React.FC<AdminNavigationPanelProps> = ({ onSelect, currentView }) => {
    const theme = useTheme();

    return (
        <Paper
            elevation={0}
            sx={{
                width: 260,
                p: 2,
                mr: 3,
                height: 'fit-content',
                backgroundColor: 'background.paper',
                borderRadius: 2,
                border: `1px solid ${theme.palette.divider}`
            }}
        >
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 2, px: 1, pt: 1 }}>
                <SettingsIcon color="primary" />
                <Typography variant="h6" fontWeight="600">Admin</Typography>
            </Box>
            <Divider sx={{ mb: 2 }} />
            <List disablePadding>
                {Object.keys(ICON_MAP).map((item) => (
                    <ListItem key={item} disablePadding sx={{ mb: 0.5 }}>
                        <ListItemButton
                            selected={currentView === item}
                            onClick={() => onSelect(item)}
                            sx={{
                                borderRadius: 1,
                                '&.Mui-selected': {
                                    backgroundColor: alpha(theme.palette.primary.main, 0.1),
                                    color: theme.palette.primary.main,
                                    '&:hover': {
                                        backgroundColor: alpha(theme.palette.primary.main, 0.15),
                                    },
                                    '& .MuiListItemIcon-root': {
                                        color: theme.palette.primary.main,
                                    }
                                }
                            }}
                        >
                            <ListItemIcon sx={{ minWidth: 40 }}>{ICON_MAP[item]}</ListItemIcon>
                            <ListItemText
                                primary={item}
                                primaryTypographyProps={{
                                    fontWeight: currentView === item ? 600 : 500
                                }}
                            />
                        </ListItemButton>
                    </ListItem>
                ))}
            </List>
        </Paper>
    );
};

export default AdminNavigationPanel;
