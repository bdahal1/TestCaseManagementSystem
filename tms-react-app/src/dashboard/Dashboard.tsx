import React, {useEffect, useState} from "react";
import {styled} from "@mui/material/styles";
import {useNavigate} from "react-router-dom";
import {
    Alert,
    ButtonBase,
    Card,
    CardContent,
    Box,
    Drawer,
    CssBaseline,
    Toolbar,
    Typography,
    Divider,
    IconButton,
    List,
    ListItem,
    ListItemButton,
    ListItemText,
    Avatar,
    Menu,
    MenuItem,
    Tooltip
} from "@mui/material";
import {Grid} from "@mui/material";
import MuiAppBar, {AppBarProps as MuiAppBarProps} from "@mui/material/AppBar";
import MenuIcon from "@mui/icons-material/Menu";
import ChevronLeftIcon from "@mui/icons-material/ChevronLeft";
import axios from "axios";

import UserComponent from "./user/UserComponent.tsx";
import RoleComponent from "./role/RoleComponent.tsx";
import DepartmentComponent from "./department/DepartmentComponent.tsx";
import ProjectComponent from "./project/ProjectComponent.tsx";
import TestCaseComponent from "./testCase/TestCaseComponent.tsx";
import Snackbar from '@mui/material/Snackbar';
import TestFolderComponent from "./testFolder/TestFolderComponent.tsx";

const drawerWidth = 240;

