-- ============================================================
-- LAPS - Leave Application Processing System
-- Schema SQL
-- ============================================================

-- Drop tables in reverse dependency order
DROP TABLE IF EXISTS leave_application;
DROP TABLE IF EXISTS leave_balance;
DROP TABLE IF EXISTS employee;
DROP TABLE IF EXISTS leave_type;
DROP TABLE IF EXISTS public_holiday;
DROP TABLE IF EXISTS user;

-- ============================================================
-- Table: user
-- ============================================================
CREATE TABLE user (
    user_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM('EMPLOYEE', 'MANAGER', 'ADMIN') NOT NULL DEFAULT 'EMPLOYEE',
    created_by    VARCHAR(50),
    created_when  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_by    VARCHAR(50),
    updated_when  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ============================================================
-- Table: leave_type
-- ============================================================
CREATE TABLE leave_type (
    leavetype_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(50)  NOT NULL UNIQUE,
    description   VARCHAR(255),
    default_days  INT          NOT NULL DEFAULT 0,
    is_paid       BOOLEAN      NOT NULL DEFAULT TRUE
);

-- ============================================================
-- Table: public_holiday
-- ============================================================
CREATE TABLE public_holiday (
    publicholiday_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    date              DATE         NOT NULL UNIQUE,
    description       VARCHAR(100) NOT NULL
);

-- ============================================================
-- Table: employee
-- (self-referencing FK for supervisor hierarchy)
-- ============================================================
CREATE TABLE employee (
    emp_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL UNIQUE,
    first_name    VARCHAR(50)  NOT NULL,
    last_name     VARCHAR(50)  NOT NULL,
    department    VARCHAR(100),
    supervisor_id BIGINT,
    hire_date     DATE         NOT NULL,
    status        ENUM('ACTIVE', 'INACTIVE', 'ON_LEAVE', 'RESIGNED') NOT NULL DEFAULT 'ACTIVE',
    created_by    VARCHAR(50),
    created_when  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_by    VARCHAR(50),
    updated_when  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_employee_user
        FOREIGN KEY (user_id) REFERENCES user(user_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_employee_supervisor
        FOREIGN KEY (supervisor_id) REFERENCES employee(emp_id)
        ON DELETE SET NULL ON UPDATE CASCADE
);

-- ============================================================
-- Table: leave_balance
-- ============================================================
CREATE TABLE leave_balance (
    leavebalance_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    emp_id           BIGINT NOT NULL,
    leavetype_id     BIGINT NOT NULL,
    total_days       DOUBLE NOT NULL DEFAULT 0,
    used_days        DOUBLE NOT NULL DEFAULT 0,

    CONSTRAINT fk_leavebalance_employee
        FOREIGN KEY (emp_id) REFERENCES employee(emp_id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_leavebalance_leavetype
        FOREIGN KEY (leavetype_id) REFERENCES leave_type(leavetype_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT uq_leavebalance_emp_type
        UNIQUE (emp_id, leavetype_id)
);

-- ============================================================
-- Table: leave_application
-- ============================================================
CREATE TABLE leave_application (
    leaveapplication_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    emp_id               BIGINT       NOT NULL,
    leavetype_id         BIGINT       NOT NULL,
    start_date           DATE         NOT NULL,
    end_date             DATE         NOT NULL,
    duration_days        DOUBLE       NOT NULL,
    reason               TEXT         NOT NULL,
    status               ENUM('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    approved_by          BIGINT,
    created_by           VARCHAR(50),
    created_when         DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_by           VARCHAR(50),
    updated_when         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_leaveapp_employee
        FOREIGN KEY (emp_id) REFERENCES employee(emp_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_leaveapp_leavetype
        FOREIGN KEY (leavetype_id) REFERENCES leave_type(leavetype_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_leaveapp_approvedby
        FOREIGN KEY (approved_by) REFERENCES employee(emp_id)
        ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT chk_dates
        CHECK (end_date >= start_date)
);
