import { useEffect } from "react";
import "../../styles/usermanagement/Toast.css";

function Toast({ message, tone = "error", onDismiss }) {
  useEffect(() => {
    if (!message) return;
    const timer = setTimeout(onDismiss, 4000);
    return () => clearTimeout(timer);
  }, [message, onDismiss]);

  if (!message) return null;

  return (
    <div className={`toast toast--${tone}`} role="status">
      {message}
    </div>
  );
}

export default Toast;
