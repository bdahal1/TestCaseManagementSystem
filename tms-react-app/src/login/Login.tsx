import React, {useEffect, useState} from "react";
import axios from "axios";
import {
    Box,
    TextField,
    Button,
    Alert,
    CircularProgress,
    Card,
    CardContent,
    Typography,
} from "@mui/material";

const Login: React.FC<{ onLoginSuccess: () => void }> = ({onLoginSuccess}) => {
    const [userName, setUserName] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    const [showAlert, setShowAlert] = useState(false);

    useEffect(() => {
        const isLoggedIn = localStorage.getItem("isLoggedIn");
        localStorage.removeItem("isLoggedIn");
        if (isLoggedIn === "false") {
            setShowAlert(true);
            const timer = setTimeout(() => setShowAlert(false), 3000);
            return () => clearTimeout(timer);
        }
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
            const response = await axios.post("http://localhost:8080/dhtcms/api/v1/login", {
                userName,
                password,
            });
            localStorage.setItem("isLoggedIn", "true");
            localStorage.setItem("userId", response.data.data.userId);
            localStorage.setItem("authToken", response.data.data.accessToken);
            localStorage.setItem("roleList", JSON.stringify(response.data.data.rolesList));
            localStorage.setItem("fullName", response.data.data.fullName);
            onLoginSuccess();
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
                        {showAlert && (
                            <Alert severity="success" sx={{mb: 2}} onClose={() => setShowAlert(false)}>
                                Logout successful!
                            </Alert>
                        )}
                        {error && (
                            <Alert severity="error" sx={{mb: 2}}>
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
                            sx={{mt: 2}}
                            disabled={loading}
                        >
                            {loading ? <CircularProgress size={24}/> : "Login"}
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
