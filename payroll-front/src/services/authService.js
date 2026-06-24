import { apiClient, API_BASE_URL } from "./apiClient";

/**
 * Redirects the full page to the backend's OAuth2 authorization endpoint.
 * This MUST be a full browser navigation, not an axios/XHR call - Google's
 * login screen cannot be rendered inside an XHR response, it needs to take
 * over the page.
 */
function redirectToGoogleLogin() {
  window.location.href = `${API_BASE_URL}/oauth2/authorization/google`;
}

/**
 * Fetches the current authenticated user from the backend session.
 * Returns null (rather than throwing) on 401, since "not logged in"
 * is an expected state, not an error condition for this page.
 */
async function getCurrentUser() {
  try {
    const response = await apiClient.get("/api/me");
    return response.data;
  } catch (error) {
    if (error.status === 401) {
      return null;
    }
    throw error;
  }
}

async function logout() {
  const response = await apiClient.post("/logout");
  return response.data;
}

export { redirectToGoogleLogin, getCurrentUser, logout };
