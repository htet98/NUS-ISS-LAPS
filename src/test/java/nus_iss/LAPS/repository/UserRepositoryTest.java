
package nus_iss.LAPS.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import jakarta.transaction.Transactional;
import nus_iss.LAPS.model.*;


@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
	    "spring.datasource.url=jdbc:h2:mem:testdb",
	    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
	})
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepo;

    private User createUser() {
        String unique = System.currentTimeMillis() + "_" + Math.random();

        User u = new User();
        u.setUsername("user_" + unique);
        u.setEmail("user_" + unique + "@test.com");
        u.setPassword("123");
        u.setRole(Role.ADMIN);
        u.setCreatedby("system");
        u.setUpdatedby("system");
        u.setActive(true);

        return u;
    }

    @Test
    void testSaveUser() {
        User saved = userRepo.save(createUser());

        assertThat(saved.getUser_id()).isNotNull();
    }

    @Test
    void testFindById() {
        User saved = userRepo.save(createUser());

        User found = userRepo.findById(saved.getUser_id()).orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getUsername()).startsWith("user_");
    }

    @Test
    void testFindAll() {
        userRepo.save(createUser());
        userRepo.save(createUser());

        List<User> users = userRepo.findAll();

		assertThat(users).hasSize(2);
    }

    @Test
    void testDeleteUser() {
        User saved = userRepo.save(createUser());

        userRepo.deleteById(saved.getUser_id());

        assertThat(userRepo.findById(saved.getUser_id())).isEmpty();
    }
}