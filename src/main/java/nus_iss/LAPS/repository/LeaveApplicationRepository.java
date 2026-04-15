package nus_iss.LAPS.repository;

import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.model.LeaveApplication;
import nus_iss.LAPS.model.LeaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for managing LeaveApplication entities.
 *
 * Author: Htet Nandar(Grace)
 * Created on: 12/04/2026
 */
public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long> {

    Page<LeaveApplication> findLeaveApplicationsByEmployee(Employee employee, Pageable pageable);

    // ── Employee: personal history for current year (excludes DELETED) ───────
    @Query("SELECT la FROM LeaveApplication la " +
            "WHERE la.employee = :employee " +
            "AND YEAR(la.startDate) = :year " +
            "AND la.status != 'DELETED' " +
            "ORDER BY la.startDate DESC")
    List<LeaveApplication> findLeaveApplicationsByEmployeeAndYear(
            @Param("employee") Employee employee,
            @Param("year") int year);

    // ── Employee: personal history with optional status + type filter ─────────
    @Query("SELECT la FROM LeaveApplication la " +
            "WHERE la.employee = :employee " +
            "AND YEAR(la.startDate) = :year " +
            "AND (:status IS NULL OR la.status = :status) " +
            "AND (:leaveTypeId IS NULL OR la.leaveType.leaveTypeId = :leaveTypeId) " +
            "AND la.status != 'DELETED'")
    Page<LeaveApplication> findByEmployeeYearStatusAndType(
            @Param("employee") Employee employee,
            @Param("year") int year,
            @Param("status") LeaveStatus status,
            @Param("leaveTypeId") Long leaveTypeId,
            Pageable pageable);

    // ── Manager: pending applications (APPLIED or UPDATED) from subordinates ──
    @Query("SELECT la FROM LeaveApplication la " +
            "WHERE la.employee.supervisor = :manager " +
            "AND la.status IN ('APPLIED', 'UPDATED')")
    Page<LeaveApplication> findPendingLeaveApplicationsByManager(
            @Param("manager") Employee manager,
            Pageable pageable);

    // ── Manager: all applications from direct subordinates ────────────────────
    @Query("SELECT la FROM LeaveApplication la " +
            "WHERE la.employee.supervisor = :manager " +
            "AND la.status != 'DELETED'")
    Page<LeaveApplication> findAllByManager(@Param("manager") Employee manager, Pageable pageable);

    // ── Medical leave: total approved days in a calendar year ─────────────────
    @Query("SELECT COALESCE(SUM(la.durationDays), 0) FROM LeaveApplication la " +
            "WHERE la.employee = :employee " +
            "AND la.leaveType.name = 'MEDICAL' " +
            "AND la.status = 'APPROVED' " +
            "AND YEAR(la.startDate) = :year")
    double sumApprovedMedicalLeaveByEmployeeAndYear(
            @Param("employee") Employee employee,
            @Param("year") int year);

    // ── Manager: overlapping approved leave from same team ────────────────────
    @Query("SELECT la FROM LeaveApplication la " +
            "WHERE la.employee.supervisor = :manager " +
            "AND la.employee != :excludeEmployee " +
            "AND la.status = 'APPROVED' " +
            "AND la.startDate <= :endDate " +
            "AND la.endDate >= :startDate " +
            "ORDER BY la.startDate ASC")
    List<LeaveApplication> findTeamOverlapInRange(
            @Param("manager") Employee manager,
            @Param("excludeEmployee") Employee excludeEmployee,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // ── Manager: recent approved/rejected decisions (last 10) ─────────────────
    @Query("SELECT la FROM LeaveApplication la " +
            "WHERE la.approvedBy = :manager " +
            "AND la.status IN ('APPROVED', 'REJECTED') " +
            "ORDER BY la.updatedWhen DESC " +
            "LIMIT 10")
    List<LeaveApplication> findRecentDecisionsByManager(@Param("manager") Employee manager);
}
