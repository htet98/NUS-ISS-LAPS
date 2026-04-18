package nus_iss.LAPS.validators;

import nus_iss.LAPS.model.User;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

@Component
public class UserValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return User.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        User user = (User) target;

        // Validate username
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "username", "username.empty", "Username cannot be empty");
        if (user.getUsername() != null && user.getUsername().length() > 50) {
            errors.rejectValue("username", "username.tooLong", "Username cannot exceed 50 characters");
        }

        // Validate email
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "email", "email.empty", "Email cannot be empty");
        if (user.getEmail() != null && !user.getEmail().matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$")) {
            errors.rejectValue("email", "email.invalid", "Invalid email format");
        }
        if (user.getEmail() != null && user.getEmail().length() > 255) {
            errors.rejectValue("email", "email.tooLong", "Email cannot exceed 255 characters");
        }

        // Validate password
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "password", "password.empty", "Password cannot be empty");
        if (user.getPassword() != null && user.getPassword().length() > 255) {
            errors.rejectValue("password", "password.tooLong", "Password cannot exceed 255 characters");
        }

        // Validate role
        if (user.getRole() == null) {
            errors.rejectValue("role", "role.empty", "Role cannot be empty");
        }

        // Validate createdby
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "createdby", "createdby.empty", "Created by cannot be empty");
        if (user.getCreatedby() != null && user.getCreatedby().length() > 50) {
            errors.rejectValue("createdby", "createdby.tooLong", "Created by cannot exceed 50 characters");
        }

        // Validate updatedby
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "updatedby", "updatedby.empty", "Updated by cannot be empty");
        if (user.getUpdatedby() != null && user.getUpdatedby().length() > 50) {
            errors.rejectValue("updatedby", "updatedby.tooLong", "Updated by cannot exceed 50 characters");
        }

        // Note: createdwhen and updatedwhen are handled by @PrePersist and @PreUpdate
        // Note: active is handled by default value in the entity
    }
}