package nus_iss.LAPS.api;

import nus_iss.LAPS.dto.*;
import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.model.LeaveApplication;
import nus_iss.LAPS.model.HalfDayPeriod;
import nus_iss.LAPS.model.LeaveStatus;
import nus_iss.LAPS.model.LeaveType;
import nus_iss.LAPS.repository.EmployeeRepository;
import nus_iss.LAPS.repository.LeaveTypeRepository;
import nus_iss.LAPS.service.LeaveApplicationService;
import nus_iss.LAPS.util.ApiRoutes;
import nus_iss.LAPS.util.GlobalConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API Controller for Leave Application Management.
 *
 * EMPLOYEE endpoints: submit, update, delete, cancel, view
 * MANAGER  endpoints: approve, reject, view pending/all/recent
 */
@RestController
@RequestMapping(ApiRoutes.LEAVE)
public class LeaveApplicationRestController {

    @Autowired private LeaveApplicationService leaveApplicationService;
    @Autowired private LeaveTypeRepository     leaveTypeRepo;
    @Autowired private EmployeeRepository      employeeRepo;

    // ── EMPLOYEE: Get personal leave history ──────────────────────────────────
    @GetMapping(ApiRoutes.LEAVE_EMPLOYEE)
    public ResponseEntity<?> getEmployeeLeave(@PathVariable Long employeeId) {
        try {
            Employee emp = findEmployee(employeeId);
            List<LeaveResponse> resp = leaveApplicationService
                    .getPersonalLeaveHistory(emp).stream()
                    .map(LeaveResponse::fromEntity).collect(Collectors.toList());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return err(e);
        }
    }

