package nus_iss.LAPS.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import nus_iss.LAPS.model.LeaveBalance;
import nus_iss.LAPS.model.Role;
import nus_iss.LAPS.repository.LeaveBalanceRepository;
import nus_iss.LAPS.service.LeaveBalanceService;

@Controller
@RequestMapping("/leave-balances")
public class LeaveBalanceController {

	private LeaveBalanceService leaveBalanceService;

	@Autowired
	private LeaveBalanceRepository leaveBalanceRepo;

	public LeaveBalanceController(LeaveBalanceService leaveBalanceService) {
		this.leaveBalanceService = leaveBalanceService;
	}

	private boolean isLoggedIn(HttpSession session) {
		return session.getAttribute("userId") != null;
	}

	private boolean isAdmin(HttpSession session) {
		return session.getAttribute("role") == Role.ADMIN;
	}

	@GetMapping("")
	public String listLeaveBalances(Model model,
			@RequestParam(name = "keyword", required = false) String keyword,
			@RequestParam(name = "page", defaultValue = "0") int page, HttpSession session) {

		if (!isLoggedIn(session))
			return "redirect:/users/login";

		if (!isAdmin(session)) {
			model.addAttribute("error", "Unauthorized access");
			return "leave-balance/leave-balance-list";
		}

		int pageSize = 10;
		Pageable pageable = PageRequest.of(page, pageSize);

		Page<LeaveBalance> leaveBalancePage;
		if (keyword != null && !keyword.isEmpty()) {
			leaveBalancePage = leaveBalanceRepo.searchByFullName(keyword, pageable);
		} else {
			leaveBalancePage = leaveBalanceRepo.findAll(pageable);
		}

		model.addAttribute("leaveBalancePage", leaveBalancePage);
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", leaveBalancePage.getTotalPages());
		model.addAttribute("keyword", keyword);

		return "leave-balance/leave-balance-list";
	}

	// View
	@GetMapping("/view")
	public String viewLeaveBalances(Model model,
			@RequestParam(name = "keyword", required = false) String keyword,
			@RequestParam(name = "page", defaultValue = "0") int page, HttpSession session) {

		if (!isLoggedIn(session))
			return "redirect:/users/login";

		if (!isAdmin(session)) {
			model.addAttribute("error", "Unauthorized access");
			return "leave-balance/leave-balance-list";
		}

		int pageSize = 10;
		Pageable pageable = PageRequest.of(page, pageSize);

		Page<LeaveBalance> leaveBalancePage;
		if (keyword != null && !keyword.isEmpty()) {
			leaveBalancePage = leaveBalanceRepo.searchByFullName(keyword, pageable);
		} else {
			leaveBalancePage = leaveBalanceRepo.findAll(pageable);
		}

		model.addAttribute("leaveBalancePage", leaveBalancePage);
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", leaveBalancePage.getTotalPages());
		model.addAttribute("keyword", keyword);

		return "leave-balance/leave-balance-list";
	}

	// Create
	@GetMapping("/create")
	public String showCreateForm(HttpServletRequest request, HttpSession session, Model model) {

		if (!isLoggedIn(session))
			return "redirect:/users/login";
		
		if (!isAdmin(session)) {
			model.addAttribute("error", "Unauthorized access");
			return "leave-balance/leave-balance-form";
		}

		model.addAttribute("employees", leaveBalanceService.getAllEmployees());
		model.addAttribute("leaveTypes", leaveBalanceService.getAllLeaveTypes());
		model.addAttribute("currentPath", request.getRequestURI());

		return "leave-balance/leave-balance-form";
	}

	@PostMapping("/create")
	public String createLeaveBalance(@RequestParam Long employeeId, @RequestParam Long leaveTypeId,
			@RequestParam double totalDays, Model model, RedirectAttributes ra,
			HttpSession session) {

		if (!isLoggedIn(session))
			return "redirect:/users/login";
		
		try {
			leaveBalanceService.createLeaveBalance(employeeId, leaveTypeId, totalDays);
			ra.addFlashAttribute("success", "Leave balance created successfully");
			return "redirect:/leave-balances";
		} catch (IllegalArgumentException e) {
			ra.addFlashAttribute("error", e.getMessage());
			model.addAttribute("employees", leaveBalanceService.getAllEmployees());
			model.addAttribute("leaveTypes", leaveBalanceService.getAllLeaveTypes());
			return "redirect:/leave-balances/create";
		}
	}

