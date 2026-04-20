package nus_iss.LAPS.service;

import lombok.extern.slf4j.Slf4j;
import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.model.LeaveBalance;
import nus_iss.LAPS.model.LeaveType;
import nus_iss.LAPS.repository.EmployeeRepository;
import nus_iss.LAPS.repository.LeaveBalanceRepository;
import nus_iss.LAPS.repository.LeaveTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Slf4j
@Service
public class LeaveTypeService {

   @Autowired
   private LeaveTypeRepository leaveTypeRepository;
   
   @Autowired
   private EmployeeRepository employeeRepository;
   
   @Autowired
   private LeaveBalanceRepository leaveBalanceRepository;


   public List<LeaveType> getAllLeaveTypes() {
       return leaveTypeRepository.findAll();
   }


   public Optional<LeaveType> getLeaveTypeById(Long leaveTypeId){
       return leaveTypeRepository.findById(leaveTypeId);
   }


   public void createLeaveType(LeaveType leaveType){
       // Save the new leave type to database
       leaveTypeRepository.save(leaveType);
       
       // Automatically create leave balance records for all existing employees
       // This ensures the leave summary displays on HTML pages immediately
       createLeaveBalancesForAllEmployees(leaveType);
   }
   
   /**
    * Creates LeaveBalance records for all existing employees for a newly created leave type.
    * Each balance starts at the default_days specified in the LeaveType.
    * This ensures that when employees view their leave summary, the new leave type appears automatically.
    */
   private void createLeaveBalancesForAllEmployees(LeaveType leaveType) {
       try {
           List<Employee> allEmployees = employeeRepository.findAll();
           
           // Get default days from the leave type (default to 0 if null)
           Double defaultDays = leaveType.getDefaultDays() != null ? leaveType.getDefaultDays() : 0.0;
           
           for (Employee emp : allEmployees) {
               // Create a new LeaveBalance with the leave type's default days
               LeaveBalance balance = new LeaveBalance();
               balance.setEmployee(emp);
               balance.setLeaveType(leaveType);
               balance.setTotalDays(defaultDays);
               balance.setUsedDays(0.0);
               
               leaveBalanceRepository.save(balance);
               log.info("Created leave balance for employee {} with leave type {} ({} days)", 
                       emp.getEmp_id(), leaveType.getName(), defaultDays);
           }
           
           log.info("Successfully created leave balances for {} employees for leave type {}", 
                   allEmployees.size(), leaveType.getName());
       } catch (Exception e) {
           // Log the error but don't throw — the leave type was already created.
           // The admin can manually create balance records if needed.
           log.error("Error creating leave balances for new leave type {}: {}", 
                   leaveType.getName(), e.getMessage());
       }
   }


   public void changeLeaveType(LeaveType leaveType){
       leaveTypeRepository.save(leaveType);
   }


   public void removeLeaveType(LeaveType leaveType){
       leaveTypeRepository.delete(leaveType);
   }

}
