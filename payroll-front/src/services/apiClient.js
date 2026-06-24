import axios from "axios";

// Base URL of your Spring Boot backend. Override via env var in real builds.
const API_BASE_URL = "http://localhost:8080";

/**
 * `withCredentials: true` is required so the JSESSIONID cookie set by
 * Spring Security is sent with every request - without this, the backend
 * will treat each call as a fresh anonymous session even right after a
 * successful login.
 */
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

/**
 * Normalizes axios errors into the same shape the rest of the app expects:
 * a plain Error with `.message` (taken from the backend's JSON error body
 * when available) and `.status` (the HTTP status code).
 */
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const backendMessage = error.response?.data?.message;
    const normalized = new Error(
      backendMessage || error.message || "Request failed"
    );
    normalized.status = status;
    return Promise.reject(normalized);
  }
);

export { apiClient, API_BASE_URL };
