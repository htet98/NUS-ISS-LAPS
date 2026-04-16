package nus_iss.LAPS.controller;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.model.User;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {
	private User user;
	private Employee employee;
	private List<Employee> subordinates;
}
