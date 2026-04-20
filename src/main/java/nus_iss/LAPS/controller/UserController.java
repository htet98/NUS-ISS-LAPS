package nus_iss.LAPS.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.model.Role;
import nus_iss.LAPS.model.User;
import nus_iss.LAPS.repository.EmployeeRepository;
import nus_iss.LAPS.service.UserService;
import nus_iss.LAPS.util.GlobalConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; //CRUD
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.*; //CRUD
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Page; //CRUD
import org.springframework.data.domain.PageRequest; //CRUD
import org.springframework.data.domain.Pageable; //CRUD

import java.util.List; //CRUD

import java.util.Optional;

    //Loh Si Hua - 18 Apr 2026 - CRUD
    
    @Controller
    @RequestMapping("/users")
    public class UserController {

        @Autowired private UserService userService;

        // LIST + SEARCH + PAGINATION
        @GetMapping("/manage-user")
        public String manageUser(
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(required = false) String keyword,
                Model model,
                HttpSession session) {

            Role role = (Role) session.getAttribute("role");

            if (role == null || !Role.ADMIN.equals(role)) {
                return GlobalConstants.REDIRECT_LOGIN;
            }

            Page<User> userPage = userService.getUsers(keyword, page);

            model.addAttribute("userPage", userPage);
            model.addAttribute("keyword", keyword);

            return GlobalConstants.VIEW_MANAGE_USER;
        }

        // CREATE PAGE
        @GetMapping("/create")
        public String showCreateForm(Model model, HttpSession session) {
            model.addAttribute("user", new User());
            model.addAttribute("roles", Role.values());
            return "users/create-user";   // ✅ FIXED
        }
//Loh Si Hua - 19/4/2026
        // SAVE USER
        @PostMapping("/save")
        public String saveUser(@ModelAttribute User user,
                               RedirectAttributes ra,
                               Model model,
                               HttpSession session) {

            Role role = (Role) session.getAttribute("role");

            if (role == null || !Role.ADMIN.equals(role)) {
                return GlobalConstants.REDIRECT_LOGIN;
            }

            try {
                userService.saveUser(user);

                ra.addFlashAttribute("success", "User saved successfully!");
                return "redirect:/users/manage-user";

            } catch (Exception e) {
                model.addAttribute("error", "Username or email already exists!");
                model.addAttribute("roles", Role.values());

                // IMPORTANT: return correct page based on create/edit
                if (user.getUser_id() == null) {
                    return "users/create-user";
                } else {
                    return "users/edit-user";
                }
            }
        }
       
        // EDIT PAGE
        @GetMapping("/edit/{id}")
        public String editUser(@PathVariable Long id, Model model, HttpSession session) {
            User user = userService.getUserById(id);
            model.addAttribute("user", user);
            model.addAttribute("roles", Role.values());
            return "users/edit-user";     // ✅ FIXED
        }

        // DELETE USER
        @GetMapping("/delete/{id}")
        public String deleteUser(@PathVariable Long id,
                                 RedirectAttributes ra,
                                 HttpSession session) {

            Role role = (Role) session.getAttribute("role");

            if (role == null || !Role.ADMIN.equals(role)) {
                return GlobalConstants.REDIRECT_LOGIN;
            }

            userService.deleteUser(id);

            ra.addFlashAttribute("success", "User deleted!");
            return "redirect:/users/manage-user";
        }
    }
