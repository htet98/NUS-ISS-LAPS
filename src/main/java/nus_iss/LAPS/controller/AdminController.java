package nus_iss.LAPS.controller;

import jakarta.servlet.http.HttpSession;
import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.model.Role;
import nus_iss.LAPS.repository.EmployeeRepository;
import nus_iss.LAPS.util.BreadcrumbItem;
import nus_iss.LAPS.util.GlobalConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping(GlobalConstants.ROUTE_ADMIN)
public class AdminController {

    @Autowired
    private EmployeeRepository employeeRepo;

    // Check if the current user is an admin
    private boolean isAdmin(HttpSession session) {
        Role role = (Role) session.getAttribute("role");
        return Role.ADMIN.equals(role);
    }

    @GetMapping(GlobalConstants.ROUTE_ADMIN_HIERARCHY)
    public String listHierarchy(Model model, HttpSession session) {
        if (!isAdmin(session)) return GlobalConstants.REDIRECT_LOGIN;

        List<Employee> employees = employeeRepo.findAll();
        // Add breadcrumbs
        List<BreadcrumbItem> breadcrumbs = List.of(
                new BreadcrumbItem("LAPS", null),
                new BreadcrumbItem("Leave Approval Hierarchy", null)
        );
        model.addAttribute("employees", employees);
        return GlobalConstants.VIEW_ADMIN_HIERARCHY;
    }

    @GetMapping(GlobalConstants.ROUTE_ADMIN_HIERARCHY_EDIT)
    public String editHierarchy(@PathVariable Long id, Model model, HttpSession session) {
        if (!isAdmin(session)) return GlobalConstants.REDIRECT_LOGIN;

        Optional<Employee> empOpt = employeeRepo.findById(id);
        if (empOpt.isEmpty()) {
            return GlobalConstants.REDIRECT_ADMIN_HIERARCHY;
        }

        model.addAttribute("employee", empOpt.get());
        model.addAttribute("managers", employeeRepo.findAllManagers());
        // Add breadcrumbs
        List<BreadcrumbItem> breadcrumbs = List.of(
                new BreadcrumbItem("LAPS", null),
                new BreadcrumbItem("Leave Approval Hierarchy", GlobalConstants.ROUTE_ADMIN_HIERARCHY),
                new BreadcrumbItem("Edit Hierarchy", null)
        );
        return GlobalConstants.VIEW_ADMIN_EDIT_HIERARCHY;
    }

    @PostMapping(GlobalConstants.ROUTE_ADMIN_HIERARCHY_SAVE)
    public String saveHierarchy(@RequestParam Long employeeId,
                                @RequestParam(required = false) Long supervisorId,
                                HttpSession session,
                                RedirectAttributes ra) {
        if (!isAdmin(session)) return GlobalConstants.REDIRECT_LOGIN;

        Optional<Employee> empOpt = employeeRepo.findById(employeeId);
        if (empOpt.isPresent()) {
            Employee supervisor = null;
            
            if (supervisorId != null) {
                // Avoid circular dependency (simple check)
                if (employeeId.equals(supervisorId)) {
                    ra.addFlashAttribute(GlobalConstants.FLASH_ERROR, "An employee cannot be their own supervisor.");
                    return GlobalConstants.REDIRECT_ADMIN_HIERARCHY_EDIT + employeeId;
                }
                
                Optional<Employee> supOpt = employeeRepo.findById(supervisorId);
                if (supOpt.isPresent()) {
                    supervisor = supOpt.get();
                }
            }
            
            // Use JPQL update to avoid triggering full entity validation
            employeeRepo.updateSupervisor(employeeId, supervisor);
            ra.addFlashAttribute(GlobalConstants.FLASH_SUCCESS, "Approval hierarchy updated successfully.");
        }

        return GlobalConstants.REDIRECT_ADMIN_HIERARCHY;
    }
}
