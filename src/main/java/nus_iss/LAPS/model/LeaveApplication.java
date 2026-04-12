package nus_iss.LAPS.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 *  Represents a leave application submitted by an employee.
 *  Contains details about the leave type, duration, reason, and approval status.
 *
 *  Author: Htet Nandar(Grace)
 *  Created on: 11/04/2026
 */
@Entity
@Table(name = "leave_application")
public class LeaveApplication {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "leaveapplication_id")
	private Long leaveApplicationId;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "emp_id", nullable = false)
	private Employee employee;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "leavetype_id", nullable = false)
	private LeaveType leaveType;
	
	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;
	
	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;
	
	@Column(name = "duration_days", nullable = false)
	private Double durationDays;

    /** Why the employee is taking leave (mandatory). */
	@Column(name = "reason", nullable = false, columnDefinition = "TEXT")
	private String reason;

    /**
     * Work dissemination — who will cover the employee's duties during absence.
     * Optional, but encouraged for longer leave periods.
     */
    @Column(name = "work_dissemination", columnDefinition = "TEXT")
    private String workDissemination;

    /**
     * Flag indicating whether the employee will be overseas during leave.
     * When true, contactDetails is mandatory (enforced in service layer).
     */
    @Column(name = "is_overseas", nullable = false)
    private Boolean isOverseas = false;

    /**
     * Contact details while on leave — required only for overseas trips.
     * May contain phone number, address, or other reachable contact info.
     */
    @Column(name = "contact_details", columnDefinition = "TEXT")
    private String contactDetails;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private LeaveStatus status = LeaveStatus.PENDING;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "approver_by", referencedColumnName = "emp_id")
	private Employee approvedBy;

	@Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_when")
    private LocalDateTime createdWhen;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "updated_when")
    private LocalDateTime updatedWhen;
    
 // Lifecycle hooks
    @PrePersist
    protected void onCreate() {
        this.createdWhen = LocalDateTime.now();
        this.updatedWhen = LocalDateTime.now();
        if (this.status == null) {
            this.status = LeaveStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedWhen = LocalDateTime.now();
    }
    
    public LeaveApplication() {}

    public LeaveApplication(Employee employee, LeaveType leaveType,
                            LocalDate startDate, LocalDate endDate,
                            Double durationDays, String reason,
                            String workDissemination, Boolean isOverseas,
                            String contactDetails) {
		this.employee = employee;
		this.leaveType = leaveType;
		this.startDate = startDate;
		this.endDate = endDate;
		this.durationDays = durationDays;
		this.reason = reason;
        this.workDissemination = workDissemination;
        this.isOverseas       = isOverseas != null ? isOverseas : false;
        this.contactDetails   = contactDetails;
		this.status = LeaveStatus.PENDING;
	}

	public Long getLeaveApplicationId() {
		return leaveApplicationId;
	}

	public void setLeaveApplicationId(Long leaveApplicationId) {
		this.leaveApplicationId = leaveApplicationId;
	}

	public Employee getEmployee() {
		return employee;
	}

	public void setEmployee(Employee employee) {
		this.employee = employee;
	}

	public LeaveType getLeaveType() {
		return leaveType;
	}

	public void setLeaveType(LeaveType leaveType) {
		this.leaveType = leaveType;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public Double getDurationDays() {
		return durationDays;
	}

	public void setDurationDays(Double durationDays) {
		this.durationDays = durationDays;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public LeaveStatus getStatus() {
		return status;
	}

	public void setStatus(LeaveStatus status) {
		this.status = status;
	}

	public Employee getApprovedBy() {
		return approvedBy;
	}

	public void setApprovedBy(Employee approvedBy) {
		this.approvedBy = approvedBy;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public LocalDateTime getCreatedWhen() {
		return createdWhen;
	}

	public void setCreatedWhen(LocalDateTime createdWhen) {
		this.createdWhen = createdWhen;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public LocalDateTime getUpdatedWhen() {
		return updatedWhen;
	}

	public void setUpdatedWhen(LocalDateTime updatedWhen) {
		this.updatedWhen = updatedWhen;
	}

    public String getWorkDissemination() {
        return workDissemination;
    }

    public void setWorkDissemination(String workDissemination) {
        this.workDissemination = workDissemination;
    }

    public String getContactDetails() {
        return contactDetails;
    }

    public void setContactDetails(String contactDetails) {
        this.contactDetails = contactDetails;
    }

    public Boolean getOverseas() {
        return isOverseas;
    }

    public void setOverseas(Boolean overseas) {
        isOverseas = overseas;
    }

    @Override
	public String toString() {
        return "LeaveApplication{" +
                "leaveApplicationId=" + leaveApplicationId +
                ", employee=" + (employee != null ? employee.getEmpId() : null) +
                ", leaveType=" + (leaveType != null ? leaveType.getLeaveTypeId() : null) +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", durationDays=" + durationDays +
                ", status=" + status +
                '}';
    }
    
}
