# NUS ISS LAPS - Leave Application Processing System

## 📋 Project Overview

**NUS ISS LAPS** is a comprehensive Leave Application Processing System developed for the National University of Singapore Institute of Systems Science (NUS ISS). This system streamlines the leave management process by allowing employees to apply for leave, managers to review and approve/reject requests, and administrators to manage the system.

The application provides role-based access control with three user roles:

-   **Employee**: Can apply for leave, view their leave balance, and track application history
-   **Manager**: Can review employee leave applications and provide approval/rejection decisions
-   **Admin**: Can manage employees, leave types, public holidays, and system configurations

---

## 🛠️ Technology Stack

### Backend

-   **Framework**: Spring Boot 4.0.5
-   **Language**: Java 17
-   **Build Tool**: Apache Maven 3.x
-   **ORM**: Spring Data JPA with Hibernate
-   **Validation**: Spring Validation Framework
-   **Session Management**: Spring Session Core

### Database

-   **Primary Database**: MySQL 8.0+
-   **Testing Database**: H2 (in-memory for unit tests)

### Frontend

-   **Template Engine**: Thymeleaf
-   **HTML/CSS ([Bootstrap@5.3.3](mailto:Bootstrap@5.3.3))/JavaScript**: Static assets for UI styling
-   **Server**: Embedded Tomcat (Spring Boot default)

### Additional Libraries

-   **Lombok**: Simplifies Java POJO development
-   **Apache Commons CSV**: CSV file processing and import
-   **Spring Mail**: Email notifications for leave approvals/rejections
-   **Spring HATEOAS**: REST API support with hypermedia

### Testing

-   Spring Boot Test Suite with JUnit
-   TestContainers support for integration testing

---

## 🏗️ System Architecture

### Layered Architecture

```
┌─────────────────────────────────────┐
│        Presentation Layer           │
│    (Thymeleaf Templates/Views)      │
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│      Controller Layer               │
│  (Request Handling & Routing)       │
├─────────────────────────────────────┤
│ - AuthController                    │
│ - AdminController                   │
│ - EmployeeController                │
│ - LeaveApplicationController        │
│ - LeaveBalanceController            │
│ - LeaveTypeController               │
│ - UserController                    │
│ - MovementController                │
│ - ReportController                  │
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│      Service Layer                  │
│  (Business Logic & Validation)      │
├─────────────────────────────────────┤
│ - LeaveApplicationService           │
│ - LeaveTypeService                  │
│ - LeaveBalanceService               │
│ - EmployeeService                   │
│ - UserService                       │
│ - EmailService                      │
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│      Repository Layer               │
│  (Data Access Objects)              │
├─────────────────────────────────────┤
│ - LeaveApplicationRepository        │
│ - LeaveTypeRepository               │
│ - LeaveBalanceRepository            │
│ - EmployeeRepository                │
│ - UserRepository                    │
│ - PublicHolidayRepository           │
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│      Database Layer                 │
│      (MySQL Database)               │
└─────────────────────────────────────┘
```

### Key Components

**Models** (`nus_iss.LAPS.model`):

-   `User`: System user authentication and role management
-   `Employee`: Employee information and hierarchy
-   `LeaveType`: Different types of leaves (Annual, Medical, Compensation, Personal, Maternity)
-   `LeaveApplication`: Employee leave requests and approval workflow
-   `LeaveBalance`: Track employee leave balance for each leave type
-   `PublicHoliday`: Public holidays that affect leave calculations

**DTOs** (`nus_iss.LAPS.dto`):

-   Data Transfer Objects for API communication and form binding

**Validators** (`nus_iss.LAPS.validators`):

-   Custom validation logic for business rules

**Utilities** (`nus_iss.LAPS.util`):

-   Helper functions and utility methods

**Configuration** (`nus_iss.LAPS.config`):

-   Spring configuration and bean definitions

---

## 🔌 REST API Layer

### LeaveApplicationRestController

The `LeaveApplicationRestController` (`nus_iss.LAPS.api`) serves as a unified gateway for all leave management operations. This controller ensures business logic is written once and used by both web and API clients, eliminating duplication.

**Key Features:**

-   **Consistent Response Format**: All endpoints return `ResponseEntity<?>` with `ApiResponse` wrapper
    -   Success: `ApiResponse(true, "message", data)` with HTTP 200/201
    -   Failure: `ApiResponse(false, "error message", null)` with HTTP 400/403/500
