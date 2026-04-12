package sg.edu.nus.lms.model;

import java.io.Serializable;
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
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import sg.edu.nus.lms.helper.EmployeeStatusENUM;

@Entity
@Table(name = "employees")
public class Employee implements Serializable {

	private static final long serialVersionUID = 6529685098267757670L;
	
	@Id
	@Column(name= "emp_id")
	private String emp_id;
	
	@Column(name = "first_name")
	private String first_name;
	
	@Column(name= "last_name")
	private String last_name;
	
	@Column(name="department")
	private String department;
	
	@Column(name="hire_date")
	private LocalDate hire_date;
	
	@Column(name="EmployeeStatus")
	@Enumerated(EnumType.STRING)
	private EmployeeStatusENUM employeeStatus;
	
	@Column(name="createdBy")
	private String createdBy;
	
	@Column(name="createdWhen")
	private LocalDateTime createdWhen;
	
	@Column(name="updatedBy")
	private String updatedBy;
	
	@Column(name="updatedWhen")
	private LocalDateTime updatedWhen;
	
	@OneToOne(cascade=CascadeType.ALL, fetch=FetchType.LAZY)
	@JoinColumn(name="user_id")
	private User user;
	
	@OneToMany(mappedBy = "employee", cascade=CascadeType.ALL, fetch=FetchType.LAZY)
	private List<LeaveBalance> LeaveBalances = new ArrayList<>();
	
	//Self-referenced
	@ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.LAZY)
	@JoinColumn(name="supervisor_id")
	private Employee supervisor;
	
	
}
