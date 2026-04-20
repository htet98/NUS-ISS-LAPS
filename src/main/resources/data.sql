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
('manager1', 'james@laps.com', 'password123', 'MANAGER',  'system', 'system', NOW(), NOW(), true),
('alice',    'alice@laps.com',    'password123', 'EMPLOYEE', 'system', 'system', NOW(), NOW(), true),
('bob',      'bob@laps.com',      'password123', 'EMPLOYEE', 'system', 'system', NOW(), NOW(), true),
('admin',    'admin@laps.com',    'password123', 'ADMIN',    'system', 'system', NOW(), NOW(), true);

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
VALUES ('James', 'Tan', 'james@laps.com', '91234567', 'Engineering', 'PROFESSIONAL',
        '2020-01-10', 'ACTIVE', 'system', NOW(), 'system', NOW(), 1, NULL);

-- Employees reporting to manager1 (supervisor_id = 1)
INSERT INTO employees (first_name, last_name, email, phone_number, department, designation,
                       hire_date, employee_status, created_by, created_when, updated_by, updated_when,
                       user_id, supervisor_id)
VALUES ('Alice', 'Lim', 'alice@laps.com', '82345678', 'Engineering', 'PROFESSIONAL',
        '2022-03-15', 'ACTIVE', 'system', NOW(), 'system', NOW(), 2, 1);

INSERT INTO employees (first_name, last_name, email, phone_number, department, designation,
                       hire_date, employee_status, created_by, created_when, updated_by, updated_when,
                       user_id, supervisor_id)
VALUES ('Bob', 'Ng', 'bob@laps.com', '63456789', 'Engineering', 'ADMINISTRATIVE',
        '2023-06-01', 'ACTIVE', 'system', NOW(), 'system', NOW(), 3, 1);

-- ============================================================
-- 5. leave_balances
--    Each employee gets a balance row per leave type.
--    Column: leavebalance_id, total_days, used_days, leavetype_id, emp_id
-- ============================================================

-- Manager (emp_id = 1) - Professional (18 days annual)
INSERT INTO leave_balances (total_days, used_days, leavetype_id, emp_id) VALUES
(18.0, 0.0, 1, 1),   -- Annual (Professional)
(60.0, 2.0, 2, 1),   -- Medical (James took 2 days of medical leave)
(5.0, 0.0, 3, 1);   -- Compensation

