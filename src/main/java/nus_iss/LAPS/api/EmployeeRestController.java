package nus_iss.LAPS.api;

import lombok.RequiredArgsConstructor;
import nus_iss.LAPS.model.Employee;
import nus_iss.LAPS.service.EmployeeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor

/**
 * Author: Junior
 * Created on: 15/04/2026
 **/

public class EmployeeRestController {
			
		private final EmployeeService eService;
		
		//employee list
        @GetMapping
        public List<Employee> getAll() {
            return eService.findAllEmployees();
        }

        @GetMapping("/{id}")
        public ResponseEntity<?> getById(@PathVariable Long id) {
            return eService.findEmployee(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }


    //create Employee
        @PostMapping
        public ResponseEntity<?> create(@RequestBody Employee employee) {
            return ResponseEntity.ok(eService.createEmployee(employee));
        }
		
		//update Employee
        @PutMapping("/{id}")
        public ResponseEntity<?> update(@PathVariable Long id,
                                        @RequestBody Employee employee) {
            employee.setEmp_id(id);
            return ResponseEntity.ok(eService.changeEmployee(employee));
        }
		
		//delete Employee
        @DeleteMapping("/{id}")
        public ResponseEntity<?> delete(@PathVariable Long id) {
            eService.findEmployee(id).ifPresent(eService::removeEmployee);
            return ResponseEntity.ok().build();
        }
}
