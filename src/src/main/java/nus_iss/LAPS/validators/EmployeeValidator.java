package nus_iss.LAPS.validators;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import lombok.extern.slf4j.Slf4j;
import nus_iss.LAPS.model.Employee;

@Component
@Slf4j
public class EmployeeValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return Employee.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Employee employee = (Employee) target;

        // ===== First Name =====
        if (employee.getFirst_name() == null || employee.getFirst_name().isBlank()) {
            errors.rejectValue("first_name", "first_name.empty", "Please enter first name");
        } else if (employee.getFirst_name().length() > 50) {
            errors.rejectValue("first_name", "first_name.tooLong", "First name cannot exceed 50 characters");
        }

        // ===== Last Name =====
        if (employee.getLast_name() == null || employee.getLast_name().isBlank()) {
            errors.rejectValue("last_name", "last_name.empty", "Last name cannot be empty");
        } else if (employee.getLast_name().length() > 50) {
            errors.rejectValue("last_name", "last_name.tooLong", "Last name cannot exceed 50 characters");
        }

        // ===== Email =====
        if (employee.getEmail() == null || employee.getEmail().isBlank()) {
            errors.rejectValue("email", "email.empty", "Email cannot be empty");
        } else if (!employee.getEmail().matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$")) {
            errors.rejectValue("email", "email.invalid", "Invalid email format");
        } else if (employee.getEmail().length() > 255) {
            errors.rejectValue("email", "email.tooLong", "Email cannot exceed 255 characters");
        }

        // ===== Phone Number =====
        if (employee.getPhoneNumber() == null || employee.getPhoneNumber().isBlank()) {
            errors.rejectValue("phoneNumber", "phoneNumber.empty", "Phone number cannot be empty");
        } else if (!employee.getPhoneNumber().matches("^[689]\\d{7}$")) {
            errors.rejectValue("phoneNumber", "phoneNumber.invalid",
                    "Phone must be 8 digits and start with 6, 8, or 9");
        }

        // ===== Department =====
        if (employee.getDepartment() == null || employee.getDepartment().isBlank()) {
            errors.rejectValue("department", "department.empty", "Department name cannot be empty");
        } else if (employee.getDepartment().length() > 50) {
            errors.rejectValue("department", "department.tooLong",
                    "Department name cannot exceed 50 characters");
        }

        // ===== Designation =====
        if (employee.getDesignation() == null) {
            errors.rejectValue("designation", "designation.empty", "Please select a designation");
        }

        // ===== Employee Status =====
        if (employee.getEmployeeStatus() == null) {
            errors.rejectValue("employeeStatus", "employeeStatus.empty", "Please select employee status");
        }

        // ===== Hire Date =====
        if (employee.getHire_date() == null) {
            errors.rejectValue("hire_date", "hire_date.empty", "Hire date is required");
        }

        // ===== Business Rule =====
        if (employee.getFirst_name() != null &&
            employee.getLast_name() != null &&
            employee.getFirst_name().equalsIgnoreCase(employee.getLast_name())) {

            errors.rejectValue("last_name",
                    "name.same",
                    "First name and last name cannot be the same");
        }

        log.debug("Employee validation completed for: {}", employee);
    }
}