-   **Role-Based Endpoints**: Separate operations for employees and managers
-   **Pagination Support**: Large datasets handled with Spring Data pagination
-   **Email Notifications**: Automated notifications on application status changes

**HTTP Status Codes Used:**

-   `200 OK` - Read operations and state changes (approve, reject, cancel)
-   `201 CREATED` - Successful new leave submission
-   `400 BAD REQUEST` - Validation errors or illegal state
-   `403 FORBIDDEN` - Unauthorized access (e.g., employee modifying another's record)
-   `404 NOT FOUND` - Resource not found
-   `500 INTERNAL SERVER ERROR` - Unexpected runtime errors

#### Employee Endpoints

**Read Operations:**

HTTP

Endpoint

Description

GET

`/api/leave/employee/{employeeId}`

Get all leave applications for current year

GET

`/api/leave/{id}`

Get single leave application by ID

GET

`/api/leave/employee/{employeeId}/filter`

Filter by status and/or leave type

GET

`/api/leave/employee/{employeeId}/paginated`

Paginated leave history with optional filters

**Write Operations:**

HTTP

Endpoint

Description

POST

`/api/leave`

Submit new leave application

PUT

`/api/leave/{id}`

Update pending leave application

DELETE

`/api/leave/{id}`

Soft-delete leave (APPLIED/UPDATED only)

PATCH

`/api/leave/{id}/cancel`

Cancel approved leave + restore balance

**Example Requests:**

```bash
# Submit leave
POST /api/leave
{
  "employeeId": 5,
  "leaveTypeId": 1,
  "startDate": "2026-05-15",
  "endDate": "2026-05-20",
  "duration": 5,
  "reason": "Family vacation",
  "workDissemination": "John will cover my tasks",
  "isOverseas": false,
  "isHalfDay": false
}

# Update leave application
PUT /api/leave/42
{
  "employeeId": 5,
  "leaveTypeId": 1,
  "startDate": "2026-05-16",
  "endDate": "2026-05-22",
  "reason": "Extended family vacation"
}

# Cancel approved leave
PATCH /api/leave/42/cancel?employeeId=5
```

#### Manager Endpoints

**Read Operations:**

HTTP

Endpoint

Description

GET

`/api/leave/manager/{managerId}/pending`

Get pending applications from subordinates

GET

`/api/leave/manager/{managerId}/pending/paginated`

Paginated pending applications

GET

`/api/leave/manager/{managerId}/subordinates`

All leaves from subordinates (all statuses)

GET

`/api/leave/manager/{managerId}/subordinates/paginated`

Paginated subordinate leave history

GET

`/api/leave/manager/{managerId}/recent`

Last 10 decisions made by manager

**Write Operations:**

HTTP

Endpoint

Description

PATCH

`/api/leave/{id}/approve`

Approve pending leave + deduct balance

PATCH

`/api/leave/{id}/reject`

Reject leave with manager comment

**Example Requests:**

```bash
# Get pending applications for manager (paginated)
GET /api/leave/manager/3/pending/paginated?page=0&size=10

# Approve leave
PATCH /api/leave/42/approve?managerId=3

# Reject leave with comment
PATCH /api/leave/42/reject?managerId=3
{
  "comment": "Team at minimum staffing that week. Please apply for another date."
}
```

#### Query Parameters

**Filtering:**

-   `status`: Leave status (APPLIED, UPDATED, APPROVED, REJECTED, CANCELLED, DELETED)
-   `leaveTypeId`: Leave type identifier (1=Annual, 2=Medical, etc.)

**Pagination:**

-   `page`: Zero-indexed page number (default: 0)
-   `size`: Items per page (default: 10)

**Response Examples:**

```json
{
  "success": true,
  "message": "Leave application submitted",
  "data": {
    "leaveApplicationId": 42,
    "employee": { "empId": 5, "firstName": "John", "lastName": "Doe" },
    "leaveType": { "leaveTypeId": 1, "name": "ANNUAL" },
    "startDate": "2026-05-15",
    "endDate": "2026-05-20",
    "durationDays": 5,
    "status": "APPLIED",
    "createdWhen": "2026-04-20T10:30:00"
  }
}
```

---

## 📊 Database Schema

The system uses the following main entities:

### Users Table

-   User authentication and role management
-   Roles: EMPLOYEE, MANAGER, ADMIN

### Employees Table

-   Employee details and organizational structure
-   Self-referencing foreign key for supervisor relationships
-   Links to Users table for authentication

### Leave Types Table

-   Predefined leave categories: ANNUAL, MEDICAL, COMPENSATION, PERSONAL, MATERNITY
-   Default days and payment status per leave type

### Leave Balances Table

-   Tracks available and used leave days per employee and leave type
-   Updated when applications are approved/rejected

### Leave Applications Table

-   Leave requests with comprehensive workflow states
-   Status: APPLIED, UPDATED, APPROVED, REJECTED, CANCELLED, DELETED
-   Supports half-day leaves with morning/afternoon periods
-   Overseas leave flag with contact details
-   Manager comments and approval tracking

### Public Holidays Table

-   List of public holidays for leave calculations

---

## 🚀 Getting Started

### Prerequisites

-   **Java 17** or higher
-   **MySQL 8.0** or higher
-   **Maven 3.6** or higher
-   **Git** (for version control)

### Installation Steps

#### 1. Clone the Repository

```bash
git clone <repository-url>
cd NUS_ISS_LAPS
```

#### 2. Configure Database Connection

Update `src/main/resources/application.properties` with your MySQL credentials:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/laps?createDatabaseIfNotExist=true
spring.datasource.username=<your_username>
spring.datasource.password=<your_password>
```

Or set environment variables on Windows (PowerShell):

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_password"
```

#### 3. Configure Email Settings (Optional)

For development with **MailDev** (captures emails without sending):

```bash
npm install -g maildev
maildev
```

The default config is already set for MailDev (localhost:1025). Check `application.properties`:

```properties
spring.mail.host=localhost
spring.mail.port=1025
```

For production with real SMTP (Gmail/NUS):

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

#### 4. Build the Project

```bash
mvn clean install
```

#### 5. Run the Application

```bash
mvn spring-boot:run
```

Or run the JAR directly:

```bash
java -jar target/NUS_ISS_LAPS-0.0.1-SNAPSHOT.jar
```

#### 6. Access the Application

Open your browser and navigate to:

```
http://localhost:8080
```

---

## 📖 How to Use the Project

### 1. User Authentication

**Login** using your credentials:

-   Default admin account will be created during application startup
-   Username and password set via SQL initialization scripts

**Available Roles**:

-   **Employee**: Standard user who can apply for leave
-   **Manager**: Can approve/reject employee leave requests
-   **Admin**: Full system management capabilities

### 2. Employee Workflows

#### Applying for Leave

1.  Navigate to **Leave → Apply**
2.  Fill in leave details:
    -   Leave Type (Annual, Medical, etc.)
    -   Start Date & End Date
    -   Duration (auto-calculated)
    -   Reason for leave
    -   Work dissemination plan
3.  Indicate if overseas travel
4.  Submit application

#### Viewing Leave Balance

1.  Go to **Leave Balance** section
2.  View available and used leave for each type
3.  Track usage across the year

#### Checking Application History

1.  Navigate to **Leave → History**
2.  View all submitted applications
3.  Filter by status (Applied, Approved, Rejected, etc.)
4.  Edit pending applications if needed

### 3. Manager Workflows

#### Review Leave Applications

1.  Go to **Leave → Manager Dashboard**
2.  View subordinates' leave applications
3.  Filter by status and date range
4.  Click to review detailed requests

#### Approve or Reject

1.  Open the application
2.  Review employee details and leave reason
3.  Add comments (optional)
4.  Click **Approve** or **Reject**
5.  Email notification sent automatically to employee

### 4. Administrator Functions

#### Manage Employees

1.  **Admin → Employees → List**: View all employees
2.  **Admin → Employees → New**: Add new employee
3.  **Admin → Employees → Edit**: Update employee information
4.  Edit supervisor hierarchy for approval workflows

#### Manage Leave Types

1.  **Admin → Leave Types**: Add/edit leave type definitions
2.  Set default days allocated per year
3.  Configure paid vs. unpaid leave types

#### Manage Public Holidays

1.  **Admin → Holidays**: Add public holidays
2.  System automatically excludes holidays from leave duration calculations

#### Manage Users

1.  **Admin → Users**: Create/edit user accounts
2.  Assign roles (Employee, Manager, Admin)
3.  Activate/deactivate users
4.  Reset passwords if needed

### 5. Reports

#### Leave Application Reports

1.  Go to **Reports → Leave Reports**
2.  Generate reports by:
    -   Department
    -   Employee
    -   Leave Type
    -   Date Range
3.  Export reports as CSV or PDF

---

## 🔧 Configuration Guide

### Application Properties

Key configurable properties in `application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/laps
spring.jpa.hibernate.ddl-auto=create-drop  # auto, create-drop, update, validate

# Application Settings
app.base-url=http://localhost:8080
server.port=8080

# Session Configuration
server.servlet.session.timeout=30m

# Email Configuration
spring.mail.host=localhost
spring.mail.port=1025

# Logging
logging.file.name=logs/laps.log
```

### Environment Variables

Set these for environment-specific configurations:

```bash
# Database
DB_USERNAME=root
DB_PASSWORD=your_password

# Email
MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_USER=anything
MAIL_PASS=anything
MAIL_FROM=noreply@laps.com
MAIL_AUTH=false
MAIL_TLS=false

# Application
APP_BASE_URL=http://localhost:8080
```

---

## 🧪 Testing

### Run Tests

```bash
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=YourTestClass
```

### Generate Coverage Report

```bash
mvn jacoco:report
```

---

## 📁 Project Structure

```
NUS_ISS_LAPS/
├── src/
│   ├── main/
│   │   ├── java/nus_iss/LAPS/
│   │   │   ├── controller/           # REST/Web controllers
│   │   │   ├── service/              # Business logic
│   │   │   ├── repository/           # Data access layer
│   │   │   ├── model/                # JPA entities
│   │   │   ├── dto/                  # Data transfer objects
│   │   │   ├── config/               # Spring configuration
│   │   │   ├── validators/           # Custom validators
│   │   │   ├── util/                 # Utility classes
│   │   │   └── NusIssLapsApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── schema.sql            # Database schema
│   │       ├── data.sql              # Initial data
│   │       ├── templates/            # Thymeleaf templates
│   │       └── static/               # CSS, JS, images
│   └── test/
│       ├── java/nus_iss/LAPS/        # Unit tests
│       └── resources/
│           └── application-test.properties
├── pom.xml                            # Maven configuration
├── README.md                          # This file
└── logs/
    └── laps.log                       # Application logs
```

---

## 🐛 Troubleshooting

### Database Connection Issues

-   Ensure MySQL is running on localhost:3306
-   Check credentials in `application.properties`
-   Verify database user has appropriate permissions

### Email Not Sending

-   For development: Ensure MailDev server is running (`maildev` command)
-   For production: Update SMTP credentials and enable TLS/Auth
-   Check `logs/laps.log` for email service errors

### Port Already in Use

-   Change port in `application.properties`: `server.port=8081`
-   Or kill the process using port 8080

### Maven Build Issues

-   Clear Maven cache: `mvn clean`
-   Reload IDE to refresh dependencies
-   Check internet connection for dependency downloads

---

## 📝 Environment Setup (Windows PowerShell)

Quick setup script:

```powershell
# Set database environment variables
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "yourpassword"

# Set email configuration (for development with MailDev)
$env:MAIL_HOST = "localhost"
$env:MAIL_PORT = "1025"

# Start MailDev (if installed)
maildev

# In another terminal, build and run
mvn clean install
mvn spring-boot:run
```

---

## 🔐 Security Considerations

-   **Password**: Stored securely (Spring Security should be configured in production)
-   **Session**: 30-minute timeout for security
-   **Role-Based Access**: Controller-level authorization
-   **Data Validation**: Input validation on both client and server-side

---

## 📞 Support & Documentation

For additional information:

-   Spring Boot: [https://spring.io/projects/spring-boot](https://spring.io/projects/spring-boot)
-   Spring Data JPA: [https://spring.io/projects/spring-data-jpa](https://spring.io/projects/spring-data-jpa)
-   Thymeleaf: [https://www.thymeleaf.org/](https://www.thymeleaf.org/)
-   MySQL: [https://dev.mysql.com/doc/](https://dev.mysql.com/doc/)

---

## 📄 License

Please see the LICENSE file for licensing information.

---

## ✍️ Author

NUS ISS Development Team

**Version**: 0.0.1-SNAPSHOT  
**Last Updated**: April 2026

---

## 🚦 Quick Start Summary

1.  **Setup Database**: Configure MySQL connection in `application.properties`
2.  **Build Project**: `mvn clean install`
3.  **Run Application**: `mvn spring-boot:run`
4.  **Access Application**: `http://localhost:8080`
5.  **Login**: Use provided credentials
6.  **Explore Features**: Navigate through employee/manager/admin dashboards

---

Enjoy using NUS ISS LAPS!