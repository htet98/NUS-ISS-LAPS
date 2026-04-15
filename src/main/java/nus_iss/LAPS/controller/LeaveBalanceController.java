package nus_iss.LAPS.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.model.LeaveBalance;
import nus_iss.LAPS.service.LeaveBalanceService;

@Controller
@RequestMapping("/leave-balances")
public class LeaveBalanceController {

	private final LeaveBalanceService leaveBalanceService;

	public LeaveBalanceController(LeaveBalanceService leaveBalanceService) {
		this.leaveBalanceService = leaveBalanceService;
	}

	@GetMapping("/view")
	public String viewLeaveBalances(@RequestParam(required = false) Long employeeId, Model model) {

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

//		model.addAttribute("employeeName", employeeName);

		return "leave-balance-list";
	}

	@GetMapping("/create")
	public String showCreateForm(Model model) {

		model.addAttribute("employees", leaveBalanceService.getAllEmployees());
		model.addAttribute("leaveTypes", leaveBalanceService.getAllLeaveTypes());

		return "leave-balance-form";
	}

	@PostMapping("/create")
	public String createLeaveBalance(@RequestParam Long employeeId, @RequestParam Long leaveTypeId,
			@RequestParam double totalDays) {
		leaveBalanceService.createLeaveBalance(employeeId, leaveTypeId, totalDays);

//		return "redirect:/leave-balances/view";
		return "redirect:/leave-balances/view?employeeId=" + employeeId;
	}

	@GetMapping("/edit/{leaveBalanceId}")
	public String editLeaveBalance(@PathVariable Long leaveBalanceId, Model model) {
		LeaveBalance balance = leaveBalanceService.getLeaveBalanceById(leaveBalanceId)
				.orElseThrow(() -> new RuntimeException("Leave Balance record not found"));
		model.addAttribute("balance", balance);
		return "leave-balance-edit-form";
	}

	@PostMapping("/update-total-days")
	public String updateLeaveBalanceTotalDays(@RequestParam Long leaveBalanceId,
			@RequestParam double totalDays) {
		leaveBalanceService.updateTotalDays(leaveBalanceId, totalDays);

		return "redirect:/leave-balances/view";
	}

	@PostMapping("/update-used-days")
	public String updateLeaveBalanceUsedDays(@RequestParam Long leaveBalanceId,
			@RequestParam double usedDays) {

		leaveBalanceService.updateUsedDays(leaveBalanceId, usedDays);

		return "redirect:/leave-balances/view";
	}

	@PostMapping("/increment-used-days")
	public String incrementLeaveBalanceUsedDays(@RequestParam Long leaveBalanceId,
			@RequestParam double leaveDurationDays) {

		leaveBalanceService.incrementUsedDays(leaveBalanceId, leaveDurationDays);

		return "redirect:/leave-balances/view";
	}

	@PostMapping("/decrement-used-days")
	public String decrementLeaveBalanceUsedDays(@RequestParam Long leaveBalanceId,
			@RequestParam double leaveDurationDays) {

		leaveBalanceService.decrementUsedDays(leaveBalanceId, leaveDurationDays);

		return "redirect:/leave-balances/view";
	}

	@GetMapping("/delete/{leaveBalanceId}")
	public String showLeaveBalance(@PathVariable Long leaveBalanceId, Model model) {
		LeaveBalance balance = leaveBalanceService.getLeaveBalanceById(leaveBalanceId)
				.orElseThrow(() -> new RuntimeException("Leave Balance record not found"));
		model.addAttribute("balance", balance);
		return "leave-balance-details";
	}

	@PostMapping("/delete/{leaveBalanceId}")
	public String deleteLeaveBalance(@PathVariable Long leaveBalanceId) {

		leaveBalanceService.deleteLeaveBalance(leaveBalanceId);

		return "redirect:/leave-balances/view";
	}
}
