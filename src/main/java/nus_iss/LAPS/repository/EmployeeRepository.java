package nus_iss.LAPS.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.model.User;

/**
	* Author: Junior
 	* Created on: 13/04/2026
 	* Updated on: 15/04/2026
**/

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
	@Query("SELECT e FROM Employee e WHERE e.emp_id = :emp_id")
    Optional<Employee> findEmployeeById(@Param("emp_id") Long emp_id);

    @Query("SELECT e FROM Employee e WHERE e.supervisor.emp_id = :supid")
    List<Employee> findEmployeesBySupervisorId(@Param("supid") Long supid);

    @Query("SELECT DISTINCT e FROM Employee e " +
           "WHERE e.emp_id IN (" +
           "SELECT DISTINCT emp.supervisor.emp_id FROM Employee emp WHERE emp.supervisor IS NOT NULL)")
    List<Employee> findAllSupervisors();

    // Grace added CONCAT
    @Query("SELECT DISTINCT CONCAT(e.first_name, ' ', e.last_name) FROM Employee e " +
           "WHERE e.emp_id IN (" +
           "SELECT DISTINCT emp.supervisor.emp_id FROM Employee emp WHERE emp.supervisor IS NOT NULL)")
    List<Long> findAllSupervisorNames();

    @Query("SELECT e FROM Employee e WHERE e.supervisor.emp_id = :emp_id")
    List<Employee> findSubordinates(@Param("emp_id") Long emp_id);

    @Query("SELECT DISTINCT e.emp_id FROM Employee e")
    List<Long> findAllEmployeeIDs();

    Optional<Employee> findByUser(User user);
}
