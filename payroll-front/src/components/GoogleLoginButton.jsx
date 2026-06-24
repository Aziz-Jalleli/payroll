import { redirectToGoogleLogin } from "../services/authService";
import "../styles/GoogleLoginButton.css";

function GoogleLoginButton() {
  return (
    <button
      type="button"
      className="google-login-button"
      onClick={redirectToGoogleLogin}
    >
      <svg
        className="google-login-button__icon"
        viewBox="0 0 18 18"
        aria-hidden="true"
      >
        <path
          fill="#4285F4"
          d="M17.64 9.2c0-.64-.06-1.25-.16-1.84H9v3.48h4.84a4.14 4.14 0 0 1-1.8 2.72v2.26h2.9C16.7 14.2 17.64 11.9 17.64 9.2z"
        />
        <path
          fill="#34A853"
          d="M9 18c2.43 0 4.47-.8 5.96-2.18l-2.9-2.26c-.8.55-1.84.86-3.06.86-2.35 0-4.34-1.58-5.05-3.71H.97v2.33A9 9 0 0 0 9 18z"
        />
        <path
          fill="#FBBC05"
          d="M3.95 10.71a5.4 5.4 0 0 1 0-3.42V4.96H.97a9 9 0 0 0 0 8.08l2.98-2.33z"
        />
        <path
          fill="#EA4335"
          d="M9 3.58c1.32 0 2.5.45 3.44 1.35l2.58-2.58A8.6 8.6 0 0 0 9 0 9 9 0 0 0 .97 4.96l2.98 2.33C4.66 5.16 6.65 3.58 9 3.58z"
        />
      </svg>
      <span>Continue with Google</span>
    </button>
  );
}

export default GoogleLoginButton;
