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
}