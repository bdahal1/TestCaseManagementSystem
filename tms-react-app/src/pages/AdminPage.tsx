import React, { useState } from 'react';
import { Box } from '@mui/material';
import AdminNavigationPanel from '../components/admin/AdminNavigationPanel';
import UserComponent from '../components/users/UserManager';
import RoleComponent from '../components/roles/RoleManager';
import DepartmentComponent from '../components/departments/DepartmentManager';
import ProjectComponent from '../components/projects/ProjectManager';
import TagManager from '../components/tags/TagManager';

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
                {currentView === 'Tags' && <TagManager />}
            </Box>
        </Box>
    );
};

export default AdminPage;
