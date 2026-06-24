import { Routes, Route, Navigate } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import LoginRejectionPage from "./pages/Loginrejectionpage ";
import "./styles/global.css";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/auth/error" element={<LoginRejectionPage />} />

      {/* Catch-all: redirect unknown paths to login */}
      {/* <Route path="*" element={<Navigate to="/login" replace />} /> */}
    </Routes>
  );
}