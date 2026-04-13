package nus_iss.LAPS.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

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
	@Column(name = "Phone_number")
	@Pattern(regexp="^[689]\\d{7}$") //Not include Toll-free
	@Max(8)
	private int phoneNumber;
	
	@Column(name="department", nullable=false)
	private String department;
	
	@Column(name="designation", nullable=false)
	private String designation;
	
	@Min(value=2020, message= "hire date should not be lesser than 2020")
	@Max(value=2099, message= "hire date must be 2099 or earlier")
	@Column(name="hire_date", nullable=false)
	private LocalDate hire_date;
	
	@Column(name="EmployeeStatus")
	@Enumerated(EnumType.STRING)
	private EmployeeStatus employeeStatus;
	
	@Column(name="createdBy", nullable=false)
	private String createdBy;
	
	@Column(name="createdWhen", nullable=false)
	private LocalDateTime createdWhen;
	
	@Column(name="updatedBy")
	private String updatedBy;
	
	@Column(name="updatedWhen")
	private LocalDateTime updatedWhen;
	
	@OneToOne(cascade=CascadeType.ALL, fetch=FetchType.LAZY)
	@JoinColumn(name="user_id", nullable=false, unique=true)
	//private User user;
	
	@OneToMany(mappedBy = "employees", cascade=CascadeType.ALL, fetch=FetchType.LAZY)
	//private List<LeaveBalance> LeaveBalances = new ArrayList<>();
	
	//Self-referenced
	@ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.LAZY)
	@JoinColumn(name="supervisor_id")
	private Employee supervisor;
	
	@OneToMany(mappedBy = "supervisor")
	private List<Employee> subordinates;

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

	public int getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(int phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public String getDesignation() {
		return designation;
	}

	public void setDesignation(String designation) {
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

	/*public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}*/

	/*public List<LeaveBalance> getLeaveBalances() {
		return LeaveBalances;
	}

	public void setLeaveBalances(List<LeaveBalance> leaveBalances) {
		LeaveBalances = leaveBalances;
	}
	 */
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
			@NotNull(message = "Phone Number is required") @Max(11) int phoneNumber, String department,
			String designation,
			@Min(value = 2020, message = "hire date should not be lesser than 2020") @Max(value = 2099, message = "hire date must be 2099 or earlier") LocalDate hire_date,
			EmployeeStatus employeeStatus, String createdBy, LocalDateTime createdWhen, String updatedBy,
			LocalDateTime updatedWhen, /*User user, List<LeaveBalance> leaveBalances,*/ Employee supervisor,
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
		//this.user = user;
		//LeaveBalances = leaveBalances;
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
				+ /*", user=" + user + ", LeaveBalances=" + LeaveBalances + */", supervisor=" + supervisor
				+ ", subordinates=" + subordinates + "]";
	}
	
	
	
	
	
	
	
	
	
	
}
