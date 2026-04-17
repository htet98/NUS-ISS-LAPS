package nus_iss.LAPS.service;

import java.util.List;

import org.springframework.stereotype.Service;

import nus_iss.LAPS.model.LeaveType;
import nus_iss.LAPS.repository.LeaveTypeRepository;

/**
	* Author: Junior
	* Created on: 15/04/2026
**/

@Service
public class LeaveTypeService {
	
	    private final LeaveTypeRepository leaveTypeRepository;

	    public LeaveTypeService(LeaveTypeRepository leaveTypeRepository) {
	        this.leaveTypeRepository = leaveTypeRepository;
	    }

	    public List<LeaveType> findAll() {
	        return leaveTypeRepository.findAll();
	    }

	    public LeaveType findById(Long id) {
	        return leaveTypeRepository.findById(id)
	                .orElseThrow(() -> new RuntimeException("Leave type not found with id: " + id));
	    }

	    public LeaveType saveLeaveType(LeaveType leaveType) {
	        if (leaveTypeRepository.existsByName(leaveType.getName())) {
	            throw new RuntimeException("Leave type already exists.");
	        }
	        return leaveTypeRepository.save(leaveType);
	    }

	    public LeaveType updateLeaveType(Long id, LeaveType updatedLeaveType) {

	        LeaveType existing = findById(id);

	        if (leaveTypeRepository.existsByNameAndLeaveTypeIdNot(
	                updatedLeaveType.getName(), id)) {
	            throw new RuntimeException("Leave type name already exists.");
	        }

	        existing.setName(updatedLeaveType.getName());
	        existing.setDescription(updatedLeaveType.getDescription());
	        existing.setDefaultDays(updatedLeaveType.getDefaultDays());
	        existing.setPaid(updatedLeaveType.isPaid());

	        return leaveTypeRepository.save(existing);
	    }

	    public void deleteLeaveType(Long id) {
	        LeaveType existing = findById(id);
	        leaveTypeRepository.delete(existing);
	    }
}
