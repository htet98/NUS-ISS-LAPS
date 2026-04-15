package nus_iss.LAPS.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;


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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name ="created_by", nullable = false, length = 50)
    private String createdby;

    @Column(name = "created_when")
    private LocalDateTime createdwhen;

    @Column(name = "updated_by", nullable = false, length = 50)
    private String updatedby;

    @Column(name = "updated_when")
    private LocalDateTime updatedwhen;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    // ── Associations ──────────────────────────────────────────────────────────

    /** Bidirectional — Employee holds the FK (user_id). */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Employee employee;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        this.createdwhen = LocalDateTime.now();
        this.updatedwhen = LocalDateTime.now();
        if (this.active == null) this.active = true;
    }

    /**
     * Author: Htet Nandar(Grace)
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedwhen = LocalDateTime.now();
    }

    // ── Constructors ──────────────────────────────────────────────────────────

    public User() {}

    public User(String username, String email, String password,
                String createdby, String updatedby) {
        this.username  = username;
        this.email     = email;
        this.password  = password;
        this.createdby = createdby;
        this.updatedby = updatedby;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getUser_id()                  { return user_id; }
    public void setUser_id(Long user_id)      { this.user_id = user_id; }

    public String getUsername()               { return username; }
    public void setUsername(String username)  { this.username = username; }

    public String getEmail()                  { return email; }
    public void setEmail(String email)        { this.email = email; }

    public String getPassword()               { return password; }
    public void setPassword(String password)  { this.password = password; }

    public Role getRole()                     { return role; }
    public void setRole(Role role)            { this.role = role; }

    public String getCreatedby()              { return createdby; }
    public void setCreatedby(String createdby){ this.createdby = createdby; }

    public String getUpdatedby()              { return updatedby; }
    public void setUpdatedby(String updatedby){ this.updatedby = updatedby; }

    public LocalDateTime getCreatedwhen()     { return createdwhen; }
    public void setCreatedwhen(LocalDateTime v){ this.createdwhen = v; }

    public LocalDateTime getUpdatedwhen()     { return updatedwhen; }
    public void setUpdatedwhen(LocalDateTime v){ this.updatedwhen = v; }

    public Boolean getActive()                { return active; }
    public void setActive(Boolean active)     { this.active = active; }

    public Employee getEmployee()             { return employee; }
    public void setEmployee(Employee employee){ this.employee = employee; }

    @Override
    public String toString() {
        return "User{id=" + user_id + ", username='" + username + "', role=" + role + '}';
    }
}
