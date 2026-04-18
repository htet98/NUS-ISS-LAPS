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
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepo;

    // Helper method to create valid Employee
   private Employee createEmployee() {
	   
	String unique = System.currentTimeMillis() + "_" + Math.random();
	
    Employee e = new Employee();

    e.setFirst_name("Test");
    e.setLast_name("User");
    e.setEmail("TestUser_" + unique + "@test.com"); // optional but safer
    e.setPhoneNumber("12345678");
    e.setDepartment("IT");
    e.setDesignation(Designation.ADMINISTRATIVE);
    e.setHire_date(LocalDate.now());
    e.setEmployeeStatus(EmployeeStatus.ACTIVE);
    e.setCreatedBy("system");
    e.setCreatedWhen(LocalDateTime.now());

    // FIXED USER CREATION
    User user = new User();
    user.setUsername("user_" + unique);
    user.setEmail("user_" + unique + "@test.com");
    user.setPassword("123");
    user.setRole(Role.ADMIN);
    user.setCreatedby("system");
    user.setUpdatedby("system");
    user.setActive(true);

    e.setUser(user);

    return e;
}

    @Test
    void testSaveEmployee() {
        Employee saved = employeeRepo.save(createEmployee());

        assertThat(saved.getEmp_id()).isNotNull();
    }

    @Test
    void testFindById() {
        Employee saved = employeeRepo.save(createEmployee());

        Employee found = employeeRepo.findById(saved.getEmp_id()).orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getEmail()).contains("TestUser_");
    }

    @Test
    void testFindAll() {
        employeeRepo.save(createEmployee());
        employeeRepo.save(createEmployee());

        List<Employee> list = employeeRepo.findAll();

        assertThat(list.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void testDeleteEmployee() {
        Employee saved = employeeRepo.save(createEmployee());

        employeeRepo.deleteById(saved.getEmp_id());

        assertThat(employeeRepo.findById(saved.getEmp_id())).isEmpty();
    }
}