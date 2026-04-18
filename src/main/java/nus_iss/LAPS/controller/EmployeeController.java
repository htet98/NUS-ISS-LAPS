package nus_iss.LAPS.controller;

import java.time.LocalDateTime;
import java.util.List;

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
import lombok.extern.slf4j.Slf4j;
import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.model.Role;
import nus_iss.LAPS.model.User;
import nus_iss.LAPS.service.EmployeeService;
import nus_iss.LAPS.validators.EmployeeValidator;

@Controller
@RequestMapping("/employees")
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
		
		@InitBinder("employee")
		private void initEmployeeBinder(WebDataBinder binder) {
			binder.addValidators(eValidator);
		}
		
		//session
		private boolean isLoggedIn(HttpSession session) {
	        return session.getAttribute("user") != null;
	    }

	    private boolean isAdmin(HttpSession session) {
	        User user = (User) session.getAttribute("user");
	        return user != null && user.getRole() == Role.ADMIN;
	    }
	    
	    private String getActor(HttpSession session) {
	        User user = (User) session.getAttribute("user");
	        if (user != null && user.getUsername() != null) {
	            return user.getUsername();
	        }
	        return "system";
	    }
	    
	    //Test
	    @GetMapping
	    public String listEmployees(HttpSession session, Model model, RedirectAttributes ra) {
	        List<Employee> employees = eService.findAllEmployees();
	        System.out.println("Employee size = " + employees.size());
	        model.addAttribute("employeeList", employees);
	        return "employee-list";
	    }
	    
	    //List employees
	    // check admin or not. If not, error redirect back to login.
	    /*@GetMapping
	    public String listEmployees(HttpSession session, Model model, RedirectAttributes ra) {
	        if (!isLoggedIn(session)) {
	            return "redirect:/login";
	        }

	        if (!isAdmin(session)) {
	            ra.addFlashAttribute("errorMessage", "Only Admin can manage employee records.");
	            return "redirect:/";
	        }

	        model.addAttribute("employeeList", eService.findAllEmployees());
	        return "employee-list";
	    }*/

	    //CRUD
	    //Create
		@GetMapping("/create")
		public ModelAndView newEmployeePage(HttpSession session, 
											RedirectAttributes redirect) {
			if (!isLoggedIn(session)) {
	            return new ModelAndView("redirect:/login");
	        }

	        if (!isAdmin(session)) {
	            redirect.addFlashAttribute("errorMessage", "Only Admin can manage employee records.");
	            return new ModelAndView("redirect:/login");
	        }
	        
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
			if (!isLoggedIn(session)) {
	            return new ModelAndView("redirect:/login");
	        }

	        if (!isAdmin(session)) {
	            redirect.addFlashAttribute("errorMessage", "Only Admin can manage employee records.");
	            return new ModelAndView("redirect:/login");
	        }
	        
			if(result.hasErrors())
			{
				return new ModelAndView("employee-new");
			}
			
			employee.setCreatedBy(getActor(session));
	        employee.setCreatedWhen(LocalDateTime.now());
	        
			log.info("New Employee {} was successfully created.", employee.getEmp_id());
			eService.createEmployee(employee);
			
			redirect.addFlashAttribute("successMessage", "Employee created successfully.");
			return new ModelAndView("redirect:/employees");
		}
		
		//edit
		@GetMapping("/edit/{id}")
		public ModelAndView editEmployeePage(@PathVariable Long id,
	            								HttpSession session,
	            								RedirectAttributes redirect) {
			if (!isLoggedIn(session)) {
	            return new ModelAndView("redirect:/login");
	        }

	        if (!isAdmin(session)) {
	            redirect.addFlashAttribute("errorMessage", "Only Admin can manage employee records.");
	            return new ModelAndView("redirect:/login");
	        }
	        
			var mav = new ModelAndView("employee-edit");
			eService.findEmployee(id).ifPresent(emp -> mav.addObject("employee", emp));
			mav.addObject("eidlist", eService.findAllEmployeeIDs());
			mav.addObject("supervisorList", eService.findAllSupervisors());
			return mav;
		}
		
		@PostMapping("/edit/{id}")
		public ModelAndView editEmployee(@ModelAttribute("employee") 
										@Valid Employee employee,
	            						BindingResult result,
	            						@PathVariable Long id,
	            						HttpSession session,
	            						RedirectAttributes redirect) {
			
			 if (!isLoggedIn(session)) {
		           return new ModelAndView("redirect:/login");
		        }

		     if (!isAdmin(session)) {
		           redirect.addFlashAttribute("errorMessage", "Only Admin can manage employee records.");
		           return new ModelAndView("redirect:/login");
		        }
		     
			if(result.hasErrors()) {
				return new ModelAndView("employee-edit");
			}
			
			employee.setEmp_id(id);
		    employee.setUpdatedBy(getActor(session));
		    employee.setUpdatedWhen(LocalDateTime.now());
		     
			log.info("Employee {} was successfully updated.", employee.getEmp_id());
			eService.changeEmployee(employee);
			
			redirect.addFlashAttribute("successMessage", "Employee updated successfully.");
			
			return new ModelAndView("redirect:/employees");
		}
		
		@GetMapping("/delete/{id}")
		public ModelAndView deleteEmployee(@PathVariable Long id,
	            							HttpSession session,
	            							RedirectAttributes redirect) {
			
			  if (!isLoggedIn(session)) {
		            return new ModelAndView("redirect:/login");
		      }

		      if (!isAdmin(session)) {
		            redirect.addFlashAttribute("errorMessage", "Only Admin can manage employee records.");
		            return new ModelAndView("redirect:/login");
		      }
		      
			eService.findEmployee(id).ifPresent(employee -> {
			eService.removeEmployee(employee);
				log.info("The employee {} was successfully deleted.", employee.getEmp_id());
				redirect.addFlashAttribute("successMessage", "Employee deleted successfully.");
			});
			return new ModelAndView("redirect:/employees");
		}
	}
