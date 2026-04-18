
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
class PublicHolidayRepositoryTest {

    @Autowired
    private PublicHolidayRepository repo;

    private PublicHoliday createHoliday() {
        String unique = String.valueOf(System.currentTimeMillis());

        PublicHoliday ph = new PublicHoliday();
        ph.setName("Holiday " + unique);
        ph.setDate(LocalDate.now().plusDays((long)(Math.random()*100))); // avoid duplicate
        ph.setDescription("Test holiday");

        return ph;
    }

    @Test
    void testSave() {
        PublicHoliday saved = repo.save(createHoliday());

        assertThat(saved.getHolidayId()).isNotNull();
    }

    @Test
    void testFindById() {
        PublicHoliday saved = repo.save(createHoliday());

        PublicHoliday found = repo.findById(saved.getHolidayId()).orElse(null);

        assertThat(found).isNotNull();
    }

    @Test
    void testFindAll() {
        repo.save(createHoliday());
        repo.save(createHoliday());

        assertThat(repo.findAll()).hasSize(2);
    }

    @Test
    void testDelete() {
        PublicHoliday saved = repo.save(createHoliday());

        repo.deleteById(saved.getHolidayId());

        assertThat(repo.findById(saved.getHolidayId())).isEmpty();
    }
}