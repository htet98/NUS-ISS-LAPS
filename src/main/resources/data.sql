-- ============================================================
-- LAPS - Leave Application Processing System
-- Seed Data (data.sql)
-- Runs automatically after Hibernate creates tables
-- (spring.sql.init.mode=always + spring.jpa.defer-datasource-initialization=true)
--
-- Test accounts (plain-text passwords stored as BCrypt hash of "Password123!"):
--   manager1 / Password123!   → emp_id = 1  (Manager)
--   alice     / Password123!  → emp_id = 2  (Employee, reports to manager1)
--   bob       / Password123!  → emp_id = 3  (Employee, reports to manager1)
-- ============================================================

-- ============================================================
-- 1. leave_types
--    Column: leavetype_id, name (enum string), description, default_days, is_paid
-- ============================================================
INSERT INTO leave_types (name, description, default_days, is_paid) VALUES
('ANNUAL',       'Annual leave entitlement',                      14, true),
('MEDICAL',      'Medical leave (max 60 days per year)',           60, true),
('COMPENSATION', 'Compensation leave in lieu of overtime worked',   0, true);

-- ============================================================
-- 2. public_holidays  (Singapore 2026)
--    Column: holiday_id, name, date, description
-- ============================================================
INSERT INTO public_holidays (name, date, description) VALUES
                                                                      (  'New Year''s Day',              '2026-01-01', 'New Year''s Day'), -- Use double single-quote for escaping
                                                                      (  'Chinese New Year Day 1',       '2026-02-17', 'Chinese New Year'),
                                                                      (  'Chinese New Year Day 2',       '2026-02-18', 'Chinese New Year Holiday'),
                                                                      (  'Good Friday',                  '2026-04-03', 'Good Friday'),
                                                                      (  'Labour Day',                   '2026-05-01', 'International Labour Day'),
                                                                      (  'Vesak Day',                    '2026-05-19', 'Vesak Day'),
                                                                      ( 'Hari Raya Haji',               '2026-06-27', 'Hari Raya Haji'),
                                                                      (  'National Day',                 '2026-08-09', 'Singapore National Day'),
                                                                      (  'Deepavali',                    '2026-10-29', 'Deepavali'),
                                                                      ( 'Christmas Day',                '2026-12-25', 'Christmas Day');

-- ============================================================
-- 3. users
--    Column: user_id, username, email, password, role,
--            createdby, updatedby, created_when, updated_when, active
--
--    BCrypt hash of "Password123!" (cost 10):
--    $2a$10$7EqJtq98hPqEX7fNZaFWoOe5A91SFSyWA.DQZVFUADKDSGqzCOtOm
-- ============================================================
-- Plain-text passwords for demo. All accounts use: password123
INSERT INTO users (username, email, password, role, created_by, updated_by, created_when, updated_when, active) VALUES
('manager1', 'manager1@laps.com', 'password123', 'MANAGER',  'system', 'system', NOW(), NOW(), true),
('alice',    'alice@laps.com',    'password123', 'EMPLOYEE', 'system', 'system', NOW(), NOW(), true),
('bob',      'bob@laps.com',      'password123', 'EMPLOYEE', 'system', 'system', NOW(), NOW(), true);

-- ============================================================
-- 4. employees
--    SpringPhysicalNamingStrategy converts all @Column names to snake_case:
--      @Column(name="EmployeeStatus") → employee_status
--      @Column(name="Phone_number")   → phone_number
--      @Column(name="createdBy")      → created_by
--      @Column(name="createdWhen")    → created_when
--      @Column(name="updatedBy")      → updated_by
--      @Column(name="updatedWhen")    → updated_when
-- ============================================================

-- Manager first (no supervisor)
INSERT INTO employees (first_name, last_name, email, phone_number, department, designation,
                       hire_date, employee_status, created_by, created_when, updated_by, updated_when,
                       user_id, supervisor_id)
VALUES ('James', 'Tan', 'manager1@laps.com', '91234567', 'Engineering', 'Engineering Manager',
        '2020-01-10', 'ACTIVE', 'system', NOW(), 'system', NOW(), 1, NULL);

-- Employees reporting to manager1 (supervisor_id = 1)
INSERT INTO employees (first_name, last_name, email, phone_number, department, designation,
                       hire_date, employee_status, created_by, created_when, updated_by, updated_when,
                       user_id, supervisor_id)
VALUES ('Alice', 'Lim', 'alice@laps.com', '82345678', 'Engineering', 'Software Engineer',
        '2022-03-15', 'ACTIVE', 'system', NOW(), 'system', NOW(), 2, 1);

INSERT INTO employees (first_name, last_name, email, phone_number, department, designation,
                       hire_date, employee_status, created_by, created_when, updated_by, updated_when,
                       user_id, supervisor_id)
VALUES ('Bob', 'Ng', 'bob@laps.com', '63456789', 'Engineering', 'Software Engineer',
        '2023-06-01', 'ACTIVE', 'system', NOW(), 'system', NOW(), 3, 1);

-- ============================================================
-- 5. leave_balances
--    Each employee gets a balance row per leave type.
--    Column: leavebalance_id, total_days, used_days, leavetype_id, emp_id
-- ============================================================

-- Manager (emp_id = 1)
INSERT INTO leave_balances (total_days, used_days, leavetype_id, emp_id) VALUES
(14.0, 0.0, 1, 1),   -- Annual
(60.0, 0.0, 2, 1),   -- Medical
(5.0, 0.0, 3, 1);   -- Compensation

-- Alice (emp_id = 2)
INSERT INTO leave_balances (total_days, used_days, leavetype_id, emp_id) VALUES
(14.0, 3.0, 1, 2),   -- Annual  (3 days already used)
(60.0, 2.0, 2, 2),   -- Medical (2 days already used)
(2.5, 0.0, 3, 2);   -- Compensation

-- Bob (emp_id = 3)
INSERT INTO leave_balances (total_days, used_days, leavetype_id, emp_id) VALUES
( 14.0, 0.0, 1, 3),  -- Annual
( 60.0, 0.0, 2, 3),  -- Medical
(  1.0, 0.5, 3, 3);  -- Compensation (0.5 days used)
