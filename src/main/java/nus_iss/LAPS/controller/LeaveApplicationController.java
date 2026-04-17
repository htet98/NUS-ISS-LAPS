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

@Slf4j
@Controller
@RequestMapping(GlobalConstants.ROUTE_LEAVE)
public class LeaveApplicationController {

    @Autowired private LeaveApplicationService leaveApplicationService;
    @Autowired private LeaveBalanceService     leaveBalanceService;
    @Autowired private LeaveTypeRepository     leaveTypeRepo;
    @Autowired private LeaveApplicationRestController restController;

    // ─────────────────────────────────────────────────────────────────────────
    // Helper: retrieve logged-in employee from session, redirect if not set
    // ─────────────────────────────────────────────────────────────────────────
    private Employee getSessionEmployee(HttpSession session) {
        return (Employee) session.getAttribute("employee");
    }

    private boolean isLoggedIn(HttpSession session) {
        return session.getAttribute("userId") != null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EMPLOYEE — Apply for leave
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping(GlobalConstants.ROUTE_LEAVE_APPLY)
    public String applyForm(HttpSession session, Model model) {
        if (!isLoggedIn(session)) return GlobalConstants.REDIRECT_LOGIN;

        Employee emp = getSessionEmployee(session);
        List<LeaveType> leaveTypes = leaveTypeRepo.findAll();

        model.addAttribute("leaveApplication", new LeaveApplication());
        model.addAttribute("leaveTypes", leaveTypes);
        model.addAttribute("leaveBalances",
                leaveBalanceService.getLeaveBalancesByEmployeeId(emp.getEmp_id()));
        
        // Add breadcrumbs
        List<BreadcrumbItem> breadcrumbs = List.of(
                new BreadcrumbItem("LAPS", null),
                new BreadcrumbItem("Leave", GlobalConstants.ROUTE_LEAVE + GlobalConstants.ROUTE_LEAVE_HISTORY),
                new BreadcrumbItem("Apply for Leave", null)
        );
        model.addAttribute("breadcrumbs", breadcrumbs);
        
        return GlobalConstants.VIEW_LEAVE_APPLY;
    }

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
            
            // Build LeaveRequest from form data and call REST controller
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
                ra.addFlashAttribute(GlobalConstants.FLASH_SUCCESS, "Leave application submitted successfully.");
                return GlobalConstants.REDIRECT_LEAVE_HISTORY;
            } else {
                Object body = response.getBody();
                String errMsg = (body instanceof ApiResponse ap) ? ap.message() : "Error submitting leave application.";
                log.error("Validation error for employee {}: {}", emp.getEmp_id(), errMsg);
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

        // Apply filters if provided
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

        model.addAttribute("applications", applicationPage.getContent().stream()
                .map(LeaveResponse::fromEntity).collect(Collectors.toList()));
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", applicationPage.getTotalPages());
        model.addAttribute("totalItems", applicationPage.getTotalElements());

        model.addAttribute("leaveBalances",
                leaveBalanceService.getLeaveBalancesByEmployeeId(emp.getEmp_id()));
        model.addAttribute("leaveStatuses", Arrays.asList(LeaveStatus.values()));
        model.addAttribute("leaveTypes", leaveTypeRepo.findAll());
        model.addAttribute("selectedStatus", status != null ? status : "");
        model.addAttribute("selectedLeaveTypeId", leaveTypeId);
        
        // Add breadcrumbs
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

    @GetMapping(GlobalConstants.ROUTE_LEAVE_DETAIL)
    public String detail(@PathVariable Long id, HttpSession session, Model model,
                         RedirectAttributes ra) {
        if (!isLoggedIn(session)) return GlobalConstants.REDIRECT_LOGIN;

        Employee emp = getSessionEmployee(session);
        return leaveApplicationService.findById(id).map(leaveApplication -> {
            model.addAttribute("leaveApp", LeaveResponse.fromEntity(leaveApplication));
            model.addAttribute("employee", emp);
            
            // Add breadcrumbs
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

    @GetMapping(GlobalConstants.ROUTE_LEAVE_EDIT)
    public String editForm(@PathVariable Long id, HttpSession session, Model model,
                           RedirectAttributes ra) {
        if (!isLoggedIn(session)) return GlobalConstants.REDIRECT_LOGIN;
        Employee emp = getSessionEmployee(session);

        return leaveApplicationService.findById(id).map(la -> {
            if (!la.isEditable()) {
                ra.addFlashAttribute(GlobalConstants.FLASH_ERROR,
                        "This application cannot be edited (status: " + la.getStatus() + ").");
                return GlobalConstants.REDIRECT_LEAVE_HISTORY;
            }
            if (!la.getEmployee().getEmp_id().equals(emp.getEmp_id())) {
                ra.addFlashAttribute(GlobalConstants.FLASH_ERROR, "Not authorised.");
                return GlobalConstants.REDIRECT_LEAVE_HISTORY;
            }
            model.addAttribute("leaveApplication", la);
            model.addAttribute("leaveTypes", leaveTypeRepo.findAll());
            model.addAttribute("leaveBalances",
                    leaveBalanceService.getLeaveBalancesByEmployeeId(emp.getEmp_id()));

            // Add breadcrumbs
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
            
            // Build LeaveRequest from form data and call REST controller
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
                Object body = response.getBody();
                String errMsg = (body instanceof ApiResponse ap) ? ap.message() : "Error updating leave application.";
                log.error("Validation error updating application {} for employee {}: {}", id, emp.getEmp_id(), errMsg);
                // Stay on edit form so user can correct the error
                leaveApplication.setLeaveApplicationId(id);
                model.addAttribute(GlobalConstants.FLASH_ERROR, errMsg);
                model.addAttribute("leaveApplication", leaveApplication);
                model.addAttribute("leaveTypes", leaveTypeRepo.findAll());
                model.addAttribute("leaveBalances",
                        leaveBalanceService.getLeaveBalancesByEmployeeId(emp.getEmp_id()));
                return GlobalConstants.VIEW_LEAVE_EDIT;
            }

        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            log.error("Error updating leave application {} for employee {}: {}", id, emp.getEmp_id(), e.getMessage());
            ra.addFlashAttribute(GlobalConstants.FLASH_ERROR, e.getMessage());
            return "redirect:" + GlobalConstants.ROUTE_LEAVE + "/" + id + "/edit";
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EMPLOYEE — Delete leave application (soft delete)
    // ─────────────────────────────────────────────────────────────────────────

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

        model.addAttribute("leaveBalances",
                leaveBalanceService.getLeaveBalancesByEmployeeId(mgr.getEmp_id()));

        // Add breadcrumbs
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

        // Add breadcrumbs
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
