import { useEffect, useState } from "react";
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import axios from "axios";

import Login from "./login/Login";
import Dashboard from "./dashboard/Dashboard";

const API_BASE_URL = "http://localhost:8080/dhtcms/api/v1";

function App() {
    const [isAuthenticated, setIsAuthenticated] = useState<boolean>(() => {
        return Boolean(localStorage.getItem("authToken"));
    });

    const handleLoginSuccess = () => {
        setIsAuthenticated(true);
    };

    const handleLogout = () => {
        setIsAuthenticated(false);
        clearAuthStorage();
    };

    const clearAuthStorage = () => {
        localStorage.removeItem("authToken");
        localStorage.removeItem("isLoggedIn");
        localStorage.removeItem("userId");
    };

    useEffect(() => {
        const checkToken = async () => {
            const token = localStorage.getItem("authToken");
            if (!token) {
                clearAuthStorage();
                return setIsAuthenticated(false);
            }

            try {
                await axios.get(`${API_BASE_URL}/validate`, {
                    headers: { Authorization: `Bearer ${token}` },
                });
                setIsAuthenticated(true);
            } catch (err) {
                console.error("Token validation failed:", err);
                clearAuthStorage();
                setIsAuthenticated(false);
            }
        };

        checkToken().then();
    }, []);

    return (
        <Router>
            <Routes>
                <Route
                    path="/login"
                    element={
                        isAuthenticated ? <Navigate to="/dashboard" /> : <Login onLoginSuccess={handleLoginSuccess} />
                    }
                />
                <Route
                    path="/dashboard"
                    element={
                        isAuthenticated ? <Dashboard onLogout={handleLogout} /> : <Navigate to="/login" />
                    }
                />
                <Route path="/" element={<Navigate to="/login" />} />
            </Routes>
        </Router>
    );
}

export default App;