    // ── EMPLOYEE: Filter leave history ────────────────────────────────────────
    @GetMapping(ApiRoutes.LEAVE_FILTER)
    public ResponseEntity<?> getEmployeeLeaveFiltered(
            @PathVariable Long employeeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long leaveTypeId) {
        try {
            Employee emp = findEmployee(employeeId);
            LeaveStatus statusEnum = status != null ? LeaveStatus.valueOf(status) : null;
            List<LeaveApplication> apps = leaveTypeId != null
                    ? leaveApplicationService.getPersonalLeaveHistoryByStatusAndType(emp, statusEnum, leaveTypeId)
                    : leaveApplicationService.getPersonalLeaveHistoryByStatus(emp, statusEnum);
            return ResponseEntity.ok(apps.stream().map(LeaveResponse::fromEntity).collect(Collectors.toList()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Invalid status: " + e.getMessage(), null));
        } catch (Exception e) {
            return err(e);
        }
    }

    // ── Get single application ─────────────────────────────────────────────────
    @GetMapping(ApiRoutes.LEAVE_ID)
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return leaveApplicationService.findById(id)
                    .map(la -> ResponseEntity.ok((Object) LeaveResponse.fromEntity(la)))
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new ApiResponse(false, "Leave application not found", null)));
        } catch (Exception e) {
            return err(e);
        }
    }

    // ── EMPLOYEE: Submit new leave ─────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> submitLeave(@RequestBody LeaveRequest req) {
        try {
            if (req.employeeId() == null || req.leaveTypeId() == null)
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "employeeId and leaveTypeId are required", null));

            Employee emp = findEmployee(req.employeeId());
            LeaveApplication la = buildFromRequest(req, emp);
            LeaveApplication saved = leaveApplicationService.submitLeaveApplication(la);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Leave application submitted", LeaveResponse.fromEntity(saved)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        } catch (Exception e) {
            return err(e);
        }
    }

    // ── EMPLOYEE: Update leave ─────────────────────────────────────────────────
    @PutMapping(ApiRoutes.LEAVE_ID)
    public ResponseEntity<?> updateLeave(@PathVariable Long id, @RequestBody LeaveRequest req) {
        try {
            Employee emp = findEmployee(req.employeeId());
            LeaveApplication la = buildFromRequest(req, emp);
            la.setLeaveApplicationId(id);
            LeaveApplication updated = leaveApplicationService.updateLeaveApplication(la);
            return ResponseEntity.ok(new ApiResponse(true, "Leave application updated", LeaveResponse.fromEntity(updated)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponse(false, e.getMessage(), null));
        } catch (Exception e) {
            return err(e);
        }
    }

    // ── EMPLOYEE: Delete leave ─────────────────────────────────────────────────
    @DeleteMapping(ApiRoutes.LEAVE_ID)
    public ResponseEntity<?> deleteLeave(@PathVariable Long id, @RequestParam Long employeeId) {
        try {
            Employee emp = findEmployee(employeeId);
            leaveApplicationService.deleteLeave(id, emp);
            return ResponseEntity.ok(new ApiResponse(true, "Leave application deleted", null));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponse(false, e.getMessage(), null));
        } catch (Exception e) {
            return err(e);
        }
    }

    // ── EMPLOYEE: Cancel approved leave ───────────────────────────────────────
    @PatchMapping(ApiRoutes.LEAVE_CANCEL)
    public ResponseEntity<?> cancelLeave(@PathVariable Long id, @RequestParam Long employeeId) {
        try {
            Employee emp = findEmployee(employeeId);
            LeaveApplication cancelled = leaveApplicationService.cancelLeave(id, emp);
            return ResponseEntity.ok(new ApiResponse(true, "Leave application cancelled", LeaveResponse.fromEntity(cancelled)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponse(false, e.getMessage(), null));
        } catch (Exception e) {
            return err(e);
        }
    }

    // ── MANAGER: Pending applications ─────────────────────────────────────────
    @GetMapping(ApiRoutes.MANAGER_PENDING)
    public ResponseEntity<?> getPendingForManager(@PathVariable Long managerId) {
        try {
            Employee mgr = findEmployee(managerId);
            return ResponseEntity.ok(
                    leaveApplicationService.getPendingApplicationsForManager(mgr)
                            .stream().map(LeaveResponse::fromEntity).collect(Collectors.toList()));
        } catch (Exception e) { return err(e); }
    }

    // ── MANAGER: All subordinate history ──────────────────────────────────────
    @GetMapping(ApiRoutes.MANAGER_SUBORDINATES)
    public ResponseEntity<?> getSubordinateHistory(@PathVariable Long managerId) {
        try {
            Employee mgr = findEmployee(managerId);
            return ResponseEntity.ok(
                    leaveApplicationService.getAllApplicationsForManager(mgr)
                            .stream().map(LeaveResponse::fromEntity).collect(Collectors.toList()));
        } catch (Exception e) { return err(e); }
    }

    // ── MANAGER: Recent decisions ─────────────────────────────────────────────
    @GetMapping(ApiRoutes.MANAGER_RECENT)
    public ResponseEntity<?> getRecentDecisions(@PathVariable Long managerId) {
        try {
            Employee mgr = findEmployee(managerId);
            return ResponseEntity.ok(
                    leaveApplicationService.getRecentDecisionsByManager(mgr)
                            .stream().map(LeaveResponse::fromEntity).collect(Collectors.toList()));
        } catch (Exception e) { return err(e); }
    }

    // ── MANAGER: Approve ──────────────────────────────────────────────────────
    @PatchMapping(ApiRoutes.MANAGER_APPROVE)
    public ResponseEntity<?> approveLeave(@PathVariable Long id, @RequestParam Long managerId) {
        try {
            Employee mgr = findEmployee(managerId);
            LeaveApplication approved = leaveApplicationService.approveLeave(id, mgr);
            return ResponseEntity.ok(new ApiResponse(true, "Leave application approved", LeaveResponse.fromEntity(approved)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        } catch (Exception e) { return err(e); }
    }

    // ── MANAGER: Reject ───────────────────────────────────────────────────────
    @PatchMapping(ApiRoutes.MANAGER_REJECT)
    public ResponseEntity<?> rejectLeave(
            @PathVariable Long id,
            @RequestParam Long managerId,
            @RequestBody ManagerActionRequest req) {
        try {
            Employee mgr = findEmployee(managerId);
            LeaveApplication rejected = leaveApplicationService.rejectLeave(id, mgr, req.comment());
            return ResponseEntity.ok(new ApiResponse(true, "Leave application rejected", LeaveResponse.fromEntity(rejected)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        } catch (Exception e) { return err(e); }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Fetch a real Employee from DB (API callers pass a real ID). */
    private Employee findEmployee(Long empId) {
        return employeeRepo.findById(empId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + empId));
    }

    private LeaveApplication buildFromRequest(LeaveRequest req, Employee emp) {
        LeaveType leaveType = leaveTypeRepo.findById(req.leaveTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid leave type ID: " + req.leaveTypeId()));

        LeaveApplication leaveApplication = new LeaveApplication();
        leaveApplication.setEmployee(emp);
        leaveApplication.setLeaveType(leaveType);
        leaveApplication.setStartDate(req.startDate());
        leaveApplication.setEndDate(req.endDate());
        leaveApplication.setReason(req.reason());
        leaveApplication.setWorkDissemination(req.workDissemination());
        leaveApplication.setOverseas(req.isOverseas() != null ? req.isOverseas() : false);
        leaveApplication.setContactDetails(req.contactDetails());
        leaveApplication.setIsHalfDay(req.isHalfDay() != null ? req.isHalfDay() : false);

        if (req.halfDayPeriod() != null && Boolean.TRUE.equals(req.isHalfDay())) {
            try {
                leaveApplication.setHalfDayPeriod(HalfDayPeriod.valueOf(req.halfDayPeriod().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid half-day period. Must be MORNING or AFTERNOON.");
            }
        }
        return leaveApplication;
    }

    private ResponseEntity<ApiResponse> err(Exception e) {
        return ResponseEntity.internalServerError()
                .body(new ApiResponse(false, "Error: " + e.getMessage(), null));
    }
}
