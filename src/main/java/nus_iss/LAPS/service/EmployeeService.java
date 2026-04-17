package nus_iss.LAPS.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.model.Role;
import nus_iss.LAPS.model.User;
import nus_iss.LAPS.repository.EmployeeRepository;
import nus_iss.LAPS.repository.UserRepository;

@Service
public class EmployeeService 
{

    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Autowired
    private UserRepository userRepository;
    

    public List<Employee> getAllEmployees() 
    {
        return employeeRepository.findAll();
    }
    
    /*
    public void deleteEmployee(Long id) 
    {
        employeeRepository.deleteById(id);
    } */
    
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
