package nus_iss.LAPS.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nus_iss.LAPS.model.LeaveType;
import nus_iss.LAPS.repository.LeaveTypeRepository;


@Service
public class LeaveTypeService {


   @Autowired
   private LeaveTypeRepository leaveTypeRepository;


   public List<LeaveType> getAllLeaveTypes() {
       return leaveTypeRepository.findAll();
   }


   public Optional<LeaveType> getLeaveTypeById(Long leaveTypeId){
       return leaveTypeRepository.findById(leaveTypeId);
   }


   public void createLeaveType(LeaveType leaveType){
       leaveTypeRepository.save(leaveType);
   }


   public void changeLeaveType(LeaveType leaveType){
       leaveTypeRepository.save(leaveType);
   }


   public void removeLeaveType(LeaveType leaveType){
       leaveTypeRepository.delete(leaveType);
   }

}
