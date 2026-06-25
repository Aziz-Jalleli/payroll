import { createContext, useContext, useState } from "react";
import Toast from "./Toast";

const ToastContext = createContext();

export function ToastProvider({ children }) {
  const [toast, setToast] = useState(null);

  const showToast = (tone, message) => {
    setToast({ tone, message });
  };

  const hideToast = () => setToast(null);

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}

      <Toast
        message={toast?.message}
        tone={toast?.tone}
        onDismiss={hideToast}
      />
    </ToastContext.Provider>
  );
}

export function useToast() {
  return useContext(ToastContext);
}