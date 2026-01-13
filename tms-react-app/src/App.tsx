import { BrowserRouter as Router, Routes, Route, Navigate, useParams } from "react-router-dom";
import { AuthProvider, useAuth } from "./context/AuthContext";
import Login from "./pages/Login";
import DashboardLayout from "./layouts/DashboardLayout";
import ProjectList from "./pages/DashboardHome";
import TestCaseComponent from "./components/testCases/TestCaseManager";
import TestFolderComponent from "./components/testFolders/TestFolderManager";
import TestExecutionComponent from "./components/testExecutions/TestExecutionManager";
import AdminPage from "./pages/AdminPage";

// Wrapper components to extract route params
const TestCaseRoute = () => {
    const { projectId } = useParams();
    return projectId ? <TestCaseComponent projId={Number(projectId)} /> : <Navigate to="/dashboard" />;
};

const TestFolderRoute = () => {
    const { projectId } = useParams();
    return projectId ? <TestFolderComponent projId={Number(projectId)} /> : <Navigate to="/dashboard" />;
};

const TestExecutionRoute = () => {
    const { projectId } = useParams();
    return projectId ? <TestExecutionComponent projId={Number(projectId)} /> : <Navigate to="/dashboard" />;
};

function AppRoutes() {
    const { isAuthenticated, loading } = useAuth();

    if (loading) {
        return <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>Loading...</div>;
    }

    return (
        <Router>
            <Routes>
                <Route
                    path="/login"
                    element={
                        isAuthenticated ? <Navigate to="/dashboard" /> : <Login />
                    }
                />

                <Route
                    path="/dashboard"
                    element={
                        isAuthenticated ? <DashboardLayout /> : <Navigate to="/login" />
                    }
                >
                    <Route index element={<ProjectList />} />
                    <Route path="project/:projectId/test-cases" element={<TestCaseRoute />} />
                    <Route path="project/:projectId/test-folders" element={<TestFolderRoute />} />
                    <Route path="project/:projectId/test-executions" element={<TestExecutionRoute />} />
                    <Route path="admin" element={<AdminPage />} />
                </Route>

                <Route path="/" element={<Navigate to="/dashboard" />} />
                <Route path="*" element={<Navigate to="/dashboard" />} />
            </Routes>
        </Router>
    );
}

function App() {
    return (
        <AuthProvider>
            <AppRoutes />
        </AuthProvider>
    );
}

export default App;
