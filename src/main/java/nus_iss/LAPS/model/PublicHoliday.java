package nus_iss.LAPS.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Represents a public holiday.
 * Used by LeaveApplicationValidator to exclude holidays from annual-leave working-day counts.
 *
 * Author: Htet Nandar(Grace)
 * Created on: 14/04/2026
 */
@Entity
@Table(name = "public_holidays")
public class PublicHoliday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "holiday_id")
    private Long holidayId;

    @NotBlank(message = "Holiday name is required.")
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull(message = "Holiday date is required.")
    @Column(name = "date", nullable = false, unique = true)
    private LocalDate date;

    @Column(name = "description")
    private String description;

    // ── Constructors ──────────────────────────────────────────────────────────
    public PublicHoliday() {}

    public PublicHoliday(String name, LocalDate date, String description) {
        this.name        = name;
        this.date        = date;
        this.description = description;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public Long getHolidayId()                   { return holidayId; }
    public void setHolidayId(Long holidayId)     { this.holidayId = holidayId; }

    public String getName()                      { return name; }
    public void setName(String name)             { this.name = name; }

    public LocalDate getDate()                   { return date; }
    public void setDate(LocalDate date)          { this.date = date; }

    public String getDescription()               { return description; }
    public void setDescription(String desc)      { this.description = desc; }

    @Override
    public String toString() {
        return String.format("PublicHoliday[id=%d, name='%s', date='%s']",
                holidayId, name, date);
    }
}
