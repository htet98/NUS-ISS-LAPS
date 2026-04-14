package nus_iss.LAPS.model;

import jakarta.persistence.*;

@Entity
@Table(name = "leave_types")
public class LeaveType
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leavetype_id")
    private Long leaveTypeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, length = 50)
    private NameTypeEnum name;

    @Column(name = "description")
    private String description;

    @Column(name = "default_days")
    private int defaultDays;

    @Column(name = "is_paid")
    private boolean isPaid;

    public LeaveType() {}

    public LeaveType(NameTypeEnum name, String description, int defaultDays, boolean isPaid) 
    {
        this.name = name;
        this.description = description;
        this.defaultDays = defaultDays;
        this.isPaid = isPaid;
    }

    public Long getLeaveTypeId() { return leaveTypeId; }
    public void setLeaveTypeId(Long leaveTypeId) { this.leaveTypeId = leaveTypeId; }

    public NameTypeEnum getName() { return name; }
    public void setName(NameTypeEnum name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getDefaultDays() { return defaultDays; }
    public void setDefaultDays(int defaultDays) { this.defaultDays = defaultDays; }

    public boolean isPaid() { return isPaid; }
    public void setPaid(boolean paid) { isPaid = paid; }
    
    
    @Override
	public String toString() 
    {
		return "LeaveType [id=" + leaveTypeId + ", Type=" + name + ", Description=" + description +
	           ", DefaultDays=" + defaultDays + ", IsPaid=" + isPaid + "]" ;
	}
    
    
}