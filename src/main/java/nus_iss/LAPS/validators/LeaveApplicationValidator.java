package nus_iss.LAPS.validators;

import lombok.extern.slf4j.Slf4j;
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

/**
 * Validates LeaveApplication objects before they are saved (submit or update).
 *
 * Implements Spring's Validator interface so it can be wired into the service layer
 * and reused for both the MVC form submission and the REST API path.
 *
 * Validation is split into two layers:
 *   1. Common rules (mandatory fields, date order, no past dates) — applied to every leave type.
 *   2. Leave-type-specific rules (balance check, working-day check, annual cap) — dispatched
 *      via a switch on the leave type name.
 *
 * Author: Htet Nandar (Grace)
 */
@Slf4j
@Component
public class LeaveApplicationValidator implements Validator {

    // @Lazy is required here to break a circular Spring bean dependency.
    // LeaveApplicationService injects LeaveApplicationValidator,
    // and LeaveApplicationValidator injects LeaveApplicationService.
    // Without @Lazy, Spring cannot resolve which bean to create first and throws a
    // BeanCurrentlyInCreationException at startup.
    // @Lazy tells Spring to create a proxy for LeaveApplicationService and only
    // instantiate the real bean the first time it is actually called at runtime.
    @Lazy
    @Autowired
    private LeaveApplicationService leaveApplicationService;

    @Autowired
    private LeaveApplicationRepository leaveApplicationRepo;

    // Used to check available leave balance for Annual and Compensation leave
    @Autowired
    private LeaveBalanceService leaveBalanceService;

    // Used to look up public holidays when counting working days
    @Autowired
    private PublicHolidayRepository publicHolidayRepository;

    /**
     * Tells Spring MVC which model class this validator handles.
     * Returns true only for LeaveApplication (or its subclasses).
     */
    @Override
    public boolean supports(Class<?> clazz) {
        return LeaveApplication.class.isAssignableFrom(clazz);
    }

