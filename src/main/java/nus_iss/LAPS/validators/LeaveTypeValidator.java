package nus_iss.LAPS.validators;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import nus_iss.LAPS.model.LeaveType;
import nus_iss.LAPS.repository.LeaveTypeRepository;


@Component
public class LeaveTypeValidator implements Validator {


   @Autowired
   private LeaveTypeRepository leaveTypeRepo;


   @Override
   public boolean supports(Class<?> clazz) {
       return LeaveType.class.isAssignableFrom(clazz);
   }


   @Override
   public void validate(Object target, Errors errors) {
       LeaveType leaveType = (LeaveType) target;


       ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "name.empty", "Name cannot be empty");


       ValidationUtils.rejectIfEmpty(errors, "description", "description.empty", "Description cannot be empty");


       boolean nameExists = leaveTypeRepo.existsByName(leaveType.getName());
       if (nameExists) {
           errors.rejectValue("name", "name.exists", "Leave type already exists");
       }


       if (leaveType.getDescription() != null && leaveType.getDescription().trim().length() > 150) {
           errors.rejectValue("description", "description.length", "Description is too long");
       }


       if(leaveType.getDefaultDays() == null || leaveType.getDefaultDays() < 0) {
           errors.rejectValue("defaultDays", "defaultDays.empty", "Default days cannot be empty");
       }


       if(leaveType.getIsPaid() == null){
           errors.rejectValue("isPaid", "isPaid.empty", "Paid type cannot be empty");
       }


       if (leaveType.getDefaultDays() != null) {


           // Check for Compensation Leave (Must be divisible by 0.5)
           if (leaveType.getName().name().equals("COMPENSATION")) {
               if (leaveType.getDefaultDays() % 0.5 != 0) {
                   errors.rejectValue("defaultDays", "error.defaultDays",
                           "Compensation leave must be in half-day (0.5) increments.");
               }
           }
           // Check for Annual/Medical Leave (Must be divisible by 1.0)
           else {
               if (leaveType.getDefaultDays() % 1 != 0) {
                   errors.rejectValue("defaultDays", "error.defaultDays",
                           "This leave type only allows full-day (1.0) increments.");
               }
           }
       }


       boolean isEdit = leaveType.getLeaveTypeId() != null;


       if (isEdit && leaveType.getName() != null) {
           boolean exists = leaveTypeRepo.existsByName(leaveType.getName());


           if (exists) {
               errors.rejectValue("name", "name.exists", "Leave type name " + leaveType.getName() + " already exists");
           }
       }
   }


}

