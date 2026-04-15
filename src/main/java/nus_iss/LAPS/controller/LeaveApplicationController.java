package nus_iss.LAPS.controller;

import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.model.LeaveApplication;
import nus_iss.LAPS.model.LeaveStatus;
import nus_iss.LAPS.model.LeaveType;
import nus_iss.LAPS.repository.LeaveTypeRepository;
import nus_iss.LAPS.service.LeaveApplicationService;
import nus_iss.LAPS.service.LeaveBalanceService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/leave")
public class LeaveApplicationController {

    @Autowired private LeaveApplicationService leaveApplicationService;
    @Autowired private LeaveBalanceService     leaveBalanceService;
    @Autowired private LeaveTypeRepository     leaveTypeRepo;

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

    @GetMapping("/apply")
    public String applyForm(HttpSession session, Model model) {
        if (!isLoggedIn(session)) return "redirect:/login";

        Employee emp = getSessionEmployee(session);
        List<LeaveType> leaveTypes = leaveTypeRepo.findAll();

        model.addAttribute("leaveApplication", new LeaveApplication());
        model.addAttribute("leaveTypes", leaveTypes);
        model.addAttribute("leaveBalances",
                leaveBalanceService.getLeaveBalancesByEmployeeId(emp.getEmp_id()));
        return "leave/apply";
    }

