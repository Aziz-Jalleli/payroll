-- Bootstrap seed data. Run once (or make idempotent) against your DB.
-- This solves the chicken-and-egg problem: the first admin needs a role
-- with USER_ROLE:UPDATE before anyone can grant roles through the API.

-- Permissions
INSERT INTO permissions (resource, action, description) VALUES
  ('USER_ROLE', 'UPDATE', 'Assign or revoke roles from users'),
  ('ROLE', 'CREATE', 'Create new roles'),
  ('ROLE', 'READ', 'View roles'),
  ('ROLE', 'UPDATE', 'Attach/detach permissions on a role'),
  ('ROLE', 'DELETE', 'Delete roles'),
  ('PERMISSION', 'CREATE', 'Create new permissions'),
  ('PERMISSION', 'READ', 'View permissions'),
  ('PERMISSION', 'DELETE', 'Delete permissions'),
  ('EMPLOYEE', 'READ', 'View employee records'),
  ('EMPLOYEE', 'CREATE', 'Create employee records'),
  ('EMPLOYEE', 'UPDATE', 'Update employee records'),
  ('EMPLOYEE', 'DELETE', 'Delete employee records'),
  ('PAYROLL', 'READ', 'View payroll data'),
  ('PAYROLL', 'CREATE', 'Run/create payroll'),
  ('PAYROLL', 'APPROVE', 'Approve payroll runs'),
  ('DEPARTMENT', 'READ', 'View departments'),
  ('DEPARTMENT', 'UPDATE', 'Manage departments')
ON CONFLICT (resource, action) DO NOTHING;

-- Roles
INSERT INTO roles (name, description) VALUES
  ('ADMIN', 'Full system access'),
  ('HR_MANAGER', 'Manages employee records and departments'),
  ('PAYROLL_OFFICER', 'Runs and views payroll'),
  ('EMPLOYEE_VIEWER', 'Read-only access to own/general employee info')
ON CONFLICT (name) DO NOTHING;

-- ADMIN gets every permission
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

-- HR_MANAGER gets employee + department permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'HR_MANAGER'
  AND p.resource IN ('EMPLOYEE', 'DEPARTMENT')
ON CONFLICT DO NOTHING;

-- PAYROLL_OFFICER gets payroll permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'PAYROLL_OFFICER'
  AND p.resource = 'PAYROLL'
ON CONFLICT DO NOTHING;

-- EMPLOYEE_VIEWER gets read-only employee access
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'EMPLOYEE_VIEWER'
  AND p.resource = 'EMPLOYEE' AND p.action = 'READ'
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- IMPORTANT: after your first admin logs in via Google once (which
-- creates their User row with zero roles), run this manually to grant
-- them ADMIN so they can use the /api/admin/users endpoints to manage
-- everyone else from then on:
--
-- UPDATE users SET id = id WHERE email = 'admin@yourcompany.com'; -- (no-op, just locate them)
-- INSERT INTO user_roles (user_id, role_id)
-- SELECT u.id, r.id FROM users u, roles r
-- WHERE u.email = 'admin@yourcompany.com' AND r.name = 'ADMIN';
-- ---------------------------------------------------------------------