	// Edit
	@GetMapping("/edit/{leaveBalanceId}")
	public String editLeaveBalance(HttpServletRequest request, HttpSession session,
			@PathVariable Long leaveBalanceId, Model model) {

		if (!isLoggedIn(session))
			return "redirect:/users/login";
		
		if (!isAdmin(session)) {
			model.addAttribute("error", "Unauthorized access");
			return "leave-balance/leave-balance-form";
		}
		
		LeaveBalance balance = leaveBalanceService.getLeaveBalanceById(leaveBalanceId)
				.orElseThrow(() -> new RuntimeException("Leave Balance record not found"));
		model.addAttribute("balance", balance);
		model.addAttribute("currentPath", request.getRequestURI());
		return "leave-balance/leave-balance-form";
	}

	@PostMapping("/update-total-days")
	public String updateLeaveBalanceTotalDays(@RequestParam Long leaveBalanceId,
			@RequestParam double totalDays, RedirectAttributes ra, HttpSession session) {

		if (!isLoggedIn(session))
			return "redirect:/users/login";
		try {
			leaveBalanceService.updateTotalDays(leaveBalanceId, totalDays);
			ra.addFlashAttribute("success", "Updated successfully");
			return "redirect:/leave-balances";
		} catch (RuntimeException e) {
			ra.addFlashAttribute("error", e.getMessage());
			return "redirect:/leave-balances/edit/" + leaveBalanceId;
		}
	}

	@PostMapping("/update-used-days")
	public String updateLeaveBalanceUsedDays(@RequestParam Long leaveBalanceId,
			@RequestParam double usedDays, HttpSession session) {

		if (!isLoggedIn(session))
			return "redirect:/users/login";
		leaveBalanceService.updateUsedDays(leaveBalanceId, usedDays);

		return "redirect:/leave-balances";
	}

	@PostMapping("/increment-used-days")
	public String incrementLeaveBalanceUsedDays(@RequestParam Long leaveBalanceId,
			@RequestParam double leaveDurationDays, HttpSession session) {

		if (!isLoggedIn(session))
			return "redirect:/users/login";
		leaveBalanceService.incrementUsedDays(leaveBalanceId, leaveDurationDays);

		return "redirect:/leave-balances";
	}

	@PostMapping("/decrement-used-days")
	public String decrementLeaveBalanceUsedDays(@RequestParam Long leaveBalanceId,
			@RequestParam double leaveDurationDays, HttpSession session, RedirectAttributes ra) {

		if (!isLoggedIn(session))
			return "redirect:/users/login";
		
		try {			
			leaveBalanceService.decrementUsedDays(leaveBalanceId, leaveDurationDays);
			return "redirect:/leave-balances";
		} catch (Exception e) {
			ra.addFlashAttribute("error", e.getMessage());
			return "redirect:/leave-balances";
		}	
	}

	// Delete
	@GetMapping("/delete/{leaveBalanceId}")
	public String showLeaveBalance(@PathVariable Long leaveBalanceId, Model model,
			HttpServletRequest request, HttpSession session) {

		if (!isLoggedIn(session))
			return "redirect:/users/login";
		
		if (!isAdmin(session)) {
			model.addAttribute("error", "Unauthorized access");
			return "leave-balance/leave-balance-form";
		}

		LeaveBalance balance = leaveBalanceService.getLeaveBalanceById(leaveBalanceId)
				.orElseThrow(() -> new RuntimeException("Leave Balance record not found"));
		model.addAttribute("balance", balance);
		model.addAttribute("currentPath", request.getRequestURI());
		return "leave-balance/leave-balance-form";
	}

	@PostMapping("/delete/{leaveBalanceId}")
	public String deleteLeaveBalance(@PathVariable Long leaveBalanceId, RedirectAttributes ra,
			HttpSession session) {

		if (!isLoggedIn(session))
			return "redirect:/users/login";

		try {
			leaveBalanceService.deleteLeaveBalance(leaveBalanceId);
			ra.addFlashAttribute("success", "Deleted successfully");
			return "redirect:/leave-balances";
		} catch (RuntimeException e) {
			ra.addFlashAttribute("error", e.getMessage());
			return "redirect:/leave-balances/delete/" + leaveBalanceId;
		}
	}
}
