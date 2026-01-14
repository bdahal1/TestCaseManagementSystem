import React, { useState, useEffect } from "react";
import { styled, alpha, useTheme } from "@mui/material/styles";
import {
    Avatar,
    Box,
    CssBaseline,
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
    useMediaQuery
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

const drawerWidth = 260;

const Main = styled("main", { shouldForwardProp: (prop) => prop !== "open" })<{
    open?: boolean;
}>(({ theme, open }) => ({
    flexGrow: 1,
    padding: theme.spacing(4),
    transition: theme.transitions.create(["margin", "width"], {
        easing: theme.transitions.easing.sharp,
        duration: theme.transitions.duration.leavingScreen,
    }),
    marginLeft: `-${drawerWidth}px`,
    width: `calc(100% - ${drawerWidth}px)`,
    [theme.breakpoints.down('sm')]: {
        marginLeft: 0,
        width: '100%',
    },
    ...(open && {
        transition: theme.transitions.create(["margin", "width"], {
            easing: theme.transitions.easing.easeOut,
            duration: theme.transitions.duration.enteringScreen,
        }),
        marginLeft: 0,
    }),
}));

const AppBar = styled(MuiAppBar, {
    shouldForwardProp: (prop) => prop !== "open",
})<MuiAppBarProps & { open?: boolean }>(({ theme, open }) => ({
    background: alpha(theme.palette.background.default, 0.8),
    backdropFilter: 'blur(12px)',
    boxShadow: 'none',
    borderBottom: `1px solid ${alpha(theme.palette.divider, 0.1)}`,
    color: theme.palette.text.primary,
    transition: theme.transitions.create(["width", "margin"], {
        easing: theme.transitions.easing.sharp,
        duration: theme.transitions.duration.leavingScreen,
    }),
    ...(open && {
        marginLeft: drawerWidth,
        width: `calc(100% - ${drawerWidth}px)`,
        transition: theme.transitions.create(["width", "margin"], {
            easing: theme.transitions.easing.easeOut,
            duration: theme.transitions.duration.enteringScreen,
        }),
    }),
}));

const DrawerHeader = styled("div")(({ theme }) => ({
    display: "flex",
    alignItems: "center",
    padding: theme.spacing(0, 2),
    ...theme.mixins.toolbar,
    justifyContent: "space-between",
}));

const StyledDrawer = styled(Drawer)(({ theme }) => ({
    width: drawerWidth,
    flexShrink: 0,
    "& .MuiDrawer-paper": {
        width: drawerWidth,
        boxSizing: "border-box",
        borderRight: 'none',
        backgroundColor: theme.palette.background.paper,
        boxShadow: '4px 0 24px rgba(0,0,0,0.02)',
    },
}));

const NavItemButton = styled(ListItemButton)<{ selected?: boolean }>(({ theme, selected }) => ({
    borderRadius: theme.shape.borderRadius,
    margin: '4px 12px',
    padding: '10px 16px',
    transition: 'all 0.2s',
    backgroundColor: selected ? alpha(theme.palette.primary.main, 0.08) : 'transparent',
    color: selected ? theme.palette.primary.main : theme.palette.text.secondary,
    '&:hover': {
        backgroundColor: selected
            ? alpha(theme.palette.primary.main, 0.12)
            : alpha(theme.palette.text.primary, 0.04),
        transform: 'translateX(4px)',
    },
    '& .MuiListItemIcon-root': {
        color: selected ? theme.palette.primary.main : theme.palette.text.secondary,
        minWidth: 40,
    },
    '& .MuiTypography-root': {
        fontWeight: selected ? 600 : 500,
        fontSize: '0.95rem',
    }
}));

const DashboardLayout: React.FC = () => {
    const theme = useTheme();
    const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
    const { logout, user } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();

    // Default to open on desktop, closed on mobile
    const [open, setOpen] = useState(!isMobile);

    // Sync state with media query only on mount/change of screen size if needed,
    // but typically we let user control it. 
    // Effect to auto-close on mobile route change
    useEffect(() => {
        if (isMobile) setOpen(false);
    }, [location.pathname, isMobile]);


    const projectIdMatch = location.pathname.match(/\/project\/(\d+)/);
    const projectId = projectIdMatch ? parseInt(projectIdMatch[1], 10) : null;

    const [anchorElUser, setAnchorElUser] = useState<null | HTMLElement>(null);
    const [projectName, setProjectName] = useState('');

    const fullName = user?.fullName || "User";
    const photoURL = user?.photoURL || "https://i.pravatar.cc/100";
    const roleList = user?.roleList || [];
    const isQAManager = roleList.some(role => role === "ROLE_QAManager" || role === "ROLE_ADMIN");

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
            if (projectId && user?.userId) {
                try {
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
        <Box sx={{ display: "flex", backgroundColor: 'background.default', minHeight: '100vh' }}>
            <CssBaseline />
            <AppBar position="fixed" open={open}>
                <Toolbar sx={{ minHeight: 70 }}>
                    <IconButton
                        color="inherit"
                        aria-label="open drawer"
                        onClick={handleDrawerToggle}
                        edge="start"
                        sx={{ mr: 2, ...(open && { display: 'none' }) }}
                    >
                        <MenuIcon />
                    </IconButton>

                    <Box sx={{ display: 'flex', alignItems: 'center', flexGrow: 1 }}>
                        <Typography variant="h6" noWrap component="div" sx={{
                            fontWeight: 700,
                            background: `linear-gradient(45deg, ${theme.palette.primary.main}, ${theme.palette.secondary.main})`,
                            backgroundClip: "text",
                            textFillColor: "transparent",
                            WebkitBackgroundClip: "text",
                            WebkitTextFillColor: "transparent"
                        }}>
                            {projectName ? `${projectName}  /  ` : ""} GN-TMS
                        </Typography>
                    </Box>

                    <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
                        <Tooltip title="Account settings">
                            <Box
                                onClick={handleOpenUserMenu}
                                sx={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: 1.5,
                                    cursor: 'pointer',
                                    padding: '6px 12px',
                                    borderRadius: '50px',
                                    transition: 'all 0.2s',
                                    '&:hover': { backgroundColor: alpha(theme.palette.text.primary, 0.05) }
                                }}
                            >
                                <Typography variant="subtitle2" sx={{ display: { xs: 'none', sm: 'block' } }}>
                                    {fullName}
                                </Typography>
                                <Avatar
                                    alt={fullName}
                                    src={photoURL}
                                    sx={{ width: 36, height: 36, boxShadow: '0 2px 8px rgba(0,0,0,0.1)' }}
                                />
                            </Box>
                        </Tooltip>
                        <Menu
                            sx={{ mt: '45px' }}
                            id="menu-appbar"
                            anchorEl={anchorElUser}
                            anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
                            keepMounted
                            transformOrigin={{ vertical: 'top', horizontal: 'right' }}
                            open={Boolean(anchorElUser)}
                            onClose={handleCloseUserMenu}
                            PaperProps={{
                                elevation: 0,
                                sx: {
                                    overflow: 'visible',
                                    filter: 'drop-shadow(0px 2px 8px rgba(0,0,0,0.15))',
                                    mt: 1.5,
                                },
                            }}
                        >
                            {isQAManager && (
                                <MenuItem onClick={() => { handleCloseUserMenu(); navigate("/dashboard/admin"); }}>
                                    <ListItemIcon><SettingsIcon fontSize="small" /></ListItemIcon>
                                    <Typography variant="body2">Admin Settings</Typography>
                                </MenuItem>
                            )}
                            <MenuItem onClick={handleLogoutClick} sx={{ color: 'error.main' }}>
                                <ListItemIcon><LogoutIcon fontSize="small" color="error" /></ListItemIcon>
                                <Typography variant="body2">Logout</Typography>
                            </MenuItem>
                        </Menu>
                    </Box>
                </Toolbar>
            </AppBar>

            <StyledDrawer
                variant={isMobile ? "temporary" : "persistent"}
                anchor="left"
                open={open}
                onClose={isMobile ? handleDrawerToggle : undefined}
            >
                <DrawerHeader>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, px: 1 }}>
                        <Box
                            component="img"
                            src="/src/assets/images.jpg"
                            alt="Logo"
                            sx={{
                                height: 32,
                                width: 32,
                                borderRadius: '10px',
                                boxShadow: '0 4px 12px rgba(99, 102, 241, 0.3)'
                            }}
                        />
                        <Typography variant="h6" fontWeight="bold" color="text.primary">
                            GN-Test
                        </Typography>
                    </Box>
                    <IconButton onClick={handleDrawerToggle}>
                        <ChevronLeftIcon />
                    </IconButton>
                </DrawerHeader>

                <Box sx={{ mt: 2 }}>
                    <List>
                        {menuItems.map(({ label, icon, path }) => (
                            <ListItem key={label} disablePadding>
                                <NavItemButton
                                    selected={location.pathname === path}
                                    onClick={() => navigate(path)}
                                >
                                    <ListItemIcon>{icon}</ListItemIcon>
                                    <ListItemText primary={label} />
                                </NavItemButton>
                            </ListItem>
                        ))}
                    </List>
                </Box>
            </StyledDrawer>

            <Main open={open}>
                <DrawerHeader />
                {/* Fade in animation for route transitions could be added here */}
                <Box sx={{ maxWidth: 1600, mx: 'auto', animation: 'fadeIn 0.5s ease-in-out' }}>
                    <style>
                        {`@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }`}
                    </style>
                    <Outlet />
                </Box>
            </Main>
        </Box>
    );
};

export default DashboardLayout;
