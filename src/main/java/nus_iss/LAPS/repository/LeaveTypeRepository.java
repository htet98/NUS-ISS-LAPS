package nus_iss.LAPS.repository;

import nus_iss.LAPS.model.LeaveType;
import nus_iss.LAPS.model.NameTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for LeaveType entities.
 * Note: LeaveType.name is a NameTypeEnum (ANNUAL, MEDICAL, COMPENSATION),
 * so all name-based lookups use NameTypeEnum, not String.
 */
@Repository
public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {

    /** Find by enum constant, e.g. NameTypeEnum.ANNUAL */
    Optional<LeaveType> findByName(NameTypeEnum name);

    /** Find all paid leave types */
    List<LeaveType> findByIsPaidTrue();

    /** Find all unpaid leave types */
    List<LeaveType> findByIsPaidFalse();

    /** Check if a leave type with this name already exists */
    boolean existsByName(NameTypeEnum name);
    
    //Author: Junior added for LeaveType-manage
    boolean existsByNameAndLeaveTypeIdNot(NameTypeEnum name, Long leaveTypeId);
}
