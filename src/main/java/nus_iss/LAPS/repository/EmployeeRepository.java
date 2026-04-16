package nus_iss.LAPS.repository;

import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
	* Author: Junior
 	* Created on: 13/04/2026
**/

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
	@Query("SELECT e FROM Employee e WHERE e.emp_id = :emp_id")
	Optional<Employee> findEmployeeById(@Param("emp_id") Long emp_id);
	
	@Query("SELECT e FROM Employee e WHERE e.supervisor.emp_id = :supid")
	List<Employee> findEmployeesBySupervisorId(@Param("supid") Long supid);
	
	@Query("SELECT DISTINCT e FROM Employee e " +
            "WHERE e.emp_id IN (SELECT DISTINCT emp.supervisor.emp_id FROM Employee emp WHERE emp.supervisor IS NOT NULL)")
	List<Employee> findAllSupervisor();

    // Htet Nandar (Grace) - 14/04/2026
	// Bug fix: Employee has no 'name' field. Concatenate first_name + last_name instead.
	@Query("SELECT DISTINCT CONCAT(e.first_name, ' ', e.last_name) FROM Employee e " +
		   "WHERE e.emp_id IN (SELECT DISTINCT emp.supervisor.emp_id FROM Employee emp WHERE emp.supervisor IS NOT NULL)")
	List<String> findAllSupervisorNames();

<<<<<<< HEAD
    @Query("SELECT DISTINCT e FROM Employee e " +
           "WHERE e.emp_id IN (" +
           "SELECT DISTINCT emp.supervisor.emp_id FROM Employee emp WHERE emp.supervisor IS NOT NULL)")
    List<Employee> findAllSupervisors();
    
    @Query("SELECT e FROM Employee e LEFT JOIN FETCH e.supervisor")
    List<Employee> findAllWithSupervisor();

    @Query("SELECT DISTINCT CONCAT(e.first_name, ' ', e.last_name) FROM Employee e " +
           "WHERE e.emp_id IN (" +
           "SELECT DISTINCT emp.supervisor.emp_id FROM Employee emp WHERE emp.supervisor IS NOT NULL)")
    List<Long> findAllSupervisorNames();

    @Query("SELECT e FROM Employee e WHERE e.supervisor.emp_id = :emp_id")
    List<Employee> findSubordinates(@Param("emp_id") Long emp_id);

    @Query("SELECT DISTINCT e.emp_id FROM Employee e")
    List<Long> findAllEmployeeIDs();

    Optional<Employee> findByUser(User user);
=======
    // Htet Nandar (Grace) - 14/04/2026
	// Bug fix: 'supervisor_id' is a DB column, not an entity path. Use supervisor.emp_id.
	@Query("SELECT e FROM Employee e WHERE e.supervisor.emp_id = :emp_id")
	List<Employee> findSubordinates(@Param("emp_id") Long emp_id);
	
	@Query("SELECT DISTINCT e.emp_id FROM Employee e")
	List<String> findAllEmployeeIDs();
	
	Optional<Employee> findByUser(User user);
>>>>>>> parent of 6ebfb44 (Added RestController, Controller and Service. Updated Model with LOMBOK, ENUM changed.)
}
