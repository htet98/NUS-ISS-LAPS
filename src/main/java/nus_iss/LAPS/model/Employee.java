package nus_iss.LAPS.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
	* Author: Junior
 	* Created on: 13/04/2026
 	* Updated on 15/04/2026 (LOMBOK, Designation change to ENUM)
**/

@Entity
@Table(name = "employees")
@Getter
@Setter
public class Employee {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name= "emp_id")
	private Long emp_id;
	
	@Column(name = "first_name")
	private String first_name;
	
	@NotNull(message = "Last_name is required")
	@Column(name= "last_name")
	private String last_name;
	
	@NotNull(message = "Email Address is required")
	@Column(name = "email")
	@Email
	private String email;
	
	@NotNull(message = "Phone Number is required")
	@Column(name = "phone_number", length = 15)
	@Pattern(regexp="^[689]\\d{7}$") //Not include Toll-free
	private String phoneNumber;
	
	@Column(name="department", nullable=false)
	private String department;
	
	@Enumerated(EnumType.STRING)
	@Column(name="designation")
	private Designation designation;
	
	@Column(name="hire_date", nullable=false)
	private LocalDate hire_date;
	
	@Enumerated(EnumType.STRING)
	@Column(name="employee_status")
	private EmployeeStatus employeeStatus;

	@Column(name="created_by", nullable=false)
	private String createdBy;

	@Column(name="created_when", nullable=false)
	private LocalDateTime createdWhen;

	@Column(name="updated_by")
	private String updatedBy;

	@Column(name="updated_when")
	private LocalDateTime updatedWhen;
	
	@OneToOne(cascade=CascadeType.ALL, fetch=FetchType.LAZY)
	@JoinColumn(name="user_id", nullable=false, unique=true)
	private User user;
	
	@OneToMany(mappedBy = "employee", cascade=CascadeType.ALL, fetch=FetchType.LAZY)
	private List<LeaveBalance> LeaveBalances = new ArrayList<>();
	
	//Self-referenced
	@ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.LAZY)
	@JoinColumn(name="supervisor_id")
	private Employee supervisor;
	
	@OneToMany(mappedBy = "supervisor")
	private List<Employee> subordinates;

	// Htet Nandar (Grace) - 14/04/2026
    // Bug fix: Phone number should be stored without dashes for easier validation and querying.

	public Employee(Long emp_id, String first_name, @NotNull(message = "Last_name is required") String last_name,
			@NotNull(message = "Email Address is required") @Email String email,
			@NotNull(message = "Phone Number is required") String phoneNumber, String department,
			Designation designation, LocalDate hire_date,
			EmployeeStatus employeeStatus, String createdBy, LocalDateTime createdWhen, String updatedBy,
			LocalDateTime updatedWhen, User user, List<LeaveBalance> leaveBalances, Employee supervisor,
			List<Employee> subordinates) {
		super();
		this.emp_id = emp_id;
		this.first_name = first_name;
		this.last_name = last_name;
		this.email = email;
		this.phoneNumber = phoneNumber;
		this.department = department;
		this.designation = designation;
		this.hire_date = hire_date;
		this.employeeStatus = employeeStatus;
		this.createdBy = createdBy;
		this.createdWhen = createdWhen;
		this.updatedBy = updatedBy;
		this.updatedWhen = updatedWhen;
		this.user = user;
		this.LeaveBalances = leaveBalances;
		this.supervisor = supervisor;
		this.subordinates = subordinates;
	}

	public Employee() {
	}

	@Override
	public String toString() {
		return "Employee [emp_id=" + emp_id + ", first_name=" + first_name + ", last_name=" + last_name + ", email="
				+ email + ", phoneNumber=" + phoneNumber + ", department=" + department + ", designation=" + designation
				+ ", hire_date=" + hire_date + ", employeeStatus=" + employeeStatus + ", createdBy=" + createdBy
				+ ", createdWhen=" + createdWhen + ", updatedBy=" + updatedBy + ", updatedWhen=" + updatedWhen
				+ ", user=" + user + ", LeaveBalances=" + LeaveBalances + ", supervisor=" + supervisor
				+ ", subordinates=" + subordinates + "]";
	}
}
