package nus_iss.LAPS.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nus_iss.LAPS.model.LeaveType;
import nus_iss.LAPS.model.Role;
import nus_iss.LAPS.service.LeaveTypeService;
import nus_iss.LAPS.validators.LeaveTypeValidator;

@Controller
@RequestMapping("/admin/leave-type")
@RequiredArgsConstructor
public class LeaveTypeController {

	private final LeaveTypeService ltService;
	private final LeaveTypeValidator ltValidator;

	@InitBinder("leaveType")
	protected void initBinder(WebDataBinder binder) {
		binder.addValidators(ltValidator);
	}

	private boolean isLoggedIn(HttpSession session) {
		return (session.getAttribute("user") != null);
	}

	private boolean isAdmin(HttpSession session) {
		Role role = (Role) session.getAttribute("role");
		return Role.ADMIN.equals(role);
	}

	@GetMapping("/create")
	public ModelAndView newLeaveTypePage(HttpSession session, RedirectAttributes redirect) {
		
		 if(!isLoggedIn(session)){ return new ModelAndView("redirect:/login"); }
		 
		 
		 if(!isAdmin(session)){ redirect.addFlashAttribute("errorMessage",
		 "Only admins can view leave types"); return new
		 ModelAndView("redirect:/login"); }
		 

		var mav = new ModelAndView("leave-type-new", "leaveType", new LeaveType());
		mav.addObject("ltidlist", ltService.getAllLeaveTypes());
		return mav;
	}

	@PostMapping("/create")
	public ModelAndView createLeaveType(@ModelAttribute @Valid LeaveType leaveType, BindingResult bindingResult,
			HttpSession session, RedirectAttributes redirect) {

		
		 if(!isLoggedIn(session)){ return new ModelAndView("redirect:/login"); }
		 
		 
		 if(!isAdmin(session)){ redirect.addFlashAttribute("errorMessage",
		 "Only admins can view leave types"); return new
		 ModelAndView("redirect:/login"); }
		 

		if (bindingResult.hasErrors()) {
			return new ModelAndView("leave-type-new");
		}

		try {
			ltService.createLeaveType(leaveType);
			redirect.addFlashAttribute("successMessage", "Leave type created successfully");
		} catch (Exception e) {

			var mav = new ModelAndView("leave-type-new");
			mav.addObject("errorMessage", "Could not create leave type");
			return mav;
		}
		return new ModelAndView("redirect:/admin/leave-type/list");
	}

	@RequestMapping("/list")
	public String listLeaveType(HttpSession session, RedirectAttributes redirect, Model model) {
		
		 if(!isLoggedIn(session)){ return "redirect:/login"; }
		 
		 
		 if(!isAdmin(session)){ redirect.addFlashAttribute("errorMessage",
		 "Only admins can view leave types list"); return "redirect:/login"; }
		 

		model.addAttribute("leaveTypesList", ltService.getAllLeaveTypes());
		return "leave-type-list";
	}

	@GetMapping("/edit/{id}")
	public ModelAndView editLeaveTypePage(@PathVariable("id") Long id, HttpSession session,
			RedirectAttributes redirect) {
		
		 if(!isLoggedIn(session)){ return new ModelAndView("redirect:/login"); }
	
		 
		 if(!isAdmin(session)){ redirect.addFlashAttribute("errorMessage",
		 "Only admins can edit leave types"); return new
		 ModelAndView("redirect:/login"); }
		 

		var mav = new ModelAndView("leave-type-edit");
		ltService.getLeaveTypeById(id).ifPresent(leaveT -> mav.addObject("leaveType", leaveT));

		mav.addObject("ltidlist", ltService.getAllLeaveTypes());

		return mav;
	}

	@PostMapping("/edit/{id}")
	public ModelAndView editLeaveType(@ModelAttribute("leaveType") @Valid LeaveType leaveType,
			BindingResult bindingResult, @PathVariable("id") Long id, HttpSession session,
			RedirectAttributes redirect) {
		
		 if(!isLoggedIn(session)){ return new ModelAndView("redirect:/login"); }
		 
		 
		 if(!isAdmin(session)){ redirect.addFlashAttribute("errorMessage",
		 "Only admins can edit leave types"); return new
		 ModelAndView("redirect:/login"); }
		 

		if (bindingResult.hasErrors()) {
			return new ModelAndView("leave-type-edit");
		}

		try {
			leaveType.setLeaveTypeId(id);
			ltService.changeLeaveType(leaveType);
			redirect.addFlashAttribute("successMessage", "Leave type updated successfully");
		} catch (Exception e) {
			redirect.addFlashAttribute("errorMessage", "Could not edit leave type");
			return new ModelAndView("leave-type-edit");
		}
		return new ModelAndView("redirect:/admin/leave-type/list");
	}

	@RequestMapping("/delete/{id}")
	public ModelAndView deleteLeaveType(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirect) {
		
		 if(!isLoggedIn(session)){ return new ModelAndView("redirect:/login"); }
		 if(!isAdmin(session)){ redirect.addFlashAttribute("message",
		 "Only admins can delete leave types"); return new
		 ModelAndView("redirect:/login"); }
		 

		try {
			ltService.getLeaveTypeById(id).ifPresent(ltService::removeLeaveType);
			redirect.addFlashAttribute("successMessage", "Leave type has been deleted");

		} catch (Exception e) {
			redirect.addFlashAttribute("errorMessage", "An unexpected error has occurred");
		}

		return new ModelAndView("redirect:/admin/leave-type/list");

	}

}