-- Alice (emp_id = 2) - Professional (18 days annual)
INSERT INTO leave_balances (total_days, used_days, leavetype_id, emp_id) VALUES
(18.0, 3.0, 1, 2),   -- Annual (Professional)
(60.0, 0.0, 2, 2),   -- Medical (Alice hasn't taken medical leave yet)
(2.5, 0.0, 3, 2);   -- Compensation

-- Bob (emp_id = 3) - Administrative (14 days annual)
INSERT INTO leave_balances (total_days, used_days, leavetype_id, emp_id) VALUES
( 14.0, 0.0, 1, 3),  -- Annual (Administrative)
( 60.0, 0.0, 2, 3),  -- Medical
(  1.0, 0.5, 3, 3);  -- Compensation (Bob has 1 day of compensation leave, used 0.5 days)

-- ============================================================
-- Additional leave_application records (Auto-increment ID)
-- ============================================================

-- 1. James Tan (emp_id 1) taking Medical Leave
-- Status: APPROVED (System/Auto-approved for Manager)
INSERT INTO leave_application (
    emp_id, leavetype_id, start_date, end_date,
    duration_days, reason, work_dissemination, is_overseas,
    status, created_by, created_when
) VALUES (
             1, 2, '2026-03-10', '2026-03-11',
             2.0, 'Annual health checkup and recovery', 'Emergency contact via email', false,
             'APPROVED', 'manager1', '2026-03-9 09:00:00'
         );

-- 2. Alice (emp_id 2) applied for Compensation Leave (0.5 days)
-- Status: APPLIED (Pending approval from James Tan)
INSERT INTO leave_application (
    emp_id, leavetype_id, start_date, end_date,
    duration_days, reason, is_overseas, is_half_day, half_day_period, status,
    created_by, created_when
) VALUES (
             2, 3, '2026-04-15', '2026-04-15',
             0.5, 'Family matters in the afternoon', false,  true, 'AFTERNOON', 'APPLIED',
             'alice', '2026-04-14 14:00:00'
         );

-- 3. Bob (emp_id 3) applied for Annual Leave
-- Status: REJECTED by James Tan (emp_id 1)
INSERT INTO leave_application (
    emp_id, leavetype_id, start_date, end_date,
    duration_days, reason, work_dissemination, is_overseas,
    status, approved_by, created_by, created_when
) VALUES (
             3, 1, '2026-01-29', '2026-02-03',
             5.0, 'Summer hiking trip', 'Work shared with Alice', false,
             'REJECTED', 1, 'bob', '2026-01-28 09:00:00'
         );

-- 4. Alice (emp_id 2) updated an existing plan
-- Status: UPDATED
INSERT INTO leave_application (
    emp_id, leavetype_id, start_date, end_date,
    duration_days, reason, is_overseas, status,
    created_by, created_when
) VALUES (
             2, 1, '2026-02-12', '2026-02-14',
             3.0, 'Revised dates for wedding attendance', false, 'UPDATED',
             'alice', '2026-02-11 10:00:00'
         );

INSERT INTO leave_application (
    emp_id, leavetype_id, start_date, end_date,
    duration_days, reason, work_dissemination, is_overseas,
    contact_details, status, approved_by, created_by, created_when
) VALUES (
             2, 1, '2026-04-06', '2026-04-08',
             3.0, 'Family vacation to Malaysia', 'Tasks handed over to Bob Ng', true,
             'Stay at Hotel Grand, KL. +60 12-345 6789', 'APPROVED', 1, 'alice', '2026-04-01 09:00:00'
         );

-- Bob (emp_id 3) applied for 1 day of Medical Leave (leavetype_id 2)
-- Status: APPLIED (Pending)
INSERT INTO leave_application (
    emp_id, leavetype_id, start_date, end_date,
    duration_days, reason, work_dissemination, is_overseas,
    status, created_by, created_when
) VALUES (
             3, 2, '2026-04-15', '2026-04-15',
             1.0, 'High fever and flu', 'Internal documentation', false,
             'APPLIED', 'bob', '2026-04-14 09:00:00'
         );

-- Bob (emp_id 3) applied for 0.5 days of Compensation Leave (leavetype_id 3)
-- Status: APPROVED, Actioned by James Tan (emp_id 1)
INSERT INTO leave_application (
    emp_id, leavetype_id, start_date, end_date,
    duration_days, reason, is_overseas, is_half_day, half_day_period, status, approved_by, created_by, created_when
) VALUES (
         3, 3, '2026-04-20', '2026-04-20',
         0.5, 'Personal errands', false, true,  'MORNING','APPROVED', 1, 'bob', '2026-04-17 10:00:00'
     );

-- ============================================================
-- Additional FUTURE leave applications for emp_id 1 (James Tan)
-- ============================================================

-- James Tan (emp_id 1) applied for Annual Leave - May 2026
-- Status: APPLIED (Pending approval)
INSERT INTO leave_application (
    emp_id, leavetype_id, start_date, end_date,
    duration_days, reason, work_dissemination, is_overseas,
    status, created_by, created_when
) VALUES (
         1, 1, '2026-05-10', '2026-05-15',
         5.0, 'Team building vacation to Bali', 'Deputy manager will handle critical approvals', true,
         'APPLIED', 'manager1', '2026-04-19 10:00:00'
     );

-- James Tan (emp_id 1) applied for Annual Leave - June 2026
-- Status: APPLIED (Pending approval)
INSERT INTO leave_application (
    emp_id, leavetype_id, start_date, end_date,
    duration_days, reason, work_dissemination, is_overseas,
    status, created_by, created_when
) VALUES (
         1, 1, '2026-06-08', '2026-06-12',
         4.0, 'Summer holidays with family', 'Tasks delegated to senior staff', false,
         'APPLIED', 'manager1', '2026-04-19 11:00:00'
     );

-- James Tan (emp_id 1) applied for Medical Leave - May 2026
-- Status: APPLIED (Pending approval)
INSERT INTO leave_application (
    emp_id, leavetype_id, start_date, end_date,
    duration_days, reason, work_dissemination, is_overseas,
    status, created_by, created_when
) VALUES (
         1, 2, '2026-05-25', '2026-05-26',
         2.0, 'Dental surgery and recovery', 'Emergency decisions delegated to team lead', false,
         'APPLIED', 'manager1', '2026-04-19 12:00:00'
     );

-- James Tan (emp_id 1) applied for Compensation Leave - July 2026 (Half day)
-- Status: APPLIED (Pending approval)
INSERT INTO leave_application (
    emp_id, leavetype_id, start_date, end_date,
    duration_days, reason, is_overseas, is_half_day, half_day_period, status,
    created_by, created_when
) VALUES (
         1, 3, '2026-07-15', '2026-07-15',
         0.5, 'Personal appointment in morning', false, true, 'MORNING', 'APPLIED',
         'manager1', '2026-04-19 13:00:00'
     );

-- James Tan (emp_id 1) applied for Annual Leave - August 2026
-- Status: APPLIED (Pending approval)
INSERT INTO leave_application (
    emp_id, leavetype_id, start_date, end_date,
    duration_days, reason, work_dissemination, is_overseas,
    status, created_by, created_when
) VALUES (
         1, 1, '2026-08-02', '2026-08-07',
         5.0, 'National Day holidays and family visit', 'Team will run independently', false,
         'APPLIED', 'manager1', '2026-04-19 14:00:00'
     );

-- ============================================================
-- Additional FUTURE leave applications for emp_id 2 (Alice Lim)
-- ============================================================

-- Alice (emp_id 2) applied for Annual Leave - May 2026
-- Status: APPLIED (Pending approval from James Tan)
INSERT INTO leave_application (
    emp_id, leavetype_id, start_date, end_date,
    duration_days, reason, work_dissemination, is_overseas,
    status, created_by, created_when
) VALUES (
         2, 1, '2026-05-18', '2026-05-22',
         4.0, 'Travel to Japan for holiday', 'Work delegated to Bob Ng', true,
         'APPLIED', 'alice', '2026-04-19 09:30:00'
     );

-- Alice (emp_id 2) applied for Medical Leave - June 2026
-- Status: APPLIED (Pending approval from James Tan)
INSERT INTO leave_application (
    emp_id, leavetype_id, start_date, end_date,
    duration_days, reason, work_dissemination, is_overseas,
    status, created_by, created_when
) VALUES (
         2, 2, '2026-06-03', '2026-06-04',
         2.0, 'Eye examination and follow-up', 'Emergency contact via email', false,
         'APPLIED', 'alice', '2026-04-19 10:00:00'
     );

-- Alice (emp_id 2) applied for Annual Leave - July 2026
-- Status: APPLIED (Pending approval from James Tan)
INSERT INTO leave_application (
    emp_id, leavetype_id, start_date, end_date,
    duration_days, reason, work_dissemination, is_overseas, contact_details,
    status, created_by, created_when
) VALUES (
         2, 1, '2026-07-20', '2026-07-24',
         4.0, 'Holiday in Thailand with friends', 'Tasks handed over to team', true,
         'Bangkok, Thailand. Contact: +66 8-1234-5678', 'APPLIED', 'alice', '2026-04-19 11:00:00'
     );

-- Alice (emp_id 2) applied for Compensation Leave - May 2026 (Half day)
-- Status: APPLIED (Pending approval from James Tan)
INSERT INTO leave_application (
    emp_id, leavetype_id, start_date, end_date,
    duration_days, reason, is_overseas, is_half_day, half_day_period, status,
    created_by, created_when
) VALUES (
         2, 3, '2026-05-28', '2026-05-28',
         0.5, 'Personal matters in afternoon', false, true, 'AFTERNOON', 'APPLIED',
         'alice', '2026-04-19 12:00:00'
     );

-- Alice (emp_id 2) applied for Annual Leave - September 2026
-- Status: APPLIED (Pending approval from James Tan)
INSERT INTO leave_application (
    emp_id, leavetype_id, start_date, end_date,
    duration_days, reason, work_dissemination, is_overseas,
    status, created_by, created_when
) VALUES (
         2, 1, '2026-09-05', '2026-09-11',
         6.0, 'Extended holiday - South Korea trip', 'Complete handover to backup team', true,
         'APPLIED', 'alice', '2026-04-19 13:00:00'
     );

-- Alice (emp_id 2) applied for Medical Leave - August 2026
-- Status: APPLIED (Pending approval from James Tan)
INSERT INTO leave_application (
    emp_id, leavetype_id, start_date, end_date,
    duration_days, reason, work_dissemination, is_overseas,
    status, created_by, created_when
) VALUES (
         2, 2, '2026-08-17', '2026-08-18',
         2.0, 'Planned surgery and recovery', 'Projects delegated to team members', false,
         'APPLIED', 'alice', '2026-04-19 14:00:00'
     );

-- ============================================================
-- Additional 2 MORE leave applications
-- ============================================================

-- James Tan (emp_id 1) applied for Annual Leave - October 2026
-- Status: APPLIED (Pending approval)
INSERT INTO leave_application (
    emp_id, leavetype_id, start_date, end_date,
    duration_days, reason, work_dissemination, is_overseas, contact_details,
    status, created_by, created_when
) VALUES (
         1, 1, '2026-10-12', '2026-10-16',
         4.0, 'Diwali holidays and family gathering', 'Team lead will manage operations', false,
         'Home in Singapore', 'APPLIED', 'manager1', '2026-04-19 15:00:00'
     );

-- Alice (emp_id 2) applied for Compensation Leave - June 2026 (Half day)
-- Status: APPLIED (Pending approval from James Tan)
INSERT INTO leave_application (
    emp_id, leavetype_id, start_date, end_date,
    duration_days, reason, is_overseas, is_half_day, half_day_period, status,
    created_by, created_when
) VALUES (
         2, 3, '2026-06-15', '2026-06-15',
         0.5, 'Doctor appointment in the morning', false, true, 'MORNING', 'APPLIED',
         'alice', '2026-04-19 15:30:00'
     );

-- Alice (emp_id 2) applied for Annual Leave - November 2026
-- Status: APPLIED (Pending approval from James Tan)
INSERT INTO leave_application (
    emp_id, leavetype_id, start_date, end_date,
    duration_days, reason, work_dissemination, is_overseas, contact_details,
    status, created_by, created_when
) VALUES (
         2, 1, '2026-11-02', '2026-11-06',
         4.0, 'Year-end vacation and shopping', 'Team will handle ongoing projects', false,
         'Shopping malls and home', 'APPLIED', 'alice', '2026-04-19 16:00:00'
     );


