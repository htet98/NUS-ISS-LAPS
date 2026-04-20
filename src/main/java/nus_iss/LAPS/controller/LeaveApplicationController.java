package nus_iss.LAPS.controller;

import nus_iss.LAPS.api.LeaveApplicationRestController;
import nus_iss.LAPS.dto.*;
import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.model.HalfDayPeriod;
import nus_iss.LAPS.model.LeaveApplication;
import nus_iss.LAPS.model.LeaveStatus;
import nus_iss.LAPS.model.LeaveType;
import nus_iss.LAPS.repository.LeaveTypeRepository;
import nus_iss.LAPS.service.LeaveApplicationService;
import nus_iss.LAPS.service.LeaveBalanceService;
import nus_iss.LAPS.util.BreadcrumbItem;
import nus_iss.LAPS.util.GlobalConstants;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MVC controller for the Leave Application web UI.
 *
 * This controller handles all HTTP requests from the browser (form submissions and page loads).
 * It does NOT contain any business logic — instead it delegates every write operation to
 * LeaveApplicationRestController, which acts as the single business-logic gateway.
 * This means validation, balance checks, and email notifications are only written once
 * in the REST layer and are reused by both the web UI and any future API clients.
 *
 * Flow for write operations (submit, update, approve, etc.):
 *   Browser → LeaveApplicationController (MVC) → LeaveApplicationRestController (REST) → LeaveApplicationService
 *
 * Session contract:
 *   The logged-in employee is stored in the HTTP session under the key "employee".
 *   This is a detached JPA entity — its lazy-loaded collections must NOT be accessed.
 *   For manager operations the session entity is used only for its ID; all fresh data
 *   is loaded by the service via the repository within the current transaction.
 *
 * All routes are defined as constants in GlobalConstants to avoid scattered hardcoded strings.
 *
 * Routes covered:
 *   GET/POST  /leave/apply              – employee apply form
 *   GET       /leave/history            – employee personal history (paginated, filterable)
 *   GET       /leave/{id}               – single application detail
 *   GET/POST  /leave/{id}/edit          – edit an APPLIED or UPDATED application
 *   POST      /leave/{id}/delete        – soft-delete an APPLIED or UPDATED application
 *   POST      /leave/{id}/cancel        – cancel an APPROVED application (balance restored)
 *   GET       /leave/manager/pending    – manager: pending approvals (paginated)
 *   GET       /leave/manager/all        – manager: full team history (paginated)
 *   POST      /leave/{id}/approve       – manager: approve a pending application
 *   POST      /leave/{id}/reject        – manager: reject with mandatory comment
 *
 * Author: Htet Nandar (Grace)
 */
@Slf4j
@Controller
@RequestMapping(GlobalConstants.ROUTE_LEAVE)
public class LeaveApplicationController {

    // Delegates all business operations — this controller calls the REST layer,
    // not the service directly.
    @Autowired private LeaveApplicationService leaveApplicationService;

    // Used to display leave balance summaries on the apply and history pages.
    @Autowired private LeaveBalanceService     leaveBalanceService;

    // Used to populate the leave type dropdown on the apply and edit forms.
    @Autowired private LeaveTypeRepository     leaveTypeRepo;

    // The REST controller is injected and called directly (internal method call,
    // not an actual HTTP request) so business logic is not duplicated.
    @Autowired private LeaveApplicationRestController restController;

    // ─────────────────────────────────────────────────────────────────────────
    // Session helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the Employee stored in the HTTP session.
     * The object is a detached JPA entity — safe for reading basic fields (ID, name)
     * but do NOT call lazy-loaded collections on it (supervisor, subordinates, etc.).
     */
    private Employee getSessionEmployee(HttpSession session) {
        return (Employee) session.getAttribute("employee");
    }

