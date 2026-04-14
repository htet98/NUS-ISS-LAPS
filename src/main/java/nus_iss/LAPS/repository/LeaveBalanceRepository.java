package nus_iss.LAPS.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nus_iss.LAPS.model.LeaveBalance;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long>{

//	List<LeaveBalance> findByEmployeeId(Long employeeId);
	@Query("SELECT lb FROM LeaveBalance lb WHERE lb.employee.emp_id = :employeeId")
	List<LeaveBalance> findByEmployeeId(@Param("employeeId") Long employeeId);
	

}