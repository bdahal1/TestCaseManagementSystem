import { BrowserRouter as Router, Routes, Route, Navigate, useParams } from "react-router-dom";
import { AuthProvider, useAuth } from "./context/AuthContext";
import Login from "./pages/Login";
import DashboardLayout from "./layouts/DashboardLayout";
import ProjectList from "./pages/DashboardHome";
import TestCaseComponent from "./components/testCases/TestCaseManager";
import TestFolderComponent from "./components/testFolders/TestFolderManager";
import TestPlans from "./components/testPlans/TestPlans";
import TestPlanWorkspace from "./components/testPlans/TestPlanWorkspace";
import AdminPage from "./pages/AdminPage";
import ProjectReports from './components/reports/ProjectReports';
import AutomationHistoryPage from './components/automation/AutomationHistoryPage';

const AutomationHistoryRoute = () => {
    const { projectId } = useParams();
    return projectId ? <AutomationHistoryPage projectId={Number(projectId)} /> : <Navigate to="/dashboard" />;
};

const ProjectReportsRoute = () => {
    const { projectId } = useParams();
    return projectId ? <ProjectReports projectId={Number(projectId)} /> : <Navigate to="/dashboard" />;
};

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
    return projectId ? <TestPlans projectId={Number(projectId)} /> : <Navigate to="/dashboard" />;
};

const TestPlanRoute = () => {
    const { projectId, planId } = useParams();
    return projectId && planId ? <TestPlanWorkspace projectId={Number(projectId)} planId={Number(planId)} /> : <Navigate to="/dashboard" />;
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
                    <Route path="project/:projectId/test-plans" element={<TestExecutionRoute />} />
                    <Route path="project/:projectId/test-plans/:planId" element={<TestPlanRoute />} />
                    <Route path="project/:projectId/automation-history" element={<AutomationHistoryRoute />} />
                    <Route path="project/:projectId/reports" element={<ProjectReportsRoute />} />
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
