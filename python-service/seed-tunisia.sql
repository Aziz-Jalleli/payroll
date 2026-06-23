-- ─────────────────────────────────────────────
-- Tunisian Payroll Seed Data (Finance Law 2024)
-- Run once after first startup
-- ─────────────────────────────────────────────

-- CNSS Rate (current)
INSERT INTO cnss_rates (employee_rate, employer_rate, effective_date, is_current, description)
VALUES (0.0918, 0.1657, '2024-01-01', true, 'Taux CNSS 2024 - Loi de finances 2024');

-- IRPP Brackets 2024 (annual income in TND)
INSERT INTO irpp_brackets (min_income, max_income, rate, fiscal_year, effective_date, bracket_order)
VALUES
  (0,      5000,   0.00, 2024, '2024-01-01', 1),
  (5000,   20000,  0.26, 2024, '2024-01-01', 2),
  (20000,  30000,  0.28, 2024, '2024-01-01', 3),
  (30000,  50000,  0.32, 2024, '2024-01-01', 4),
  (50000,  NULL,   0.35, 2024, '2024-01-01', 5);

-- Legal Config
INSERT INTO legal_config (key, value, unit, description, effective_date)
VALUES
  ('SMIG',                    '500.000',  'TND/month',  'Salaire Minimum Interprofessionnel Garanti',          '2024-01-01'),
  ('SMAG',                    '350.000',  'TND/month',  'Salaire Minimum Agricole Garanti',                    '2024-01-01'),
  ('DEDUCTION_CHEF_FAMILLE',  '300.000',  'TND/year',   'Déduction chef de famille (marié)',                   '2024-01-01'),
  ('DEDUCTION_PER_CHILD',     '100.000',  'TND/year',   'Déduction par enfant à charge (max 4 enfants)',       '2024-01-01'),
  ('MAX_CHILDREN_DEDUCTION',  '4',        'children',   'Nombre maximum d enfants déductibles',                '2024-01-01'),
  ('FISCAL_YEAR',             '2024',     'year',       'Année fiscale en cours',                              '2024-01-01'),
  ('CNSS_CEILING',            'none',     'TND',        'Plafond CNSS (pas de plafond en Tunisie)',             '2024-01-01');
