package nus_iss.LAPS.validators;

import nus_iss.LAPS.model.Designation;
import nus_iss.LAPS.model.LeaveBalance;
import nus_iss.LAPS.model.NameTypeEnum;
import nus_iss.LAPS.repository.LeaveBalanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class LeaveBalanceValidator implements Validator {

	@Autowired
	private LeaveBalanceRepository leaveBalanceRepo;

	@Override
	public boolean supports(Class<?> clazz) {
		return LeaveBalance.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {

		LeaveBalance balance = (LeaveBalance) target;

		// 1. Null checks
		if (balance.getEmployee() == null) {
			errors.rejectValue("employee", "employee.null", "Employee is required.");
		}

		if (balance.getLeaveType() == null) {
			errors.rejectValue("leaveType", "leaveType.null", "Leave type is required.");
		}

		if (errors.hasErrors())
			return;

		double totalDays = balance.getTotalDays();
		double usedDays = balance.getUsedDays();

		// 2. Basic validation
		if (totalDays < 0) {
			errors.rejectValue("totalDays", "error.total.negative",
					"Total days cannot be negative.");
		}

		if (usedDays < 0) {
			errors.rejectValue("usedDays", "error.used.negative", "Used days cannot be negative.");
		}

		if (usedDays > totalDays) {
			errors.rejectValue("usedDays", "error.used.exceed",
					"Total days cannot be less than used days.");
		}

		if (usedDays > totalDays) {
			errors.rejectValue("usedDays", "error.used.exceed",
					"Total days cannot be less than used days.");
		}

		// 3. Leave type rules
		NameTypeEnum type = balance.getLeaveType().getName();
		if (type == NameTypeEnum.MEDICAL) {
			if (totalDays > 60) {
				errors.rejectValue("totalDays", "error.total.day",
						balance.getLeaveType().getName() + " leave : maximum 60 days ");
			}
		}

		Designation designation = balance.getEmployee().getDesignation();

		if (type == NameTypeEnum.ANNUAL) {
			if (designation == Designation.ADMINISTRATIVE) {
				if (totalDays > 14) {
					errors.rejectValue("totalDays", "error.total.day",
							balance.getLeaveType().getName() + " leave for " + designation
									+ " staff : maximum 14 days.");
				}
			}
			if (designation == Designation.PROFESSIONAL) {
				if (totalDays > 18) {
					errors.rejectValue("totalDays", "error.total.day",
							balance.getLeaveType().getName() + " leave for " + designation
									+ " staff : maximum 18 days.");
				}
			}
		}
		if (type == NameTypeEnum.COMPENSATION) {
			if ((totalDays * 2) % 1 != 0) {
				errors.rejectValue("totalDays", "error.total.half",
						balance.getLeaveType().getName() + " leave must be in 0.5 increments.");
			}
			if ((usedDays * 2) % 1 != 0) {
				errors.rejectValue("usedDays", "error.used.half",
						balance.getLeaveType().getName() + " leave must be in 0.5 increments.");
			}
		} else {
			if (totalDays % 1 != 0) {
				errors.rejectValue("totalDays", "error.total.whole",
						balance.getLeaveType().getName() + " leave must be whole days.");
			}
			if (usedDays % 1 != 0) {
				errors.rejectValue("usedDays", "error.used.whole",
						balance.getLeaveType().getName() + " leave must be whole days.");
			}

		}

		// 4. Duplicate check
		boolean isEdit = balance.getLeaveBalanceId() != null;

		if (!isEdit) {
			if (balance.getEmployee() != null && balance.getLeaveType() != null) {
				boolean exists = leaveBalanceRepo.existsByEmployeeAndLeaveType(
						balance.getEmployee().getEmp_id(), balance.getLeaveType().getLeaveTypeId());

				if (exists) {
					errors.rejectValue("leaveType", "error.duplicate",
							"This employee " + balance.getEmployee().getFirst_name() + " "
									+ balance.getEmployee().getLast_name() + " already has "
									+ balance.getLeaveType().getName() + " leave type.");
				}
			}
		}

	}
}