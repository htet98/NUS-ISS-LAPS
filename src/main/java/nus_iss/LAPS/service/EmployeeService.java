package nus_iss.LAPS.service;


import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.repository.EmployeeRepository;


// Cecil added code
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import nus_iss.LAPS.model.Role;
import nus_iss.LAPS.model.User;
import nus_iss.LAPS.repository.UserRepository;


@Service
public class EmployeeService {
	/**
	 * Author: Junior
	 * Created on: 14/04/2026
	 **/
	@Autowired
	private EmployeeRepository employeeRepo;
	
	//Cecil added code
	 @Autowired
	 private EmployeeRepository employeeRepository;
	 @Autowired
	 private UserRepository userRepository;
	

    @Transactional(readOnly = true)
    public List<Employee> findEmployeesByManager(Long s) {
        return employeeRepo.findEmployeesBySupervisorId(s);
    }

    @Transactional(readOnly = true)
    public Optional<Employee> findEmployeeById(Long s) {
        return employeeRepo.findEmployeeById(s);
    }

    @Transactional(readOnly = true)
    public List<Employee> findAllEmployees() {
        return employeeRepo.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Employee> findEmployee(Long id) {
        return employeeRepo.findById(id);
    }

    public Employee createEmployee(Employee emp) {
        return employeeRepo.save(emp);
    }

    public Employee changeEmployee(Employee emp) {
        return employeeRepo.save(emp);
    }

    public void removeEmployee(Employee emp) {
    	employeeRepo.delete(emp);
    }

    @Transactional(readOnly = true)
    public List<Employee> findAllSupervisors() {
        return employeeRepo.findAllSupervisors();
    }

    @Transactional(readOnly = true)
    public List<Employee> findSubordinates(Long empId) {
        return employeeRepo.findSubordinates(empId);
    }

    @Transactional(readOnly = true)
    public List<Long> findAllEmployeeIDs() {
        return employeeRepo.findAllEmployeeIDs();
    }


    // Cecil added code
	public List<Employee> getAllEmployees() 
	{
    	return employeeRepository.findAll();
	}

	public void deleteEmployee(Long id) 
	{

		Employee emp = employeeRepository.findById(id).orElse(null);

		if (emp != null) {
			User user = emp.getUser();

			employeeRepository.delete(emp);   // delete employee

			if (user != null) {
            userRepository.delete(user); // delete linked user
			}
		}
	}

	public Employee findById(Long id) 
	{
		return employeeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Employee not found"));
	}

	public void createEmployeeWithUser(Employee emp, String username, String password, String role) 
	{
		// 1. Create User
		User user = new User();
		user.setUsername(username);
		user.setEmail(emp.getEmail());
		user.setPassword(password); // later you can encrypt
		user.setRole(Role.valueOf(role));
		user.setCreatedby("admin");
		user.setUpdatedby("admin");

		userRepository.save(user);

		// 2. Link Employee to User
		emp.setUser(user);
    
		emp.setCreatedBy("admin");
		emp.setCreatedWhen(LocalDateTime.now());
		emp.setUpdatedBy("admin");
		emp.setUpdatedWhen(LocalDateTime.now());

		// 3. Save Employee
		employeeRepository.save(emp);
   
	}
	
	public void updateEmployee(Employee updatedEmployee, String username, String password, String role) 
    {
        Employee existing = employeeRepository.findById(updatedEmployee.getEmp_id())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // update fields manually
        existing.setFirst_name(updatedEmployee.getFirst_name());
        existing.setLast_name(updatedEmployee.getLast_name());
        existing.setEmail(updatedEmployee.getEmail());
        existing.setPhoneNumber(updatedEmployee.getPhoneNumber());
        existing.setDepartment(updatedEmployee.getDepartment());
        existing.setDesignation(updatedEmployee.getDesignation());
        existing.setHire_date(updatedEmployee.getHire_date());
        existing.setEmployeeStatus(updatedEmployee.getEmployeeStatus());
        existing.setSupervisor(updatedEmployee.getSupervisor());

        // update user info
        User user = existing.getUser();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(Role.valueOf(role));

        employeeRepository.save(existing);
    }

}
