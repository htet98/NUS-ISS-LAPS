package nus_iss.LAPS.api;

import nus_iss.LAPS.dto.ApiResponse;
import nus_iss.LAPS.dto.LeaveRequest;
import nus_iss.LAPS.dto.LeaveResponse;
import nus_iss.LAPS.dto.ManagerActionRequest;
import nus_iss.LAPS.model.*;
import nus_iss.LAPS.repository.EmployeeRepository;
import nus_iss.LAPS.repository.LeaveTypeRepository;
import nus_iss.LAPS.service.EmailService;
import nus_iss.LAPS.service.LeaveApplicationService;
import nus_iss.LAPS.util.ApiRoutes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API controller for Leave Application management.
 *
 * This controller acts as the single business-logic gateway for all leave operations.
 * Both the MVC web layer (LeaveApplicationController) and any external API clients
 * call this controller — business rules are only written once here, not duplicated
 * across both layers.
 *
 * All responses use ResponseEntity<?> with a consistent ApiResponse wrapper body:
 *   - Success:  ApiResponse(true,  "message", data)
 *   - Failure:  ApiResponse(false, "error message", null)
 *
 * HTTP status codes used:
 *   200 OK         – read operations and state changes (approve, reject, cancel)
 *   201 CREATED    – successful new leave submission
 *   400 BAD REQUEST – validation errors or illegal state (e.g., editing an approved leave)
 *   403 FORBIDDEN  – employee attempting to modify another employee's record
 *   500 INTERNAL   – unexpected runtime errors
 *
 * Employee endpoints : submit, update, delete, cancel, view own history
 * Manager  endpoints : approve, reject, view pending/all/recent decisions
 *
 * Author: Htet Nandar (Grace)
 */
@RestController
@RequestMapping(ApiRoutes.LEAVE)
public class LeaveApplicationRestController {

    // Service layer handles all business logic (validation, balance deduction, email notifications).
    @Autowired private LeaveApplicationService leaveApplicationService;

    // Used to look up the LeaveType entity by ID when building a LeaveApplication from a request.
    @Autowired private LeaveTypeRepository     leaveTypeRepo;

    // Used to look up the Employee entity by ID — API callers pass numeric IDs, not objects.
    @Autowired private EmployeeRepository      employeeRepo;

    // Service for sending email notifications to employees and managers
    @Autowired private EmailService            emailService;

    // ─────────────────────────────────────────────────────────────────────────
    // EMPLOYEE — Read: personal leave history
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns all non-deleted leave applications for the given employee
     * in the current calendar year, sorted by start date descending.
     *
     * GET /api/leave/employee/{employeeId}
     */
    @GetMapping(ApiRoutes.LEAVE_EMPLOYEE)
    public ResponseEntity<?> getEmployeeLeave(@PathVariable Long employeeId) {
        try {
            Employee emp = findEmployee(employeeId);
            // Map each LeaveApplication entity to a LeaveResponse DTO before returning.
            // DTOs prevent Hibernate lazy-load issues when serialising to JSON.
            List<LeaveResponse> resp = leaveApplicationService
                    .getPersonalLeaveHistory(emp).stream()
                    .map(LeaveResponse::fromEntity).collect(Collectors.toList());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return err(e);
        }
    }

    /**
     * Returns the employee's leave history filtered by optional status and/or leave type.
     * If neither filter is provided, returns all non-deleted records (same as getEmployeeLeave).
     *
     * GET /api/leave/employee/{employeeId}/filter?status=APPROVED&leaveTypeId=1
     */
    @GetMapping(ApiRoutes.LEAVE_FILTER)
    public ResponseEntity<?> getEmployeeLeaveFiltered(
            @PathVariable Long employeeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long leaveTypeId) {
        try {
            Employee emp = findEmployee(employeeId);

            // Convert the status string to the enum. IllegalArgumentException is thrown
            // if the string does not match any enum constant — caught below and returned as 400.
            LeaveStatus statusEnum = status != null ? LeaveStatus.valueOf(status) : null;

            // Apply the most specific filter available:
            // both status + type → single filtered query
            // only status or only type → respective partial filter
            List<LeaveApplication> apps = leaveTypeId != null
                    ? leaveApplicationService.getPersonalLeaveHistoryByStatusAndType(emp, statusEnum, leaveTypeId)
                    : leaveApplicationService.getPersonalLeaveHistoryByStatus(emp, statusEnum);

            return ResponseEntity.ok(apps.stream().map(LeaveResponse::fromEntity).collect(Collectors.toList()));
        } catch (IllegalArgumentException e) {
            // Invalid status string (e.g. "APPROVEDX") → return 400 with a clear message
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Invalid status: " + e.getMessage(), null));
        } catch (Exception e) {
            return err(e);
        }
    }

