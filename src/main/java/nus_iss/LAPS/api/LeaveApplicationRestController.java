package nus_iss.LAPS.api;

import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.model.LeaveApplication;
import nus_iss.LAPS.model.HalfDayPeriod;
import nus_iss.LAPS.model.LeaveStatus;
import nus_iss.LAPS.model.LeaveType;
import nus_iss.LAPS.repository.EmployeeRepository;
import nus_iss.LAPS.repository.LeaveTypeRepository;
import nus_iss.LAPS.service.LeaveApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API Controller for Leave Application Management.
 *
 * EMPLOYEE endpoints: submit, update, delete, cancel, view
 * MANAGER  endpoints: approve, reject, view pending/all/recent
 */
@RestController
@RequestMapping("/api/leave")
public class LeaveApplicationRestController {

    @Autowired private LeaveApplicationService leaveApplicationService;
    @Autowired private LeaveTypeRepository     leaveTypeRepo;
    @Autowired private EmployeeRepository      employeeRepo;

    // ── DTOs ──────────────────────────────────────────────────────────────────

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

    public record LeaveResponse(
            Long      leaveApplicationId,
            Long      employeeId,
            String    employeeName,
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
            String    status,
            Long      actionedByManagerId,
            String    actionedByName,
            String    managerComment
    ) {}

    public record ManagerActionRequest(String comment) {}

    public record ApiResponse(boolean success, String message, Object data) {}

    // ── EMPLOYEE: Get personal leave history ──────────────────────────────────
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<?> getEmployeeLeave(@PathVariable Long employeeId) {
        try {
            Employee emp = findEmployee(employeeId);
            List<LeaveResponse> resp = leaveApplicationService
                    .getPersonalLeaveHistory(emp).stream()
                    .map(this::toResponse).collect(Collectors.toList());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return err(e);
        }
    }

    // ── EMPLOYEE: Filter leave history ────────────────────────────────────────
    @GetMapping("/employee/{employeeId}/filter")
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
            return ResponseEntity.ok(apps.stream().map(this::toResponse).collect(Collectors.toList()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Invalid status: " + e.getMessage(), null));
        } catch (Exception e) {
            return err(e);
        }
    }

    // ── Get single application ─────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return leaveApplicationService.findById(id)
                    .map(la -> ResponseEntity.ok((Object) toResponse(la)))
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
                    .body(new ApiResponse(true, "Leave application submitted", toResponse(saved)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        } catch (Exception e) {
            return err(e);
        }
    }

    // ── EMPLOYEE: Update leave ─────────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<?> updateLeave(@PathVariable Long id, @RequestBody LeaveRequest req) {
        try {
            Employee emp = findEmployee(req.employeeId());
            LeaveApplication la = buildFromRequest(req, emp);
            la.setLeaveApplicationId(id);
            LeaveApplication updated = leaveApplicationService.updateLeaveApplication(la);
            return ResponseEntity.ok(new ApiResponse(true, "Leave application updated", toResponse(updated)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponse(false, e.getMessage(), null));
        } catch (Exception e) {
            return err(e);
        }
    }

