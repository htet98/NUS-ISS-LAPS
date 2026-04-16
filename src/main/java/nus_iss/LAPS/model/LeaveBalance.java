package nus_iss.LAPS.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Entity
// constraints prevent duplicate leave type for 1 employee
@Table(name = "leave_balances", uniqueConstraints = @UniqueConstraint(columnNames = { "emp_id",
		"leavetype_id" }))
public class LeaveBalance {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "leavebalance_id")
	private Long leaveBalanceId;

	// DataType double or BigDecimal
	// Cater For compensation leave the granularity is 0.5 day.

	@Column(name = "total_days", nullable = false)
	@NotNull(message = "Total days is required")
	@PositiveOrZero(message = "Total days must be >= 0")
	private double totalDays;

	@Column(name = "used_days", nullable = false)
	@NotNull(message = "Used days is required")
	@PositiveOrZero(message = "Used days must be >= 0")
	private double usedDays;

	// Mapping relation - LeaveBalance - LeaveType
	@ManyToOne
	@JoinColumn(name = "leavetype_id", nullable = false)
	private LeaveType leaveType;

	@ManyToOne
	@JoinColumn(name = "emp_id", nullable = false)
	private Employee employee;

	// getters and setters
	public Long getLeaveBalanceId() {
		return leaveBalanceId;
	}

	public double getTotalDays() {
		return totalDays;
	}

	public void setTotalDays(double totalDays) {
		this.totalDays = totalDays;
	}

	public double getUsedDays() {
		return usedDays;
	}

	public void setUsedDays(double usedDays) {
		this.usedDays = usedDays;
	}

	public LeaveType getLeaveType() {
		return leaveType;
	}

	public void setLeaveType(LeaveType leaveType) {
		this.leaveType = leaveType;
	}

	public Employee getEmployee() {
		return employee;
	}

	public void setEmployee(Employee employee) {
		this.employee = employee;
	}

	@Override
	public String toString() {
		return "LeaveBalance [id=" + leaveBalanceId + ", totalDays=" + totalDays + ", usedDays="
				+ usedDays + ", leaveTypeId="
				+ (leaveType != null ? leaveType.getLeaveTypeId() : null) +
//	           ", employeeId=" + (employee != null ? employee.getId() : null) +
				"]";
	}

	// constructors
	public LeaveBalance() {
	}

	public LeaveBalance(double totalDays, double usedDays, LeaveType leaveType, Employee employee) {
		super();
		this.totalDays = totalDays;
		this.usedDays = usedDays;
		this.leaveType = leaveType;
		this.employee = employee;
	}

}
