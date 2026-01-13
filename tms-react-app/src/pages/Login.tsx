import React, { useEffect, useState } from "react";
import { Alert, Box, Button, Card, CardContent, CircularProgress, TextField, Typography, } from "@mui/material";
import api from "../services/api";
import { useAuth } from "../context/AuthContext";

const Login: React.FC = () => {
    const { login } = useAuth();
    const [userName, setUserName] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");

    const [loading, setLoading] = useState(false);

    useEffect(() => {
        document.title = 'GN-Test';
    }, []);

    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setLoading(true);
        if (!userName || !password) {
            setError("Both fields are required");
            setLoading(false);
            return;
        }

        setError("");
        try {
            const response = await api.post("/login", {
                userName,
                password,
            });

            const { userId, accessToken, rolesList, fullName } = response.data.data;

            login(accessToken, {
                userId,
                fullName,
                roleList: rolesList
            });

        } catch (err: any) {
            setError(err.response?.data?.message || "Something went wrong");
        } finally {
            setLoading(false);
        }
    };

    return (
        <Box
            sx={{
                position: "fixed",
                top: 0,
                left: 0,
                width: "100vw",
                height: "100vh",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                backgroundImage: `url("/src/assets/images.jpg")`,
                backgroundSize: "cover",
                backgroundPosition: "center",
                backgroundRepeat: "no-repeat",
                overflow: "hidden",
                m: 0,
                p: 0,
            }}
        >
            <Card
                sx={{
                    width: 400,
                    p: 3,
                    boxShadow: 6,
                    borderRadius: 3,
                    backdropFilter: "blur(8px)",
                    backgroundColor: "rgba(255, 255, 255, 0.85)",
                }}
            >
                <CardContent>
                    <Typography variant="h5" textAlign="center" mb={2} fontWeight="bold" color="primary">
                        Login
                    </Typography>

                    <form onSubmit={handleSubmit}>
                        {/* Alert removed as logout alert should be handled by Dashboard or Toast */}
                        {error && (
                            <Alert severity="error" sx={{ mb: 2 }}>
                                {error}
                            </Alert>
                        )}

                        <TextField
                            fullWidth
                            label="Username"
                            variant="outlined"
                            margin="normal"
                            value={userName}
                            onChange={(e) => setUserName(e.target.value)}
                            required
                        />
                        <TextField
                            fullWidth
                            type="password"
                            label="Password"
                            variant="outlined"
                            margin="normal"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />

                        <Button
                            type="submit"
                            fullWidth
                            variant="contained"
                            color="primary"
                            sx={{ mt: 2 }}
                            disabled={loading}
                        >
                            {loading ? <CircularProgress size={24} /> : "Login"}
                        </Button>
                    </form>
                </CardContent>
            </Card>
            <Typography
                variant="body2"
                color="white"
                sx={{
                    position: "absolute",
                    bottom: 16,
                    width: "100%",
                    textAlign: "center",
                    fontSize: 13,
                }}
            >
                © {new Date().getFullYear()} GN Tech Nepal Pvt. Ltd.
            </Typography>
        </Box>
    );
};

export default Login;
