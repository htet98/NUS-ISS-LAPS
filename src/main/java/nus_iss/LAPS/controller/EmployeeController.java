package nus_iss.LAPS.controller;

import java.time.LocalDateTime;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
import nus_iss.LAPS.validators.EmployeeValidator;

@Controller
@RequestMapping("admin/employee")
@RequiredArgsConstructor
@Slf4j
public class EmployeeController {

	private final EmployeeService eService;
	private final EmployeeValidator eValidator;
	
	private boolean isAdmin(HttpSession session) {
        Role role = (Role) session.getAttribute("role");
        return Role.ADMIN.equals(role);
    }
	
	//getActor for createdBy   
    private String getActor(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null && user.getUsername() != null) {
            return user.getUsername();
        }
        return "system";
    }
      
    //Create
	@GetMapping("/create")
	public ModelAndView newEmployeePage(HttpSession session, 
										RedirectAttributes redirect) {
		
		
		var mav = new ModelAndView("employee-new", "employee", new Employee());
		mav.addObject("eidlist", eService.findAllEmployeeIDs());
		mav.addObject("supervisorList", eService.findAllSupervisors());
		return mav;
	}
	
	@PostMapping("/create")
	public ModelAndView createNewEmployee(@ModelAttribute 
											@Valid Employee employee,
											BindingResult result,
											HttpSession session,
											RedirectAttributes redirect) {
		
		if (!isAdmin(session)) {
            redirect.addFlashAttribute("errorMessage", "Access denied.");
            return new ModelAndView("redirect:/login");
        }
        
		if(result.hasErrors())
		{
			return new ModelAndView("employee-new");
		}
		
		employee.setCreatedBy(getActor(session));
        employee.setCreatedWhen(LocalDateTime.now());
        employee.setUpdatedBy(null);
        employee.setUpdatedWhen(null);
        
		log.info("New Employee {} was successfully created.", employee.getEmp_id());
		eService.createEmployee(employee);
		
		redirect.addFlashAttribute("successMessage", "Employee created successfully.");
		return new ModelAndView("forward:/admin/employee/list");
	}
	
	//List employees
    // check admin or not. If not, error redirect back to login.
    @RequestMapping("/list")
    public ModelAndView listEmployeePage(HttpSession session, 
    								Model model, RedirectAttributes redirect) {

    	
    	if (!isAdmin(session)) {
            redirect.addFlashAttribute("errorMessage", "Access denied.");
            return new ModelAndView("redirect:/login");
        }
    	
    	var mav = new ModelAndView("employee-list");
    	mav.addObject("employeeList", eService.findAllEmployees());
        return mav;
    }
	
	//edit
	@GetMapping("/edit/{id}")
	public ModelAndView editEmployeePage(@PathVariable Long id,
            								HttpSession session,
            								RedirectAttributes redirect) {
		
		if (!isAdmin(session)) {
            redirect.addFlashAttribute("errorMessage", "Access denied.");
            return new ModelAndView("redirect:/login");
        }
        
		var mav = new ModelAndView("employee-edit");
		eService.findEmployee(id).ifPresent(emp -> mav.addObject("employee", emp));
		mav.addObject("eidlist", eService.findAllEmployeeIDs());
		mav.addObject("supervisorList", eService.findAllSupervisors());
		return mav;
	}
	
	@PostMapping("/edit/{id}")
	public ModelAndView editEmployee(@ModelAttribute 
									@Valid Employee employee,
            						BindingResult result,
            						@PathVariable Long id,
            						HttpSession session,
            						RedirectAttributes redirect) {
		
		if (!isAdmin(session)) {
            redirect.addFlashAttribute("errorMessage", "Access denied.");
            return new ModelAndView("redirect:/login");
        }
	     
		if(result.hasErrors()) {
			return new ModelAndView("employee-edit");
		}
		
		employee.setEmp_id(id);
		employee.setCreatedBy(null);
		employee.setCreatedWhen(null);
	    employee.setUpdatedBy(getActor(session));
	    employee.setUpdatedWhen(LocalDateTime.now());
	     
		log.info("Employee {} was successfully updated.", employee.getEmp_id());
		eService.changeEmployee(employee);
		
		redirect.addFlashAttribute("successMessage", "Employee updated successfully.");
		
		return new ModelAndView("redirect:/admin/employee/list");
	}
	
	@GetMapping("/delete/{id}")
	public ModelAndView deleteEmployee(@PathVariable Long id,
            							HttpSession session,
            							RedirectAttributes redirect) {
		
		if (!isAdmin(session)) {
            redirect.addFlashAttribute("errorMessage", "Access denied.");
            return new ModelAndView("redirect:/login");
        }
	      
		eService.findEmployee(id).ifPresent(employee -> {
		eService.removeEmployee(employee);
			log.info("The employee {} was successfully deleted.", employee.getEmp_id());
			redirect.addFlashAttribute("successMessage", "Employee deleted successfully.");
		});
		return new ModelAndView("forward:/admin/employee/list");
	}
}