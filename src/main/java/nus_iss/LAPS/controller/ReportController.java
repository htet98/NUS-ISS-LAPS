package nus_iss.LAPS.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.model.LeaveApplication;
import nus_iss.LAPS.repository.EmployeeRepository;
import nus_iss.LAPS.repository.LeaveApplicationRepository;
import nus_iss.LAPS.repository.LeaveTypeRepository;
import nus_iss.LAPS.util.BreadcrumbItem;
import nus_iss.LAPS.util.GlobalConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * Manager-facing reporting controller.
 *
 * Routes:
 *   GET  /report                     – report selection page
 *   GET  /report/leave               – leave report (HTML)
 *   GET  /report/leave/export        – leave report (CSV download)
 *
 * Design note: the Employee stored in the HTTP session is a detached JPA entity.
 * Never call lazy collections on it (e.g. getSubordinates(), getSupervisor()).
 * Always reload what you need via the repository within the current request.
 *
 * Author: Htet Nandar (Grace)
 */
@Controller
@RequestMapping(GlobalConstants.ROUTE_REPORT)
public class ReportController {

    @Autowired private LeaveApplicationRepository leaveAppRepo;
    @Autowired private LeaveTypeRepository        leaveTypeRepo;
    @Autowired private EmployeeRepository         employeeRepo;

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Long getSessionUserId(HttpSession session) {
        return (Long) session.getAttribute("userId");
    }

    private boolean isLoggedIn(HttpSession session) {
        return getSessionUserId(session) != null;
    }

    /**
     * Returns a fresh, attached Employee from the DB using the session's userId.
     * Never uses the detached entity stored in session directly.
     */
    private Employee getManager(HttpSession session) {
        Employee sessionEmp = (Employee) session.getAttribute("employee");
        if (sessionEmp == null) return null;
        return employeeRepo.findById(sessionEmp.getEmp_id()).orElse(null);
    }

    // ── Index ─────────────────────────────────────────────────────────────────

    @GetMapping
    public String index(HttpSession session, Model model) {
        if (!isLoggedIn(session)) return GlobalConstants.REDIRECT_LOGIN;
        Employee mgr = getManager(session);
        if (mgr == null) return GlobalConstants.REDIRECT_LOGIN;

        // Load subordinates fresh from DB — never use session entity's lazy collection
        List<Employee> subordinates = employeeRepo.findSubordinates(mgr.getEmp_id());

        model.addAttribute("leaveTypes",   leaveTypeRepo.findAll());
        model.addAttribute("subordinates", subordinates);
        model.addAttribute("defaultStart", LocalDate.now().withDayOfMonth(1).toString());
        model.addAttribute("defaultEnd",   LocalDate.now().toString());
        
        // Add breadcrumbs
        List<BreadcrumbItem> breadcrumbs = Arrays.asList(
            new BreadcrumbItem("Reports", null)
        );
        model.addAttribute("breadcrumbs", breadcrumbs);

        return GlobalConstants.VIEW_REPORT_INDEX;
    }

    // ── Leave Report (HTML) ───────────────────────────────────────────────────

    @GetMapping(GlobalConstants.ROUTE_REPORT_LEAVE)
    public String leaveReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long leaveTypeId,
            HttpSession session, Model model) {

        if (!isLoggedIn(session)) return GlobalConstants.REDIRECT_LOGIN;
        Employee mgr = getManager(session);
        if (mgr == null) return GlobalConstants.REDIRECT_LOGIN;

        List<LeaveApplication> results = leaveAppRepo.findApprovedLeavesForReport(
                mgr, startDate, endDate, leaveTypeId);

        model.addAttribute("applications", results);
        model.addAttribute("startDate",    startDate);
        model.addAttribute("endDate",      endDate);
        model.addAttribute("leaveTypeId",  leaveTypeId);
        model.addAttribute("leaveTypes",   leaveTypeRepo.findAll());
        model.addAttribute("subordinates", employeeRepo.findSubordinates(mgr.getEmp_id()));
        
        // Add breadcrumbs
        List<BreadcrumbItem> breadcrumbs = Arrays.asList(
            new BreadcrumbItem("Reports", GlobalConstants.ROUTE_REPORT),
            new BreadcrumbItem("Leave Report", null)
        );
        model.addAttribute("breadcrumbs", breadcrumbs);

        return GlobalConstants.VIEW_REPORT_LEAVE;
    }

    // ── Leave Report (CSV) ────────────────────────────────────────────────────

    @GetMapping(GlobalConstants.ROUTE_REPORT_LEAVE_EXPORT)
    public void leaveReportCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long leaveTypeId,
            HttpSession session,
            HttpServletResponse response) throws IOException {

        if (!isLoggedIn(session)) { response.sendRedirect("/login"); return; }  // raw servlet redirect — no "redirect:" prefix
        Employee mgr = getManager(session);
        if (mgr == null) { response.sendRedirect("/login"); return; }

        List<LeaveApplication> results = leaveAppRepo.findApprovedLeavesForReport(
                mgr, startDate, endDate, leaveTypeId);

        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"leave_report_" + startDate + "_to_" + endDate + ".csv\"");

        try (PrintWriter pw = response.getWriter()) {
            pw.println("Application ID,Employee Name,Leave Type,Start Date,End Date,Duration (days),Reason,Work Dissemination,Overseas");
            for (LeaveApplication la : results) {
                pw.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        csvEsc(String.valueOf(la.getLeaveApplicationId())),
                        csvEsc(la.getEmployee().getFirst_name() + " " + la.getEmployee().getLast_name()),
                        csvEsc(la.getLeaveType() != null ? la.getLeaveType().getName().toString() : ""),
                        la.getStartDate() != null ? la.getStartDate().format(DISPLAY_FMT) : "",
                        la.getEndDate()   != null ? la.getEndDate().format(DISPLAY_FMT)   : "",
                        la.getDurationDays() != null ? la.getDurationDays() : "",
                        csvEsc(la.getReason()),
                        csvEsc(la.getWorkDissemination() != null ? la.getWorkDissemination() : ""),
                        la.isOverseas() ? "Yes" : "No");
            }
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /** Escapes a CSV field: wraps in quotes if it contains comma, quote, or newline. */
    private String csvEsc(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
