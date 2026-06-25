import { apiClient } from "../apiClient";

/** Fetches all roles, each including its attached permissions. */
async function listRoles() {
  const response = await apiClient.get("/api/admin/roles");
  return response.data;
}

/**
 * Creates a new role. permissionIds is optional - if provided, those
 * existing permissions are attached immediately (backend:
 * CreateRoleRequest.permissionIds).
 */
async function createRole({ name, description, permissionIds }) {
  const response = await apiClient.post("/api/admin/roles", {
    name,
    description,
    permissionIds: permissionIds && permissionIds.length > 0 ? permissionIds : undefined,
  });
  return response.data;
}

/** Updates a role's own name/description. Permission attachment is separate. */
async function updateRole(id, { name, description }) {
  const response = await apiClient.put(`/api/admin/roles/${id}`, { name, description });
  return response.data;
}

async function deleteRole(id) {
  await apiClient.delete(`/api/admin/roles/${id}`);
}

export { listRoles, createRole, updateRole, deleteRole };
