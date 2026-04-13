User.java

package nus_iss.LAPS.model;

	import jakarta.persistence.*;
	import java.time.LocalDate;
	import java.time.LocalDateTime;
	import java.util.ArrayList;
	import java.util.List;

import javax.management.relation.Role;

	/**
	 * Registered user.
	 *
	 * Associations demonstrated:
	 *   @OneToOne  → Employee (a user owns exactly one role:Employee)
	 *
	 * Data types:
	 *   Long          
	 *   – user_id VARCHAR(50)
	 *   String        
	 *   – username VARCHAR(50)
	 *   - email VARCHAR(255)
	 *   - password VARCHAR(255)
	 *   - role ENUM('EMPLOYEE', 'MANAGER', 'ADMIN') NOT NULL
	 *   - createdby VARCHAR(50)
	 *   - updatedby VARCHAR(50)
	 *   
	 *   LocalDateTime 
	 *   – createdwhen VANCHARD FAULT CURRENT TIMESTAMP
	 *   - updatedwhen TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON (java.sql.Timestamp equivalent)
	 */
	@Entity
	@Table(name = "users")
	public class User {

	    @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long user_id;

	    @Column(nullable = false, unique = true, length = 50)
	    private String username;

	    @Column(nullable = false, unique = true, length = 255)
	    private String email;

	    @Column(nullable = false, length = 255)
	    private String password;
	    
	    @Enumerated(EnumType.STRING) // IMPORTANT
	    @Column(nullable = false)
	    private Role role;
	    
	    @Column(nullable = false, length = 50)
	    private String createdby;
	    
	    @Column(nullable = false, length = 50)
	    private String updatedby;


	    /** java.time.LocalDateTime → TIMESTAMP column */
	    @Column(name = "created_when")
	    private LocalDateTime createdwhen;
	    
	    @Column(name = "updated_when")
	    private LocalDateTime updatedwhen;

	    // private Boolean active;

	    //  Associations 

	    /** @OneToOne – bidirectional; Employee holds the FK (user_id). */
	    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	    private Employee employee;

		private Object active1;

		private Object active;


	    //  Lifecycle 

	    @PrePersist
	    protected void onCreate() {
	        this.createdwhen = LocalDateTime.now();
	        if (this.active1 == null) this.active1 = true;
	    }
	    
	    @PrePersist
	    protected void onUpdate() {
	        this.updatedwhen = LocalDateTime.now();
	        if (this.active1 == null) this.active1 = true;
	    }

	    //  Constructors 

	    public User() {}

	    public User(String username, String email, String password,
	                Role role, String createdby, String updatedby) {
	        this.username  = username;
	        this.email     = email;
	        this.password  = password;
	        this.role = role;
	        this.createdby  = createdby;
	        this.updatedby = updatedby;
	    }

	//  Getters & Setters 
		public Long getUser_id() {
			return user_id;
		}

		public void setUser_id(Long user_id) {
			this.user_id = user_id;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public Role getRole() {
			return role;
		}

		public void setRole(Role role) {
			this.role = role;
		}

		public String getCreatedby() {
			return createdby;
		}

		public void setCreatedby(String createdby) {
			this.createdby = createdby;
		}

		public String getUpdatedby() {
			return updatedby;
		}

		public void setUpdatedby(String updatedby) {
			this.updatedby = updatedby;
		}

		public LocalDateTime getCreatedwhen() {
			return createdwhen;
		}

		public void setCreatedwhen(LocalDateTime createdwhen) {
			this.createdwhen = createdwhen;
		}

		public Employee getEmployee() {
			return employee;
		}

		public void setEmployee(Employee employee) {
			this.employee = employee;
		}

	}

