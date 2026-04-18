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
        return GlobalConstants.VIEW_ADMIN_LEAVETYPE_MANAGE;
    }

    @PostMapping(GlobalConstants.ROUTE_ADMIN_LEAVETYPE_SAVE)
    public String save(@ModelAttribute("leaveType") LeaveType leaveType) {
        if (leaveType.getLeaveTypeId() == null) {
            leaveTypeService.saveLeaveType(leaveType);
        } else {
            leaveTypeService.updateLeaveType(leaveType.getLeaveTypeId(), leaveType);
        }

        return GlobalConstants.REDIRECT_ADMIN_LEAVETYPE_LIST;
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, Model model) {
        model.addAttribute("leaveType", leaveTypeService.findById(id));
        model.addAttribute("leaveTypes", leaveTypeService.findAll());
        model.addAttribute("nameTypes", NameTypeEnum.values());
        return GlobalConstants.VIEW_ADMIN_LEAVETYPE_MANAGE;
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable("id") Long id) {
        leaveTypeService.deleteLeaveType(id);
        return GlobalConstants.REDIRECT_ADMIN_LEAVETYPE_LIST;
    }
}
