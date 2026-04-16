package nus_iss.LAPS.dto;

import java.time.LocalDate;

public record LeaveRequest(
        Long       employeeId,
        Long       leaveTypeId,
        LocalDate  startDate,
        LocalDate  endDate,
        String     reason,
        String     workDissemination,
        Boolean    isOverseas,
        String     contactDetails,
        Boolean    isHalfDay,
        String     halfDayPeriod
) {}
