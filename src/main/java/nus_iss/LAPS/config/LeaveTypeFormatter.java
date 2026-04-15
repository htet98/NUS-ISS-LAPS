package nus_iss.LAPS.config;

import nus_iss.LAPS.model.LeaveType;
import nus_iss.LAPS.repository.LeaveTypeRepository;
import org.springframework.format.Formatter;

import java.text.ParseException;
import java.util.Locale;

/**
 * Formatter for converting between LeaveType objects and their ID strings.
 * Used for Spring MVC form binding when th:field references nested objects.
 */
public class LeaveTypeFormatter implements Formatter<LeaveType> {

    private final LeaveTypeRepository leaveTypeRepository;

    public LeaveTypeFormatter(LeaveTypeRepository leaveTypeRepository) {
        this.leaveTypeRepository = leaveTypeRepository;
    }

    @Override
    public String print(LeaveType leaveType, Locale locale) {
        if (leaveType == null) {
            return "";
        }
        return leaveType.getLeaveTypeId().toString();
    }

    @Override
    public LeaveType parse(String id, Locale locale) throws ParseException {
        if (id == null || id.isEmpty()) {
            return null;
        }
        try {
            Long leaveTypeId = Long.parseLong(id);
            return leaveTypeRepository.findById(leaveTypeId)
                    .orElseThrow(() -> new ParseException("LeaveType not found with id: " + id, 0));
        } catch (NumberFormatException e) {
            throw new ParseException("Invalid LeaveType id format: " + id, 0);
        }
    }
}

