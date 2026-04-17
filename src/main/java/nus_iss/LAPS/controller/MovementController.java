package nus_iss.LAPS.controller;

import jakarta.servlet.http.HttpSession;
import nus_iss.LAPS.model.LeaveApplication;
import nus_iss.LAPS.repository.LeaveApplicationRepository;
import nus_iss.LAPS.util.BreadcrumbItem;
import nus_iss.LAPS.util.GlobalConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Movement Register — shows all employees on approved leave during a given month.
 * Available to ALL logged-in users (employees, managers, and admins).
 *
 * Routes:
 *   GET /movement/register?year=2026&month=4
 *
 * Author: Htet Nandar (Grace)
 */
@Controller
@RequestMapping(GlobalConstants.ROUTE_MOVEMENT)
public class MovementController {

    @Autowired
    private LeaveApplicationRepository leaveAppRepo;

    private boolean isLoggedIn(HttpSession session) {
        return session.getAttribute("userId") != null;
    }

    @GetMapping(GlobalConstants.ROUTE_MOVEMENT_REGISTER)
    public String register(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            HttpSession session, Model model) {

        if (!isLoggedIn(session)) return GlobalConstants.REDIRECT_LOGIN;

        LocalDate today = LocalDate.now();
        int selectedYear  = (year  != null) ? year  : today.getYear();
        int selectedMonth = (month != null) ? month : today.getMonthValue();

        // Clamp to valid month range
        if (selectedMonth < 1)  selectedMonth = 1;
        if (selectedMonth > 12) selectedMonth = 12;

        YearMonth ym = YearMonth.of(selectedYear, selectedMonth);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd   = ym.atEndOfMonth();

        List<LeaveApplication> applications = leaveAppRepo.findApprovedLeavesInMonth(monthStart, monthEnd);

        // Build the list of months to show in the navigation dropdown (current year ± 1)
        List<YearMonth> monthOptions = new ArrayList<>();
        for (int y = today.getYear() - 1; y <= today.getYear() + 1; y++) {
            for (int m = 1; m <= 12; m++) {
                monthOptions.add(YearMonth.of(y, m));
            }
        }

        model.addAttribute("applications",   applications);
        model.addAttribute("selectedYear",   selectedYear);
        model.addAttribute("selectedMonth",  selectedMonth);
        model.addAttribute("monthStart",     monthStart);
        model.addAttribute("monthEnd",       monthEnd);
        model.addAttribute("monthOptions",   monthOptions);
        model.addAttribute("currentYearMonth", ym);
        
        // Add breadcrumbs
        model.addAttribute("breadcrumbs", List.of(
            new BreadcrumbItem("LAPS", "/"),
            new BreadcrumbItem("Movement Register", null)
        ));

        return GlobalConstants.VIEW_MOVEMENT_REGISTER;
    }
}