    // ── EMPLOYEE: Delete leave ─────────────────────────────────────────────────
    @DeleteMapping("/{id}")
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
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<?> cancelLeave(@PathVariable Long id, @RequestParam Long employeeId) {
        try {
            Employee emp = findEmployee(employeeId);
            LeaveApplication cancelled = leaveApplicationService.cancelLeave(id, emp);
            return ResponseEntity.ok(new ApiResponse(true, "Leave application cancelled", toResponse(cancelled)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponse(false, e.getMessage(), null));
        } catch (Exception e) {
            return err(e);
        }
    }

    // ── MANAGER: Pending applications ─────────────────────────────────────────
    @GetMapping("/manager/{managerId}/pending")
    public ResponseEntity<?> getPendingForManager(@PathVariable Long managerId) {
        try {
            Employee mgr = findEmployee(managerId);
            return ResponseEntity.ok(
                    leaveApplicationService.getPendingApplicationsForManager(mgr)
                            .stream().map(this::toResponse).collect(Collectors.toList()));
        } catch (Exception e) { return err(e); }
    }

    // ── MANAGER: All subordinate history ──────────────────────────────────────
    @GetMapping("/manager/{managerId}/subordinates")
    public ResponseEntity<?> getSubordinateHistory(@PathVariable Long managerId) {
        try {
            Employee mgr = findEmployee(managerId);
            return ResponseEntity.ok(
                    leaveApplicationService.getAllApplicationsForManager(mgr)
                            .stream().map(this::toResponse).collect(Collectors.toList()));
        } catch (Exception e) { return err(e); }
    }

    // ── MANAGER: Recent decisions ─────────────────────────────────────────────
    @GetMapping("/manager/{managerId}/recent-decisions")
    public ResponseEntity<?> getRecentDecisions(@PathVariable Long managerId) {
        try {
            Employee mgr = findEmployee(managerId);
            return ResponseEntity.ok(
                    leaveApplicationService.getRecentDecisionsByManager(mgr)
                            .stream().map(this::toResponse).collect(Collectors.toList()));
        } catch (Exception e) { return err(e); }
    }

    // ── MANAGER: Approve ──────────────────────────────────────────────────────
    @PatchMapping("/{id}/approve")
    public ResponseEntity<?> approveLeave(@PathVariable Long id, @RequestParam Long managerId) {
        try {
            Employee mgr = findEmployee(managerId);
            LeaveApplication approved = leaveApplicationService.approveLeave(id, mgr);
            return ResponseEntity.ok(new ApiResponse(true, "Leave application approved", toResponse(approved)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        } catch (Exception e) { return err(e); }
    }

    // ── MANAGER: Reject ───────────────────────────────────────────────────────
    @PatchMapping("/{id}/reject")
    public ResponseEntity<?> rejectLeave(
            @PathVariable Long id,
            @RequestParam Long managerId,
            @RequestBody ManagerActionRequest req) {
        try {
            Employee mgr = findEmployee(managerId);
            LeaveApplication rejected = leaveApplicationService.rejectLeave(id, mgr, req.comment());
            return ResponseEntity.ok(new ApiResponse(true, "Leave application rejected", toResponse(rejected)));
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

        LeaveApplication la = new LeaveApplication();
        la.setEmployee(emp);
        la.setLeaveType(leaveType);
        la.setStartDate(req.startDate());
        la.setEndDate(req.endDate());
        la.setReason(req.reason());
        la.setWorkDissemination(req.workDissemination());
        la.setOverseas(req.isOverseas() != null ? req.isOverseas() : false);
        la.setContactDetails(req.contactDetails());
        la.setIsHalfDay(req.isHalfDay() != null ? req.isHalfDay() : false);

        if (req.halfDayPeriod() != null && Boolean.TRUE.equals(req.isHalfDay())) {
            try {
                la.setHalfDayPeriod(HalfDayPeriod.valueOf(req.halfDayPeriod().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid half-day period. Must be MORNING or AFTERNOON.");
            }
        }
        return la;
    }

    private LeaveResponse toResponse(LeaveApplication la) {
        String empName = la.getEmployee() != null
                ? la.getEmployee().getFirst_name() + " " + la.getEmployee().getLast_name()
                : "Unknown";

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
                empName, leaveTypeName,
                la.getStartDate(), la.getEndDate(), la.getDurationDays(),
                la.getReason(), la.getWorkDissemination(),
                la.getIsOverseas(), la.getContactDetails(),
                la.getIsHalfDay(), halfDay,
                la.getStatus().name(),
                mgrId, mgrName, la.getManagerComment());
    }

    private ResponseEntity<ApiResponse> err(Exception e) {
        return ResponseEntity.internalServerError()
                .body(new ApiResponse(false, "Error: " + e.getMessage(), null));
    }
}
