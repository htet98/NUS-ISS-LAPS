package nus_iss.LAPS.repository;

import nus_iss.LAPS.model.LeaveBalance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

	@Query("SELECT lb FROM LeaveBalance lb WHERE lb.employee.emp_id = :employeeId")
	List<LeaveBalance> findByEmployeeId(@Param("employeeId") Long employeeId);

	// Htet Nandar(Grace) - 12/04/2026
	// Used by LeaveBalanceService.getAvailableBalance / deductBalance /
	// restoreBalance
	@Query("SELECT lb FROM LeaveBalance lb " + "WHERE lb.employee.emp_id = :empId "
			+ "AND lb.leaveType.leaveTypeId = :leaveTypeId")
	Optional<LeaveBalance> findByEmployeeIdAndLeaveTypeId(@Param("empId") Long empId,
			@Param("leaveTypeId") Long leaveTypeId);

	@Query("""
			    SELECT COUNT(lb) > 0
			    FROM LeaveBalance lb
			    WHERE lb.employee.emp_id = :employeeId
			    AND lb.leaveType.leaveTypeId = :leaveTypeId
			""")
	boolean existsByEmployeeAndLeaveType(@Param("employeeId") Long employeeId,
			@Param("leaveTypeId") Long leaveTypeId);
	
	Page<LeaveBalance> findAll(Pageable pageable);
	
	@Query("""
		    SELECT lb FROM LeaveBalance lb
		    WHERE LOWER(CONCAT(lb.employee.first_name, ' ', lb.employee.last_name))
		          LIKE LOWER(CONCAT('%', :keyword, '%'))
		""")
		Page<LeaveBalance> searchByFullName(String keyword, Pageable pageable);
}
