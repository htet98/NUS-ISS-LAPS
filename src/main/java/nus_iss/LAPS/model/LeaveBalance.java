package nus_iss.LAPS.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "leave_balances")
public class LeaveBalance {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "leavebalance_id")
	private Long leaveBalanceId;

	// DataType Double or BigDecimal
	// They are annual leave, medical leave, and compensation leave.
	// An employee must take full day leave for all entitlement except compensation
	// leave.
	// However For compensation leave the granularity is 0.5 day.

	@Column(name = "total_days", nullable = false)
	private double totalDays;

	@Column(name = "used_days", nullable = false)
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
				+ usedDays +
//	           ", leaveTypeId=" + (leaveType != null ? leaveType.getId() : null) +
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
