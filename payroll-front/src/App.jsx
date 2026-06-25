import { Routes, Route, Navigate } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import LoginRejectionPage from "./pages/Loginrejectionpage ";
import "./styles/global.css";
import UserManagementPage from "./pages/UserManagementPage";
import PermissionManagementPage from "./pages/PermissionManagementPage";
import { Layout } from "./components/Layout";
import PointageOverviewPage from "./pages/PointageOverviewPage";
import PointageDetailPage   from "./pages/PointageDetailPage";
import RoleManagementPage from "./pages/RoleManagementPage";
import AllowancesPage from "./pages/AllowancesPage";
export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/auth/error" element={<LoginRejectionPage />} />
      <Route path="/admin/users" element={<Layout><UserManagementPage /></Layout>} />
      <Route path="/admin/permissions" element={<Layout><PermissionManagementPage /></Layout>} />
      <Route path="/pointage"
        element={<Layout><PointageOverviewPage /></Layout>} />
      <Route path="/pointage/:year/:month"
        element={<Layout><PointageDetailPage /></Layout>} />
      <Route path="/admin/roles" element={<Layout><RoleManagementPage /></Layout>} />
      <Route path="/admin/allowances" element={<Layout><AllowancesPage /></Layout>} />
      
      {/* Catch-all: redirect unknown paths to login */}
      {/* <Route path="*" element={<Navigate to="/login" replace />} /> */}
    </Routes>
  );
}