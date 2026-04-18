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
class LeaveTypeRepositoryTest {

    @Autowired
    private LeaveTypeRepository repo;

    private LeaveType createLeaveType() {
        LeaveType lt = new LeaveType();
        lt.setName(NameTypeEnum.ANNUAL);
        lt.setDescription("Annual Leave"); 
        lt.setDefaultDays(14.0); 
        lt.setIsPaid(true);
        return lt;
    }

    @Test
    void testSave() {
        LeaveType saved = repo.save(createLeaveType());

        assertThat(saved.getLeaveTypeId()).isNotNull();
    }

    @Test
    void testFindById() {
        LeaveType saved = repo.save(createLeaveType());

        LeaveType found = repo.findById(saved.getLeaveTypeId()).orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo(NameTypeEnum.ANNUAL);
    }

    @Test
    void testFindAll() {
        repo.save(createLeaveType());
        repo.save(createLeaveType());

        assertThat(repo.findAll()).hasSize(2);
    }

    @Test
    void testDelete() {
        LeaveType saved = repo.save(createLeaveType());

        repo.deleteById(saved.getLeaveTypeId());

        assertThat(repo.findById(saved.getLeaveTypeId())).isEmpty();
    }
}