    /**
     * Returns true if the user is currently logged in.
     * Checks for "userId" in the session — set during login, cleared on logout.
     */
    private boolean isLoggedIn(HttpSession session) {
        return session.getAttribute("userId") != null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EMPLOYEE — Apply for leave
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Displays the leave application form.
     * Pre-populates the leave type dropdown and the employee's current balance summary.
     *
     * GET /leave/apply
     */
    @GetMapping(GlobalConstants.ROUTE_LEAVE_APPLY)
    public String applyForm(HttpSession session, Model model) {
        if (!isLoggedIn(session)) return GlobalConstants.REDIRECT_LOGIN;

        Employee emp = getSessionEmployee(session);
        List<LeaveType> leaveTypes = leaveTypeRepo.findAll();

        // Empty LeaveApplication as the form-backing object for th:object binding.
        model.addAttribute("leaveApplication", new LeaveApplication());
        model.addAttribute("leaveTypes", leaveTypes);

        // Show the employee's remaining balance for each leave type on the form
        // so they know how many days they have left before submitting.
        model.addAttribute("leaveBalances",
                leaveBalanceService.getLeaveBalancesByEmployeeId(emp.getEmp_id()));

        List<BreadcrumbItem> breadcrumbs = List.of(
                new BreadcrumbItem("LAPS", null),
                new BreadcrumbItem("Leave", GlobalConstants.ROUTE_LEAVE + GlobalConstants.ROUTE_LEAVE_HISTORY),
                new BreadcrumbItem("Apply for Leave", null)
        );
        model.addAttribute("breadcrumbs", breadcrumbs);

        return GlobalConstants.VIEW_LEAVE_APPLY;
    }

    /**
     * Processes the leave application form submission.
     *
     * Builds a LeaveRequest DTO from the form data and delegates to the REST controller.
     * On success: redirects to the history page with a success flash message.
     * On validation failure: stays on the apply form and shows the error inline.
     *
     * Using the REST controller instead of calling the service directly ensures
     * that validation errors are handled the same way for both web and API callers.
     *
     * POST /leave/apply
     */
    @PostMapping(GlobalConstants.ROUTE_LEAVE_APPLY)
    public String applySubmit(
            @ModelAttribute LeaveApplication leaveApplication,
            HttpSession session,
            RedirectAttributes ra,
            Model model) {

        if (!isLoggedIn(session)) return GlobalConstants.REDIRECT_LOGIN;
        Employee emp = getSessionEmployee(session);

        try {
            log.info("Employee {} submitting leave application from {} to {}",
                emp.getEmp_id(), leaveApplication.getStartDate(), leaveApplication.getEndDate());

            // Convert the form-bound LeaveApplication into a LeaveRequest DTO.
            // The REST controller expects a DTO (with primitive IDs) rather than a full entity.
            LeaveRequest req = new LeaveRequest(
                    emp.getEmp_id(),
                    leaveApplication.getLeaveType() != null ? leaveApplication.getLeaveType().getLeaveTypeId() : null,
                    leaveApplication.getStartDate(),
                    leaveApplication.getEndDate(),
                    leaveApplication.getReason(),
                    leaveApplication.getWorkDissemination(),
                    leaveApplication.getIsOverseas(),
                    leaveApplication.getContactDetails(),
                    leaveApplication.getIsHalfDay(),
                    leaveApplication.getHalfDayPeriod() != null ? leaveApplication.getHalfDayPeriod().toString() : null
            );

            ResponseEntity<?> response = restController.submitLeave(req);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Leave application submitted successfully for employee {}", emp.getEmp_id());
                // Flash attributes survive a redirect — shown as a success banner on the history page.
                ra.addFlashAttribute(GlobalConstants.FLASH_SUCCESS, "Leave application submitted successfully.");
                return GlobalConstants.REDIRECT_LEAVE_HISTORY;
            } else {
                // Validation failed (e.g. insufficient balance, past date).
                // Extract the error message from the ApiResponse body and redisplay the form.
                Object body = response.getBody();
                String errMsg = (body instanceof ApiResponse ap) ? ap.message() : "Error submitting leave application.";
                log.error("Validation error for employee {}: {}", emp.getEmp_id(), errMsg);

                // Add error to model (not flash — we're staying on the same page, not redirecting).
                model.addAttribute(GlobalConstants.FLASH_ERROR, errMsg);
                model.addAttribute("leaveTypes", leaveTypeRepo.findAll());
                model.addAttribute("leaveBalances",
                        leaveBalanceService.getLeaveBalancesByEmployeeId(emp.getEmp_id()));
                model.addAttribute("leaveApplication", leaveApplication);
                return GlobalConstants.VIEW_LEAVE_APPLY;
            }

        } catch (IllegalArgumentException e) {
            log.error("Error submitting leave application for employee {}: {}", emp.getEmp_id(), e.getMessage());
            model.addAttribute(GlobalConstants.FLASH_ERROR, e.getMessage());
            model.addAttribute("leaveTypes", leaveTypeRepo.findAll());
            model.addAttribute("leaveBalances",
                    leaveBalanceService.getLeaveBalancesByEmployeeId(emp.getEmp_id()));
            model.addAttribute("leaveApplication", leaveApplication);
            return GlobalConstants.VIEW_LEAVE_APPLY;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EMPLOYEE — Personal leave history
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Displays the employee's personal leave history with optional filters and pagination.
     *
     * Supports filtering by status (e.g. APPROVED, REJECTED) and/or leave type.
     * Results are sorted by start date descending (most recent first).
     * Also displays the employee's current leave balance summary at the top.
     *
     * GET /leave/history?status=APPROVED&leaveTypeId=1&page=0&size=10
     */
    @GetMapping(GlobalConstants.ROUTE_LEAVE_HISTORY)
    public String history(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long leaveTypeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = GlobalConstants.DEFAULT_PAGE_SIZE) int size,
            HttpSession session, Model model) {

        if (!isLoggedIn(session)) return GlobalConstants.REDIRECT_LOGIN;
        Employee emp = getSessionEmployee(session);

        Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").descending());

        // Choose the most specific query method based on which filters were provided.
        // Four combinations: both status + type, status only, type only, no filter.
        Page<LeaveApplication> applicationPage;
        if (status != null && !status.isBlank() && leaveTypeId != null) {
            LeaveStatus leaveStatus = LeaveStatus.valueOf(status);
            applicationPage = leaveApplicationService
                    .getPersonalLeaveHistoryByStatusAndTypePaginated(emp, leaveStatus, leaveTypeId, pageable);
        } else if (status != null && !status.isBlank()) {
            LeaveStatus leaveStatus = LeaveStatus.valueOf(status);
            applicationPage = leaveApplicationService
                    .getPersonalLeaveHistoryByStatusPaginated(emp, leaveStatus, pageable);
        } else if (leaveTypeId != null) {
            applicationPage = leaveApplicationService
                    .getPersonalLeaveHistoryByTypePaginated(emp, leaveTypeId, pageable);
        } else {
            applicationPage = leaveApplicationService.getPersonalLeaveHistoryPaginated(emp, pageable);
        }

        // Map to DTO before passing to Thymeleaf to avoid lazy-load issues during rendering.
        model.addAttribute("applications", applicationPage.getContent().stream()
                .map(LeaveResponse::fromEntity).collect(Collectors.toList()));

        // Pagination metadata used by the Thymeleaf pagination fragment.
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", applicationPage.getTotalPages());
        model.addAttribute("totalItems", applicationPage.getTotalElements());

        // Balance summary and filter options for the dropdowns.
        model.addAttribute("leaveBalances",
                leaveBalanceService.getLeaveBalancesByEmployeeId(emp.getEmp_id()));
        model.addAttribute("leaveStatuses", Arrays.asList(LeaveStatus.values()));
        model.addAttribute("leaveTypes", leaveTypeRepo.findAll());

        // Preserve the currently selected filter values so the dropdowns stay populated
        // after the page reloads.
        model.addAttribute("selectedStatus", status != null ? status : "");
        model.addAttribute("selectedLeaveTypeId", leaveTypeId);

        List<BreadcrumbItem> breadcrumbs = List.of(
                new BreadcrumbItem("LAPS", null),
                new BreadcrumbItem("Leave", null)
        );
        model.addAttribute("breadcrumbs", breadcrumbs);
        return GlobalConstants.VIEW_LEAVE_HISTORY;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EMPLOYEE — View single application detail
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Displays the detail view for a single leave application.
     * Accessible by the owning employee and their manager (read-only).
     * Redirects to history with an error flash if the ID does not exist.
     *
     * GET /leave/{id}
     */
    @GetMapping(GlobalConstants.ROUTE_LEAVE_DETAIL)
    public String detail(@PathVariable Long id, HttpSession session, Model model,
                         RedirectAttributes ra) {
        if (!isLoggedIn(session)) return GlobalConstants.REDIRECT_LOGIN;

        Employee emp = getSessionEmployee(session);

        // findById returns an Optional. Use map/orElseGet to handle the not-found case cleanly.
        return leaveApplicationService.findById(id).map(leaveApplication -> {
            // Convert to DTO to avoid exposing the JPA entity directly to Thymeleaf.
            model.addAttribute("leaveApp", LeaveResponse.fromEntity(leaveApplication));
            model.addAttribute("employee", emp);

            List<BreadcrumbItem> breadcrumbs = Arrays.asList(
                new BreadcrumbItem("Leave", GlobalConstants.ROUTE_LEAVE + GlobalConstants.ROUTE_LEAVE_HISTORY),
                new BreadcrumbItem("Details", null)
            );
            model.addAttribute("breadcrumbs", breadcrumbs);

            return GlobalConstants.VIEW_LEAVE_DETAIL;
        }).orElseGet(() -> {
            ra.addFlashAttribute(GlobalConstants.FLASH_ERROR, "Leave application not found.");
            return GlobalConstants.REDIRECT_LEAVE_HISTORY;
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EMPLOYEE — Edit leave application
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Displays the edit form for an existing leave application.
     *
     * Two guards are checked before showing the form:
     *   1. The application must be in an editable state (APPLIED or UPDATED).
     *   2. The logged-in employee must be the owner of the application.
     *
     * GET /leave/{id}/edit
     */
    @GetMapping(GlobalConstants.ROUTE_LEAVE_EDIT)
    public String editForm(@PathVariable Long id, HttpSession session, Model model,
                           RedirectAttributes ra) {
        if (!isLoggedIn(session)) return GlobalConstants.REDIRECT_LOGIN;
        Employee emp = getSessionEmployee(session);

        return leaveApplicationService.findById(id).map(la -> {
            // isEditable() returns true only for APPLIED and UPDATED status.
            // Approved, rejected, cancelled, or deleted applications cannot be modified.
            if (!la.isEditable()) {
                ra.addFlashAttribute(GlobalConstants.FLASH_ERROR,
                        "This application cannot be edited (status: " + la.getStatus() + ").");
                return GlobalConstants.REDIRECT_LEAVE_HISTORY;
            }

            // Prevent an employee from accessing another employee's edit form via URL manipulation.
            if (!la.getEmployee().getEmp_id().equals(emp.getEmp_id())) {
                ra.addFlashAttribute(GlobalConstants.FLASH_ERROR, "Not authorised.");
                return GlobalConstants.REDIRECT_LEAVE_HISTORY;
            }

            // Pass the full entity (not DTO) as the form-backing object so th:field can bind
            // the existing values into the form inputs for editing.
            model.addAttribute("leaveApplication", la);
            model.addAttribute("leaveTypes", leaveTypeRepo.findAll());
            model.addAttribute("leaveBalances",
                    leaveBalanceService.getLeaveBalancesByEmployeeId(emp.getEmp_id()));

            List<BreadcrumbItem> breadcrumbs = Arrays.asList(
                    new BreadcrumbItem("Leave", GlobalConstants.ROUTE_LEAVE + GlobalConstants.ROUTE_LEAVE_HISTORY),
                    new BreadcrumbItem("Details", null)
            );
            model.addAttribute("breadcrumbs", breadcrumbs);

            return GlobalConstants.VIEW_LEAVE_EDIT;
        }).orElseGet(() -> {
            ra.addFlashAttribute(GlobalConstants.FLASH_ERROR, "Leave application not found.");
            return GlobalConstants.REDIRECT_LEAVE_HISTORY;
        });
    }

    /**
     * Processes the edit form submission.
     *
     * Delegates to the REST controller (PUT) which validates the updated data,
     * checks ownership and status, and changes the status from APPLIED → UPDATED.
     *
     * On success: redirects to history with a success flash.
     * On validation failure: stays on the edit form with an inline error.
     * On unexpected exception: redirects back to the edit form with a flash error.
     *
     * POST /leave/{id}/edit
     */
    @PostMapping(GlobalConstants.ROUTE_LEAVE_EDIT)
    public String editSubmit(
            @PathVariable Long id,
            @ModelAttribute LeaveApplication leaveApplication,
            HttpSession session,
            RedirectAttributes ra,
            Model model) {

        if (!isLoggedIn(session)) return GlobalConstants.REDIRECT_LOGIN;
        Employee emp = getSessionEmployee(session);

        try {
            log.info("Employee {} updating leave application {}", emp.getEmp_id(), id);

            LeaveRequest req = new LeaveRequest(
                    emp.getEmp_id(),
                    leaveApplication.getLeaveType() != null ? leaveApplication.getLeaveType().getLeaveTypeId() : null,
                    leaveApplication.getStartDate(),
                    leaveApplication.getEndDate(),
                    leaveApplication.getReason(),
                    leaveApplication.getWorkDissemination(),
                    leaveApplication.getIsOverseas(),
                    leaveApplication.getContactDetails(),
                    leaveApplication.getIsHalfDay(),
                    leaveApplication.getHalfDayPeriod() != null ? leaveApplication.getHalfDayPeriod().toString() : null
            );

            ResponseEntity<?> response = restController.updateLeave(id, req);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Leave application {} updated successfully by employee {}", id, emp.getEmp_id());
                ra.addFlashAttribute(GlobalConstants.FLASH_SUCCESS, "Leave application updated successfully.");
                return GlobalConstants.REDIRECT_LEAVE_HISTORY;
            } else {
                // Validation error from the REST layer — stay on the edit form and show the message.
                Object body = response.getBody();
                String errMsg = (body instanceof ApiResponse ap) ? ap.message() : "Error updating leave application.";
                log.error("Validation error updating application {} for employee {}: {}", id, emp.getEmp_id(), errMsg);

                // Restore the application ID so the form action URL is correct.
                leaveApplication.setLeaveApplicationId(id);
                model.addAttribute(GlobalConstants.FLASH_ERROR, errMsg);
                model.addAttribute("leaveApplication", leaveApplication);
                model.addAttribute("leaveTypes", leaveTypeRepo.findAll());
                model.addAttribute("leaveBalances",
                        leaveBalanceService.getLeaveBalancesByEmployeeId(emp.getEmp_id()));
                return GlobalConstants.VIEW_LEAVE_EDIT;
            }

        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            // These exceptions come from the REST/service layer for known error scenarios.
            // Redirect back to the edit form with the error as a flash message.
            log.error("Error updating leave application {} for employee {}: {}", id, emp.getEmp_id(), e.getMessage());
            ra.addFlashAttribute(GlobalConstants.FLASH_ERROR, e.getMessage());
            return "redirect:" + GlobalConstants.ROUTE_LEAVE + "/" + id + "/edit";
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EMPLOYEE — Delete leave application (soft delete)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Soft-deletes a leave application by setting its status to DELETED.
     *
     * Only APPLIED or UPDATED applications can be deleted via this endpoint.
     * The record is kept in the database for audit purposes — it is simply
     * hidden from normal queries by filtering on status != 'DELETED'.
     * Redirects to history with a success or error flash message.
     *
     * POST /leave/{id}/delete
     */
    @PostMapping(GlobalConstants.ROUTE_LEAVE_DELETE)
    public String delete(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!isLoggedIn(session)) return GlobalConstants.REDIRECT_LOGIN;
        Employee emp = getSessionEmployee(session);

        try {
            log.info("Employee {} deleting leave application {}", emp.getEmp_id(), id);
            ResponseEntity<?> response = restController.deleteLeave(id, emp.getEmp_id());
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Leave application {} deleted successfully by employee {}", id, emp.getEmp_id());
                ra.addFlashAttribute(GlobalConstants.FLASH_SUCCESS, "Leave application deleted.");
            } else {
                log.error("REST controller returned error for employee {}: {}", emp.getEmp_id(), response.getBody());
                ra.addFlashAttribute(GlobalConstants.FLASH_ERROR, "Error deleting leave application.");
            }
        } catch (Exception e) {
            log.error("Error deleting leave application {} for employee {}: {}", id, emp.getEmp_id(), e.getMessage());
            ra.addFlashAttribute(GlobalConstants.FLASH_ERROR, e.getMessage());
        }
        return GlobalConstants.REDIRECT_LEAVE_HISTORY;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EMPLOYEE — Cancel approved leave
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Cancels an APPROVED leave application.
     *
     * Cancel is different from delete:
     *   - Delete: removes APPLIED/UPDATED leaves before a manager has acted.
     *   - Cancel: reverses an APPROVED leave — the balance is restored to the employee.
     *
     * The service calls leaveBalanceService.restoreBalance() to add the days back
     * before setting the status to CANCELLED.
     *
     * POST /leave/{id}/cancel
     */
    @PostMapping(GlobalConstants.ROUTE_LEAVE_CANCEL)
    public String cancel(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!isLoggedIn(session)) return GlobalConstants.REDIRECT_LOGIN;
        Employee emp = getSessionEmployee(session);

        try {
            log.info("Employee {} cancelling leave application {}", emp.getEmp_id(), id);
            ResponseEntity<?> response = restController.cancelLeave(id, emp.getEmp_id());
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Leave application {} cancelled successfully by employee {}", id, emp.getEmp_id());
                ra.addFlashAttribute(GlobalConstants.FLASH_SUCCESS, "Leave application cancelled and balance restored.");
            } else {
                log.error("REST controller returned error for employee {}: {}", emp.getEmp_id(), response.getBody());
                ra.addFlashAttribute(GlobalConstants.FLASH_ERROR, "Error cancelling leave application.");
            }
        } catch (Exception e) {
            log.error("Error cancelling leave application {} for employee {}: {}", id, emp.getEmp_id(), e.getMessage());
            ra.addFlashAttribute(GlobalConstants.FLASH_ERROR, e.getMessage());
        }
        return GlobalConstants.REDIRECT_LEAVE_HISTORY;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MANAGER — Pending applications awaiting approval
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Displays the manager's pending approval queue — leave applications with
     * status APPLIED or UPDATED submitted by their direct subordinates.
     *
     * Results are paginated and sorted by start date descending.
     * The manager can approve or reject each application from this page.
     *
     * GET /leave/manager/pending?page=0&size=10
     */
    @GetMapping(GlobalConstants.ROUTE_MANAGER_PENDING)
    public String managerPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = GlobalConstants.DEFAULT_PAGE_SIZE) int size,
            HttpSession session, Model model) {
        if (!isLoggedIn(session)) return GlobalConstants.REDIRECT_LOGIN;
        Employee mgr = getSessionEmployee(session);

        Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").descending());
        Page<LeaveApplication> applicationPage = leaveApplicationService.getPendingApplicationsForManagerPaginated(mgr, pageable);

        model.addAttribute("applications",
                applicationPage.getContent().stream().map(LeaveResponse::fromEntity).collect(Collectors.toList()));
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", applicationPage.getTotalPages());
        model.addAttribute("totalItems", applicationPage.getTotalElements());

        // Display the manager's own leave balance summary in the sidebar.
        model.addAttribute("leaveBalances",
                leaveBalanceService.getLeaveBalancesByEmployeeId(mgr.getEmp_id()));

        List<BreadcrumbItem> breadcrumbs = Arrays.asList(
                new BreadcrumbItem("LAPS", null),
                new BreadcrumbItem("Leave Approvals", null)
        );
        model.addAttribute("breadcrumbs", breadcrumbs);

        return GlobalConstants.VIEW_MANAGER_PENDING;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MANAGER — All subordinate leave history
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Displays the full leave history (all statuses except DELETED) for the manager's
     * direct subordinates. Paginated, sorted by start date descending.
     *
     * GET /leave/manager/all?page=0&size=10
     */
    @GetMapping(GlobalConstants.ROUTE_MANAGER_ALL)
    public String managerAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = GlobalConstants.DEFAULT_PAGE_SIZE) int size,
            HttpSession session, Model model) {
        if (!isLoggedIn(session)) return GlobalConstants.REDIRECT_LOGIN;
        Employee mgr = getSessionEmployee(session);

        Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").descending());
        Page<LeaveApplication> applicationPage = leaveApplicationService.getAllApplicationsForManagerPaginated(mgr, pageable);

        model.addAttribute("applications",
                applicationPage.getContent().stream().map(LeaveResponse::fromEntity).collect(Collectors.toList()));
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", applicationPage.getTotalPages());
        model.addAttribute("totalItems", applicationPage.getTotalElements());

        model.addAttribute("leaveBalances",
                leaveBalanceService.getLeaveBalancesByEmployeeId(mgr.getEmp_id()));

        List<BreadcrumbItem> breadcrumbs = Arrays.asList(
                new BreadcrumbItem("LAPS", null),
                new BreadcrumbItem("Team Leave History", null)
        );
        model.addAttribute("breadcrumbs", breadcrumbs);

        return GlobalConstants.VIEW_MANAGER_ALL;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MANAGER — Approve
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Approves a pending leave application.
     *
     * Delegates to the REST controller which:
     *   1. Validates the application is still APPLIED or UPDATED.
     *   2. Deducts the leave balance from the employee's entitlement.
     *   3. Sets status to APPROVED and records the approving manager.
     *   4. Sends an approval email notification to the employee.
     *
     * Always redirects back to the pending list after processing.
     *
     * POST /leave/{id}/approve
     */
    @PostMapping(GlobalConstants.ROUTE_MANAGER_APPROVE)
    public String approve(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!isLoggedIn(session)) return GlobalConstants.REDIRECT_LOGIN;
        Employee mgr = getSessionEmployee(session);

        try {
            log.info("Manager {} approving leave application {}", mgr.getEmp_id(), id);
            ResponseEntity<?> response = restController.approveLeave(id, mgr.getEmp_id());
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Leave application {} approved successfully by manager {}", id, mgr.getEmp_id());
                ra.addFlashAttribute(GlobalConstants.FLASH_SUCCESS, "Leave application approved.");
            } else {
                Object body = response.getBody();
                String errMsg = (body instanceof ApiResponse ap) ? ap.message() : "Error approving leave application.";
                log.error("Error approving application {} for manager {}: {}", id, mgr.getEmp_id(), errMsg);
                ra.addFlashAttribute(GlobalConstants.FLASH_ERROR, errMsg);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Error approving leave application {} by manager {}: {}", id, mgr.getEmp_id(), e.getMessage());
            ra.addFlashAttribute(GlobalConstants.FLASH_ERROR, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error approving leave application {} by manager {}: {}", id, mgr.getEmp_id(), e.getMessage());
            ra.addFlashAttribute(GlobalConstants.FLASH_ERROR, "An error occurred: " + e.getMessage());
        }
        return GlobalConstants.REDIRECT_MANAGER_PENDING;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MANAGER — Reject
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Rejects a pending leave application with a mandatory manager comment.
     *
     * The comment is required — the REST layer throws IllegalArgumentException
     * if it is blank, which surfaces as a flash error on the pending page.
     * No balance is deducted on rejection.
     * An email notification is sent to the employee with the rejection reason.
     *
     * Always redirects back to the pending list after processing.
     *
     * POST /leave/{id}/reject
     * Form param: managerComment (required)
     */
    @PostMapping(GlobalConstants.ROUTE_MANAGER_REJECT)
    public String reject(@PathVariable Long id,
                         @RequestParam String managerComment,
                         HttpSession session, RedirectAttributes ra) {
        if (!isLoggedIn(session)) return GlobalConstants.REDIRECT_LOGIN;
        Employee mgr = getSessionEmployee(session);

        try {
            log.info("Manager {} rejecting leave application {}", mgr.getEmp_id(), id);
            ResponseEntity<?> response = restController.rejectLeave(id, mgr.getEmp_id(),
                    new ManagerActionRequest(managerComment));
            if (response.getStatusCode().is2xxSuccessful()) {
                ra.addFlashAttribute(GlobalConstants.FLASH_SUCCESS, "Leave application rejected.");
            } else {
                Object body = response.getBody();
                String errMsg = (body instanceof ApiResponse ap) ? ap.message() : "Error rejecting leave application.";
                ra.addFlashAttribute(GlobalConstants.FLASH_ERROR, errMsg);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute(GlobalConstants.FLASH_ERROR, e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute(GlobalConstants.FLASH_ERROR, "An error occurred: " + e.getMessage());
        }
        return GlobalConstants.REDIRECT_MANAGER_PENDING;
    }
}
