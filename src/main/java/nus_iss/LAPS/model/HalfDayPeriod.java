package nus_iss.LAPS.model;

/**
 * Which half of the working day is being taken for a half-day leave.
 * Applies only when LeaveApplication.isHalfDay is true (Compensation Leave).
 */
public enum HalfDayPeriod {
    MORNING,
    AFTERNOON
}