package nus_iss.LAPS.validator;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import lombok.extern.slf4j.Slf4j;
import nus_iss.LAPS.model.User;

@Component
@Slf4j
public class UserValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return User.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		User user = (User) target;
		ValidationUtils.rejectIfEmpty(errors, "user_id", "error.user.user_id.empty");
		ValidationUtils.rejectIfEmpty(errors, "emp_Id", "error.user.emp_id.empty");
		ValidationUtils.rejectIfEmpty(errors, "name", "error.user.name.empty");
		ValidationUtils.rejectIfEmpty(errors, "password", "error.user.password.empty");
		log.debug("Validating user: {}", user);
	}

}

