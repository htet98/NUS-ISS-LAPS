package nus_iss.LAPS.util;

public final class ApiRoutes {
    private ApiRoutes() {} // Prevent instantiation

    public static final String BASE = "/api";
    public static final String LEAVE = BASE + "/leave";
    public static final String LEAVE_EMPLOYEE = "/employee/{employeeId}";
    public static final String LEAVE_FILTER = "/employee/{employeeId}/filter";
    public static final String LEAVE_ID = "/{id}";
    public static final String LEAVE_CANCEL = "/{id}/cancel";
    public static final String MANAGER_PENDING = "/manager/{managerId}/pending";
    public static final String MANAGER_SUBORDINATES = "/manager/{managerId}/subordinates";
    public static final String MANAGER_RECENT = "/manager/{managerId}/recent-decisions";
    public static final String MANAGER_APPROVE = "/{id}/approve";
    public static final String MANAGER_REJECT = "/{id}/reject";
}
