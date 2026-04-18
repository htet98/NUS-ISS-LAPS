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
class LeaveBalanceRepositoryTest {

    @Autowired
    private LeaveBalanceRepository repo;

    @Autowired
    private EmployeeRepository employeeRepo;

    @Autowired
    private LeaveTypeRepository leaveTypeRepo;

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

        e.setUser(user);

        return employeeRepo.save(e);
    }

    private LeaveType createLeaveType(NameTypeEnum type) {
        LeaveType lt = new LeaveType();
        lt.setName(type);
        lt.setDefaultDays(10.0);
        lt.setIsPaid(true);
        return leaveTypeRepo.save(lt);
    }

    private LeaveBalance createLeaveBalance(Employee emp, LeaveType lt) {
        LeaveBalance lb = new LeaveBalance();
        lb.setEmployee(emp);
        lb.setLeaveType(lt);
        lb.setTotalDays(10);
        lb.setUsedDays(2);
        return lb;
    }

    @Test
    void testSave() {
        Employee emp = createEmployee();
        LeaveType lt = createLeaveType(NameTypeEnum.ANNUAL);

        LeaveBalance saved = repo.save(createLeaveBalance(emp, lt));

        assertThat(saved.getLeaveBalanceId()).isNotNull();
    }

    @Test
    void testFindAll() {
        Employee emp = createEmployee();

        repo.save(createLeaveBalance(emp, createLeaveType(NameTypeEnum.ANNUAL)));
        repo.save(createLeaveBalance(emp, createLeaveType(NameTypeEnum.MEDICAL))); // different type

        assertThat(repo.findAll()).hasSize(2);
    }

    @Test
    void testDelete() {
        Employee emp = createEmployee();
        LeaveType lt = createLeaveType(NameTypeEnum.ANNUAL);

        LeaveBalance saved = repo.save(createLeaveBalance(emp, lt));

        repo.deleteById(saved.getLeaveBalanceId());

        assertThat(repo.findById(saved.getLeaveBalanceId())).isEmpty();
    }
}