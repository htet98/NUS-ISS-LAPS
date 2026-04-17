package nus_iss.LAPS.service;

import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.model.LeaveApplication;
import nus_iss.LAPS.model.LeaveStatus;
import nus_iss.LAPS.repository.LeaveApplicationRepository;
import nus_iss.LAPS.validators.LeaveApplicationValidator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class LeaveApplicationService {

    @Autowired
    private LeaveApplicationRepository leaveApplicationRepo;

    @Autowired
    private LeaveBalanceService leaveBalanceService;

    @Autowired
    private LeaveApplicationValidator leaveValidator;

    @Autowired
    private EmailService emailService;

    // ─────────────────────────────────────────────────────────────────────────
    // SUBMIT  (was submitLeave — renamed to match REST controller call)
    // ─────────────────────────────────────────────────────────────────────────

    public LeaveApplication submitLeaveApplication(LeaveApplication application) {
        runValidator(application);
        application.setDurationDays(leaveValidator.computeDuration(application));
        
        // If employee has no supervisor (e.g. top manager), auto-approve
        if (application.getEmployee().getSupervisor() == null) {
            application.setStatus(LeaveStatus.APPROVED);
            // Self-approved or system approved? Usually we just set it to APPROVED
            // and leaveBalanceService.deductBalance(application); should be called
            leaveBalanceService.deductBalance(application);
        } else {
            application.setStatus(LeaveStatus.APPLIED);
        }

        String actor = resolveActor(application.getEmployee());
        application.setCreatedBy(actor);
        application.setUpdatedBy(actor);
        return leaveApplicationRepo.save(application);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE  (was updateLeave — renamed to match REST controller call)
    // ─────────────────────────────────────────────────────────────────────────

    public LeaveApplication updateLeaveApplication(LeaveApplication updated) {
        LeaveApplication existing = leaveApplicationRepo
                .findById(updated.getLeaveApplicationId())
                .orElseThrow(() -> new IllegalArgumentException("Leave application not found."));

        if (existing.getStatus() != LeaveStatus.APPLIED
                && existing.getStatus() != LeaveStatus.UPDATED) {
            throw new IllegalStateException(
                    "Only APPLIED or UPDATED applications can be modified. Current status: "
                    + existing.getStatus());
        }

        // Bug fix: was getEmpId() — correct getter is getEmp_id()
        if (!existing.getEmployee().getEmp_id().equals(updated.getEmployee().getEmp_id())) {
            throw new SecurityException("You are not authorised to update this application.");
        }

        runValidator(updated);

        existing.setLeaveType(updated.getLeaveType());
        existing.setStartDate(updated.getStartDate());
        existing.setEndDate(updated.getEndDate());
        existing.setReason(updated.getReason());
        existing.setWorkDissemination(updated.getWorkDissemination());
        existing.setOverseas(updated.getIsOverseas());
        existing.setContactDetails(updated.getContactDetails());
        existing.setIsHalfDay(updated.getIsHalfDay());
        // Only persist halfDayPeriod when isHalfDay is actually checked
        existing.setHalfDayPeriod(Boolean.TRUE.equals(updated.getIsHalfDay()) ? updated.getHalfDayPeriod() : null);
        existing.setDurationDays(leaveValidator.computeDuration(updated));
        existing.setStatus(LeaveStatus.UPDATED);
        existing.setUpdatedBy(resolveActor(updated.getEmployee()));
        return leaveApplicationRepo.save(existing);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE (soft)
    // ─────────────────────────────────────────────────────────────────────────

    public void deleteLeave(Long applicationId, Employee requestingEmployee) {
        LeaveApplication existing = leaveApplicationRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Leave application not found."));

        if (existing.getStatus() != LeaveStatus.APPLIED
                && existing.getStatus() != LeaveStatus.UPDATED) {
            throw new IllegalStateException(
                    "Only APPLIED or UPDATED applications can be deleted. Current status: "
                    + existing.getStatus());
        }

        if (!existing.getEmployee().getEmp_id().equals(requestingEmployee.getEmp_id())) {
            throw new SecurityException("You are not authorised to delete this application.");
        }

        existing.setStatus(LeaveStatus.DELETED);
        existing.setUpdatedBy(resolveActor(requestingEmployee));
        leaveApplicationRepo.save(existing);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CANCEL (APPROVED → CANCELLED)
    // ─────────────────────────────────────────────────────────────────────────

    public LeaveApplication cancelLeave(Long applicationId, Employee requestingEmployee) {
        LeaveApplication existing = leaveApplicationRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Leave application not found."));

        if (existing.getStatus() != LeaveStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only APPROVED applications can be cancelled. Current status: "
                    + existing.getStatus());
        }

        if (!existing.getEmployee().getEmp_id().equals(requestingEmployee.getEmp_id())) {
            throw new SecurityException("You are not authorised to cancel this application.");
        }

        leaveBalanceService.restoreBalance(existing);   // restore balance on cancellation
        existing.setStatus(LeaveStatus.CANCELLED);
        existing.setUpdatedBy(resolveActor(requestingEmployee));
        return leaveApplicationRepo.save(existing);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // APPROVE
    // ─────────────────────────────────────────────────────────────────────────

    public LeaveApplication approveLeave(Long applicationId, Employee manager) {
        LeaveApplication existing = leaveApplicationRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Leave application not found."));

        if (existing.getStatus() != LeaveStatus.APPLIED
                && existing.getStatus() != LeaveStatus.UPDATED) {
            throw new IllegalStateException("Only APPLIED or UPDATED applications can be approved.");
        }

        leaveBalanceService.deductBalance(existing);    // deduct balance on approval
        existing.setStatus(LeaveStatus.APPROVED);
        existing.setApprovedBy(manager);
        existing.setUpdatedBy(resolveActor(manager));
        LeaveApplication approved = leaveApplicationRepo.save(existing);
        emailService.sendApprovalNotification(approved);   // notify employee
        return approved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REJECT
    // ─────────────────────────────────────────────────────────────────────────

    public LeaveApplication rejectLeave(Long applicationId, Employee manager, String comment) {
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("A rejection comment is mandatory.");
        }

        LeaveApplication existing = leaveApplicationRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Leave application not found."));

        if (existing.getStatus() != LeaveStatus.APPLIED
                && existing.getStatus() != LeaveStatus.UPDATED) {
            throw new IllegalStateException("Only APPLIED or UPDATED applications can be rejected.");
        }

        existing.setStatus(LeaveStatus.REJECTED);
        existing.setApprovedBy(manager);
        existing.setManagerComment(comment);
        existing.setUpdatedBy(resolveActor(manager));
        LeaveApplication rejected = leaveApplicationRepo.save(existing);
        emailService.sendRejectionNotification(rejected);  // notify employee
        return rejected;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FIND
    // ─────────────────────────────────────────────────────────────────────────

    public Optional<LeaveApplication> findById(Long id) {
        return leaveApplicationRepo.findById(id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VIEW helpers — EMPLOYEE
    // ─────────────────────────────────────────────────────────────────────────

    /** All leave for current calendar year (excludes DELETED). */
    public List<LeaveApplication> getPersonalLeaveHistory(Employee employee) {
        return leaveApplicationRepo.findLeaveApplicationsByEmployeeAndYear(
                employee, LocalDate.now().getYear());
    }

    public Page<LeaveApplication> getPersonalLeaveHistoryPaginated(Employee employee, Pageable pageable) {
        return leaveApplicationRepo.findLeaveApplicationsByEmployee(employee, pageable);
    }

    /** Filter by status only (null status → all non-deleted). */
    public List<LeaveApplication> getPersonalLeaveHistoryByStatus(Employee employee, LeaveStatus status) {
        if (status == null) {
            return getPersonalLeaveHistory(employee);
        }
        return leaveApplicationRepo.findByEmployeeYearStatusAndType(
                employee, LocalDate.now().getYear(), status, null, Pageable.unpaged()).getContent();
    }

    public Page<LeaveApplication> getPersonalLeaveHistoryByStatusPaginated(Employee employee, LeaveStatus status, Pageable pageable) {
        return leaveApplicationRepo.findByEmployeeYearStatusAndType(
                employee, LocalDate.now().getYear(), status, null, pageable);
    }

    /** Filter by leave type only (all statuses). */
    public List<LeaveApplication> getPersonalLeaveHistoryByType(
            Employee employee, Long leaveTypeId) {
        return leaveApplicationRepo.findByEmployeeYearStatusAndType(
                employee, LocalDate.now().getYear(), null, leaveTypeId, Pageable.unpaged()).getContent();
    }

    public Page<LeaveApplication> getPersonalLeaveHistoryByTypePaginated(
            Employee employee, Long leaveTypeId, Pageable pageable) {
        return leaveApplicationRepo.findByEmployeeYearStatusAndType(
                employee, LocalDate.now().getYear(), null, leaveTypeId, pageable);
    }

    /** Filter by status and/or leave type. */
    public List<LeaveApplication> getPersonalLeaveHistoryByStatusAndType(
            Employee employee, LeaveStatus status, Long leaveTypeId) {
        return leaveApplicationRepo.findByEmployeeYearStatusAndType(
                employee, LocalDate.now().getYear(), status, leaveTypeId, Pageable.unpaged()).getContent();
    }

    public Page<LeaveApplication> getPersonalLeaveHistoryByStatusAndTypePaginated(
            Employee employee, LeaveStatus status, Long leaveTypeId, Pageable pageable) {
        return leaveApplicationRepo.findByEmployeeYearStatusAndType(
                employee, LocalDate.now().getYear(), status, leaveTypeId, pageable);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VIEW helpers — MANAGER
    // ─────────────────────────────────────────────────────────────────────────

    /** Pending applications (APPLIED or UPDATED) from direct subordinates. */
    public List<LeaveApplication> getPendingApplicationsForManager(Employee manager) {
        return leaveApplicationRepo.findPendingLeaveApplicationsByManager(manager, Pageable.unpaged()).getContent();
    }

    public Page<LeaveApplication> getPendingApplicationsForManagerPaginated(Employee manager, Pageable pageable) {
        return leaveApplicationRepo.findPendingLeaveApplicationsByManager(manager, pageable);
    }

    /** All leave from direct subordinates (excludes DELETED). */
    public List<LeaveApplication> getAllApplicationsForManager(Employee manager) {
        return leaveApplicationRepo.findAllByManager(manager, Pageable.unpaged()).getContent();
    }

    public Page<LeaveApplication> getAllApplicationsForManagerPaginated(Employee manager, Pageable pageable) {
        return leaveApplicationRepo.findAllByManager(manager, pageable);
    }

    /** Last 10 approved/rejected decisions made by this manager. */
    public List<LeaveApplication> getRecentDecisionsByManager(Employee manager) {
        return leaveApplicationRepo.findRecentDecisionsByManager(manager);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internals
    // ─────────────────────────────────────────────────────────────────────────

    private void runValidator(LeaveApplication application) {
        Errors errors = new BeanPropertyBindingResult(application, "leaveApplication");
        leaveValidator.validate(application, errors);
        if (errors.hasErrors()) {
            String msg = errors.getAllErrors().get(0).getDefaultMessage();
            throw new IllegalArgumentException(msg);
        }
    }

    /** Returns username if available; falls back to "emp-{id}" for API stubs. */
    private String resolveActor(Employee employee) {
        if (employee == null) return "system";
        if (employee.getUser() != null) return employee.getUser().getUsername();
        return "emp-" + employee.getEmp_id();
    }
}
