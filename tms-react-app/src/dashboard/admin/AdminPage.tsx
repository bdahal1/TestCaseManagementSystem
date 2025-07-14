import React, { useState } from 'react';
import { Box } from '@mui/material';
import AdminNavigationPanel from './AdminNavigationPanel.tsx';
import UserComponent from '../user/UserComponent';
import RoleComponent from '../role/RoleComponent';
import DepartmentComponent from '../department/DepartmentComponent';
import ProjectComponent from '../project/ProjectComponent';

const AdminPage: React.FC = () => {
    const [currentView, setCurrentView] = useState('Users');

    return (
        <Box display="flex">
            <AdminNavigationPanel onSelect={setCurrentView} currentView={currentView} />
            <Box sx={{ flexGrow: 1, p: 3 }}>
                {currentView === 'Users' && <UserComponent />}
                {currentView === 'Roles' && <RoleComponent />}
                {currentView === 'Department' && <DepartmentComponent />}
                {currentView === 'Project' && <ProjectComponent />}
            </Box>
        </Box>
    );
};

export default AdminPage;
