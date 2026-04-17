package nus_iss.LAPS.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.repository.EmployeeRepository;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepo;

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
    public List<Long> findAllEmployeeIDs() {
        return employeeRepo.findAllEmployeeIDs();
    }

    @Transactional(readOnly = true)
    public List<Employee> findEmployeesByManager(Long id) {
        return employeeRepo.findEmployeesBySupervisorId(id);
    }
}