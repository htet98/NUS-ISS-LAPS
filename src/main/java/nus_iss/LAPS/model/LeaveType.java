package nus_iss.LAPS.model;

import jakarta.persistence.*;

@Entity
public class LeaveType 
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long leaveTypeId;

    @Enumerated(EnumType.STRING)
    private NameTypeEnum name;
    
    private String description;
    private int defaultDays;
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

enum NameTypeEnum 
{
    ANNUAL,
    MEDICAL,
    COMPENSATION
    
}