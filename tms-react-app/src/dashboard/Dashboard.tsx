import React, {useEffect, useState} from "react";
import {styled} from "@mui/material/styles";
import {useNavigate} from "react-router-dom";
import {Alert, ButtonBase, Card, CardContent, Grid2, useTheme} from "@mui/material";
import axios from "axios";
import Box from "@mui/material/Box";
import Drawer from "@mui/material/Drawer";
import CssBaseline from "@mui/material/CssBaseline";
import MuiAppBar, {AppBarProps as MuiAppBarProps} from "@mui/material/AppBar";
import Toolbar from "@mui/material/Toolbar";
import List from "@mui/material/List";
import Typography from "@mui/material/Typography";
import Divider from "@mui/material/Divider";
import IconButton from "@mui/material/IconButton";
import MenuIcon from "@mui/icons-material/Menu";
import ChevronLeftIcon from "@mui/icons-material/ChevronLeft";
import ChevronRightIcon from "@mui/icons-material/ChevronRight";
import ListItem from "@mui/material/ListItem";
import ListItemButton from "@mui/material/ListItemButton";
import ListItemText from "@mui/material/ListItemText";
import UserComponent from "./user/UserComponent.tsx";
import RoleComponent from "./role/RoleComponent.tsx";
import DepartmentComponent from "./department/DepartmentComponent.tsx";
import ProjectComponent from "./project/ProjectComponent.tsx";
import TestCaseComponent from "./testCase/TestCaseComponent.tsx";

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
    padding: theme.spacing(0, 1),
    ...theme.mixins.toolbar,
    justifyContent: "flex-end",
}));

interface Project {
    id: number;
    projectInitials: string;
    projectName: string;
}
const Dashboard: React.FC<{ onLogout: () => void }> = ({onLogout}) => {
    const navigate = useNavigate();
    const theme = useTheme();
    const [open, setOpen] = useState(false);
    const [showAlert, setShowAlert] = useState(false);
    const [view, setView] = useState("Dashboard");
    const [projects, setProjects] = useState<Project[]>([]);
    const [selectedProjectId, setSelectedProjectId] = useState<number | null>(null);

    const handleDrawerOpen = () => {
        setOpen(true);
    };

    const handleDrawerClose = () => {
        setOpen(false);
    };

    const handleLogout = async () => {
        try {
            await axios.post("http://localhost:8080/dhtcms/api/v1/logout", {}, {withCredentials: true});
            localStorage.setItem("isLoggedIn", "false");
            onLogout();
        } catch (err: any) {
            console.error("Error during API call:", err);
        }
    };

    useEffect(() => {
        const fetchProjects = async () => {
            try {
                const userId = localStorage.getItem("userId");
                const response = await axios.get(`http://localhost:8080/dhtcms/api/v1/users/${userId}/projects`,
                    {
                        headers: {Authorization: `Bearer ` + localStorage.getItem("authToken")},
                    });
                setProjects(response.data);
            } catch (error) {
                console.error("Error fetching projects:", error);
            }
        };

        fetchProjects().then();
        const isLoggedIn = localStorage.getItem("isLoggedIn");
        if (isLoggedIn && isLoggedIn === "true") {
            setShowAlert(true);
            const timeId = setTimeout(() => {
                setShowAlert(false);
            }, 3000);
            return () => clearTimeout(timeId);
        }
        localStorage.removeItem("isLoggedIn");
    }, [navigate]);

    // @ts-ignore
    // @ts-ignore
    return (
        <Box sx={{display: "flex"}}>
            <CssBaseline/>
            <AppBar position="fixed" open={open}>
                <Toolbar>
                    <IconButton
                        color="inherit"
                        aria-label="open drawer"
                        onClick={handleDrawerOpen}
                        edge="start"
                        sx={{...(open && {display: "none"})}}
                    >
                        <MenuIcon/>
                    </IconButton>
                    <Typography variant="h6" noWrap component="div">
                        {view}
                    </Typography>
                    <Box sx={{ marginLeft: "auto" }}>
                        <ListItem disablePadding>
                            <ListItemButton onClick={handleLogout}>
                                <ListItemText primary="Logout" />
                            </ListItemButton>
                        </ListItem>
                    </Box>
                </Toolbar>
            </AppBar>
            <Drawer
                sx={{
                    width: drawerWidth,
                    flexShrink: 0,
                    "& .MuiDrawer-paper": {
                        width: drawerWidth,
                        boxSizing: "border-box",
                    },
                }}
                variant="persistent"
                anchor="left"
                open={open}
            >
                <DrawerHeader>
                    <img src="../../src/assets/images.jpeg" height="80%" width="40%"/>
                    <IconButton onClick={handleDrawerClose}>
                        {theme.direction === "ltr" ? <ChevronLeftIcon/> : <ChevronRightIcon/>}
                    </IconButton>
                </DrawerHeader>
                <Divider/>
                <List>
                    <ListItem disablePadding>
                        <ListItemButton
                            onClick={() => {
                                setView("Dashboard");
                            }}
                        >
                            <ListItemText primary="Dashboard"/>
                        </ListItemButton>
                    </ListItem>
                    <ListItem disablePadding>
                        <ListItemButton
                            onClick={() => {
                                setView("Users");
                            }}
                        >
                            <ListItemText primary="Users"/>
                        </ListItemButton>
                    </ListItem>
                    <ListItem disablePadding>
                        <ListItemButton
                            onClick={() => {
                                setView("Roles");
                            }}
                        >
                            <ListItemText primary="Roles"/>
                        </ListItemButton>
                    </ListItem>
                    <ListItem disablePadding>
                        <ListItemButton
                            onClick={() => {
                                setView("Department");
                            }}
                        >
                            <ListItemText primary="Department"/>
                        </ListItemButton>
                    </ListItem>
                    <ListItem disablePadding>
                        <ListItemButton
                            onClick={() => {
                                setView("Project");
                            }}
                        >
                            <ListItemText primary="Project"/>
                        </ListItemButton>
                    </ListItem>
                </List>
                <Divider/>
            </Drawer>
            <Main open={open}>
                <DrawerHeader/>
                {showAlert && (
                    <Alert severity="success" onClose={() => setShowAlert(false)}>
                        Login successful!
                    </Alert>
                )}
                {view === "Users" && <UserComponent/>}
                {view === "Roles" && <RoleComponent/>}
                {view === "Department" && <DepartmentComponent/>}
                {view === "Project" && <ProjectComponent/>}
                {view === "TestCase" && <TestCaseComponent projId={selectedProjectId}/>}
                {view === "Dashboard" && (
                    <Grid2 container spacing={12} sx={{ padding: 4 }}>
                        {projects.map((project) => (
                            <ButtonBase
                                onClick={() => {
                                    setSelectedProjectId(project.id); // Store the selected project ID
                                    setView("TestCase");         // Trigger view change
                                }}
                                sx={{ width: "100%" }}
                            >
                                <Card
                                    variant="outlined"
                                    sx={{ width: "100%", textAlign: "left" }} // ensure full-width card
                                >
                                    <CardContent>
                                        <Typography variant="h6" component="div">
                                            {project.projectInitials}
                                        </Typography>
                                        <Typography color="text.secondary">
                                            {project.projectName}
                                        </Typography>
                                    </CardContent>
                                </Card>
                            </ButtonBase>
                        ))}
                    </Grid2>
                )}
            </Main>
        </Box>
    );
};

export default Dashboard;
