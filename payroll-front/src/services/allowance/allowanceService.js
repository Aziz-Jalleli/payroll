// src/services/allowanceService.js
import { apiClient } from "../apiClient";

const BASE = "/api/allowances";

/** List all allowances (all employees). Requires ALLOWANCE:READ. */
export const fetchAllowances = async () => {
  const res = await apiClient.get(BASE);
  return res.data;
};

/** List allowances for a specific employee. Requires ALLOWANCE:READ. */
export const fetchAllowancesByEmployee = async (employeeId) => {
  const res = await apiClient.get(`${BASE}/employee/${employeeId}`);
  return res.data;
};

/** Get a single allowance by ID. Requires ALLOWANCE:READ. */
export const fetchAllowanceById = async (id) => {
  const res = await apiClient.get(`${BASE}/${id}`);
  return res.data;
};

/** Create a new allowance. Requires ALLOWANCE:CREATE. */
export const createAllowance = async (data) => {
  const res = await apiClient.post(BASE, data);
  return res.data;
};

/** Update an existing allowance by ID. Requires ALLOWANCE:UPDATE. */
export const updateAllowance = async (id, data) => {
  const res = await apiClient.put(`${BASE}/${id}`, data);
  return res.data;
};

/** Delete an allowance by ID. Requires ALLOWANCE:DELETE. */
export const deleteAllowance = async (id) => {
  await apiClient.delete(`${BASE}/${id}`);
};