    @PostMapping("/apply")
    public String applySubmit(
            @ModelAttribute LeaveApplication leaveApplication,
            HttpSession session,
            RedirectAttributes ra,
            Model model) {

        if (!isLoggedIn(session)) return "redirect:/login";
        Employee emp = getSessionEmployee(session);

        try {
            leaveApplication.setEmployee(emp);
            leaveApplication.setStatus(LeaveStatus.APPLIED);

            // Resolve LeaveType entity from the ID sent by the form
            if (leaveApplication.getLeaveType() != null
                    && leaveApplication.getLeaveType().getLeaveTypeId() != null) {
                LeaveType lt = leaveTypeRepo
                        .findById(leaveApplication.getLeaveType().getLeaveTypeId())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid leave type."));
                leaveApplication.setLeaveType(lt);
            }

            leaveApplicationService.submitLeaveApplication(leaveApplication);
            ra.addFlashAttribute("successMessage", "Leave application submitted successfully.");
            return "redirect:/leave/history";

        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("leaveTypes", leaveTypeRepo.findAll());
            model.addAttribute("leaveBalances",
                    leaveBalanceService.getLeaveBalancesByEmployeeId(emp.getEmp_id()));
            return "leave/apply";
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EMPLOYEE — Personal leave history
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/history")
    public String history(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long leaveTypeId,
            HttpSession session, Model model) {

        if (!isLoggedIn(session)) return "redirect:/login";
        Employee emp = getSessionEmployee(session);

        // Apply filters if provided
        List<LeaveApplication> applications;
        if (status != null && !status.isBlank() && leaveTypeId != null) {
            LeaveStatus leaveStatus = LeaveStatus.valueOf(status);
            applications = leaveApplicationService
                    .getPersonalLeaveHistoryByStatusAndType(emp, leaveStatus, leaveTypeId);
        } else if (status != null && !status.isBlank()) {
            LeaveStatus leaveStatus = LeaveStatus.valueOf(status);
            applications = leaveApplicationService
                    .getPersonalLeaveHistoryByStatus(emp, leaveStatus);
        } else if (leaveTypeId != null) {
            applications = leaveApplicationService
                    .getPersonalLeaveHistoryByType(emp, leaveTypeId);
        } else {
            applications = leaveApplicationService.getPersonalLeaveHistory(emp);
        }

        model.addAttribute("applications", applications);
        model.addAttribute("leaveBalances",
                leaveBalanceService.getLeaveBalancesByEmployeeId(emp.getEmp_id()));
        model.addAttribute("leaveStatuses", Arrays.asList(LeaveStatus.values()));
        model.addAttribute("leaveTypes", leaveTypeRepo.findAll());
        model.addAttribute("selectedStatus", status != null ? status : "");
        model.addAttribute("selectedLeaveTypeId", leaveTypeId);
        return "leave/history";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EMPLOYEE — View single application detail
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, HttpSession session, Model model,
                         RedirectAttributes ra) {
        if (!isLoggedIn(session)) return "redirect:/login";

        Employee emp = getSessionEmployee(session);
        return leaveApplicationService.findById(id).map(leaveApplication -> {
            model.addAttribute("leaveApp", leaveApplication);
            model.addAttribute("employee", emp);
            return "leave/detail";
        }).orElseGet(() -> {
            ra.addFlashAttribute("errorMessage", "Leave application not found.");
            return "redirect:/leave/history";
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EMPLOYEE — Edit leave application
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, HttpSession session, Model model,
                           RedirectAttributes ra) {
        if (!isLoggedIn(session)) return "redirect:/login";
        Employee emp = getSessionEmployee(session);

        return leaveApplicationService.findById(id).map(la -> {
            if (!la.isEditable()) {
                ra.addFlashAttribute("errorMessage",
                        "This application cannot be edited (status: " + la.getStatus() + ").");
                return "redirect:/leave/history";
            }
            if (!la.getEmployee().getEmp_id().equals(emp.getEmp_id())) {
                ra.addFlashAttribute("errorMessage", "Not authorised.");
                return "redirect:/leave/history";
            }
            model.addAttribute("leaveApplication", la);
            model.addAttribute("leaveTypes", leaveTypeRepo.findAll());
            model.addAttribute("leaveBalances",
                    leaveBalanceService.getLeaveBalancesByEmployeeId(emp.getEmp_id()));
            return "leave/edit";
        }).orElseGet(() -> {
            ra.addFlashAttribute("errorMessage", "Leave application not found.");
            return "redirect:/leave/history";
        });
    }

    @PostMapping("/{id}/edit")
    public String editSubmit(
            @PathVariable Long id,
            @ModelAttribute LeaveApplication leaveApplication,
            HttpSession session,
            RedirectAttributes ra,
            Model model) {

        if (!isLoggedIn(session)) return "redirect:/login";
        Employee emp = getSessionEmployee(session);

        try {
            leaveApplication.setLeaveApplicationId(id);
            leaveApplication.setEmployee(emp);

            if (leaveApplication.getLeaveType() != null
                    && leaveApplication.getLeaveType().getLeaveTypeId() != null) {
                LeaveType lt = leaveTypeRepo
                        .findById(leaveApplication.getLeaveType().getLeaveTypeId())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid leave type."));
                leaveApplication.setLeaveType(lt);
            }

            leaveApplicationService.updateLeaveApplication(leaveApplication);
            ra.addFlashAttribute("successMessage", "Leave application updated successfully.");
            return "redirect:/leave/history";

        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/leave/" + id + "/edit";
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EMPLOYEE — Delete leave application (soft delete)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!isLoggedIn(session)) return "redirect:/login";
        Employee emp = getSessionEmployee(session);

        try {
            leaveApplicationService.deleteLeave(id, emp);
            ra.addFlashAttribute("successMessage", "Leave application deleted.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/leave/history";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EMPLOYEE — Cancel approved leave
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!isLoggedIn(session)) return "redirect:/login";
        Employee emp = getSessionEmployee(session);

        try {
            leaveApplicationService.cancelLeave(id, emp);
            ra.addFlashAttribute("successMessage", "Leave application cancelled and balance restored.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/leave/history";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MANAGER — Pending applications awaiting approval
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/manager/pending")
    public String managerPending(HttpSession session, Model model) {
        if (!isLoggedIn(session)) return "redirect:/login";
        Employee mgr = getSessionEmployee(session);

        model.addAttribute("applications",
                leaveApplicationService.getPendingApplicationsForManager(mgr));
        return "leave/manager-pending";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MANAGER — All subordinate leave history
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/manager/all")
    public String managerAll(HttpSession session, Model model) {
        if (!isLoggedIn(session)) return "redirect:/login";
        Employee mgr = getSessionEmployee(session);

        model.addAttribute("applications",
                leaveApplicationService.getAllApplicationsForManager(mgr));
        return "leave/manager-all";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MANAGER — Approve
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!isLoggedIn(session)) return "redirect:/login";
        Employee mgr = getSessionEmployee(session);

        try {
            leaveApplicationService.approveLeave(id, mgr);
            ra.addFlashAttribute("successMessage", "Leave application approved.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/leave/manager/pending";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MANAGER — Reject
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/reject")
    public String reject(
            @PathVariable Long id,
            @RequestParam String managerComment,
            HttpSession session,
            RedirectAttributes ra) {

        if (!isLoggedIn(session)) return "redirect:/login";
        Employee mgr = getSessionEmployee(session);

        try {
            leaveApplicationService.rejectLeave(id, mgr, managerComment);
            ra.addFlashAttribute("successMessage", "Leave application rejected.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/leave/manager/pending";
    }
}