import React, { useEffect, useState } from "react";
import { Box, Card, Typography, useTheme, alpha } from "@mui/material";
import { useNavigate } from "react-router-dom";
import api from "../services/api";
import { useAuth } from "../context/AuthContext";

interface Project {
    id: number;
    projectInitials: string;
    projectName: string;
}

const ProjectList: React.FC = () => {
    const { user } = useAuth();
    const navigate = useNavigate();
    const [projects, setProjects] = useState<Project[]>([]);
    const theme = useTheme();

    useEffect(() => {
        const fetchProjects = async () => {
            try {
                if (!user?.userId) return;
                const { data } = await api.get(
                    `/users/${user.userId}/projects`
                );
                const sortedData = (data as Project[]).sort((a, b) => a.id - b.id);
                setProjects(sortedData);
            } catch (error) {
                console.error("Error fetching projects:", error);
            }
        };

        if (user?.userId) {
            fetchProjects();
        }
    }, [user?.userId]);

    return (
        <Box sx={{ mt: 4, px: 2, pb: 4 }}>
            <Typography variant="h4" fontWeight="700" gutterBottom sx={{ mb: 4, color: 'text.primary' }}>
                Your Projects
            </Typography>
            <Box sx={{ display: "flex", flexWrap: "wrap", gap: 3 }}>
                {projects.map((project) => (
                    <Card
                        key={project.id}
                        onClick={() => navigate(`/dashboard/project/${project.id}/test-cases`)}
                        sx={{
                            width: 280,
                            height: 160,
                            cursor: "pointer",
                            padding: 3,
                            display: "flex",
                            flexDirection: "column",
                            justifyContent: "space-between",
                            backgroundColor: theme.palette.background.paper,
                            border: `1px solid ${alpha(theme.palette.divider, 0.1)}`,
                            transition: "all 0.3s ease",
                            "&:hover": {
                                transform: "translateY(-4px)",
                                boxShadow: `0 12px 24px -10px ${alpha(theme.palette.primary.main, 0.3)}`,
                                borderColor: theme.palette.primary.main,
                            },
                        }}
                    >
                        <Box
                            sx={{
                                width: 48,
                                height: 48,
                                borderRadius: '12px',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                backgroundColor: alpha(theme.palette.primary.main, 0.1),
                                color: theme.palette.primary.main,
                                mb: 2
                            }}
                        >
                            <Typography variant="h6" fontWeight="bold">
                                {project.projectInitials}
                            </Typography>
                        </Box>

                        <Box>
                            <Typography variant="h6" fontWeight="600" color="text.primary" noWrap>
                                {project.projectName}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                View Test Cases
                            </Typography>
                        </Box>
                    </Card>
                ))}
            </Box>
        </Box>
    );
};

export default ProjectList;
