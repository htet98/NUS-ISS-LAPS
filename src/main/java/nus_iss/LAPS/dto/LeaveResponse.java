package nus_iss.LAPS.dto;

import nus_iss.LAPS.model.LeaveApplication;

import java.time.LocalDate;

public record LeaveResponse(
        Long      leaveApplicationId,
        Long      employeeId,
        String    employeeName,
        String    employeeDesignation,
        String    employeeDepartment,
        String    leaveType,
        LocalDate startDate,
        LocalDate endDate,
        Double    durationDays,
        String    reason,
        String    workDissemination,
        Boolean   isOverseas,
        String    contactDetails,
        Boolean   isHalfDay,
        String    halfDayPeriod,
        String status,
        Long      actionedByManagerId,
        String    actionedByName,
        String    managerComment,
        boolean   editable,
        boolean   deletable,
        boolean   cancellable,
        String    durationDisplay,
        java.time.LocalDateTime createdWhen,
        java.time.LocalDateTime updatedWhen
) {
    public static LeaveResponse fromEntity(LeaveApplication la) {
        String empName = la.getEmployee() != null
                ? la.getEmployee().getFirst_name() + " " + la.getEmployee().getLast_name()
                : "Unknown";

        String empDesignation = la.getEmployee() != null ? la.getEmployee().getDesignation() : null;
        String empDepartment  = la.getEmployee() != null ? la.getEmployee().getDepartment() : null;

        // getName() returns NameTypeEnum — .name() gives the String constant
        String leaveTypeName = la.getLeaveType() != null
                ? la.getLeaveType().getName().name()
                : "Unknown";

        Long   mgrId   = la.getApprovedBy() != null ? la.getApprovedBy().getEmp_id() : null;
        String mgrName = la.getApprovedBy() != null
                ? la.getApprovedBy().getFirst_name() + " " + la.getApprovedBy().getLast_name()
                : null;
        String halfDay = la.getHalfDayPeriod() != null ? la.getHalfDayPeriod().name() : null;

        return new LeaveResponse(
                la.getLeaveApplicationId(),
                la.getEmployee() != null ? la.getEmployee().getEmp_id() : null,
                empName, empDesignation, empDepartment, leaveTypeName,
                la.getStartDate(), la.getEndDate(), la.getDurationDays(),
                la.getReason(), la.getWorkDissemination(),
                la.getIsOverseas(), la.getContactDetails(),
                la.getIsHalfDay(), halfDay,
                la.getStatus().name(),
                mgrId, mgrName, la.getManagerComment(),
                la.isEditable(),
                la.isDeletable(),
                la.isCancellable(),
                la.getDurationDisplay(),
                la.getCreatedWhen(),
                la.getUpdatedWhen());
    }
}
