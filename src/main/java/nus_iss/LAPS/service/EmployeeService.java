package nus_iss.LAPS.service;

import lombok.RequiredArgsConstructor;
import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmployeeService {
	
	/**
	 * Author: Junior
	 * Created on: 14/04/2026
	 **/
	
	private final EmployeeRepository employeeRepo;

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
        return employeeRepo.findAllSupervisor();
    }

    @Transactional(readOnly = true)
    public List<Employee> findSubordinates(Long empId) {
        return employeeRepo.findSubordinates(empId);
    }

    @Transactional(readOnly = true)
    public List<Long> findAllEmployeeIDs() {
        return employeeRepo.findAllEmployeeIDs();
    }
}