    /**
     * Main entry point called by LeaveApplicationService before every save.
     * Runs all common rules first, then delegates to the leave-type-specific method.
     *
     * Any error added to the Errors object causes the service to throw an
     * IllegalArgumentException with the first error message, which is shown to the user.
     */
    @Override
    public void validate(Object target, Errors errors) {
        LeaveApplication application = (LeaveApplication) target;

        // ── Rule 1: Mandatory field checks ────────────────────────────────────
        // All of these must be present before any date or business logic can run.

        if (application.getLeaveType() == null) {
            errors.rejectValue("leaveType", "leaveType.null", "Leave type is required.");
        }
        if (application.getStartDate() == null) {
            errors.rejectValue("startDate", "startDate.null", "Start date is required.");
        }
        if (application.getEndDate() == null) {
            errors.rejectValue("endDate", "endDate.null", "End date is required.");
        }

        // ValidationUtils.rejectIfEmptyOrWhitespace checks for null, empty string, and
        // strings that are only whitespace — equivalent to a @NotBlank constraint.
        ValidationUtils.rejectIfEmptyOrWhitespace(
                errors, "reason", "error.reason", "Reason for leave is required.");

        if (application.getStatus() == null) {
            errors.rejectValue("status", "status.null", "Status is required.");
        }

        // isOverseas is a Boolean (nullable), so null means the user did not answer the question.
        if (application.getIsOverseas() == null) {
            errors.rejectValue("isOverseas", "error.isOverseas",
                    "Please indicate whether you will be overseas.");
        }

        // If the employee is travelling overseas, a contact number or address is required
        // so the manager can reach them in an emergency.
        // Boolean.TRUE.equals() is used instead of == true to safely handle null without NPE.
        if (Boolean.TRUE.equals(application.getIsOverseas())
                && (application.getContactDetails() == null
                    || application.getContactDetails().isBlank())) {
            errors.rejectValue("contactDetails", "error.contactDetails",
                    "Contact details are required when travelling overseas.");
        }

        // Stop immediately if any mandatory field is missing.
        // The date and business rules below would throw NullPointerExceptions if we continued.
        if (errors.hasErrors()) return;

        // ── Rule 2: Date order check ───────────────────────────────────────────
        // End date must be the same as or after start date.
        // Example: start = April 5, end = April 3 → rejected.
        if (application.getEndDate().isBefore(application.getStartDate())) {
            errors.rejectValue("endDate", "error.endDate.order",
                    "End date must be on or after start date.");
            return;
        }

        // ── Rule 3: No past start dates ────────────────────────────────────────
        // Employees cannot apply for leave that has already started or ended in the past.
        // LocalDate.now() returns today's date without a time component.
        if (application.getStartDate().isBefore(LocalDate.now())) {
            errors.rejectValue("startDate", "error.startDate.past",
                    "Start date must be today or a future date.");
            return;
        }

        // ── Rule 4: Leave-type-specific validation ─────────────────────────────
        // Each leave type has its own business rules (balance, working days, caps).
        // The switch routes to the correct private method based on the leave type name enum.
        // For custom/dynamically created leave types, apply generic leave validation
        // (no special balance check or working-day restrictions).
        NameTypeEnum typeName = application.getLeaveType().getName();
        switch (typeName) {
            case ANNUAL       -> validateAnnualLeave(application, errors);
            case MEDICAL      -> validateMedicalLeave(application, errors);
            case COMPENSATION -> validateCompensationLeave(application, errors);
            default -> {
                // New/custom leave types created by admins are allowed.
                // They pass through with only basic validation (dates, mandatory fields).
                // No special balance or working-day constraints are applied.
                log.info("Processing custom leave type: {}", typeName);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Leave-type-specific validation methods
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Annual Leave rules:
     *   - Start and end dates must both fall on working days (not weekends or public holidays).
     *   - The requested duration must not exceed the employee's available annual leave balance.
     *
     * Duration is counted as working days for periods ≤ 14 days,
     * and as calendar days for longer periods (see computeDuration).
     */
    private void validateAnnualLeave(LeaveApplication app, Errors errors) {
        LocalDate start = app.getStartDate();
        LocalDate end   = app.getEndDate();

        // Annual leave must start and end on a working day.
        // Taking leave on a weekend would be unusual and likely a data entry mistake.
        if (!isWorkingDay(start)) {
            errors.rejectValue("startDate", "error.startDate.weekend",
                    "Annual leave cannot start on a weekend or public holiday.");
        }
        if (!isWorkingDay(end)) {
            errors.rejectValue("endDate", "error.endDate.weekend",
                    "Annual leave cannot end on a weekend or public holiday.");
        }

        // If either date is on a non-working day, stop here — no point computing duration.
        if (errors.hasErrors()) return;

        // computeDuration applies the ≤ 14-day working-day rule vs calendar-day rule.
        double duration  = computeDuration(app);

        // getAvailableBalance queries the leave_balances table for this employee and leave type,
        // returning the remaining entitlement as a double (e.g. 14.0 days remaining).
        // Bug fix: the old code incorrectly called getLeaveBalancesByEmployeeId() which returns
        // a List<LeaveBalance>, not a numeric balance. Fixed to use getAvailableBalance().
        double available = leaveBalanceService.getAvailableBalance(app.getEmployee(), app.getLeaveType());

        if (duration <= 0) {
            // This should rarely happen if date order is already validated, but is a safety net.
            errors.rejectValue("startDate", "error.duration.invalid",
                    "Leave duration must be at least 1 day.");
        } else if (duration > available) {
            // Prevent the employee from applying for more days than they have.
            errors.rejectValue("leaveType", "error.balance.insufficient",
                    String.format(
                            "Insufficient annual leave balance. " +
                            "Requested: %.1f day(s), Available: %.1f day(s).",
                            duration, available));
        }
    }

    /**
     * Medical Leave rules:
     *   - Total approved medical leave for the current calendar year must not exceed 60 days.
     *   - Weekends are included in the count (a doctor's MC covers calendar days, not working days).
     *
     * There is no balance deduction for medical leave — it draws from a fixed 60-day annual cap
     * tracked by summing approved records in the database, not from a leave_balances row.
     */
    private void validateMedicalLeave(LeaveApplication app, Errors errors) {
        // Get the year of the start date to scope the annual cap check.
        int year = app.getStartDate().getYear();

        // Query the database to sum all previously APPROVED medical leave days for this employee
        // in this calendar year. This includes applications already approved before this one.
        double approvedDays  = leaveApplicationRepo.sumApprovedMedicalLeaveByEmployeeAndYear(
                app.getEmployee(), year);

        // Compute the duration of the current request (calendar days for medical leave).
        double requestedDays = computeDuration(app);

        // The combined total of already-approved days and the current request must not exceed 60.
        // Example: employee has 55 approved days and is requesting 7 → 62 > 60 → rejected.
        if (approvedDays + requestedDays > 60.0) {
            errors.rejectValue("leaveType", "error.medicalLeave.limitExceeded",
                    String.format(
                            "Medical leave limit exceeded. " +
                            "Used: %.1f day(s), Requested: %.1f day(s). Annual limit: 60 days.",
                            approvedDays, requestedDays));
        }
    }

    /**
     * Compensation Leave rules:
     *   - Must be taken in 0.5-day increments (half-day units): 0.5, 1.0, 1.5, 2.0, etc.
     *   - The requested duration must not exceed the employee's available compensation balance.
     *
     * The half-day increment check uses the formula: (duration × 2) % 1 == 0.
     * Multiplying by 2 converts halves to whole numbers, then % 1 checks for any fractional part.
     * Example: 1.5 × 2 = 3.0 → 3.0 % 1 = 0.0 → valid.
     *          1.3 × 2 = 2.6 → 2.6 % 1 = 0.6 → invalid.
     */
    private void validateCompensationLeave(LeaveApplication app, Errors errors) {
        // computeDuration returns 0.5 if isHalfDay is true, otherwise counts calendar days.
        double duration  = computeDuration(app);

        // Check that the duration is a valid half-day multiple (0.5, 1.0, 1.5, ...).
        // (duration * 2) converts 0.5 → 1, 1.0 → 2, 1.5 → 3, etc.
        // If the result has a fractional part (% 1 != 0), the duration is not a valid unit.
        if ((duration * 2) % 1 != 0) {
            errors.rejectValue("endDate", "error.comp.halfDay",
                    "Compensation leave must be taken in half-day units (0.5, 1.0, 1.5, ...).");
            return;
        }

        // Check the employee has enough compensation leave balance remaining.
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

    // ─────────────────────────────────────────────────────────────────────────
    // Duration computation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Computes the leave duration in days based on the leave type and half-day flag.
     *
     * Rules by leave type:
     *
     *   ANNUAL (≤ 14 calendar days):
     *     Count only working days — weekends and public holidays are excluded.
     *     Example: Mon Apr 7 – Fri Apr 11 = 5 working days (weekend not counted).
     *
     *   ANNUAL (> 14 calendar days):
     *     Use raw calendar days — weekends are included because the employee
     *     is away for an extended period regardless.
     *     Example: Apr 1 – Apr 30 = 30 calendar days.
     *
     *   COMPENSATION + isHalfDay = true:
     *     Always exactly 0.5 days, regardless of the date range.
     *     The start and end date will be the same single day in this case.
     *
     *   MEDICAL or COMPENSATION (full day):
     *     Raw calendar days — weekends and public holidays are included.
     *     A medical certificate covers the full period including rest days.
     *
     * This method is also called by the balance deduction logic in LeaveBalanceService,
     * so the duration computed here directly determines how many days are deducted.
     */
    public double computeDuration(LeaveApplication app) {
        LocalDate start   = app.getStartDate();
        LocalDate end     = app.getEndDate();
        NameTypeEnum type = app.getLeaveType().getName();

        // ── Annual Leave ───────────────────────────────────────────────────────
        if (NameTypeEnum.ANNUAL.equals(type)) {
            // ChronoUnit.DAYS.between() is exclusive of the end date, so +1 makes it inclusive.
            // Example: between(Apr 1, Apr 3) = 2 → +1 = 3 calendar days.
            long calendarDays = ChronoUnit.DAYS.between(start, end) + 1;

            // For short leave (≤ 14 days): charge only working days.
            // For long leave (> 14 days): charge raw calendar days (includes weekends).
            return calendarDays <= 14
                    ? countWorkingDays(start, end)
                    : calendarDays;
        }

        // ── Compensation Leave (half-day) ──────────────────────────────────────
        // If the half-day checkbox is ticked, duration is always 0.5 regardless of dates.
        // Boolean.TRUE.equals() safely handles null without throwing NullPointerException
        // (unboxing a null Boolean directly would cause an NPE).
        if (NameTypeEnum.COMPENSATION.equals(type) && Boolean.TRUE.equals(app.getIsHalfDay())) {
            return 0.5;
        }

        // ── Medical and Compensation (full day) ────────────────────────────────
        // Both use raw calendar days. Weekends and public holidays are counted.
        // +1 for the same inclusive-end reason as above.
        return ChronoUnit.DAYS.between(start, end) + 1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helper methods
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Counts the number of working days between start and end (both inclusive).
     *
     * Strategy:
     *   1. Fetch all public holidays in the range from the database in a single query
     *      (more efficient than querying per date).
     *   2. Generate a stream of every date in the range using datesUntil().
     *      datesUntil() is exclusive of the end date, so end.plusDays(1) makes it inclusive.
     *   3. Filter out weekends and public holidays.
     *   4. Count what remains — those are the billable working days.
     *
     * Returns a double to be compatible with the 0.5 half-day value used elsewhere.
     */
    private double countWorkingDays(LocalDate start, LocalDate end) {
        // Single DB query to get all public holidays in the range, stored in a Set for O(1) lookup.
        Set<LocalDate> publicHolidays = getPublicHolidaysInRange(start, end);

        // datesUntil(end.plusDays(1)) generates: start, start+1, start+2, ..., end (inclusive).
        // Each date is kept only if it is NOT a weekend AND NOT a public holiday.
        return start.datesUntil(end.plusDays(1))
                .filter(d -> !isWeekend(d) && !publicHolidays.contains(d))
                .count();
    }

    /**
     * Returns true if the given date is a working day — not a weekend and not a public holiday.
     * Used to validate that Annual Leave start/end dates fall on working days.
     */
    private boolean isWorkingDay(LocalDate date) {
        return !isWeekend(date) && !publicHolidayRepository.existsByDate(date);
    }

    /**
     * Returns true if the given date falls on Saturday or Sunday.
     */
    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    /**
     * Queries the database for all public holidays between start and end (inclusive),
     * and returns them as a Set<LocalDate> for fast O(1) lookup during stream filtering.
     *
     * Fetching all holidays in one query is more efficient than calling existsByDate()
     * for each individual date when processing a range.
     */
    private Set<LocalDate> getPublicHolidaysInRange(LocalDate start, LocalDate end) {
        return publicHolidayRepository.findByDateBetween(start, end)
                .stream()
                .map(PublicHoliday::getDate)
                .collect(Collectors.toSet());
    }
}
