package nus_iss.LAPS.util;

public final class GlobalConstants {
    private GlobalConstants() {}

    public static final String DEFAULT_PAGE_SIZE = "10";
    public static final int DEFAULT_PAGE_SIZE_INT = 10;

    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATE_DISPLAY_FORMAT = "dd MMM yyyy";
    public static final String DATE_TIME_DISPLAY_FORMAT = "dd MMM yyyy HH:mm";

    // Roles
    public static final String ROLE_EMPLOYEE = "EMPLOYEE";
    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_ADMIN = "ADMIN";

    // View names
    public static final String VIEW_ADMIN_HIERARCHY = "admin/hierarchy";
    public static final String VIEW_ADMIN_EDIT_HIERARCHY = "admin/edit-hierarchy";
    public static final String VIEW_LOGIN = "login";
    public static final String VIEW_LEAVE_HISTORY = "leave/history";
    public static final String VIEW_LEAVE_APPLY = "leave/apply";
    public static final String VIEW_LEAVE_DETAIL = "leave/detail";
    public static final String VIEW_LEAVE_EDIT = "leave/edit";
    public static final String VIEW_MANAGER_PENDING = "leave/manager/history/pending";
    public static final String VIEW_MANAGER_ALL = "leave/manager/history/all";

    // Redirects
    public static final String REDIRECT_LOGIN = "redirect:/login";
    public static final String REDIRECT_ADMIN_HIERARCHY = "redirect:/admin/hierarchy";
    public static final String REDIRECT_ADMIN_HIERARCHY_EDIT = "redirect:/admin/hierarchy/edit/";
    public static final String REDIRECT_LEAVE_HISTORY = "redirect:/leave/history";
    public static final String REDIRECT_MANAGER_PENDING = "redirect:/leave/manager/pending";

    // Controller Mapping Routes
    public static final String ROUTE_ADMIN = "/admin";
    public static final String ROUTE_ADMIN_HIERARCHY = "/hierarchy";
    public static final String ROUTE_ADMIN_HIERARCHY_EDIT = "/hierarchy/edit/{id}";
    public static final String ROUTE_ADMIN_HIERARCHY_SAVE = "/hierarchy/save";

    public static final String ROUTE_LEAVE = "/leave";
    public static final String ROUTE_LEAVE_APPLY = "/apply";
    public static final String ROUTE_LEAVE_HISTORY = "/history";
    public static final String ROUTE_LEAVE_DETAIL = "/{id}";
    public static final String ROUTE_LEAVE_EDIT = "/{id}/edit";
    public static final String ROUTE_LEAVE_DELETE = "/{id}/delete";
    public static final String ROUTE_LEAVE_CANCEL = "/{id}/cancel";
    public static final String ROUTE_MANAGER_PENDING = "/manager/history/pending";
    public static final String ROUTE_MANAGER_ALL = "/manager/history/all";
    public static final String ROUTE_MANAGER_APPROVE = "/{id}/approve";
    public static final String ROUTE_MANAGER_REJECT = "/{id}/reject";

    // Flash message keys
    public static final String FLASH_SUCCESS = "successMessage";
    public static final String FLASH_ERROR = "errorMessage";

    // Leave Statuses
    public static final String STATUS_APPLIED = "APPLIED";
    public static final String STATUS_UPDATED = "UPDATED";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_DELETED = "DELETED";
}
