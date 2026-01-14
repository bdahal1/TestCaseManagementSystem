import React, { useEffect, useState } from "react";
import {
    Alert,
    Box,
    Button,
    Card,
    CardContent,
    CircularProgress,
    TextField,
    Typography,
    useTheme,
    alpha,
    InputAdornment,
    IconButton
} from "@mui/material";
import { Visibility, VisibilityOff } from "@mui/icons-material";
import api from "../services/api";
import { useAuth } from "../context/AuthContext";

const Login: React.FC = () => {
    const theme = useTheme();
    const { login } = useAuth();
    const [userName, setUserName] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        document.title = 'Login | GN-Test';
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
            setError(err.response?.data?.message || "Invalid credentials");
        } finally {
            setLoading(false);
        }
    };

    return (
        <Box
            sx={{
                width: "100vw",
                height: "100vh",
                overflow: "hidden",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                // Animated modern gradient background
                background: `linear-gradient(-45deg, ${theme.palette.primary.dark}, ${theme.palette.secondary.main}, ${theme.palette.primary.main}, #23a6d5)`,
                backgroundSize: "400% 400%",
                animation: "gradient 15s ease infinite",
                "@keyframes gradient": {
                    "0%": { backgroundPosition: "0% 50%" },
                    "50%": { backgroundPosition: "100% 50%" },
                    "100%": { backgroundPosition: "0% 50%" },
                },
            }}
        >
            <Card
                sx={{
                    width: 420,
                    maxWidth: "90%",
                    p: 4,
                    boxShadow: "0 8px 32px 0 rgba(31, 38, 135, 0.37)",
                    backdropFilter: "blur(12px)",
                    backgroundColor: "rgba(255, 255, 255, 0.85)",
                    borderRadius: "24px",
                    border: "1px solid rgba(255, 255, 255, 0.18)",
                    textAlign: "center"
                }}
            >
                <CardContent sx={{ p: 0 }}>
                    <Box component="img" src="/src/assets/images.jpg" alt="Logo" sx={{ width: 64, height: 64, borderRadius: "50%", mb: 2, boxShadow: '0 4px 14px rgba(0,0,0,0.1)' }} />
                    <Typography variant="h4" fontWeight="800" gutterBottom sx={{ color: theme.palette.text.primary }}>
                        Welcome Back
                    </Typography>
                    <Typography variant="body2" color="text.secondary" mb={4}>
                        Enter your credentials to access your workspace
                    </Typography>

                    <form onSubmit={handleSubmit}>
                        {error && (
                            <Alert severity="error" sx={{ mb: 3, borderRadius: '12px' }}>
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
                            sx={{ mb: 2 }}
                            InputProps={{
                                sx: { backgroundColor: alpha('#fff', 0.6) }
                            }}
                        />
                        <TextField
                            fullWidth
                            type={showPassword ? "text" : "password"}
                            label="Password"
                            variant="outlined"
                            margin="normal"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                            sx={{ mb: 3 }}
                            InputProps={{
                                sx: { backgroundColor: alpha('#fff', 0.6) },
                                endAdornment: (
                                    <InputAdornment position="end">
                                        <IconButton
                                            aria-label="toggle password visibility"
                                            onClick={() => setShowPassword(!showPassword)}
                                            edge="end"
                                        >
                                            {showPassword ? <VisibilityOff /> : <Visibility />}
                                        </IconButton>
                                    </InputAdornment>
                                )
                            }}
                        />

                        <Button
                            type="submit"
                            fullWidth
                            variant="contained"
                            size="large"
                            disabled={loading}
                            sx={{
                                mt: 2,
                                py: 1.5,
                                fontSize: '1rem',
                                fontWeight: 700,
                                textTransform: 'none',
                                borderRadius: '12px',
                                boxShadow: '0 4px 14px 0 rgba(99, 102, 241, 0.39)',
                            }}
                        >
                            {loading ? <CircularProgress size={24} color="inherit" /> : "Sign In"}
                        </Button>
                    </form>
                </CardContent>
            </Card>

            <Typography
                variant="body2"
                sx={{
                    position: "absolute",
                    bottom: 24,
                    color: "rgba(255,255,255,0.7)",
                    fontWeight: 500
                }}
            >
                © {new Date().getFullYear()} GN Tech Nepal Pvt. Ltd.
            </Typography>
        </Box>
    );
};

export default Login;
