// src/services/employee/employeeService.js

import { apiClient } from '../apiClient'; // adjust path if needed

export const fetchEmployees = async () => {
  const response = await apiClient.get('/api/employees', {
    params: {
      page: 0,
      size: 1000,
      sortBy: 'fullName',
      order: 'asc',
    },
  });

  return response.data.content;
};