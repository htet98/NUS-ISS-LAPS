package nus_iss.LAPS.validators;

import java.time.LocalDate;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nus_iss.LAPS.model.Employee;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeValidator implements Validator{

	/**
	 * Author: Junior
	 * Created on: 15/04/2026
	 */
	
	@Override
	public boolean supports(Class<?> clazz) {
		return Employee.class.isAssignableFrom(clazz);
	}
	
	//Author: Thiha
	@Override
	public void validate(Object target, Errors errors) {
		Employee employee = (Employee) target;

		// Validate First_name
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "first_name", "first_name.empty",
				"First name cannot be empty");
		if (employee.getFirst_name() != null && employee.getFirst_name().length() > 50) {
			errors.rejectValue("first_name", "first_name.tooLong", "First name cannot exceed 50 characters");
		}

		// Validate Last_name
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "last_name", "last_name.empty", "Last name cannot be empty");
		if (employee.getLast_name() != null && employee.getLast_name().length() > 50) {
			errors.rejectValue("last_name", "last_name.tooLong", "Last name cannot exceed 50 characters");
		}

		// Validate email
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "email", "email.empty", "Email cannot be empty");
		if (employee.getEmail() != null
				&& !employee.getEmail().matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$")) {
			errors.rejectValue("email", "email.invalid", "Invalid email format");
		}
		if (employee.getEmail() != null && employee.getEmail().length() > 255) {
			errors.rejectValue("email", "email.tooLong", "Email cannot exceed 255 characters");
		}

		// Validate phone number
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "phoneNumber", "phoneNumber.empty",
				"Phone number cannot be empty");
		if (employee.getPhoneNumber() != null && !employee.getPhoneNumber().matches("^[689]\\d{7}$")) {
			errors.rejectValue("phoneNumber", "phoneNumber.invalid", "Invalid phone number format");
		}

		// Validate department
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "department", "department.empty",
				"department name cannot be empty");
		if (employee.getDepartment() != null && employee.getDepartment().length() > 50) {
			errors.rejectValue("department", "department.tooLong", "Department name cannot exceed 50 characters");
		}

		// Validate hire date
		if (employee.getHire_date() == null) {
			errors.rejectValue("hire_date", "hire_date.empty", "Hire date is required");
		}
		if (employee.getHire_date() != null && employee.getHire_date().isAfter(LocalDate.now())) {
			errors.rejectValue("hire_date", "error.hire_date.future", "Hire date must not be in future");
			return;
		}
		
		//added by Junior
		// Validate same firstname and lastname
		if (employee.getFirst_name() != null &&
	            employee.getLast_name() != null &&
	            employee.getFirst_name().equalsIgnoreCase(employee.getLast_name())) {

	            errors.rejectValue("last_name",
	                    "error.employee.name.same",
	                    "First name and last name cannot be the same.");
	        }

	        log.debug("Validating employee: {}", employee);
	}
}
