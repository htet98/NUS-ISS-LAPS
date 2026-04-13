package nus_iss.LAPS.repository;

import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.model.LeaveApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for managing LeaveApplication entities.
 *
 * @extends JpaRepository for basic CRUD operations on LeaveApplication entities
 * Author: Htet Nandar(Grace)
 * Created on: 12/04/2026
 */
public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long> {

    /**
     * @param leaveApplicationId
     * @return
     */
    List<LeaveApplication> findLeaveApplicationsByLeaveApplicationId(Long leaveApplicationId);


    /**
     * @param empId
     * @return
     */
    List<LeaveApplication> findLeaveApplicationsByEmployeeEmpId(String empId);

    List<LeaveApplication> findAll();


    List<LeaveApplication> findLeaveApplicationsByEmployeeEmpIdAndLeaveTypeLeaveTypeId(String empId, Long leaveTypeId);


    List<LeaveApplication> findLeaveApplicationsByLeaveTypeId(Long leaveTypeId);

    // ── Employee: personal history for current year ───────────────────────

    @Query("SELECT la FROM LeaveApplication la "+
            "WHERE la.employee = :employee AND YEAR(la.startDate) = :year "+
            "ORDER BY la.startDate DESC")
    List<LeaveApplication> findLeaveApplicationsByEmployeeAndYear(@Param("employee") Employee employee, @Param("year") int year);

    // ── Manager: pending applications from direct subordinates ────────────

    @Query("SELECT la FROM LeaveApplication la "+
            "WHERE la.employee.supervisor = :manager  "+
            "AND la.status  = 'PENDING' "+
            "ORDER BY la.createdWhen  ASC")
    List<LeaveApplication> findPendingLeaveApplicationsByManager(@Param("manager") Employee manager);

    // ── Manager: all applications from direct subordinates ────────────────

    @Query("SELECT la FROM LeaveApplication la " +
            "WHERE la.employee.supervisor = :manager "+
            "ORDER BY la.employee.last_name, la.startDate DESC")
    List<LeaveApplication> findAllByManager(@Param("manager") Employee manager);

    /**
     * @param employee
     * @param year
     * @return
     */
    // ── Medical leave: total approved days in a calendar year ─────────────
    @Query("SELECT COALESCE(SUM(la.durationDays), 0) FROM LeaveApplication la "+
            "WHERE la.employee = :employee " +
            "AND la.leaveType.name = 'Medical Leave' " +
            "AND la.status = 'APPROVED' " +
            "AND YEAR(la.startDate) = :year")
    double sumApprovedMedicalLeaveByEmployeeAndYear(@Param("employee") Employee employee, @Param("year") int year);

    // ── Overlapping applications check (for movement register) ────────────
    @Query("SELECT  la FROM LeaveApplication la "+
            "WHERE la.status = 'APPROVED' "+
            "AND la.startDate <= :endDate "+
            "AND la.endDate >= :startDate")
    List<LeaveApplication> findApprovedInRange(@Param("startDate")LocalDate startDate, @Param("endDate") LocalDate endDate);

}
