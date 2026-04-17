package nus_iss.LAPS.controller;

import java.time.LocalDateTime;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.model.Role;
import nus_iss.LAPS.model.User;
import nus_iss.LAPS.service.EmployeeService;
import nus_iss.LAPS.util.GlobalConstants;
import nus_iss.LAPS.validators.EmployeeValidator;

@Controller
@RequestMapping(GlobalConstants.ROUTE_ADMIN + GlobalConstants.ROUTE_ADMIN_EMPLOYEE)
@RequiredArgsConstructor
@Slf4j
public class EmployeeController {

    private final EmployeeService eService;
    private final EmployeeValidator eValidator;

    private boolean isAdmin(HttpSession session) {
        Role role = (Role) session.getAttribute("role");
        return Role.ADMIN.equals(role);
    }

    private ModelAndView denyAccess(RedirectAttributes redirect) {
        redirect.addFlashAttribute(GlobalConstants.FLASH_ERROR, "Access denied.");
        return new ModelAndView(GlobalConstants.REDIRECT_LOGIN);
    }

    private String getActor(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return (user != null && user.getUsername() != null)
                ? user.getUsername()
                : "system";
    }

    private void populateForm(ModelAndView mav) {
        mav.addObject("eidlist", eService.findAllEmployeeIDs());
        mav.addObject("supervisorList", eService.findAllSupervisors());
    }

    @GetMapping(GlobalConstants.ROUTE_ADMIN_EMPLOYEE_NEW)
    public ModelAndView newEmployeePage(HttpSession session, RedirectAttributes redirect) {
        if (!isAdmin(session)) return denyAccess(redirect);

        ModelAndView mav = new ModelAndView(GlobalConstants.VIEW_ADMIN_EMPLOYEE_NEW);
        mav.addObject("employee", new Employee());
        populateForm(mav);
        return mav;
    }

    @PostMapping(GlobalConstants.ROUTE_ADMIN_EMPLOYEE_NEW)
    public ModelAndView createNewEmployee(
            @ModelAttribute @Valid Employee employee,
            BindingResult result,
            HttpSession session,
            RedirectAttributes redirect) {

        if (!isAdmin(session)) return denyAccess(redirect);

        eValidator.validate(employee, result);

        if (result.hasErrors()) {
            ModelAndView mav = new ModelAndView(GlobalConstants.VIEW_ADMIN_EMPLOYEE_NEW);
            mav.addObject("employee", employee);
            populateForm(mav);
            return mav;
        }

        employee.setCreatedBy(getActor(session));
        employee.setCreatedWhen(LocalDateTime.now());

        eService.createEmployee(employee);
        log.info("Employee {} created.", employee.getEmp_id());

        redirect.addFlashAttribute(GlobalConstants.FLASH_SUCCESS, "Employee created successfully.");
        return new ModelAndView(GlobalConstants.REDIRECT_ADMIN_EMPLOYEE_LIST);
    }

    @GetMapping(GlobalConstants.ROUTE_ADMIN_EMPLOYEE_LIST)
    public ModelAndView listEmployeePage(HttpSession session, RedirectAttributes redirect) {
        if (!isAdmin(session)) return denyAccess(redirect);

        ModelAndView mav = new ModelAndView(GlobalConstants.VIEW_ADMIN_EMPLOYEE_LIST);
        mav.addObject("employeeList", eService.findAllEmployees());
        return mav;
    }

    @GetMapping("/edit/{id}")
    public ModelAndView editEmployeePage(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirect) {

        if (!isAdmin(session)) return denyAccess(redirect);

        ModelAndView mav = new ModelAndView(GlobalConstants.VIEW_ADMIN_EMPLOYEE_EDIT);

        eService.findEmployee(id).ifPresentOrElse(
                emp -> mav.addObject("employee", emp),
                () -> { throw new IllegalArgumentException("Employee not found: " + id); }
        );

        populateForm(mav);
        return mav;
    }

    @PostMapping("/edit/{id}")
    public ModelAndView editEmployee(
            @ModelAttribute @Valid Employee employee,
            BindingResult result,
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirect) {

        if (!isAdmin(session)) return denyAccess(redirect);

        eValidator.validate(employee, result);

        if (result.hasErrors()) {
            ModelAndView mav = new ModelAndView(GlobalConstants.VIEW_ADMIN_EMPLOYEE_EDIT);
            mav.addObject("employee", employee);
            populateForm(mav);
            return mav;
        }

        employee.setEmp_id(id);
        employee.setUpdatedBy(getActor(session));
        employee.setUpdatedWhen(LocalDateTime.now());

        eService.changeEmployee(employee);
        log.info("Employee {} updated.", id);

        redirect.addFlashAttribute(GlobalConstants.FLASH_SUCCESS, "Employee updated successfully.");
        return new ModelAndView(GlobalConstants.REDIRECT_ADMIN_EMPLOYEE_LIST);
    }

    @GetMapping("/delete/{id}")
    public ModelAndView deleteEmployee(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirect) {

        if (!isAdmin(session)) return denyAccess(redirect);

        eService.findEmployee(id).ifPresent(emp -> {
            eService.removeEmployee(emp);
            log.info("Employee {} deleted.", id);
        });

        redirect.addFlashAttribute(GlobalConstants.FLASH_SUCCESS, "Employee deleted successfully.");
        return new ModelAndView(GlobalConstants.REDIRECT_ADMIN_EMPLOYEE_LIST);
    }
}