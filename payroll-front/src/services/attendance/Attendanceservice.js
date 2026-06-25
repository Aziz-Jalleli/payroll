import { apiClient } from "../apiClient";

/**
 * Fetch 12 month-summary cards for the given year.
 * @returns {Promise<MonthSummaryDto[]>}
 */
export async function getYearSummary(year) {
  const res = await apiClient.get(`/api/attendance/summary/${year}`);
  return res.data;
}

/**
 * Fetch the attendance table rows for a specific month.
 * EMPLOYEE_VIEWER receives only their own row.
 *
 * @param {number} year
 * @param {number} month
 * @param {{ name?: string, email?: string, department?: string }} filters
 * @returns {Promise<AttendanceRecordDto[]>}
 */
export async function getMonthDetail(year, month, filters = {}) {
  const params = {};
  if (filters.name)       params.name       = filters.name;
  if (filters.email)      params.email      = filters.email;
  if (filters.department) params.department = filters.department;

  const res = await apiClient.get(`/api/attendance/${year}/${month}`, { params });
  return res.data;
}

/**
 * Create or update a single employee attendance record.
 * @param {UpsertAttendanceRequest} payload
 * @returns {Promise<AttendanceRecordDto>}
 */
export async function upsertRecord(payload) {
  const res = await apiClient.post("/api/attendance", payload);
  return res.data;
}

/**
 * Delete a single employee's record for a given month.
 * @param {number} employeeId
 * @param {number} year
 * @param {number} month
 */
export async function deleteRecord(employeeId, year, month) {
  await apiClient.delete(`/api/attendance/${employeeId}/${year}/${month}`);
}