package nus_iss.LAPS.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.model.LeaveBalance;
import nus_iss.LAPS.service.LeaveBalanceService;

@Controller
@RequestMapping("/leave-balances")
public class LeaveBalanceController {

	private LeaveBalanceService leaveBalanceService;

	public LeaveBalanceController(LeaveBalanceService leaveBalanceService) {
		this.leaveBalanceService = leaveBalanceService;
	}

    private boolean isLoggedIn(HttpSession session) {
        return session.getAttribute("userId") != null;
    }
	
	// View
	@GetMapping("/view")
	public String viewLeaveBalances(@RequestParam(required = false) Long employeeId, HttpSession session, Model model) {

		if (!isLoggedIn(session)) return "redirect:/users/login";
		
		List<Employee> employees = leaveBalanceService.getAllEmployees();
		model.addAttribute("employees", employees);

		if (employeeId != null) {
			List<LeaveBalance> balances = leaveBalanceService
					.getLeaveBalancesByEmployeeId(employeeId);
			model.addAttribute("balances", balances);
		} else {
			List<LeaveBalance> balances = leaveBalanceService.getAllLeaveBalances();
			model.addAttribute("balances", balances);
		}

		return "leave-balance/leave-balance-list";
	}

	// Create
	@GetMapping("/create")
	public String showCreateForm(HttpServletRequest request, HttpSession session, Model model) {
		
		if (!isLoggedIn(session)) return "redirect:/users/login";
		model.addAttribute("employees", leaveBalanceService.getAllEmployees());
		model.addAttribute("leaveTypes", leaveBalanceService.getAllLeaveTypes());
		model.addAttribute("currentPath", request.getRequestURI());

		return "leave-balance/leave-balance-form";
	}

	@PostMapping("/create")
	public String createLeaveBalance(@RequestParam Long employeeId, @RequestParam Long leaveTypeId,
			@RequestParam double totalDays,
			Model model, RedirectAttributes ra, HttpSession session) {

		if (!isLoggedIn(session)) return "redirect:/users/login";
		try {
			leaveBalanceService.createLeaveBalance(employeeId, leaveTypeId, totalDays);
			ra.addFlashAttribute("success", "Leave balance created successfully");
			return "redirect:/leave-balances/view";
		} catch (IllegalArgumentException e) {
			ra.addFlashAttribute("error", e.getMessage());
			model.addAttribute("employees", leaveBalanceService.getAllEmployees());
			model.addAttribute("leaveTypes", leaveBalanceService.getAllLeaveTypes());
			return "redirect:/leave-balances/create";
		}
	}

	// Edit
	@GetMapping("/edit/{leaveBalanceId}")
	public String editLeaveBalance(HttpServletRequest request, HttpSession session, @PathVariable Long leaveBalanceId,
			Model model) {
		
		if (!isLoggedIn(session)) return "redirect:/users/login";
		LeaveBalance balance = leaveBalanceService.getLeaveBalanceById(leaveBalanceId)
				.orElseThrow(() -> new RuntimeException("Leave Balance record not found"));
		model.addAttribute("balance", balance);
		model.addAttribute("currentPath", request.getRequestURI());
		return "leave-balance/leave-balance-form";
	}

	@PostMapping("/update-total-days")
	public String updateLeaveBalanceTotalDays(@RequestParam Long leaveBalanceId,
			@RequestParam double totalDays, RedirectAttributes ra, HttpSession session) {
		
		if (!isLoggedIn(session)) return "redirect:/users/login";
		try {
			leaveBalanceService.updateTotalDays(leaveBalanceId, totalDays);
			ra.addFlashAttribute("success", "Updated successfully");
			return "redirect:/leave-balances/view";
		} catch (RuntimeException e) {
			ra.addFlashAttribute("error", e.getMessage());
			return "redirect:/leave-balances/edit/" + leaveBalanceId;
		}
	}

	@PostMapping("/update-used-days")
	public String updateLeaveBalanceUsedDays(@RequestParam Long leaveBalanceId,
			@RequestParam double usedDays, HttpSession session) {

		if (!isLoggedIn(session)) return "redirect:/users/login";
		leaveBalanceService.updateUsedDays(leaveBalanceId, usedDays);

		return "redirect:/leave-balances/view";
	}

	@PostMapping("/increment-used-days")
	public String incrementLeaveBalanceUsedDays(@RequestParam Long leaveBalanceId,
			@RequestParam double leaveDurationDays, HttpSession session) {

		if (!isLoggedIn(session)) return "redirect:/users/login";
		leaveBalanceService.incrementUsedDays(leaveBalanceId, leaveDurationDays);

		return "redirect:/leave-balances/view";
	}

	@PostMapping("/decrement-used-days")
	public String decrementLeaveBalanceUsedDays(@RequestParam Long leaveBalanceId,
			@RequestParam double leaveDurationDays, HttpSession session) {
		
		if (!isLoggedIn(session)) return "redirect:/users/login";
		leaveBalanceService.decrementUsedDays(leaveBalanceId, leaveDurationDays);

		return "redirect:/leave-balances/view";
	}

	// Delete
	@GetMapping("/delete/{leaveBalanceId}")
	public String showLeaveBalance(@PathVariable Long leaveBalanceId, Model model,
			HttpServletRequest request, HttpSession session) {
		
		if (!isLoggedIn(session)) return "redirect:/users/login";
		
		LeaveBalance balance = leaveBalanceService.getLeaveBalanceById(leaveBalanceId)
				.orElseThrow(() -> new RuntimeException("Leave Balance record not found"));
		model.addAttribute("balance", balance);
		model.addAttribute("currentPath", request.getRequestURI());
		return "leave-balance/leave-balance-form";
	}

	@PostMapping("/delete/{leaveBalanceId}")
	public String deleteLeaveBalance(@PathVariable Long leaveBalanceId, RedirectAttributes ra, HttpSession session) {
		
		if (!isLoggedIn(session)) return "redirect:/users/login";
		
		try {
			leaveBalanceService.deleteLeaveBalance(leaveBalanceId);
			ra.addFlashAttribute("success", "Deleted successfully");
			return "redirect:/leave-balances/view";
		} catch (RuntimeException e) {
			ra.addFlashAttribute("error", e.getMessage());
			return "redirect:/leave-balances/delete/" + leaveBalanceId;
		}
	}
}
