package nus_iss.LAPS.api;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.service.EmployeeService;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor

/**
 * Author: Junior
 * Created on: 15/04/2026
 **/

public class EmployeeRestController {
			
		private final EmployeeService eService;
		
		//employee list
		@GetMapping
	    public List<Employee> getAllEmployees() {
	        return eService.findAllEmployees();
	    }
		
		//create Employee
		@PostMapping
	    public Employee createEmployee(@RequestBody Employee employee) {
	        return eService.createEmployee(employee);
	    }
		
		//update Employee
		@PutMapping("/{id}")
	    public Employee updateEmployee(@PathVariable Long id,
	                                   @RequestBody Employee employee) {
			Optional<Employee> existing = eService.findEmployee(id);
			
			if (existing.isPresent()) {
	            employee.setEmp_id(id);
	            return eService.changeEmployee(employee);
	        }

	        return null;
		}
		
		//delete Employee
		@DeleteMapping("/{id}")
	    public String deleteEmployee(@PathVariable Long id) {

	        Optional<Employee> emp = eService.findEmployee(id);

	        if (emp.isPresent()) {
	        	eService.removeEmployee(emp.get());
	            return "Employee deleted successfully";
	        }

	        return "Employee not found";
		}
}
