package nus_iss.LAPS.service;

import lombok.RequiredArgsConstructor;
import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
        return employeeRepo.findEmployeeByIds(s);
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
        @Transactional
        public void updateEmployee(Long id, Employee form, String actor) {

            Employee existing = employeeRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Employee not found"));

            existing.setFirst_name(form.getFirst_name());
            existing.setLast_name(form.getLast_name());
            existing.setEmail(form.getEmail());
            existing.setPhoneNumber(form.getPhoneNumber());
            existing.setDepartment(form.getDepartment());
            existing.setDesignation(form.getDesignation());
            existing.setHire_date(form.getHire_date());
            existing.setEmployeeStatus(form.getEmployeeStatus());

            existing.setSupervisor(form.getSupervisor());
            existing.setUser(form.getUser());

            existing.setUpdatedBy(actor);
            existing.setUpdatedWhen(LocalDateTime.now());

            employeeRepo.save(existing);
        }

    }
