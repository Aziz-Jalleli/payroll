import { useEffect, useState } from "react";

const styles = `
  :root {
    --color-bg: #F7F8FA;
    --color-bg-elevated: #FFFFFF;
    --color-navy: #1B2430;
    --color-navy-soft: #2A3645;
    --color-slate: #3D5A73;
    --color-slate-light: #E8EEF2;
    --color-error: #C5392E;
    --color-error-bg: #FBEAE8;
    --color-text-primary: #1B2430;
    --color-text-secondary: #6B7280;
    --color-text-muted: #9CA3AF;
    --color-border: #E2E5EA;
    --color-border-strong: #C9CED6;
    --font-ui: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Inter", Helvetica, Arial, sans-serif;
    --font-mono: "SF Mono", "Roboto Mono", Consolas, monospace;
    --radius-sm: 6px;
    --radius-md: 10px;
    --radius-lg: 16px;
    --shadow-card: 0 1px 2px rgba(27,36,48,0.04), 0 8px 24px rgba(27,36,48,0.08);
    --ease-standard: cubic-bezier(0.4, 0, 0.2, 1);
  }

  @keyframes fadeSlideUp {
    from { opacity: 0; transform: translateY(18px); }
    to   { opacity: 1; transform: translateY(0); }
  }

  @keyframes pulse-ring {
    0%   { transform: scale(1);   opacity: 0.6; }
    100% { transform: scale(1.55); opacity: 0; }
  }

  @keyframes shake {
    0%,100% { transform: translateX(0); }
    15%      { transform: translateX(-6px); }
    30%      { transform: translateX(6px); }
    45%      { transform: translateX(-4px); }
    60%      { transform: translateX(4px); }
    75%      { transform: translateX(-2px); }
    90%      { transform: translateX(2px); }
  }

  @media (prefers-reduced-motion: reduce) {
    *, *::before, *::after {
      animation-duration: 0.01ms !important;
      animation-iteration-count: 1 !important;
      transition-duration: 0.01ms !important;
    }
  }

  .rejection-page {
    min-height: 100vh;
    background: var(--color-bg);
    background-image:
      radial-gradient(circle at 15% 0%, rgba(61,90,115,0.06), transparent 45%),
      radial-gradient(circle at 100% 100%, rgba(27,36,48,0.04), transparent 50%);
    background-attachment: fixed;
    font-family: var(--font-ui);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 2rem 1rem;
    color: var(--color-text-primary);
    -webkit-font-smoothing: antialiased;
  }

  .card {
    background: var(--color-bg-elevated);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-card);
    padding: 3rem 2.5rem 2.5rem;
    width: 100%;
    max-width: 440px;
    animation: fadeSlideUp 0.45s var(--ease-standard) both;
    position: relative;
    overflow: hidden;
  }

  .card::before {
    content: '';
    position: absolute;
    top: 0; left: 0; right: 0;
    height: 3px;
    background: linear-gradient(90deg, var(--color-error) 0%, #E8735A 60%, transparent 100%);
    border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  }

  .icon-wrap {
    position: relative;
    width: 64px;
    height: 64px;
    margin: 0 auto 1.75rem;
  }

  .icon-ring {
    position: absolute;
    inset: 0;
    border-radius: 50%;
    background: var(--color-error-bg);
    animation: pulse-ring 2s var(--ease-standard) infinite;
  }

  .icon-circle {
    position: absolute;
    inset: 0;
    border-radius: 50%;
    background: var(--color-error-bg);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .icon-circle svg {
    width: 28px;
    height: 28px;
    stroke: var(--color-error);
    stroke-width: 2;
    fill: none;
    stroke-linecap: round;
    stroke-linejoin: round;
  }

  .error-badge {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    background: var(--color-error-bg);
    color: var(--color-error);
    font-size: 0.6875rem;
    font-weight: 600;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    padding: 4px 10px;
    border-radius: 20px;
    margin-bottom: 1rem;
  }

  .error-badge-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--color-error);
    flex-shrink: 0;
  }

  .title {
    font-size: 1.5rem;
    font-weight: 700;
    letter-spacing: -0.02em;
    line-height: 1.2;
    color: var(--color-text-primary);
    margin: 0 0 0.5rem;
    text-align: center;
  }

  .subtitle {
    font-size: 0.9375rem;
    color: var(--color-text-secondary);
    line-height: 1.55;
    text-align: center;
    margin: 0 0 2rem;
  }

  .email-box {
    background: var(--color-error-bg);
    border: 1px solid rgba(197,57,46,0.18);
    border-radius: var(--radius-md);
    padding: 0.875rem 1rem;
    margin-bottom: 2rem;
    animation: shake 0.5s var(--ease-standard) 0.5s both;
  }

  .email-box-label {
    font-size: 0.6875rem;
    font-weight: 600;
    letter-spacing: 0.07em;
    text-transform: uppercase;
    color: var(--color-error);
    opacity: 0.75;
    margin-bottom: 3px;
  }

  .email-box-value {
    font-family: var(--font-mono);
    font-size: 0.875rem;
    color: var(--color-error);
    word-break: break-all;
    font-weight: 500;
  }

  .message-box {
    background: var(--color-slate-light);
    border-radius: var(--radius-md);
    padding: 1rem 1.125rem;
    margin-bottom: 2rem;
    display: flex;
    gap: 0.75rem;
    align-items: flex-start;
  }

  .message-icon {
    flex-shrink: 0;
    margin-top: 1px;
  }

  .message-icon svg {
    width: 16px;
    height: 16px;
    stroke: var(--color-slate);
    stroke-width: 2;
    fill: none;
    stroke-linecap: round;
    stroke-linejoin: round;
  }

  .message-text {
    font-size: 0.875rem;
    color: var(--color-text-secondary);
    line-height: 1.5;
    margin: 0;
  }

  .divider {
    height: 1px;
    background: var(--color-border);
    margin: 0 0 1.75rem;
  }

  .btn-primary {
    display: block;
    width: 100%;
    padding: 0.8125rem 1.25rem;
    background: var(--color-navy);
    color: #F7F8FA;
    border: none;
    border-radius: var(--radius-md);
    font-family: var(--font-ui);
    font-size: 0.9375rem;
    font-weight: 600;
    cursor: pointer;
    text-align: center;
    text-decoration: none;
    transition: background 0.15s var(--ease-standard), transform 0.1s;
    margin-bottom: 0.75rem;
  }

  .btn-primary:hover {
    background: var(--color-navy-soft);
  }

  .btn-primary:active {
    transform: scale(0.98);
  }

  .btn-secondary {
    display: block;
    width: 100%;
    padding: 0.8125rem 1.25rem;
    background: transparent;
    color: var(--color-text-secondary);
    border: 1px solid var(--color-border-strong);
    border-radius: var(--radius-md);
    font-family: var(--font-ui);
    font-size: 0.9375rem;
    font-weight: 500;
    cursor: pointer;
    text-align: center;
    text-decoration: none;
    transition: border-color 0.15s, color 0.15s, transform 0.1s;
  }

  .btn-secondary:hover {
    border-color: var(--color-slate);
    color: var(--color-text-primary);
  }

  .btn-secondary:active {
    transform: scale(0.98);
  }

  .footer {
    margin-top: 2rem;
    font-size: 0.8125rem;
    color: var(--color-text-muted);
    text-align: center;
  }

  .footer a {
    color: var(--color-slate);
    text-decoration: none;
    font-weight: 500;
  }

  .footer a:hover {
    text-decoration: underline;
  }

  .wordmark {
    font-size: 0.8125rem;
    font-weight: 600;
    letter-spacing: 0.04em;
    color: var(--color-text-muted);
    text-transform: uppercase;
    margin-bottom: 2.5rem;
    text-align: center;
  }
`;

