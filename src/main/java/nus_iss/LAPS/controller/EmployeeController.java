package nus_iss.LAPS.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.model.Role;
import nus_iss.LAPS.model.User;
import nus_iss.LAPS.service.EmployeeService;
import nus_iss.LAPS.service.UserService;
import nus_iss.LAPS.util.GlobalConstants;
import nus_iss.LAPS.validators.EmployeeValidator;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.beans.PropertyEditorSupport;
import java.time.LocalDateTime;

@Controller
@RequestMapping(GlobalConstants.ROUTE_ADMIN + GlobalConstants.ROUTE_ADMIN_EMPLOYEE)
@RequiredArgsConstructor
@Slf4j
public class EmployeeController {

		/**
		 * Author: Junior
		 * Created on: 14/04/2026
		 * Updated on 15/04/2026
		 **/

	private final EmployeeService eService;
	private final EmployeeValidator eValidator;
	private final UserService uService;

		@InitBinder("employee")
		private void initEmployeeBinder(WebDataBinder binder) {
			binder.addValidators(eValidator);
			
			// Custom converter for userId (from form) to User object
			binder.registerCustomEditor(User.class, "user", new PropertyEditorSupport() {
				@Override
				public void setAsText(String text) throws IllegalArgumentException {
					if (text == null || text.trim().isEmpty()) {
						setValue(null);
					} else {
						try {
							Long userId = Long.parseLong(text);
							User user = uService.findById(userId).orElse(null);
							setValue(user);
						} catch (Exception e) {
							setValue(null);
						}
					}
				}
			});
		}

		//session
		private boolean isLoggedIn(HttpSession session) {
	        return session.getAttribute("user") != null;
	    }

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
            // For edit form: show all users (so user can see current selection)
            // For new form: show unassigned users only (handled in views)
            mav.addObject("userList", uService.getAllUsers());
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

        if (result.hasErrors()) {
            ModelAndView mav = new ModelAndView(GlobalConstants.VIEW_ADMIN_EMPLOYEE_NEW);
            mav.addObject("employee", employee);
            populateForm(mav);
            return mav;
        }

        try {
            employee.setCreatedBy(getActor(session));
            employee.setCreatedWhen(LocalDateTime.now());

            eService.createEmployee(employee);

            log.info("Employee {} created.", employee.getEmp_id());
            redirect.addFlashAttribute(GlobalConstants.FLASH_SUCCESS, "Employee created successfully.");
            return new ModelAndView(GlobalConstants.REDIRECT_ADMIN_EMPLOYEE_LIST);

        } catch (Exception ex) {

            log.error("Error creating employee", ex);

            ModelAndView mav = new ModelAndView(GlobalConstants.VIEW_ADMIN_EMPLOYEE_NEW);

            result.rejectValue("user", "user.duplicate", "Selected user is already assigned to another employee");

            mav.addObject("employee", employee);
            populateForm(mav);
            return mav;
        }
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
            @ModelAttribute @Valid Employee formEmployee,
            BindingResult result,
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirect) {

        if (!isAdmin(session)) return denyAccess(redirect);

        if (result.hasErrors()) {
            ModelAndView mav = new ModelAndView(GlobalConstants.VIEW_ADMIN_EMPLOYEE_EDIT);
            mav.addObject("employee", formEmployee);
            populateForm(mav);
            return mav;
        }

        try {
            eService.updateEmployee(id, formEmployee, getActor(session));

            redirect.addFlashAttribute(GlobalConstants.FLASH_SUCCESS,
                    "Employee updated successfully.");

            return new ModelAndView(GlobalConstants.REDIRECT_ADMIN_EMPLOYEE_LIST);

        } catch (Exception ex) {

            log.error("Error updating employee {}", id, ex);

            ModelAndView mav = new ModelAndView(GlobalConstants.VIEW_ADMIN_EMPLOYEE_EDIT);
            mav.addObject("employee", formEmployee);
            populateForm(mav);

            result.reject("update.failed", ex.getMessage());

            return mav;
        }
    }


    @GetMapping("/delete/{id}")
        public ModelAndView deleteEmployee(
                @PathVariable Long id,
                HttpSession session,
                RedirectAttributes redirect) {

            if (!isAdmin(session)) return denyAccess(redirect);

            try {
                eService.findEmployee(id).ifPresent(emp -> {
                    eService.removeEmployee(emp);
                    log.info("Employee {} deleted.", id);
                });
                redirect.addFlashAttribute(GlobalConstants.FLASH_SUCCESS, "Employee deleted successfully.");
            } catch (Exception ex) {
                log.error("Error deleting employee {}", id, ex);
                
                // Check if it's a foreign key constraint error
                if (ex.getMessage() != null && ex.getMessage().contains("foreign key constraint")) {
                    redirect.addFlashAttribute(GlobalConstants.FLASH_ERROR, 
                            "Cannot delete employee. This employee is referenced by other records.");
                } else {
                    redirect.addFlashAttribute(GlobalConstants.FLASH_ERROR, 
                            "Cannot delete employee.");
                }
            }
            
            return new ModelAndView(GlobalConstants.REDIRECT_ADMIN_EMPLOYEE_LIST);
        }
	}
