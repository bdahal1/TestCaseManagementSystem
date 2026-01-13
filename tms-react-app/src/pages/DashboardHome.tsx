import React, { useEffect, useState } from "react";
import { Box, Card, Typography } from "@mui/material";
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

    useEffect(() => {
        const fetchProjects = async () => {
            try {
                if (!user?.userId) return;
                const { data } = await api.get(
                    `/users/${user.userId}/projects`
                );
                setProjects(data);
            } catch (error) {
                console.error("Error fetching projects:", error);
            }
        };

        if (user?.userId) {
            fetchProjects();
        }
    }, [user?.userId]);

    return (
        <Box sx={{ display: "flex", flexWrap: "wrap", gap: 2, justifyContent: "center", mt: 8, px: 2, pb: 4 }}>
            {projects.map((project) => (
                <Card
                    key={project.id}
                    onClick={() => navigate(`/dashboard/project/${project.id}/test-cases`)}
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
    );
};

export default ProjectList;
