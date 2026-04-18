package nus_iss.LAPS.repository;

import nus_iss.LAPS.model.PublicHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, Long> {

    /** Fetch all holidays between two dates (inclusive) — used for working-day duration count. */
    List<PublicHoliday> findByDateBetween(LocalDate start, LocalDate end);

    /** Check whether a specific date is a public holiday — used by isWorkingDay() in the validator. */
    boolean existsByDate(LocalDate date);

    /** Optional: find a holiday by its exact date. */
    Optional<PublicHoliday> findByDate(LocalDate date);
}
