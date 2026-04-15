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
import nus_iss.LAPS.util.GlobalConstants;
import jakarta.servlet.http.HttpSession;
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
            // Convert LeaveApplication to LeaveRequest for API call
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
                ra.addFlashAttribute(GlobalConstants.FLASH_SUCCESS, "Leave application submitted successfully.");
                return GlobalConstants.REDIRECT_LEAVE_HISTORY;
            } else {
                ApiResponse apiResp = (ApiResponse) response.getBody();
                String msg = (apiResp != null) ? apiResp.message() : "Submission failed";
                throw new IllegalArgumentException(msg);
            }

        } catch (IllegalArgumentException e) {
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
            // Convert LeaveApplication to LeaveRequest for API call
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
                ra.addFlashAttribute(GlobalConstants.FLASH_SUCCESS, "Leave application updated successfully.");
                return GlobalConstants.REDIRECT_LEAVE_HISTORY;
            } else {
                ApiResponse apiResp = (ApiResponse) response.getBody();
                String msg = (apiResp != null) ? apiResp.message() : "Update failed";
                throw new IllegalArgumentException(msg);
            }

        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            ra.addFlashAttribute(GlobalConstants.FLASH_ERROR, e.getMessage());
            return "redirect:/leave/" + id + "/edit";
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
            ResponseEntity<?> response = restController.deleteLeave(id, emp.getEmp_id());
            if (response.getStatusCode().is2xxSuccessful()) {
                ra.addFlashAttribute(GlobalConstants.FLASH_SUCCESS, "Leave application deleted.");
            } else {
                ApiResponse apiResp = (ApiResponse) response.getBody();
                String msg = (apiResp != null) ? apiResp.message() : "Delete failed";
                throw new IllegalArgumentException(msg);
            }
        } catch (Exception e) {
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
            ResponseEntity<?> response = restController.cancelLeave(id, emp.getEmp_id());
            if (response.getStatusCode().is2xxSuccessful()) {
                ra.addFlashAttribute(GlobalConstants.FLASH_SUCCESS, "Leave application cancelled and balance restored.");
            } else {
                ApiResponse apiResp = (ApiResponse) response.getBody();
                String msg = (apiResp != null) ? apiResp.message() : "Cancel failed";
                throw new IllegalArgumentException(msg);
            }
        } catch (Exception e) {
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
            ResponseEntity<?> response = restController.approveLeave(id, mgr.getEmp_id());
            if (response.getStatusCode().is2xxSuccessful()) {
                ra.addFlashAttribute(GlobalConstants.FLASH_SUCCESS, "Leave application approved.");
            } else {
                ApiResponse apiResp = (ApiResponse) response.getBody();
                String msg = (apiResp != null) ? apiResp.message() : "Approval failed";
                throw new IllegalArgumentException(msg);
            }
        } catch (Exception e) {
            ra.addFlashAttribute(GlobalConstants.FLASH_ERROR, e.getMessage());
        }
        return GlobalConstants.REDIRECT_MANAGER_PENDING;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MANAGER — Reject
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping(GlobalConstants.ROUTE_MANAGER_REJECT)
    public String reject(
            @PathVariable Long id,
            @RequestParam String managerComment,
            HttpSession session,
            RedirectAttributes ra) {

        if (!isLoggedIn(session)) return GlobalConstants.REDIRECT_LOGIN;
        Employee mgr = getSessionEmployee(session);

        try {
            ManagerActionRequest req = new ManagerActionRequest(managerComment);
            ResponseEntity<?> response = restController.rejectLeave(id, mgr.getEmp_id(), req);
            if (response.getStatusCode().is2xxSuccessful()) {
                ra.addFlashAttribute(GlobalConstants.FLASH_SUCCESS, "Leave application rejected.");
            } else {
                ApiResponse apiResp = (ApiResponse) response.getBody();
                String msg = (apiResp != null) ? apiResp.message() : "Rejection failed";
                throw new IllegalArgumentException(msg);
            }
        } catch (Exception e) {
            ra.addFlashAttribute(GlobalConstants.FLASH_ERROR, e.getMessage());
        }
        return GlobalConstants.REDIRECT_MANAGER_PENDING;
    }
}