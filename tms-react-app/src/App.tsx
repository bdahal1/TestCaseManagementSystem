import {useEffect, useState} from "react";
import {BrowserRouter as Router, Routes, Route, Navigate} from "react-router-dom";
import Login from "./login/Login";
import Dashboard from "./dashboard/Dashboard";
import axios from "axios";

function App() {
    const API_BASE_URL = "http://localhost:8080/dhtcms/api/v1";
    const [isAuthenticated, setIsAuthenticated] = useState<boolean>(() => {
        // Initialize based on token availability
        const token = localStorage.getItem("authToken");
        return !!token; // Return true if token exists
    });

    const handleLoginSuccess = () => {
        setIsAuthenticated(true);
    };

    const handleLogout = () => {
        setIsAuthenticated(false);
        localStorage.removeItem("authToken");
    };
    useEffect(() => {
        const checkToken = async () => {
            const token = localStorage.getItem("authToken");
            if (token) {
                try {
                    await axios.get(`${API_BASE_URL}/validate`, {
                        headers: { Authorization: `Bearer ${token}` }
                    });
                    setIsAuthenticated(true);
                } catch (err) {
                    localStorage.removeItem("authToken");
                    localStorage.removeItem("isLoggedIn");
                    localStorage.removeItem("userId");
                    setIsAuthenticated(false);
                    localStorage.removeItem("isLoggedIn");
                }
            } else {
                setIsAuthenticated(false);
                localStorage.removeItem("isLoggedIn");
            }
        };

        checkToken();
    }, []);


    return (
        <Router>
            <Routes>
                <Route path="/login" element={!isAuthenticated ? <Login onLoginSuccess={handleLoginSuccess} /> : <Navigate to="/dashboard" />} />
                <Route path="/dashboard" element={isAuthenticated ? <Dashboard onLogout={handleLogout} /> : <Navigate to="/login" />} />
                <Route path="/" element={<Navigate to="/login" />} />
            </Routes>
        </Router>
    );
}
export default App;
