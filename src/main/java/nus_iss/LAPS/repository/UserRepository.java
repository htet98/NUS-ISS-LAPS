package nus_iss.LAPS.repository;

import nus_iss.LAPS.model.User;

import org.springframework.data.domain.Page; //CRUD-User
import org.springframework.data.domain.Pageable; //CRUD-User
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

//Loh Si Hua - 18/04/2026 - CRUD
Page<User> findByUsernameContaining(String keyword, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.employee IS NULL and u.role <> 'ADMIN'") // Junior
    List<User> findUnassignedUsers();
}