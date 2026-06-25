import { apiClient } from "../apiClient";

/** Fetches all permissions defined in the system. */
async function listPermissions() {
  const response = await apiClient.get("/api/admin/permissions");
  return response.data;
}

/**
 * Creates a new permission. roleNames is optional - if provided, the
 * permission is attached to those existing roles immediately in the same
 * request (backend: CreatePermissionRequest.roleNames).
 */
async function createPermission({ resource, action, description, roleNames }) {
  const response = await apiClient.post("/api/admin/permissions", {
    resource,
    action,
    description,
    roleNames: roleNames && roleNames.length > 0 ? roleNames : undefined,
  });
  return response.data;
}

async function deletePermission(id) {
  await apiClient.delete(`/api/admin/permissions/${id}`);
}

/** Attaches an existing permission to an existing role. Returns the updated RoleDto. */
async function assignPermissionToRole(roleId, permissionId) {
  const response = await apiClient.post(`/api/admin/roles/${roleId}/permissions/${permissionId}`);
  return response.data;
}

/** Detaches a permission from a role. Returns the updated RoleDto. */
async function removePermissionFromRole(roleId, permissionId) {
  const response = await apiClient.delete(`/api/admin/roles/${roleId}/permissions/${permissionId}`);
  return response.data;
}

export {
  listPermissions,
  createPermission,
  deletePermission,
  assignPermissionToRole,
  removePermissionFromRole,
};
