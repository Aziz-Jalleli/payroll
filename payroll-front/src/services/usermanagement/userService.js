import { apiClient } from "../apiClient";

/**
 * Fetches all user accounts visible to the admin. Each item includes
 * roles, enabled status, and timestamps - matches AdminUserDto on the
 * backend.
 */
async function listUsers() {
  const response = await apiClient.get("/api/admin/users");
  return response.data;
}

async function getUser(id) {
  const response = await apiClient.get(`/api/admin/users/${id}`);
  return response.data;
}

/** Currently only fullName is editable - email is tied to the linked employee/Google identity. */
async function updateUser(id, { fullName }) {
  const response = await apiClient.put(`/api/admin/users/${id}`, { fullName });
  return response.data;
}

async function activateUser(id) {
  const response = await apiClient.patch(`/api/admin/users/${id}/activate`);
  return response.data;
}

async function deactivateUser(id) {
  const response = await apiClient.patch(`/api/admin/users/${id}/deactivate`);
  return response.data;
}

async function deleteUser(id) {
  await apiClient.delete(`/api/admin/users/${id}`);
}

/** Replaces the user's role with a single role - matches the table's role dropdown. */
async function setUserRole(id, roleName) {
  const response = await apiClient.patch(`/api/admin/users/${id}/role`, { role: roleName });
  return response.data;
}

export {
  listUsers,
  getUser,
  updateUser,
  activateUser,
  deactivateUser,
  deleteUser,
  setUserRole,
};
