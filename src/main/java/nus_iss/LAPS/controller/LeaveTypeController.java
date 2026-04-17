package nus_iss.LAPS.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import nus_iss.LAPS.model.LeaveType;
import nus_iss.LAPS.model.NameTypeEnum;
import nus_iss.LAPS.service.LeaveTypeService;
import nus_iss.LAPS.util.GlobalConstants;

/**
	* Author: Junior
	* Created on: 15/04/2026
**/

@Controller
@RequestMapping(GlobalConstants.ROUTE_ADMIN + GlobalConstants.ROUTE_ADMIN_LEAVETYPE)
public class LeaveTypeController {

    private final LeaveTypeService leaveTypeService;

    public LeaveTypeController(LeaveTypeService leaveTypeService) {
        this.leaveTypeService = leaveTypeService;
    }

    @GetMapping
    public String showPage(Model model) {
        model.addAttribute("leaveTypes", leaveTypeService.findAll());
        model.addAttribute("leaveType", new LeaveType());
        model.addAttribute("nameTypes", NameTypeEnum.values());
        return "admin/leavetype-manage";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute LeaveType leaveType) {
        if (leaveType.getLeaveTypeId() == null) {
            leaveTypeService.saveLeaveType(leaveType);
        } else {
            leaveTypeService.updateLeaveType(leaveType.getLeaveTypeId(), leaveType);
        }

        return "redirect:/admin/leavetype";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("leaveType", leaveTypeService.findById(id));
        model.addAttribute("leaveTypes", leaveTypeService.findAll());
        model.addAttribute("nameTypes", NameTypeEnum.values());

        return "admin/leavetype-manage";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        leaveTypeService.deleteLeaveType(id);
        return "redirect:/admin/leavetype";
    }
}