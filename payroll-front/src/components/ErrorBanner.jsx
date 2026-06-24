import "../styles/ErrorBanner.css";

function ErrorBanner({ message }) {
  if (!message) return null;

  return (
    <div className="error-banner" role="alert">
      <svg className="error-banner__icon" viewBox="0 0 20 20" aria-hidden="true">
        <path
          fill="currentColor"
          d="M10 1.5a8.5 8.5 0 1 0 0 17 8.5 8.5 0 0 0 0-17zM9.25 6h1.5v6h-1.5V6zm0 7.5h1.5V15h-1.5v-1.5z"
        />
      </svg>
      <span>{message}</span>
    </div>
  );
}

export default ErrorBanner;
