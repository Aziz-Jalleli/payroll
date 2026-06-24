import { useEffect, useState } from "react";
import GoogleLoginButton from "../components/GoogleLoginButton";
import ErrorBanner from "../components/ErrorBanner";
import SessionInspector from "../components/SessionInspector";
import { getCurrentUser, logout } from "../services/authService";
import "../styles/LoginPage.css";
import AvoCarbonLogo from "../components/AvoCarbonLogo";
function getErrorFromUrl() {
  const params = new URLSearchParams(window.location.search);
  const error = params.get("error");
  if (!error || error === "true") return error ? "Authentication failed." : null;
  return decodeURIComponent(error);
}

function LoginPage() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(getErrorFromUrl());

  useEffect(() => {
    getCurrentUser()
      .then(setUser)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  async function handleLogout() {
    try {
      await logout();
    } catch {
      // even if the call fails, drop the local session view
    } finally {
      setUser(null);
    }
  }

  return (
    <div className="login-page">
      <div className="login-page__card">
        <div className="login-page__brand">
          <AvoCarbonLogo width={200} height={80} />
          <p className="login-page__subtitle">
            Sign in to access protected resources and features of the AVO Carbon Group web application.
          </p>
        </div>

        <ErrorBanner message={error} />

        {loading ? (
          <p className="login-page__loading">Checking session…</p>
        ) : user ? (
          <SessionInspector user={user} onLogout={handleLogout} />
        ) : (
          <GoogleLoginButton />
        )}

        <p className="login-page__footnote">
          Access requires a Google account matching an active record in the{" "}
          <code>employees</code> table.
        </p>
      </div>
    </div>
  );
}

export default LoginPage;