function getErrorFromUrl() {
  if (typeof window === "undefined") return null;
  const params = new URLSearchParams(window.location.search);
  const err = params.get("error");
  if (!err) return null;
  try { return decodeURIComponent(err); } catch { return err; }
}

function extractEmail(message) {
  if (!message) return null;
  const match = message.match(/[\w.+-]+@[\w-]+\.[a-zA-Z.]+/);
  return match ? match[0] : null;
}

export default function LoginRejectionPage() {
  const [errorMsg, setErrorMsg] = useState("");
  const [email, setEmail] = useState(null);

  useEffect(() => {
    const msg = getErrorFromUrl() || "Your email address doesn't exist in the employees list.";
    setErrorMsg(msg);
    setEmail(extractEmail(msg));
  }, []);

  const handleRetry = () => {
    window.location.href = "/login";
  };

  const handleContact = () => {
    window.location.href = "mailto:hr@company.com?subject=Payroll%20Access%20Request";
  };

  return (
    <>
      <style>{styles}</style>
      <div className="rejection-page">
        <div className="wordmark">Payroll Portal</div>

        <div className="card" role="main" aria-label="Login error">
          <div style={{ textAlign: "center" }}>
            <div className="icon-wrap" aria-hidden="true">
              <div className="icon-ring" />
              <div className="icon-circle">
                <svg viewBox="0 0 24 24">
                  <path d="M12 9v4m0 4h.01M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
                </svg>
              </div>
            </div>

            <div className="error-badge">
              <span className="error-badge-dot" />
              Access denied
            </div>

            <h1 className="title">Account not found</h1>
            <p className="subtitle">
              We couldn't match your sign-in to an active employee record.
            </p>
          </div>

          {email && (
            <div className="email-box" aria-label={`Email attempted: ${email}`}>
              <div className="email-box-label">Email attempted</div>
              <div className="email-box-value">{email}</div>
            </div>
          )}

          <div className="message-box">
            <span className="message-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24">
                <circle cx="12" cy="12" r="10"/>
                <path d="M12 16v-4m0-4h.01"/>
              </svg>
            </span>
            <p className="message-text">
              Only active employees with provisioned accounts can access this portal.
              If you believe this is an error, contact HR or your system administrator.
            </p>
          </div>

          <div className="divider" />

          <button className="btn-primary" onClick={handleRetry}>
            Try a different account
          </button>
          <button className="btn-secondary" onClick={handleContact}>
            Contact HR support
          </button>
        </div>

        <p className="footer">
          Need help? Email{" "}
          <a href="mailto:hr@company.com">hr@company.com</a>
          {" "}or reach your IT team.
        </p>
      </div>
    </>
  );
}