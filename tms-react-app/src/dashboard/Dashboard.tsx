import React, {useEffect, useState} from "react";
import {styled} from "@mui/material/styles";
import {
    Alert,
    Avatar,
    Box,
    Card,
    CssBaseline,
    Divider,
    Drawer,
    IconButton,
    List,
    ListItem,
    ListItemButton,
    ListItemText,
    Menu,
    MenuItem,
    Snackbar,
    Toolbar,
    Tooltip,
    Typography,
} from "@mui/material";
import MuiAppBar, {AppBarProps as MuiAppBarProps} from "@mui/material/AppBar";
import MenuIcon from "@mui/icons-material/Menu";
import ChevronLeftIcon from "@mui/icons-material/ChevronLeft";
import {useNavigate} from "react-router-dom";
import axios from "axios";

import UserComponent from "./user/UserComponent";
import RoleComponent from "./role/RoleComponent";
import DepartmentComponent from "./department/DepartmentComponent";
import ProjectComponent from "./project/ProjectComponent";
import TestCaseComponent from "./testCase/TestCaseComponent";
import TestFolderComponent from "./testFolder/TestFolderComponent";
import TestExecutionComponent from "./testExecution/TestExecutionComponent.tsx";

const drawerWidth = 240;

const Main = styled("main", {
    shouldForwardProp: (prop) => prop !== "open",
})<{ open?: boolean }>(({theme, open}) => ({
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
})<MuiAppBarProps & { open?: boolean }>(({theme, open}) => ({
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

const DrawerHeader = styled("div")(({theme}) => ({
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    padding: theme.spacing(1.5, 2),
    backgroundColor: theme.palette.primary.main,
    color: theme.palette.common.white,
}));

interface Project {
    id: number;
    projectInitials: string;
    projectName: string;
}

const API_BASE_URL = "http://localhost:8080/dhtcms/api/v1";

const Dashboard: React.FC<{ onLogout: () => void }> = ({onLogout}) => {
    const navigate = useNavigate();
    const [open, setOpen] = useState(false);
    const [showAlert, setShowAlert] = useState(false);
    const [currentView, setCurrentView] = useState("Dashboard");
    const [projects, setProjects] = useState<Project[]>([]);
    const [selectedProjectId, setSelectedProjectId] = useState<number | null>(null);
    const [anchorElUser, setAnchorElUser] = useState<null | HTMLElement>(null);
    const [selectedProjectName, setSelectedProjectName] = useState('');
    const fullName = localStorage.getItem("fullName") || "John Doe";
    const photoURL = localStorage.getItem("photoURL") || "https://i.pravatar.cc/100";
    const roleList: string[] = JSON.parse(localStorage.getItem("roleList") || "[]");
    const isQAManager = roleList.includes("ROLE_QAManager");

    const handleDrawerToggle = () => setOpen((prev) => !prev);

    const handleLogout = async () => {
        try {
            await axios.post(`${API_BASE_URL}/logout`, {}, {withCredentials: true});
            localStorage.setItem("isLoggedIn", "false");
            onLogout();
        } catch (err) {
            console.error("Error during logout:", err);
        }
    };

    const handleOpenUserMenu = (event: React.MouseEvent<HTMLElement>) => setAnchorElUser(event.currentTarget);
    const handleCloseUserMenu = () => setAnchorElUser(null);
    const handleLogoutClick = () => {
        handleLogout().then();
        handleCloseUserMenu();
    };

    useEffect(() => {
        const fetchProjects = async () => {
            try {
                const userId = localStorage.getItem("userId");
                const token = localStorage.getItem("authToken");
                const {data} = await axios.get(
                    `${API_BASE_URL}/users/${userId}/projects`,
                    {headers: {Authorization: `Bearer ${token}`}}
                );
                setProjects(data);
            } catch (error) {
                console.error("Error fetching projects:", error);
            }
        };

        fetchProjects().then();

        if (localStorage.getItem("isLoggedIn") === "true") {
            setShowAlert(true);
            const timer = setTimeout(() => setShowAlert(false), 3000);
            return () => clearTimeout(timer);
        }

        localStorage.removeItem("isLoggedIn");
    }, [navigate]);

    const menuItems = [
        "Dashboard",
        ...(isQAManager ? ["Users", "Roles", "Department", "Project"] : []),
        ...(selectedProjectId !== null ? ["Test Cases", "Test Folders", "Test Executions"] : []),
    ];

    return (
        <Box sx={{display: "flex"}}>
            <CssBaseline/>
            <AppBar position="fixed" open={open} sx={{background: "#1976d2", boxShadow: 3}}>
                <Toolbar>
                    <IconButton color="inherit" onClick={handleDrawerToggle} edge="start">
                        <MenuIcon/>
                    </IconButton>
                    <Box
                        component="img"
                        src="src/assets/images.jpg"
                        alt="GN-Test Logo"
                        sx={{height: 40, width: 40, ml: 2, mr: 2, borderRadius: "50%"}}
                    />

                    <Typography variant="h6" noWrap sx={{fontWeight: "bold", color: "#fff"}}>
                        {selectedProjectName?selectedProjectName+" / ":""}{currentView}
                    </Typography>
                    <Box sx={{marginLeft: "auto", display: "flex", alignItems: "center"}}>
                        <Tooltip title="Open profile menu">
                            <Box
                                onClick={handleOpenUserMenu}
                                sx={{cursor: "pointer", display: "flex", alignItems: "center", gap: 1, color: "#fff"}}
                            >
                                <Avatar alt={fullName} src={photoURL}/>
                                <Typography sx={{fontWeight: "medium"}}>{fullName}</Typography>
                            </Box>
                        </Tooltip>
                        <Menu anchorEl={anchorElUser} open={Boolean(anchorElUser)} onClose={handleCloseUserMenu}>
                            <MenuItem onClick={handleLogoutClick}>
                                <Typography textAlign="center">Logout</Typography>
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
                    "& .MuiDrawer-paper": {width: drawerWidth, boxSizing: "border-box"},
                }}
            >
                <DrawerHeader>
                    <Box
                        component="img"
                        src="src/assets/images.jpg"
                        alt="GN-Test Logo"
                        sx={{height: 40, width: 40, ml: 2, mr: 2, borderRadius: "50%"}}
                    />
                    <Typography variant="subtitle1" fontWeight="bold">
                        GN-Test
                    </Typography>
                    <IconButton onClick={handleDrawerToggle} sx={{color: "white"}}>
                        <ChevronLeftIcon/>
                    </IconButton>
                </DrawerHeader>
                <List>
                    {menuItems.map((item) => (
                        <ListItem key={item} disablePadding>
                            <ListItemButton onClick={() => setCurrentView(item)}>
                                <ListItemText primary={item}/>
                            </ListItemButton>
                        </ListItem>
                    ))}
                </List>
                <Divider/>
            </Drawer>

            <Main open={open} sx={{backgroundColor: "#f9fbfc", minHeight: "100vh"}}>
                <DrawerHeader/>
                <Snackbar
                    open={showAlert}
                    autoHideDuration={3000}
                    onClose={() => setShowAlert(false)}
                    anchorOrigin={{vertical: "top", horizontal: "right"}}
                >
                    <Alert onClose={() => setShowAlert(false)} severity="success" variant="filled" sx={{width: "100%"}}>
                        Login successful!
                    </Alert>
                </Snackbar>

                {currentView === "Users" && isQAManager && <UserComponent/>}
                {currentView === "Roles" && isQAManager && <RoleComponent/>}
                {currentView === "Department" && isQAManager && <DepartmentComponent/>}
                {currentView === "Project" && isQAManager && <ProjectComponent/>}
                {currentView === "Test Folders" && selectedProjectId && (
                    <TestFolderComponent projId={selectedProjectId}/>
                )}
                {currentView === "Test Executions" && selectedProjectId && (
                    <TestExecutionComponent projId={selectedProjectId}/>
                )}
                {currentView === "Test Cases" && selectedProjectId && <TestCaseComponent projId={selectedProjectId}/>}

                {currentView === "Dashboard" && (
                    <Box
                        sx={{display: "flex", flexWrap: "wrap", gap: 2, justifyContent: "center", mt: 8, px: 2, pb: 4}}>
                        {projects.map((project) => (
                            <Card
                                key={project.id}
                                onClick={() => {
                                    setSelectedProjectId(project.id);
                                    setSelectedProjectName(project.projectName);
                                    setCurrentView("Test Cases");
                                }}
                                sx={{
                                    width: 260,
                                    height: 120,
                                    cursor: "pointer",
                                    padding: 2,
                                    display: "flex",
                                    flexDirection: "column",
                                    justifyContent: "center",
                                    alignItems: "center",
                                    backgroundColor: "#e3f2fd",
                                    "&:hover": {
                                        backgroundColor: "#bbdefb",
                                    },
                                }}
                            >
                                <Typography variant="h6" fontWeight="bold" color="primary">
                                    {project.projectInitials}
                                </Typography>
                                <Typography variant="body2" color="text.secondary">
                                    {project.projectName}
                                </Typography>
                            </Card>
                        ))}
                    </Box>
                )}
            </Main>
        </Box>
    );
};

export default Dashboard;
