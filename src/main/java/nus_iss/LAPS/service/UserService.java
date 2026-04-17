package nus_iss.LAPS.service;

import nus_iss.LAPS.model.User;
import nus_iss.LAPS.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime; //CRUD
import java.util.List; //CRUD
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
    
 // For CRUD - 
 // Loh Si Hua (Shannon) - 15/04/2026
 // CREATE
    public User createUser(User user, String createdBy) {
        user.setCreatedby(createdBy);
        user.setCreatedwhen(LocalDateTime.now());
        return userRepository.save(user);
    }

    // READ ALL
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // READ ONE
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // UPDATE
    public User updateUser(Long id, User updatedUser, String updatedBy) {
        return userRepository.findById(id).map(user -> {

            user.setUsername(updatedUser.getUsername());
            user.setRole(updatedUser.getRole());

            // Only update password if provided
            if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                user.setPassword(updatedUser.getPassword());
            }

            user.setUpdatedby(updatedBy);
            user.setUpdatedwhen(LocalDateTime.now());

            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("User not found"));
    }

    // DELETE
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
