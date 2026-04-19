package nus_iss.LAPS.service;

import nus_iss.LAPS.model.User;
import nus_iss.LAPS.repository.UserRepository;
import nus_iss.LAPS.util.GlobalConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // Htet Nandar(Grace) - 12/04/2026
    /**
     * Plain-text password comparison (demo only — use BCrypt in production).
     */
    @Transactional(propagation = Propagation.SUPPORTS,
                   isolation   = Isolation.READ_COMMITTED,
                   readOnly    = true)
    public Optional<User> login(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(u -> u.getPassword().equals(password))
                .filter(u -> Boolean.TRUE.equals(u.getActive()));
    }

    @Transactional(propagation = Propagation.SUPPORTS,
                   isolation   = Isolation.READ_COMMITTED,
                   readOnly    = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    // Loh Si Hua - 18/04/2026 - CRUD
    //LIST + SEARCH + PAGINATION
    public Page<User> getUsers(String keyword, int page) {

        Pageable pageable = PageRequest.of(page, GlobalConstants.DEFAULT_PAGE_SIZE_INT);

        if (keyword != null && !keyword.isEmpty()) {
            return userRepository.findByUsernameContaining(keyword, pageable);
        }

        return userRepository.findAll(pageable);
    }
    //SAVE USER
    public void saveUser(User user) {
    	if (user.getUser_id() != null) {
    	    User existing = userRepository.findById(user.getUser_id()).orElseThrow();

    	    if (user.getPassword() == null || user.getPassword().isEmpty()) {
    	        user.setPassword(existing.getPassword());
    	    }
    	}

        userRepository.save(user);
    }
    //EDIT PAGE
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    //Delete USER
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    // Get all users (for employee dropdown)
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    @Transactional(readOnly = true) //Junior
    public List<User> getUnassignedUsers() {
        return userRepository.findUnassignedUsers();
    }
}