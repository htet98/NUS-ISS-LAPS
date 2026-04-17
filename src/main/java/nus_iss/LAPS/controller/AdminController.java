package nus_iss.LAPS.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;

import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.model.PublicHoliday;
import nus_iss.LAPS.service.EmployeeService;
import nus_iss.LAPS.service.PublicHolidayService;

@Controller
@RequestMapping("/admin")
public class AdminController 
{

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private PublicHolidayService holidayService;

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) 
    {
        model.addAttribute("employees", employeeService.getAllEmployees());
        model.addAttribute("holidays", holidayService.getAllHolidays());

        return "admin-main"; 
        
    }
    
    @GetMapping("/holiday/add")
    public String showAddHolidayForm(Model model) 
    {
        model.addAttribute("holiday", new PublicHoliday());
        return "admin-add-holiday";
        
    }
    
    @PostMapping("/holiday/save")
    public String saveHoliday(@ModelAttribute("holiday") PublicHoliday holiday) 
    {
        holidayService.saveHoliday(holiday);

        return "redirect:/admin/dashboard";
    }
    
    @GetMapping("/holiday/edit/{id}")
    public String showEditHolidayForm(@PathVariable Long id, Model model) 
    {
        PublicHoliday holiday = holidayService.getHolidayById(id);
        model.addAttribute("holiday", holiday);

        return "admin-add-holiday";  // I am using the same adding page
    }
    
    @GetMapping("/holiday/delete/{id}")
    public String deleteHoliday(@PathVariable Long id) 
    {

        holidayService.deleteHoliday(id);

        return "redirect:/admin/dashboard";
    }
    
    
    @GetMapping("/employee/delete/{id}")
    public String deleteEmployee(@PathVariable Long id) 
    {
        employeeService.deleteEmployee(id);

        return "redirect:/admin/dashboard";
    }
    
    @GetMapping("/employee/new")
    public String showAddEmployeeForm(Model model) 
    {
        model.addAttribute("employee", new Employee());

        return "admin-add-employee";
    }
    
    /*
    @PostMapping("/employee/save")
    public String saveEmployee(
    		@Valid @ModelAttribute("employee") Employee employee,
    		BindingResult result,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String role) 
    {
    	
    	if (result.hasErrors()) 
    	{
            return "admin-add-employee"; // go back to the previous page
        }

        employeeService.createEmployeeWithUser(employee, username, password, role);

        return "redirect:/admin/dashboard";
        
    } */
    
    @PostMapping("/employee/save")
    public String saveEmployee(
            @Valid @ModelAttribute("employee") Employee employee,
            BindingResult result,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String role,
            @RequestParam(required = false) Long supervisorId,
            Model model) {

        if (result.hasErrors()) {
            return "admin-add-employee";
        }

        if (supervisorId != null) {
            Employee supervisor = employeeService.findById(supervisorId);
            employee.setSupervisor(supervisor);
        }

        // 🔥 IMPORTANT: differentiate create vs update
        if (employee.getEmp_id() != null) {
            employeeService.updateEmployee(employee, username, password, role);
        } else {
            employeeService.createEmployeeWithUser(employee, username, password, role);
        }

        return "redirect:/admin/dashboard";
    } 
    
    @GetMapping("/employee/edit/{id}")
    public String editEmployee(@PathVariable Long id, Model model) 
    {
        Employee employee = employeeService.findById(id);

        model.addAttribute("employee", employee);

        return "admin-add-employee"; // reuse same page
    }
    
    
}