    /**
     * Returns a single leave application by its ID.
     * Returns 404 if the application does not exist.
     *
     * GET /api/leave/{id}
     */
    @GetMapping(ApiRoutes.LEAVE_ID)
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            // findById returns an Optional. Map to 200 OK if present, 404 if absent.
            return leaveApplicationService.findById(id)
                    .map(la -> ResponseEntity.ok((Object) LeaveResponse.fromEntity(la)))
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new ApiResponse(false, "Leave application not found", null)));
        } catch (Exception e) {
            return err(e);
        }
    }

    /**
     * Returns a paginated list of the employee's leave history.
     * Supports optional filtering by status and/or leave type.
     * Results are sorted by start date descending (most recent first).
     *
     * GET /api/leave/employee/{employeeId}/paginated?page=0&size=10&status=APPROVED&leaveTypeId=1
     */
    @GetMapping(ApiRoutes.LEAVE_EMPLOYEE + "/paginated")
    public ResponseEntity<?> getEmployeeLeavePaginated(
            @PathVariable Long employeeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long leaveTypeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Employee emp = findEmployee(employeeId);

            // Build pageable with descending sort so the most recent leave appears first.
            Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").descending());

            // Choose the correct service method based on which filters are provided.
            // Four combinations: both, status-only, type-only, none.
            Page<LeaveApplication> applicationPage;
            if (status != null && !status.isBlank() && leaveTypeId != null) {
                LeaveStatus leaveStatus = LeaveStatus.valueOf(status);
                applicationPage = leaveApplicationService
                        .getPersonalLeaveHistoryByStatusAndTypePaginated(emp, leaveStatus, leaveTypeId, pageable);
            } else if (status != null && !status.isBlank()) {
                LeaveStatus leaveStatus = LeaveStatus.valueOf(status);
                applicationPage = leaveApplicationService
                        .getPersonalLeaveHistoryByStatusPaginated(emp, leaveStatus, pageable);
            } else if (leaveTypeId != null) {
                applicationPage = leaveApplicationService
                        .getPersonalLeaveHistoryByTypePaginated(emp, leaveTypeId, pageable);
            } else {
                applicationPage = leaveApplicationService.getPersonalLeaveHistoryPaginated(emp, pageable);
            }

            // Build a flat map response instead of returning the Page object directly,
            // so the JSON structure is predictable and doesn't expose Spring internals.
            Map<String, Object> response = new HashMap<>();
            response.put("content", applicationPage.getContent().stream()
                    .map(LeaveResponse::fromEntity).collect(Collectors.toList()));
            response.put("currentPage", page);
            response.put("size", size);
            response.put("totalPages", applicationPage.getTotalPages());
            response.put("totalItems", applicationPage.getTotalElements());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Invalid status: " + e.getMessage(), null));
        } catch (Exception e) {
            return err(e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MANAGER — Read: view team leave
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a paginated list of APPLIED and UPDATED (pending) leave applications
     * submitted by the manager's direct subordinates, sorted by start date descending.
     *
     * GET /api/leave/manager/{managerId}/pending/paginated?page=0&size=10
     */
    @GetMapping(ApiRoutes.MANAGER_PENDING + "/paginated")
    public ResponseEntity<?> getPendingForManagerPaginated(
            @PathVariable Long managerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Employee mgr = findEmployee(managerId);
            Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").descending());
            Page<LeaveApplication> applicationPage = leaveApplicationService
                    .getPendingApplicationsForManagerPaginated(mgr, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("content", applicationPage.getContent().stream()
                    .map(LeaveResponse::fromEntity).collect(Collectors.toList()));
            response.put("currentPage", page);
            response.put("size", size);
            response.put("totalPages", applicationPage.getTotalPages());
            response.put("totalItems", applicationPage.getTotalElements());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return err(e);
        }
    }

    /**
     * Returns a paginated list of all non-deleted leave applications from the manager's
     * direct subordinates (all statuses except DELETED), sorted by start date descending.
     *
     * GET /api/leave/manager/{managerId}/subordinates/paginated?page=0&size=10
     */
    @GetMapping(ApiRoutes.MANAGER_SUBORDINATES + "/paginated")
    public ResponseEntity<?> getSubordinateHistoryPaginated(
            @PathVariable Long managerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Employee mgr = findEmployee(managerId);
            Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").descending());
            Page<LeaveApplication> applicationPage = leaveApplicationService
                    .getAllApplicationsForManagerPaginated(mgr, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("content", applicationPage.getContent().stream()
                    .map(LeaveResponse::fromEntity).collect(Collectors.toList()));
            response.put("currentPage", page);
            response.put("size", size);
            response.put("totalPages", applicationPage.getTotalPages());
            response.put("totalItems", applicationPage.getTotalElements());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return err(e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EMPLOYEE — Write: submit, update, delete, cancel
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Submits a new leave application.
     *
     * Builds a LeaveApplication entity from the incoming LeaveRequest DTO,
     * then passes it to the service which runs validation (LeaveApplicationValidator)
     * and sets the initial status:
     *   - APPLIED  if the employee has a supervisor (normal flow)
     *   - APPROVED immediately if the employee has no supervisor (top-level manager)
     *
     * Returns 201 CREATED on success, 400 if validation fails.
     *
     * POST /api/leave
     * Body: { employeeId, leaveTypeId, startDate, endDate, reason, ... }
     */
    @PostMapping
    public ResponseEntity<?> submitLeave(@RequestBody LeaveRequest req) {
        try {
            // Guard against missing required IDs before attempting DB lookups.
            if (req.employeeId() == null || req.leaveTypeId() == null)
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "employeeId and leaveTypeId are required", null));

            Employee emp = findEmployee(req.employeeId());

            // Convert the flat DTO into a proper JPA entity with relationships resolved.
            LeaveApplication la = buildFromRequest(req, emp);

            LeaveApplication saved = leaveApplicationService.submitLeaveApplication(la);
            
            // Send email notification to manager
            emailService.sendApplicationNotification(saved);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Leave application submitted", LeaveResponse.fromEntity(saved)));
        } catch (IllegalArgumentException e) {
            // Validator errors (e.g. insufficient balance, past date) come back as 400.
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        } catch (Exception e) {
            return err(e);
        }
    }

    /**
     * Updates an existing leave application.
     *
     * Only APPLIED or UPDATED applications can be modified.
     * The service verifies that the requesting employee owns the application.
     * On success the status changes from APPLIED/UPDATED to UPDATED.
     *
     * Returns 400 if validation fails or the application is not in an editable state.
     * Returns 403 if the employee does not own the application.
     *
     * PUT /api/leave/{id}
     * Body: { employeeId, leaveTypeId, startDate, endDate, reason, ... }
     */
    @PutMapping(ApiRoutes.LEAVE_ID)
    public ResponseEntity<?> updateLeave(@PathVariable Long id, @RequestBody LeaveRequest req) {
        try {
            Employee emp = findEmployee(req.employeeId());
            LeaveApplication la = buildFromRequest(req, emp);

            // Set the ID so the service knows which record to update (not create a new one).
            la.setLeaveApplicationId(id);

            LeaveApplication updated = leaveApplicationService.updateLeaveApplication(la);
            return ResponseEntity.ok(new ApiResponse(true, "Leave application updated", LeaveResponse.fromEntity(updated)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            // IllegalArgumentException: validation failure (e.g. balance exceeded)
            // IllegalStateException: wrong status (e.g. trying to edit an APPROVED application)
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        } catch (SecurityException e) {
            // Employee trying to edit another employee's application → 403 Forbidden
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponse(false, e.getMessage(), null));
        } catch (Exception e) {
            return err(e);
        }
    }

    /**
     * Soft-deletes a leave application by setting its status to DELETED.
     *
     * Only APPLIED or UPDATED applications can be deleted.
     * APPROVED applications must be cancelled instead (see cancelLeave).
     * The record is never physically removed — it is kept for audit purposes.
     *
     * Returns 403 if the employee does not own the application.
     *
     * DELETE /api/leave/{id}?employeeId=5
     */
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

    /**
     * Cancels an APPROVED leave application.
     *
     * This is different from delete: only APPROVED leaves can be cancelled,
     * and cancellation restores the employee's leave balance (deduction is reversed).
     * Status changes from APPROVED to CANCELLED.
     *
     * Returns 400 if the application is not in APPROVED status.
     * Returns 403 if the employee does not own the application.
     *
     * PATCH /api/leave/{id}/cancel?employeeId=5
     */
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

    // ─────────────────────────────────────────────────────────────────────────
    // MANAGER — Read: non-paginated versions (kept for backward compatibility)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns all pending (APPLIED or UPDATED) leave applications for the manager's
     * direct subordinates, without pagination.
     *
     * GET /api/leave/manager/{managerId}/pending
     */
    @GetMapping(ApiRoutes.MANAGER_PENDING)
    public ResponseEntity<?> getPendingForManager(@PathVariable Long managerId) {
        try {
            Employee mgr = findEmployee(managerId);
            return ResponseEntity.ok(
                    leaveApplicationService.getPendingApplicationsForManager(mgr)
                            .stream().map(LeaveResponse::fromEntity).collect(Collectors.toList()));
        } catch (Exception e) { return err(e); }
    }

    /**
     * Returns all non-deleted leave from the manager's direct subordinates, without pagination.
     *
     * GET /api/leave/manager/{managerId}/subordinates
     */
    @GetMapping(ApiRoutes.MANAGER_SUBORDINATES)
    public ResponseEntity<?> getSubordinateHistory(@PathVariable Long managerId) {
        try {
            Employee mgr = findEmployee(managerId);
            return ResponseEntity.ok(
                    leaveApplicationService.getAllApplicationsForManager(mgr)
                            .stream().map(LeaveResponse::fromEntity).collect(Collectors.toList()));
        } catch (Exception e) { return err(e); }
    }

    /**
     * Returns the last 10 APPROVED or REJECTED decisions made by this manager,
     * sorted by most recently updated.
     * Used on the manager dashboard to show recent activity.
     *
     * GET /api/leave/manager/{managerId}/recent
     */
    @GetMapping(ApiRoutes.MANAGER_RECENT)
    public ResponseEntity<?> getRecentDecisions(@PathVariable Long managerId) {
        try {
            Employee mgr = findEmployee(managerId);
            return ResponseEntity.ok(
                    leaveApplicationService.getRecentDecisionsByManager(mgr)
                            .stream().map(LeaveResponse::fromEntity).collect(Collectors.toList()));
        } catch (Exception e) { return err(e); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MANAGER — Write: approve, reject
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Approves a pending leave application.
     *
     * The service:
     *   1. Validates the application is in APPLIED or UPDATED status.
     *   2. Deducts the leave balance from the employee's entitlement.
     *   3. Sets status to APPROVED and records the approving manager.
     *   4. Sends an approval email notification to the employee.
     *
     * Returns 400 if the application cannot be approved (wrong status or not found).
     *
     * PATCH /api/leave/{id}/approve?managerId=3
     */
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

    /**
     * Rejects a pending leave application.
     *
     * A manager comment (reason for rejection) is mandatory — the service throws
     * IllegalArgumentException if the comment is blank.
     * The service sends a rejection email notification to the employee.
     * No balance is deducted on rejection.
     *
     * Returns 400 if the comment is missing or the application cannot be rejected.
     *
     * PATCH /api/leave/{id}/reject?managerId=3
     * Body: { "comment": "Team at minimum staffing that week." }
     */
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

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Loads a fresh Employee entity from the database by ID.
     * All REST endpoints receive a numeric employeeId — this method resolves it
     * to the full entity needed by the service and validator layers.
     * Throws IllegalArgumentException (→ 400) if the ID does not exist.
     */
    private Employee findEmployee(Long empId) {
        return employeeRepo.findById(empId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + empId));
    }

    /**
     * Converts a LeaveRequest DTO into a LeaveApplication JPA entity.
     *
     * Resolves foreign-key IDs to their full entities:
     *   - leaveTypeId → LeaveType entity
     *   - employeeId  → Employee entity (already resolved and passed in)
     *
     * halfDayPeriod is only set when isHalfDay is true — otherwise it stays null
     * so we never store a stale AM/PM value when the employee is not taking a half-day.
     *
     * Note: status is NOT set here. It is set by the service based on business rules
     * (APPLIED for normal submission, APPROVED for top-level managers without a supervisor).
     */
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

        // Default isOverseas and isHalfDay to false if the caller did not provide them,
        // so the entity never holds a null for these Boolean fields.
        leaveApplication.setOverseas(req.isOverseas() != null ? req.isOverseas() : false);
        leaveApplication.setContactDetails(req.contactDetails());
        leaveApplication.setIsHalfDay(req.isHalfDay() != null ? req.isHalfDay() : false);

        // Only parse and set halfDayPeriod when the half-day checkbox was ticked.
        // Throws 400 if the string is not a valid HalfDayPeriod enum value (MORNING / AFTERNOON).
        if (req.halfDayPeriod() != null && Boolean.TRUE.equals(req.isHalfDay())) {
            try {
                leaveApplication.setHalfDayPeriod(HalfDayPeriod.valueOf(req.halfDayPeriod().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid half-day period. Must be MORNING or AFTERNOON.");
            }
        }
        return leaveApplication;
    }

    /**
     * Wraps any unexpected exception into a 500 Internal Server Error response.
     * Keeps all endpoint catch blocks concise while still surfacing the error message.
     */
    private ResponseEntity<ApiResponse> err(Exception e) {
        return ResponseEntity.internalServerError()
                .body(new ApiResponse(false, "Error: " + e.getMessage(), null));
    }
}