const Main = styled("main", {shouldForwardProp: (prop) => prop !== "open"})<{
    open?: boolean;
}>(({theme, open}) => ({
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

interface AppBarProps extends MuiAppBarProps {
    open?: boolean;
}

const AppBar = styled(MuiAppBar, {
    shouldForwardProp: (prop) => prop !== "open",
})<AppBarProps>(({theme, open}) => ({
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

const Dashboard: React.FC<{ onLogout: () => void }> = ({onLogout}) => {
    const navigate = useNavigate();
    const [open, setOpen] = useState(false);
    const [showAlert, setShowAlert] = useState(false);
    const [view, setView] = useState("Dashboard");
    const [projects, setProjects] = useState<Project[]>([]);
    const [selectedProjectId, setSelectedProjectId] = useState<number | null>(null);
    const [anchorElUser, setAnchorElUser] = useState<null | HTMLElement>(null);

    const fullName = localStorage.getItem("fullName") || "John Doe";
    const photoURL = localStorage.getItem("photoURL") || "https://i.pravatar.cc/100";
    const roleListString = localStorage.getItem("roleList") || "[]";

    let roleList = [];
    try {
        roleList = JSON.parse(roleListString);
        console.log(roleList)
    } catch (e) {
        console.error("Failed to parse roleList from localStorage", e);
    }
    const isQAManager = roleList.includes("ROLE_QAManager");
    console.log("Is this QA Manager : " + isQAManager)

    const handleDrawerToggle = () => setOpen(!open);

    const handleLogout = async () => {
        try {
            await axios.post("http://localhost:8080/dhtcms/api/v1/logout", {}, {withCredentials: true});
            localStorage.setItem("isLoggedIn", "false");
            onLogout();
        } catch (err: any) {
            console.error("Error during API call:", err);
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
                const response = await axios.get(
                    `http://localhost:8080/dhtcms/api/v1/users/${userId}/projects`,
                    {
                        headers: {
                            Authorization: `Bearer ` + localStorage.getItem("authToken"),
                        },
                    }
                );
                setProjects(response.data);
            } catch (error) {
                console.error("Error fetching projects:", error);
            }
        };

        fetchProjects();
        const isLoggedIn = localStorage.getItem("isLoggedIn");
        if (isLoggedIn === "true") {
            setShowAlert(true);
            const timeId = setTimeout(() => setShowAlert(false), 3000);
            return () => clearTimeout(timeId);
        }
        localStorage.removeItem("isLoggedIn");
    }, [navigate]);

    return (
        <Box sx={{display: "flex"}}>
            <CssBaseline/>
            <AppBar position="fixed" open={open} sx={{background: "#1976d2", boxShadow: 3}}>
                <Toolbar>
                    <IconButton color="inherit" onClick={handleDrawerToggle} edge="start">
                        <MenuIcon/>
                    </IconButton>
                    <Box component="img" src="src/assets/images.jpg" alt="GN-Test Logo"
                         sx={{height: 40, width: 40, ml: 2, mr: 2, borderRadius: '50%'}}/>
                    <Typography variant="h6" noWrap sx={{fontWeight: "bold", color: "#fff"}}>
                        {view}
                    </Typography>
                    <Box sx={{marginLeft: "auto", display: "flex", alignItems: "center"}}>
                        <Tooltip title="Open profile menu">
                            <Box onClick={handleOpenUserMenu}
                                 sx={{cursor: "pointer", display: "flex", alignItems: "center", gap: 1, color: "#fff"}}>
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

            <Drawer variant="persistent" anchor="left" open={open} sx={{
                width: drawerWidth,
                flexShrink: 0,
                '& .MuiDrawer-paper': {width: drawerWidth, boxSizing: 'border-box'}
            }}>
                <DrawerHeader>
                    <Box component="img" src="src/assets/images.jpg" alt="GN-Test Logo"
                         sx={{height: 40, width: 40, ml: 2, mr: 2, borderRadius: '50%'}}/>
                    <Typography variant="subtitle1" fontWeight="bold">GN-Test</Typography>
                    <IconButton onClick={handleDrawerToggle} sx={{color: "white"}}>
                        <ChevronLeftIcon/>
                    </IconButton>
                </DrawerHeader>
                <List>
                    {["Dashboard", ...(isQAManager ? ["Users", "Roles", "Department", "Project"] : []), ...(selectedProjectId !== null ? ["TestCase", "Test Folders"] : [])].map((text) => (
                        <ListItem key={text} disablePadding>
                            <ListItemButton onClick={() => setView(text)}>
                                <ListItemText primary={text}/>
                            </ListItemButton>
                        </ListItem>
                    ))}
                </List>
                <Divider/>
            </Drawer>

            <Main open={open} sx={{backgroundColor: "#f9fbfc", minHeight: "100vh"}}>
                <DrawerHeader/>
                <Snackbar open={showAlert} autoHideDuration={3000} onClose={() => setShowAlert(false)}
                          anchorOrigin={{vertical: 'top', horizontal: 'right'}}>
                    <Alert onClose={() => setShowAlert(false)} severity="success" variant="filled" sx={{width: '100%'}}>
                        Login successful!
                    </Alert>
                </Snackbar>
                {view === "Users" && isQAManager && <UserComponent/>}
                {view === "Roles" && isQAManager && <RoleComponent/>}
                {view === "Department" && isQAManager && <DepartmentComponent/>}
                {view === "Project" && isQAManager && <ProjectComponent/>}
                {view === "Test Folders" && selectedProjectId !== null && <TestFolderComponent projId={selectedProjectId}/>}
                {view === "TestCase" && selectedProjectId !== null && <TestCaseComponent projId={selectedProjectId}/>}

                {view === "Dashboard" && (
                    <Grid container spacing={4} sx={{padding: 4, justifyContent: "center", alignItems: "center"}}>
                        {projects.map((project) => (
                            <Grid item xs={12} sm={6} md={4} lg={3} key={project.id}>
                                <ButtonBase onClick={() => {
                                    setSelectedProjectId(project.id);
                                    setView("TestCase");
                                }} sx={{width: "100%"}}>
                                    <Card sx={{
                                        width: "100%",
                                        height: 180,
                                        borderRadius: 4,
                                        background: "linear-gradient(135deg, #e3f2fd, #ffffff)",
                                        boxShadow: 3,
                                        transition: "transform 0.2s, box-shadow 0.2s",
                                        '&:hover': {transform: "translateY(-6px)", boxShadow: 6},
                                        display: "flex",
                                        flexDirection: "column",
                                        justifyContent: "center",
                                        px: 3,
                                        py: 2
                                    }}>
                                        <CardContent>
                                            <Typography variant="h5" fontWeight="bold"
                                                        color="primary">{project.projectInitials}</Typography>
                                            <Typography variant="subtitle1"
                                                        color="text.secondary">{project.projectName}</Typography>
                                        </CardContent>
                                    </Card>
                                </ButtonBase>
                            </Grid>
                        ))}
                    </Grid>
                )}
            </Main>
        </Box>
    );
};

export default Dashboard;
