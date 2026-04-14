package nus_iss.LAPS.validators;

import nus_iss.LAPS.model.LeaveApplication;
import nus_iss.LAPS.model.NameTypeEnum;
import nus_iss.LAPS.model.PublicHoliday;
import nus_iss.LAPS.repository.LeaveApplicationRepository;
import nus_iss.LAPS.repository.PublicHolidayRepository;
import nus_iss.LAPS.service.LeaveApplicationService;
import nus_iss.LAPS.service.LeaveBalanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class LeaveApplicationValidator implements Validator {

    // @Lazy breaks the circular dependency:
    // LeaveApplicationService -> LeaveApplicationValidator -> LeaveApplicationService
    @Lazy
    @Autowired
    private LeaveApplicationService leaveApplicationService;

    @Autowired
    private LeaveApplicationRepository leaveApplicationRepo;

    @Autowired
    private LeaveBalanceService leaveBalanceService;

    @Autowired
    private PublicHolidayRepository publicHolidayRepository;

    @Override
    public boolean supports(Class<?> clazz) {
        return LeaveApplication.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        LeaveApplication application = (LeaveApplication) target;

        // Rule 1: Mandatory fields
        if (application.getLeaveType() == null) {
            errors.rejectValue("leaveType", "leaveType.null", "Leave type is required.");
        }
        if (application.getStartDate() == null) {
            errors.rejectValue("startDate", "startDate.null", "Start date is required.");
        }
        if (application.getEndDate() == null) {
            errors.rejectValue("endDate", "endDate.null", "End date is required.");
        }
        ValidationUtils.rejectIfEmptyOrWhitespace(
                errors, "reason", "error.reason", "Reason for leave is required.");
        if (application.getStatus() == null) {
            errors.rejectValue("status", "status.null", "Status is required.");
        }
        if (application.getIsOverseas() == null) {
            errors.rejectValue("isOverseas", "error.isOverseas",
                    "Please indicate whether you will be overseas.");
        }
        if (Boolean.TRUE.equals(application.getIsOverseas())
                && (application.getContactDetails() == null
                    || application.getContactDetails().isBlank())) {
            errors.rejectValue("contactDetails", "error.contactDetails",
                    "Contact details are required when travelling overseas.");
        }
        if (errors.hasErrors()) return;

        // Rule 2: Date order
        if (application.getEndDate().isBefore(application.getStartDate())) {
            errors.rejectValue("endDate", "error.endDate.order",
                    "End date must be on or after start date.");
            return;
        }

        // Rule 3: Start date not in the past
        if (application.getStartDate().isBefore(LocalDate.now())) {
            errors.rejectValue("startDate", "error.startDate.past",
                    "Start date must be today or a future date.");
            return;
        }

        // Rule 4: Leave-type-specific rules
        NameTypeEnum typeName = application.getLeaveType().getName();
        switch (typeName) {
            case ANNUAL       -> validateAnnualLeave(application, errors);
            case MEDICAL      -> validateMedicalLeave(application, errors);
            case COMPENSATION -> validateCompensationLeave(application, errors);
            default -> errors.rejectValue("leaveType", "error.leaveType.unknown",
                    "Unknown leave type: " + application.getLeaveType().getName());
        }
    }

    // Annual Leave validation
    private void validateAnnualLeave(LeaveApplication app, Errors errors) {
        LocalDate start = app.getStartDate();
        LocalDate end   = app.getEndDate();

        if (!isWorkingDay(start)) {
            errors.rejectValue("startDate", "error.startDate.weekend",
                    "Annual leave cannot start on a weekend or public holiday.");
        }
        if (!isWorkingDay(end)) {
            errors.rejectValue("endDate", "error.endDate.weekend",
                    "Annual leave cannot end on a weekend or public holiday.");
        }
        if (errors.hasErrors()) return;

        double duration  = computeDuration(app);
        // Bug fix: was calling getLeaveBalancesByEmployeeId() which returns List<LeaveBalance>.
        // getAvailableBalance(Employee, LeaveType) returns the remaining days as a double.
        double available = leaveBalanceService.getAvailableBalance(app.getEmployee(), app.getLeaveType());

        if (duration <= 0) {
            errors.rejectValue("startDate", "error.duration.invalid",
                    "Leave duration must be at least 1 day.");
        } else if (duration > available) {
            errors.rejectValue("leaveType", "error.balance.insufficient",
                    String.format(
                            "Insufficient annual leave balance. " +
                            "Requested: %.1f day(s), Available: %.1f day(s).",
                            duration, available));
        }
    }

    // Medical Leave validation
    private void validateMedicalLeave(LeaveApplication app, Errors errors) {
        int year = app.getStartDate().getYear();
        double approvedDays  = leaveApplicationRepo.sumApprovedMedicalLeaveByEmployeeAndYear(
                app.getEmployee(), year);
        double requestedDays = computeDuration(app);

        if (approvedDays + requestedDays > 60.0) {
            errors.rejectValue("leaveType", "error.medicalLeave.limitExceeded",
                    String.format(
                            "Medical leave limit exceeded. " +
                            "Used: %.1f day(s), Requested: %.1f day(s). Annual limit: 60 days.",
                            approvedDays, requestedDays));
        }
    }

    // Compensation Leave validation
    private void validateCompensationLeave(LeaveApplication app, Errors errors) {
        double duration  = computeDuration(app);

        if ((duration * 2) % 1 != 0) {
            errors.rejectValue("endDate", "error.comp.halfDay",
                    "Compensation leave must be taken in half-day units (0.5, 1.0, 1.5, ...).");
            return;
        }

        double available = leaveBalanceService.getAvailableBalance(
                app.getEmployee(), app.getLeaveType());

        if (duration > available) {
            errors.rejectValue("leaveType", "error.balance.insufficient",
                    String.format(
                            "Insufficient compensation leave balance. " +
                            "Requested: %.1f day(s), Available: %.1f day(s).",
                            duration, available));
        }
    }

    // Duration computation
    public double computeDuration(LeaveApplication app) {
        LocalDate start   = app.getStartDate();
        LocalDate end     = app.getEndDate();
        NameTypeEnum type = app.getLeaveType().getName();

        if (NameTypeEnum.ANNUAL.equals(type)) {
            long calendarDays = ChronoUnit.DAYS.between(start, end) + 1;
            return calendarDays <= 14
                    ? countWorkingDays(start, end)
                    : calendarDays;
        }
        // Medical and Compensation: always calendar days
        return ChronoUnit.DAYS.between(start, end) + 1;
    }

    private double countWorkingDays(LocalDate start, LocalDate end) {
        Set<LocalDate> publicHolidays = getPublicHolidaysInRange(start, end);
        return start.datesUntil(end.plusDays(1))
                .filter(d -> !isWeekend(d) && !publicHolidays.contains(d))
                .count();
    }

    private boolean isWorkingDay(LocalDate date) {
        return !isWeekend(date) && !publicHolidayRepository.existsByDate(date);
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private Set<LocalDate> getPublicHolidaysInRange(LocalDate start, LocalDate end) {
        return publicHolidayRepository.findByDateBetween(start, end)
                .stream()
                .map(PublicHoliday::getDate)
                .collect(Collectors.toSet());
    }
}
