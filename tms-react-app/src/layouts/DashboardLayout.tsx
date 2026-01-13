import React, { useState, useEffect } from "react";
import { styled } from "@mui/material/styles";
import {
    Avatar,
    Box,
    CssBaseline,
    Divider,
    Drawer,
    IconButton,
    List,
    ListItem,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    Menu,
    MenuItem,
    Toolbar,
    Tooltip,
    Typography,
} from "@mui/material";
import MuiAppBar, { AppBarProps as MuiAppBarProps } from "@mui/material/AppBar";
import MenuIcon from "@mui/icons-material/Menu";
import ChevronLeftIcon from "@mui/icons-material/ChevronLeft";
import DashboardIcon from '@mui/icons-material/Dashboard';
import FolderIcon from '@mui/icons-material/Folder';
import PlayCircleFilledIcon from '@mui/icons-material/PlayCircleFilled';
import LogoutIcon from '@mui/icons-material/Logout';
import SettingsIcon from '@mui/icons-material/Settings';
import { Outlet, useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import api from "../services/api";

const drawerWidth = 240;

const Main = styled("main", {
    shouldForwardProp: (prop) => prop !== "open",
})<{ open?: boolean }>(({ theme, open }) => ({
    flexGrow: 1,
    padding: theme.spacing(3),
    transition: theme.transitions.create("margin", {
        easing: theme.transitions.easing.sharp,
        duration: theme.transitions.duration.leavingScreen,
    }),
    marginLeft: `-${drawerWidth}px`,
    ...(open && {
        transition: theme.transitions.create("margin", {
            easing: theme.transitions.easing.easeOut,
            duration: theme.transitions.duration.enteringScreen,
        }),
        marginLeft: 0,
    }),
}));

const AppBar = styled(MuiAppBar, {
    shouldForwardProp: (prop) => prop !== "open",
})<MuiAppBarProps & { open?: boolean }>(({ theme, open }) => ({
    transition: theme.transitions.create(["margin", "width"], {
        easing: theme.transitions.easing.sharp,
        duration: theme.transitions.duration.leavingScreen,
    }),
    ...(open && {
        width: `calc(100% - ${drawerWidth}px)`,
        marginLeft: `${drawerWidth}px`,
        transition: theme.transitions.create(["margin", "width"], {
            easing: theme.transitions.easing.easeOut,
            duration: theme.transitions.duration.enteringScreen,
        }),
    }),
}));

const DrawerHeader = styled("div")(({ theme }) => ({
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    padding: theme.spacing(1.5, 2),
    backgroundColor: theme.palette.primary.main,
    color: theme.palette.common.white,
}));

const DashboardLayout: React.FC = () => {
    const { logout, user } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();

    // Actually, useParams in Layout works if the route structure captures it, or we use matchPath.
    // But since DashboardLayout is at '/dashboard', it might not see child params directly continuously if not part of its own path definition.
    // However, in RRD v6, useParams returns params from the currently matched URL.

    // We need to extract projectId from the URL manually if useParams doesn't give it (because Layout is rendered at parent path).
    // Let's use useLocation and regex or matchPath to be safe, or assume useParams works.
    // Actually, getting useParams in a parent route only works for params defined in the parent route.
    // If we define routes like <Route path="project/:projectId" element={<...>} />, the parent layout might not see it if it's just <Route path="dashboard" element={<Layout />}>.
    // Let's verify this handling. Typically we need a wrapper or hook. 
    // Easier: split path from location.pathname.

    const projectIdMatch = location.pathname.match(/\/project\/(\d+)/);
    const projectId = projectIdMatch ? parseInt(projectIdMatch[1], 10) : null;

    const [open, setOpen] = useState(false);
    const [anchorElUser, setAnchorElUser] = useState<null | HTMLElement>(null);
    const [projectName, setProjectName] = useState('');

    const fullName = user?.fullName || "User";
    const photoURL = user?.photoURL || "https://i.pravatar.cc/100";
    const roleList = user?.roleList || [];
    const isQAManager = roleList.includes("ROLE_QAManager");

    const handleDrawerToggle = () => setOpen((prev) => !prev);
    const handleOpenUserMenu = (event: React.MouseEvent<HTMLElement>) => setAnchorElUser(event.currentTarget);
    const handleCloseUserMenu = () => setAnchorElUser(null);

    const handleLogoutClick = async () => {
        try {
            await api.post(`/logout`, {}, { withCredentials: true });
        } catch (error) {
            console.error("Logout failed", error);
        } finally {
            logout();
            handleCloseUserMenu();
            navigate("/login");
        }
    };

    useEffect(() => {
        const fetchProjectName = async () => {
            if (projectId) {
                try {
                    // We might need an endpoint to get single project or just get all and find.
                    // The API structure suggests /users/{id}/projects or /projects/{id}. 
                    // Let's assume fetching all projects is cached or fast enough, OR try to fetch individual.
                    // ProjectComponent uses `api.get(API_URL)` which lists projects.
                    // Let's assume we can fetch specific project or just find it from list.
                    // Optimisation: separate endpoint.
                    // For now, let's just fetch the specific one if possible. 
                    // Checking axiosInstance or ProjectComponent... ProjectComponent uses /dhtcms/api/v1/testProjects (inferred).
                    // Actually `ProjectComponent` has `API_URL = '/dhtcms/api/v1/testProjects'`.
                    // It has fetchProjects call.
                    // Let's try `api.get('/dhtcms/api/v1/testProjects/id/' + projectId)`? 
                    // TestFolderComponent fetches `api.get('/dhtcms/api/v1/testFolders/project/' + projId)`.
                    // Let's just use the `useAuth` user projects endpoint used in ProjectList logic for consistency: `/users/${user.userId}/projects`.
                    if (!user?.userId) return;
                    const { data } = await api.get(`/users/${user.userId}/projects`);
                    const proj = data.find((p: any) => p.id === projectId);
                    if (proj) setProjectName(proj.projectName);
                } catch (e) {
                    console.error("Failed to fetch project info", e);
                }
            } else {
                setProjectName('');
            }
        };
        fetchProjectName();
    }, [projectId, user?.userId]);

    const menuItems = [
        { label: "Dashboard", icon: <DashboardIcon />, path: "/dashboard" },
        ...(projectId ? [
            { label: "Test Cases", icon: <FolderIcon />, path: `/dashboard/project/${projectId}/test-cases` },
            { label: "Test Folders", icon: <FolderIcon />, path: `/dashboard/project/${projectId}/test-folders` },
            { label: "Test Executions", icon: <PlayCircleFilledIcon />, path: `/dashboard/project/${projectId}/test-executions` }
        ] : []),
    ];

    return (
        <Box sx={{ display: "flex" }}>
            <CssBaseline />
            <AppBar position="fixed" open={open} sx={{ background: "#1976d2", boxShadow: 3 }}>
                <Toolbar>
                    <IconButton color="inherit" onClick={handleDrawerToggle} edge="start" sx={{ mr: 2, ...(open && { display: 'none' }) }}>
                        <MenuIcon />
                    </IconButton>
                    <Box component="img" src="/src/assets/images.jpg" alt="GN-Test Logo" sx={{ height: 40, width: 40, mr: 2, borderRadius: "50%" }} />
                    <Typography variant="h6" noWrap sx={{ fontWeight: "bold", color: "#fff", flexGrow: 1 }}>
                        {projectName ? `${projectName} / ` : ""} GN-Tms
                    </Typography>
                    <Box sx={{ display: "flex", alignItems: "center" }}>
                        <Tooltip title="Open profile menu">
                            <Box onClick={handleOpenUserMenu} sx={{ cursor: "pointer", display: "flex", alignItems: "center", gap: 1, color: "#fff" }}>
                                <Avatar alt={fullName} src={photoURL} />
                                <Typography sx={{ fontWeight: "medium", display: { xs: 'none', sm: 'block' } }}>{fullName}</Typography>
                            </Box>
                        </Tooltip>
                        <Menu anchorEl={anchorElUser} open={Boolean(anchorElUser)} onClose={handleCloseUserMenu}>
                            {isQAManager && (
                                <MenuItem onClick={() => { handleCloseUserMenu(); navigate("/dashboard/admin"); }}>
                                    <ListItemIcon><SettingsIcon fontSize="small" /></ListItemIcon>
                                    <Typography variant="subtitle1">Admin Settings</Typography>
                                </MenuItem>
                            )}
                            <MenuItem onClick={handleLogoutClick}>
                                <ListItemIcon><LogoutIcon fontSize="small" /></ListItemIcon>
                                Logout
                            </MenuItem>
                        </Menu>
                    </Box>
                </Toolbar>
            </AppBar>
            <Drawer
                variant="persistent"
                anchor="left"
                open={open}
                sx={{
                    width: drawerWidth,
                    flexShrink: 0,
                    "& .MuiDrawer-paper": { width: drawerWidth, boxSizing: "border-box" },
                }}
            >
                <DrawerHeader>
                    <Box component="img" src="/src/assets/images.jpg" alt="Logo" sx={{ height: 32, width: 32, mr: 1, borderRadius: "50%" }} />
                    <Typography variant="subtitle1" fontWeight="bold">GN-Test</Typography>
                    <IconButton onClick={handleDrawerToggle} sx={{ color: "white", ml: 'auto' }}>
                        <ChevronLeftIcon />
                    </IconButton>
                </DrawerHeader>
                <Divider />
                <List>
                    {menuItems.map(({ label, icon, path }) => (
                        <ListItem key={label} disablePadding>
                            <ListItemButton
                                selected={location.pathname === path}
                                onClick={() => navigate(path)}
                            >
                                <ListItemIcon>{icon}</ListItemIcon>
                                <ListItemText primary={label} />
                            </ListItemButton>
                        </ListItem>
                    ))}
                </List>
                <Divider />
            </Drawer>
            <Main open={open} sx={{ backgroundColor: "#f9fbfc", minHeight: "100vh" }}>
                <DrawerHeader />
                <Outlet />
            </Main>
        </Box>
    );
};

export default DashboardLayout;
