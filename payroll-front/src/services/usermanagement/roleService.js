import { apiClient } from "../apiClient";

/** Fetches all roles defined in the system, for populating role dropdowns/filters. */
async function listRoles() {
  const response = await apiClient.get("/api/admin/roles");
  return response.data;
}

export { listRoles };
