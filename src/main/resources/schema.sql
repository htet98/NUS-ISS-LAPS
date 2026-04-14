-- ============================================================
-- LAPS - Leave Application Processing System
-- Schema SQL
-- ============================================================

-- Drop tables in reverse dependency order
DROP TABLE IF EXISTS leave_application;
DROP TABLE IF EXISTS leave_balances;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS leave_types;
DROP TABLE IF EXISTS public_holidays;
DROP TABLE IF EXISTS users;

-- ============================================================
-- Table: user
-- ============================================================
CREATE TABLE users (
    user_id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    username          VARCHAR(50)  NOT NULL UNIQUE,
    email                 VARCHAR(100) NOT NULL UNIQUE,
    password           VARCHAR(255) NOT NULL,
    role                    ENUM('EMPLOYEE', 'MANAGER', 'ADMIN') NOT NULL DEFAULT 'EMPLOYEE',
    created_by        VARCHAR(50),
    created_when    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_by        VARCHAR(50),
    updated_when    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    active                  BOOLEAN      NOT NULL DEFAULT TRUE
);

-- ============================================================
-- Table: leave_type
-- ============================================================
CREATE TABLE leave_types (
     leavetype_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
     name                   ENUM('ANNUAL', 'MEDICAL', 'COMPENSATION') NOT NULL DEFAULT 'ANNUAL', -- NameTypeEnum: ANNUAL, MEDICAL, COMPENSATION
     description          VARCHAR(255),
     default_days       INT     NOT NULL    DEFAULT 0,
     is_paid                BOOLEAN     NOT NULL    DEFAULT TRUE
);

-- ============================================================
-- Table: public_holiday
-- ============================================================
CREATE TABLE public_holidays (
     holiday_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
     name               VARCHAR(255)  NOT NULL    UNIQUE,
     date               DATE      NOT NULL        UNIQUE,
     description    VARCHAR(255)
);

-- ============================================================
-- Table: employee
-- (self-referencing FK for supervisor hierarchy)
-- ============================================================
CREATE TABLE employees (
       emp_id               BIGINT AUTO_INCREMENT PRIMARY KEY,
       first_name          VARCHAR(255),
       last_name            VARCHAR(255) NOT NULL,
       email                    VARCHAR(255) NOT NULL,
       phone_number     VARCHAR(15) NOT NULL,
       department           VARCHAR(255) NOT NULL,
       designation           VARCHAR(255) NOT NULL,
       hire_date            DATE NOT NULL,
       employee_status ENUM('ACTIVE', 'INACTIVE', 'ON_LEAVE', 'RESIGNED'), -- ACTIVE, INACTIVE,ON_LEAVE,RESIGNED
       created_by           VARCHAR(255) NOT NULL,
       created_when       DATETIME      NOT NULL    DEFAULT CURRENT_TIMESTAMP,
       updated_by           VARCHAR(255),
       updated_when        DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
       user_id                  BIGINT      NOT NULL UNIQUE,
       supervisor_id        BIGINT,
       CONSTRAINT fk_employee_user
           FOREIGN KEY (user_id) REFERENCES users(user_id),

       CONSTRAINT fk_employee_supervisor
           FOREIGN KEY (supervisor_id) REFERENCES employees(emp_id)
);

-- ============================================================
-- Table: leave_balance
-- ============================================================
CREATE TABLE leave_balances (
    leavebalance_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    total_days          DOUBLE NOT NULL DEFAULT 0,
    used_days           DOUBLE NOT NULL DEFAULT 0,
    leavetype_id        BIGINT NOT NULL,
    emp_id                  BIGINT NOT NULL,
    CONSTRAINT fk_balance_leavetype
        FOREIGN KEY (leavetype_id) REFERENCES leave_types(leavetype_id),

    CONSTRAINT fk_balance_employee
        FOREIGN KEY (emp_id) REFERENCES employees(emp_id)
);

-- ============================================================
-- Table: leave_application
-- ============================================================
CREATE TABLE leave_application (
    leaveapplication_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    emp_id                            BIGINT       NOT NULL,
    leavetype_id                    BIGINT       NOT NULL,
    start_date                       DATE         NOT NULL,
    end_date                          DATE         NOT NULL,
    duration_days                  DOUBLE       NOT NULL,
    reason                              TEXT         NOT NULL,
    work_dissemination         TEXT,
    is_overseas                     BOOLEAN NOT NULL DEFAULT FALSE,
    contact_details             TEXT,
    status                          ENUM('APPLIED','UPDATED','APPROVED','REJECTED','CANCELLED','DELETED') NOT NULL DEFAULT 'APPLIED',
    approved_by                BIGINT DEFAULT NULL,
    manager_comment         TEXT,
    is_half_day                 BOOLEAN     NOT NULL    DEFAULT FALSE,
    half_day_period         ENUM('MORNING', 'AFTERNOON') DEFAULT NULL,
    created_by                  VARCHAR(50),
    created_when            DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_by                 VARCHAR(50),
    updated_when            DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_leave_employee
        FOREIGN KEY (emp_id) REFERENCES employees(emp_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_leave_type
        FOREIGN KEY (leavetype_id) REFERENCES leave_types(leavetype_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_leave_approver
        FOREIGN KEY (approved_by) REFERENCES employees(emp_id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CHECK (start_date <= end_date),
    CHECK (duration_days > 0)

);
