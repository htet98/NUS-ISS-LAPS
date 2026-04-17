package nus_iss.LAPS.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import nus_iss.LAPS.util.GlobalConstants;

@ControllerAdvice
public class GlobalControllerAdvice {
    @ModelAttribute("defaultPageSize")
    public int defaultPageSize() {
        return GlobalConstants.DEFAULT_PAGE_SIZE_INT;
    }

    @ModelAttribute("DATE_FORMAT")
    public String dateFormat() {
        return GlobalConstants.DATE_FORMAT;
    }

    @ModelAttribute("DATE_DISPLAY_FORMAT")
    public String dateDisplayFormat() {
        return GlobalConstants.DATE_DISPLAY_FORMAT;
    }

    @ModelAttribute("DATE_TIME_DISPLAY_FORMAT")
    public String dateTimeDisplayFormat() {
        return GlobalConstants.DATE_TIME_DISPLAY_FORMAT;
    }

    @ModelAttribute("ROLE_EMPLOYEE") public String roleEmployee() { return GlobalConstants.ROLE_EMPLOYEE; }
    @ModelAttribute("ROLE_MANAGER")  public String roleManager()  { return GlobalConstants.ROLE_MANAGER; }
    @ModelAttribute("ROLE_ADMIN")    public String roleAdmin()    { return GlobalConstants.ROLE_ADMIN; }

    @ModelAttribute("STATUS_APPLIED")   public String statusApplied()   { return GlobalConstants.STATUS_APPLIED; }
    @ModelAttribute("STATUS_UPDATED")   public String statusUpdated()   { return GlobalConstants.STATUS_UPDATED; }
    @ModelAttribute("STATUS_APPROVED")  public String statusApproved()  { return GlobalConstants.STATUS_APPROVED; }
    @ModelAttribute("STATUS_REJECTED")  public String statusRejected()  { return GlobalConstants.STATUS_REJECTED; }
    @ModelAttribute("STATUS_CANCELLED") public String statusCancelled() { return GlobalConstants.STATUS_CANCELLED; }

    @ModelAttribute("FLASH_SUCCESS") public String flashSuccess() { return GlobalConstants.FLASH_SUCCESS; }
    @ModelAttribute("FLASH_ERROR")   public String flashError()   { return GlobalConstants.FLASH_ERROR; }

    // Navigation Routes
    @ModelAttribute("ROUTE_LEAVE_APPLY") public String routeLeaveApply() { return GlobalConstants.ROUTE_LEAVE + GlobalConstants.ROUTE_LEAVE_APPLY; }
    @ModelAttribute("ROUTE_LEAVE_HISTORY") public String routeLeaveHistory() { return GlobalConstants.ROUTE_LEAVE + GlobalConstants.ROUTE_LEAVE_HISTORY; }
    @ModelAttribute("ROUTE_MANAGER_PENDING") public String routeManagerPending() { return GlobalConstants.ROUTE_LEAVE + GlobalConstants.ROUTE_MANAGER_PENDING; }
    @ModelAttribute("ROUTE_MANAGER_ALL") public String routeManagerAll() { return GlobalConstants.ROUTE_LEAVE + GlobalConstants.ROUTE_MANAGER_ALL; }
    @ModelAttribute("ROUTE_ADMIN_HIERARCHY") public String routeAdminHierarchy() { return GlobalConstants.ROUTE_ADMIN + GlobalConstants.ROUTE_ADMIN_HIERARCHY; }

    @ModelAttribute("ROUTE_LEAVE_BASE") public String routeLeaveBase() { return GlobalConstants.ROUTE_LEAVE; }
    @ModelAttribute("ROUTE_LEAVE_DETAIL") public String routeLeaveDetail() { return GlobalConstants.ROUTE_LEAVE + GlobalConstants.ROUTE_LEAVE_DETAIL; }
    @ModelAttribute("ROUTE_LEAVE_EDIT") public String routeLeaveEdit() { return GlobalConstants.ROUTE_LEAVE + GlobalConstants.ROUTE_LEAVE_EDIT; }
    @ModelAttribute("ROUTE_LEAVE_DELETE") public String routeLeaveDelete() { return GlobalConstants.ROUTE_LEAVE + GlobalConstants.ROUTE_LEAVE_DELETE; }
    @ModelAttribute("ROUTE_LEAVE_CANCEL") public String routeLeaveCancel() { return GlobalConstants.ROUTE_LEAVE + GlobalConstants.ROUTE_LEAVE_CANCEL; }
    @ModelAttribute("ROUTE_MANAGER_APPROVE") public String routeManagerApprove() { return GlobalConstants.ROUTE_LEAVE + GlobalConstants.ROUTE_MANAGER_APPROVE; }
    @ModelAttribute("ROUTE_MANAGER_REJECT") public String routeManagerReject() { return GlobalConstants.ROUTE_LEAVE + GlobalConstants.ROUTE_MANAGER_REJECT; }

    // Movement routes
    @ModelAttribute("ROUTE_MOVEMENT_REGISTER") public String routeMovementRegister() { return GlobalConstants.ROUTE_MOVEMENT + GlobalConstants.ROUTE_MOVEMENT_REGISTER; }

    // Report routes
    @ModelAttribute("ROUTE_REPORT")            public String routeReport()      { return GlobalConstants.ROUTE_REPORT; }
    @ModelAttribute("ROUTE_REPORT_LEAVE")      public String routeReportLeave() { return GlobalConstants.ROUTE_REPORT + GlobalConstants.ROUTE_REPORT_LEAVE; }
}
