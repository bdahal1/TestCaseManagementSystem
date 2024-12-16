import {useState} from "react";
import {BrowserRouter as Router, Routes, Route, Navigate} from "react-router-dom";
import Login from "./login/Login";
import Dashboard from "./dashboard/Dashboard";

function App() {
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
