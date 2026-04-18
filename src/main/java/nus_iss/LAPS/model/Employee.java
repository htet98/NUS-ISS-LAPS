package nus_iss.LAPS.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
	* Author: Junior
 	* Created on: 13/04/2026
 	* Updated on 15/04/2026 (LOMBOK, Designation change to ENUM)
**/

@Entity
@Table(name = "employees")
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
	@Pattern(regexp = "\\d{7,15}", message = "Phone number must be between 7 and 15 digits")
	private String phoneNumber;
	
	@Column(name="department", nullable=false)
	private String department;
	
	@Enumerated(EnumType.STRING)
	@Column(name="designation", nullable=false, length = 20)
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
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="supervisor_id")
	private Employee supervisor;
	
	@OneToMany(mappedBy = "supervisor", fetch=FetchType.LAZY)
	private List<Employee> subordinates;

	// Htet Nandar (Grace) - 14/04/2026
    // Bug fix: Phone number should be stored without dashes for easier validation and querying.
	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public Designation getDesignation() {
		return designation;
	}

    public Long getEmp_id() {
        return emp_id;
    }

    public void setEmp_id(Long emp_id) {
        this.emp_id = emp_id;
    }

    public String getFirst_name() {
        return first_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public void setLast_name(String last_name) {
        this.last_name = last_name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setDesignation(Designation designation) {
		this.designation = designation;
	}

	public LocalDate getHire_date() {
		return hire_date;
	}

	public void setHire_date(LocalDate hire_date) {
		this.hire_date = hire_date;
	}

	public EmployeeStatus getEmployeeStatus() {
		return employeeStatus;
	}

	public void setEmployeeStatus(EmployeeStatus employeeStatus) {
		this.employeeStatus = employeeStatus;
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

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public List<LeaveBalance> getLeaveBalances() {
		return LeaveBalances;
	}

	public void setLeaveBalances(List<LeaveBalance> leaveBalances) {
		LeaveBalances = leaveBalances;
	}
	 
	public Employee getSupervisor() {
		return supervisor;
	}

	public void setSupervisor(Employee supervisor) {
		this.supervisor = supervisor;
	}

	public List<Employee> getSubordinates() {
		return subordinates;
	}

	public void setSubordinates(List<Employee> subordinates) {
		this.subordinates = subordinates;
	}

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
        return "Employee{" +
                "emp_id=" + emp_id +
                ", first_name='" + first_name + '\'' +
                ", last_name='" + last_name + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
