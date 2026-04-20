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
class LeaveApplicationRepositoryTest {

    @Autowired
    private LeaveApplicationRepository leaveAppRepo;

    @Autowired
    private EmployeeRepository employeeRepo;

    @Autowired
    private LeaveTypeRepository leaveTypeRepo;

    // Create Employee (REUSE your fixed version)
    private Employee createEmployee() {
        String unique = System.currentTimeMillis() + "_" + Math.random();

        Employee e = new Employee();
        e.setFirst_name("Test");
        e.setLast_name("User");
        e.setEmail("emp_" + unique + "@test.com");
        e.setPhoneNumber("12345678");
        e.setDepartment("IT");
        e.setDesignation(Designation.ADMINISTRATIVE);
        e.setHire_date(LocalDate.now());
        e.setEmployeeStatus(EmployeeStatus.ACTIVE);
        e.setCreatedBy("system");
        e.setCreatedWhen(LocalDateTime.now());

        User user = new User();
        user.setUsername("user_" + unique);
        user.setEmail("user_" + unique + "@test.com");
        user.setPassword("123");
        user.setRole(Role.ADMIN);
        user.setCreatedby("system");
        user.setUpdatedby("system");
        user.setActive(true);

        e.setUser(user);

        return employeeRepo.save(e); // IMPORTANT: save first
    }

    // Create LeaveType
    private LeaveType createLeaveType() {
        LeaveType lt = new LeaveType();
        
		lt.setName(NameTypeEnum.ANNUAL); // adjust field name if needed
		
        return leaveTypeRepo.save(lt);
    }

    // Create LeaveApplication
    private LeaveApplication createLeaveApplication() {
        Employee emp = createEmployee();
        LeaveType lt = createLeaveType();

        LeaveApplication la = new LeaveApplication();
        la.setEmployee(emp);
        la.setLeaveType(lt);
        la.setStartDate(LocalDate.now());
        la.setEndDate(LocalDate.now().plusDays(2));
        la.setDurationDays(2.0);
        la.setReason("Vacation");
        la.setIsOverseas(false);

        return la;
    }

    @Test
    void testSaveLeaveApplication() {
        LeaveApplication saved = leaveAppRepo.save(createLeaveApplication());

        assertThat(saved.getLeaveApplicationId()).isNotNull();
    }

    @Test
    void testFindById() {
        LeaveApplication saved = leaveAppRepo.save(createLeaveApplication());

        LeaveApplication found = leaveAppRepo.findById(saved.getLeaveApplicationId()).orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getReason()).isEqualTo("Vacation");
    }

    @Test
    void testFindAll() {
        leaveAppRepo.save(createLeaveApplication());
        leaveAppRepo.save(createLeaveApplication());

        List<LeaveApplication> list = leaveAppRepo.findAll();

        assertThat(list).hasSize(2);
    }

    @Test
    void testDeleteLeaveApplication() {
        LeaveApplication saved = leaveAppRepo.save(createLeaveApplication());

        leaveAppRepo.deleteById(saved.getLeaveApplicationId());

        assertThat(leaveAppRepo.findById(saved.getLeaveApplicationId())).isEmpty();
    }
}