package nus_iss.LAPS.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.model.Role;
import nus_iss.LAPS.model.User;
import nus_iss.LAPS.repository.EmployeeRepository;
import nus_iss.LAPS.service.UserService;

@Controller
@RequestMapping("/users") //CRUD
public class UserController {

    @Autowired private UserService userService;
    @Autowired private EmployeeRepository employeeRepo;

    // ── Show login form ───────────────────────────────────────────────────────
    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    // ── Process login ─────────────────────────────────────────────────────────
    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        RedirectAttributes ra) {

        Optional<User> userOpt = userService.login(username, password);

        if (userOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Invalid username or password.");
            return "redirect:/login";
        }

        User user = userOpt.get();

        // Look up the Employee linked to this User
        Optional<Employee> empOpt = employeeRepo.findByUser(user);
        if (empOpt.isEmpty()) {
            ra.addFlashAttribute("error", "No employee profile found for this account.");
            return "redirect:/login";
        }

        Employee emp = empOpt.get();

        // Store key session attributes
        session.setAttribute("userId",   user.getUser_id());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("role",     user.getRole());
        session.setAttribute("empId",    emp.getEmp_id());
        session.setAttribute("employee", emp);

        return "redirect:/";
    }

    // ── Home / dashboard redirect ─────────────────────────────────────────────
    @GetMapping("/")
    public String home(HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }
        Role role = (Role) session.getAttribute("role");
        if (Role.MANAGER.equals(role)) {
            return "redirect:/leave/manager/pending";
        }
        if (Role.ADMIN.equals(role)) {
            return "redirect:/admin/employees";
        }
        return "redirect:/leave/history";
        
    }

    // ── Logout ────────────────────────────────────────────────────────────────
    @GetMapping("/logout")
    public String logout(HttpSession session, HttpServletResponse response) {
        session.invalidate();
        return "redirect:/login";
    }
    
// Loh Si Hua (Shannon) - 15/04/2026  
    
 // CREATE
    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.createUser(user, "system"); // replace with logged-in user
    }

    // READ ALL
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // READ ONE
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // UPDATE
    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User user) {
        return userService.updateUser(id, user, "system");
    }

    // DELETE
    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "User deleted successfully";
    }
}