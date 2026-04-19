package nus_iss.LAPS.controller;

import jakarta.servlet.http.HttpSession;
import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.model.Role;
import nus_iss.LAPS.model.User;
import nus_iss.LAPS.repository.EmployeeRepository;
import nus_iss.LAPS.service.UserService;
import nus_iss.LAPS.util.GlobalConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmployeeRepository employeeRepo;

    // ROOT → go to login
    @GetMapping("/")
    public String root() {
        return GlobalConstants.REDIRECT_LOGIN;
    }

    // Login page
    @GetMapping("/login")
    public String loginForm() {
        return GlobalConstants.VIEW_LOGIN;
    }

    // Process login
    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        RedirectAttributes ra) {

        Optional<User> userOpt = userService.login(username, password);

        if (userOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Invalid username or password.");
            return GlobalConstants.REDIRECT_LOGIN;
        }

        User user = userOpt.get();

        // ADMIN users don't need employee record
        if (Role.ADMIN.equals(user.getRole())) {
            // Store session for admin
            session.setAttribute("userId", user.getUser_id());
            session.setAttribute("username", user.getUsername());
            session.setAttribute("role", user.getRole());
            
            System.out.println("Logged in admin user: " + user.getUsername());
            return GlobalConstants.REDIRECT_ADMIN_HIERARCHY;
        }

        // EMPLOYEE and MANAGER users require employee record
        Optional<Employee> empOpt = employeeRepo.findByUser(user);
        if (empOpt.isEmpty()) {
            ra.addFlashAttribute("error", "No employee profile found. Please contact administrator.");
            return GlobalConstants.REDIRECT_LOGIN;
        }

        Employee emp = empOpt.get();

        // Store session for employee/manager
        session.setAttribute("userId", user.getUser_id());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("role", user.getRole());
        session.setAttribute("empId", emp.getEmp_id());
        session.setAttribute("employee", emp);
        
        System.out.println("Logged in user role: " + session.getAttribute("role"));
        
        // Redirect based on role
        if (Role.MANAGER.equals(user.getRole())) {
            return "redirect:/leave/manager/history/pending";
        } else {
            return "redirect:/leave/history";
        }
    }

    // Logout
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return GlobalConstants.REDIRECT_LOGIN;
    }
}