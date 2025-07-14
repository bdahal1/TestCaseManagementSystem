import React from 'react';
import {
    Box,
    Divider,
    List,
    ListItem,
    ListItemButton, ListItemIcon,
    ListItemText,
    Typography
} from '@mui/material';
import SettingsIcon from "@mui/icons-material/Settings";
import PeopleIcon from '@mui/icons-material/People';
import SecurityIcon from '@mui/icons-material/Security';
import BusinessIcon from '@mui/icons-material/Business';
import AssignmentIcon from '@mui/icons-material/Assignment';

interface AdminNavigationPanelProps {
    onSelect: (view: string) => void;
    currentView: string;
}

const ICON_MAP: Record<string, React.ReactElement> = {
    Users: <PeopleIcon />,
    Roles: <SecurityIcon />,
    Department: <BusinessIcon />,
    Project: <AssignmentIcon />,
};

const AdminNavigationPanel: React.FC<AdminNavigationPanelProps> = ({ onSelect, currentView }) => {
    return (
        <Box sx={{ width: 240, p: 2 }}>
            <br/>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                <SettingsIcon fontSize="small" />
                <Typography variant="h6">Admin Settings</Typography>
            </Box>
            <Divider />
            <List>
                {Object.keys(ICON_MAP).map((item) => (
                    <ListItem key={item} disablePadding>
                        <ListItemButton
                            selected={currentView === item}
                            onClick={() => onSelect(item)}
                        >
                            <ListItemIcon>{ICON_MAP[item]}</ListItemIcon>
                            <ListItemText primary={item} />
                        </ListItemButton>
                    </ListItem>
                ))}
            </List>

        </Box>
    );
};

export default AdminNavigationPanel;
