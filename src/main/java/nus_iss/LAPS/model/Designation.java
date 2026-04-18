package nus_iss.LAPS.model;
/**
 * Represents the job designation category of an employee.
 *
 * ADMINISTRATIVE – Support and administrative staff
 * PROFESSIONAL   – Technical, specialist, and managerial staff
 *
 * Author: Htet Nandar (Grace)
 */
public enum Designation {
	
	    ADMINISTRATIVE("Administrative"),
	    PROFESSIONAL("Professional");

	    private final String displayName;

	    Designation(String displayName) {
	        this.displayName = displayName;
	    }

	    /** Human-readable label used in templates and reports. */
	    public String getDisplayName() {
	        return displayName;
	    }

	    /**
	     * Returns the human-readable display name so that Thymeleaf expressions
	     * like {@code th:text="${emp.designation}"} render "Administrative" or
	     * "Professional" rather than the raw enum constant name.
	     */
	    @Override
	    public String toString() {
	        return displayName;
	    }
	}
