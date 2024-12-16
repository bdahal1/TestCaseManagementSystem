// src/Login.tsx
import React, {useEffect, useState} from "react";
import axios from "axios";
import Box from '@mui/material/Box';
import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';
import {Alert, CircularProgress} from "@mui/material";
import {useNavigate} from "react-router-dom";

const Login: React.FC<{ onLoginSuccess: () => void }> = ({onLoginSuccess}) => {
    const [userName, setUserName] = useState<string>("");
    const [password, setPassword] = useState<string>("");
    const [error, setError] = useState<string>("");
    const [loading, setLoading] = useState(false);
    const [showAlert, setShowAlert] = useState(false);
    const navigate = useNavigate();

    useEffect(()=>{
        const isLoggedIn = localStorage.getItem("isLoggedIn");
        if (isLoggedIn && isLoggedIn==="false") {
            setShowAlert(true);
            const timeId = setTimeout(() => {
                // After 3 seconds set the show value to false
                setShowAlert(false);
            }, 3000)
            return () => {
                clearTimeout(timeId)
            }
        }
        localStorage.removeItem("isLoggedIn");
    }, [navigate]);
    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setLoading(true);

        // Basic validation
        if (!userName || !password) {
            setError("Both fields are required");
            return;
        }

        setError(""); // Clear previous errors

        try {
            // API call to authenticate user
            const response = await axios.post("http://localhost:8080/dhtcms/api/v1/login", {
                userName,
                password,
            });
            console.log("API Response:", response.data);
            localStorage.setItem("isLoggedIn", "true");
            localStorage.setItem("authToken", response.data.data.accessToken); // Store token in localStorage
            onLoginSuccess(); // Trigger success in App.tsx
            console.log(localStorage.getItem("authToken"))
            ;
        } catch (err: any) {
            console.error("Error during API call:", err);
            setError(err.response?.data?.message || "Something went wrong");
        } finally {
            setLoading(false);
        }
    };

    return (
        <Box component="form" sx={{
            display: 'flex',
            flexDirection: 'column', // Ensures children stack vertically
            alignItems: 'center', // Horizontal centering
            justifyContent: 'center', // Vertical centering
            minHeight: '100vh', // Full viewport height
            '& > :not(style)': {m: 1, width: '30ch'}, // Add spacing and width to child elements
        }}
             autoComplete="off"
             onSubmit={handleSubmit}>
            {error && <Alert variant="outlined" severity="error">
                {error}
            </Alert>}
            {showAlert && (
                <Alert severity="success" onClose={() => setShowAlert(false)}>
                    Logout successful!
                </Alert>
            )}
            <TextField id="userName" label="Enter your UserComponent Name" variant="standard" value={userName}
                       onChange={(e) => setUserName(e.target.value)} required/>
            <br/>
            <TextField id="password" label="Enter your Password" type="password" variant="standard" value={password}
                       onChange={(e) => setPassword(e.target.value)} required/>
            <br/>
            <Button type="submit" variant="contained" disabled={loading} sx={{position: 'relative',}}>
                {loading ? (<CircularProgress size={24}/>) : ('Login')}
            </Button>
        </Box>
    );
};

export default Login;
