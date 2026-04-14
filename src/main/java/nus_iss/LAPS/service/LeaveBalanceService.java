package nus_iss.LAPS.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.model.LeaveBalance;
import nus_iss.LAPS.model.LeaveType;
import nus_iss.LAPS.repository.EmployeeRepository;
import nus_iss.LAPS.repository.LeaveBalanceRepository;
import nus_iss.LAPS.repository.LeaveTypeRepository;

@Service
public class LeaveBalanceService {

	@Autowired
	private EmployeeRepository employeeRepo;

	@Autowired
	private LeaveTypeRepository leaveTypeRepo;

	private final LeaveBalanceRepository leaveBalanceRepo;

	public LeaveBalanceService(LeaveBalanceRepository leaveBalanceRepo) {
		this.leaveBalanceRepo = leaveBalanceRepo;
	}

	public List<LeaveType> getAllLeaveTypes() {
		return leaveTypeRepo.findAll();
	}

	public List<Employee> getAllEmployees() {
		return employeeRepo.findAll();
	}

	public Optional<Employee> getEmployeeNameById(Long id) {
		return employeeRepo.findById(id);
	}

	public List<LeaveBalance> getAllLeaveBalances() {
		return leaveBalanceRepo.findAll();
	}

	public Optional<LeaveBalance> getLeaveBalanceById(Long leaveBalanceId) {
		return leaveBalanceRepo.findById(leaveBalanceId);
	}

	public List<LeaveBalance> getLeaveBalancesByEmployeeId(Long employeeId) {

		Employee emp = employeeRepo.findById(employeeId)
				.orElseThrow(() -> new RuntimeException("Employee not found"));

		return leaveBalanceRepo.findByEmployeeId(employeeId);
	}

	public void createLeaveBalance(Long employeeId, Long leaveTypeId, double totalDays) {

		Employee emp = employeeRepo.findById(employeeId)
				.orElseThrow(() -> new RuntimeException("Employee not found"));

		LeaveType type = leaveTypeRepo.findById(leaveTypeId)
				.orElseThrow(() -> new RuntimeException("LeaveType not found"));

		if (totalDays < 0) {
			throw new RuntimeException("Total days cannot be negative");
		}

		LeaveBalance balance = new LeaveBalance();
		balance.setEmployee(emp);
		balance.setLeaveType(type);
		balance.setTotalDays(totalDays);
		balance.setUsedDays(0);

		leaveBalanceRepo.save(balance);
	}

	public void updateTotalDays(Long id, double totalDays) {

		LeaveBalance balance = leaveBalanceRepo.findById(id)
				.orElseThrow(() -> new RuntimeException("Not found"));

		if (totalDays < 0) {
			throw new RuntimeException("Total days cannot be negative");
		}

		if (balance.getUsedDays() > totalDays) {
			throw new RuntimeException("Used days cannot exceed total days");
		}

		balance.setTotalDays(totalDays);

		leaveBalanceRepo.save(balance);
	}

	public void updateUsedDays(Long id, double usedDays) {

		LeaveBalance balance = leaveBalanceRepo.findById(id)
				.orElseThrow(() -> new RuntimeException("Not found"));

		if (usedDays < 0) {
			throw new RuntimeException("used days cannot be negative");
		}

		if (balance.getTotalDays() < usedDays) {
			throw new RuntimeException("Used days cannot exceed total days");
		}

		balance.setUsedDays(usedDays);

		leaveBalanceRepo.save(balance);
	}

	public void incrementUsedDays(Long id, double leaveDurationDays) {
		// e,g approved 2 days, increment usedDays by 2 (leaveDurationDays)

		LeaveBalance balance = leaveBalanceRepo.findById(id)
				.orElseThrow(() -> new RuntimeException("Not found"));

		if (leaveDurationDays < 0) {
			throw new RuntimeException("cannot be negative");
		}

		if (balance.getUsedDays() + leaveDurationDays > balance.getTotalDays()) {
			throw new RuntimeException("Used days cannot exceed total days");
		}

		double updatedUsedDays = balance.getUsedDays() + leaveDurationDays;
		balance.setUsedDays(updatedUsedDays);

		leaveBalanceRepo.save(balance);
	}

	public void decrementUsedDays(Long id, double leaveDurationDays) {
		// e.g usuage : rejecting an approved leaved of 2 days, increment usedDays by 2
		// (leaveDurationDays)
		// now reject or cancel, decrement the UsedDays

		LeaveBalance balance = leaveBalanceRepo.findById(id)
				.orElseThrow(() -> new RuntimeException("Not found"));

		if (leaveDurationDays < 0) {
			throw new RuntimeException("cannot be negative");
		}

		if (balance.getUsedDays() - leaveDurationDays < 0) {
			throw new RuntimeException("cannot be negative");
		}

		double updatedUsedDays = balance.getUsedDays() - leaveDurationDays;
		balance.setUsedDays(updatedUsedDays);

		leaveBalanceRepo.save(balance);
	}

	public void deleteLeaveBalance(Long leaveBalanceId) {

		LeaveBalance balance = leaveBalanceRepo.findById(leaveBalanceId)
				.orElseThrow(() -> new RuntimeException("Leave balance not found"));

		leaveBalanceRepo.delete(balance);
	}

}
