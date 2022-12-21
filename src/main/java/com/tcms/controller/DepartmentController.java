package com.tcms.controller;

import com.tcms.helper.pojo.CustomResponseMessage;
import com.tcms.models.Department;
import com.tcms.repositories.DepartmentRepository;
import com.tcms.services.DepartmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController()
@CrossOrigin()
@RequestMapping("/department")
public class DepartmentController {


    private final DepartmentRepository departmentRepository;
    private final DepartmentService departmentService;

    public DepartmentController(DepartmentRepository departmentRepository, DepartmentService departmentService) {
        this.departmentRepository = departmentRepository;
        this.departmentService = departmentService;
    }

    @GetMapping("")
    public ResponseEntity<Object> getDepartments(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Pageable paging = PageRequest.of(page, size);
        Page<Department> departmentList = departmentRepository.findAll(paging);
        if (departmentList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        }
        return ResponseEntity.status(HttpStatus.OK).body(departmentService.getDepartmentListResponse(departmentList));
    }

    @GetMapping(path = "/depName/{departmentName}")
    public ResponseEntity<Object> getDepartmentByDepartmentName(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @PathVariable String departmentName) {
        Pageable paging = PageRequest.of(page, size);
        Page<Department> departmentList = departmentRepository.findByDepNameIsContaining(departmentName, paging);
        if (departmentList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(departmentService.getDepartmentListResponse(departmentList));
        }
    }

    @GetMapping(path = "/id/{id}")
    public ResponseEntity<Object> getDepartmentById(@PathVariable int id) {
        Department department = departmentRepository.findByDepId(id);
        if (department == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found.\n");
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(department);
        }
    }

    @PostMapping("")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> saveDepartment(@RequestBody Department department) {
        if (department == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Department Info Not Found inside body.\n");
        }
        try {
            departmentRepository.save(department);
            return ResponseEntity.status(HttpStatus.OK).body(department);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CustomResponseMessage(new Date(), "Error", e.getCause().getCause().getLocalizedMessage()));
        }
    }

    @PutMapping("/{depId}")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> editDepartment(@RequestBody Department department, @PathVariable int depId) {
        if (department == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Department Info Not Found inside body.\n");
        }
        try {
            Department departments = departmentRepository.findByDepId(depId);
            if (departments == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Department Not Found in database.\n");
            }
            departments.setDepName(department.getDepName() == null ? departments.getDepName() : department.getDepName());
            departmentRepository.save(departments);
            return ResponseEntity.status(HttpStatus.OK).body(departments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CustomResponseMessage(new Date(), "Error", e.getCause().getCause().getLocalizedMessage()));
        }
    }

    @DeleteMapping("/{depId}")
    @SuppressWarnings("Duplicates")
    public ResponseEntity<Object> deleteDepartment(@PathVariable String depId) {
        if (depId == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("depId not found for delete operation.\n");
        }
        Department department = departmentRepository.findByDepId(Integer.parseInt(depId));
        if (department == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new CustomResponseMessage(new Date(), "Error", "Provided department not found for Delete operation!"));
        }
        departmentRepository.deleteById(Integer.parseInt(depId));
        return ResponseEntity.status(HttpStatus.OK).body(new CustomResponseMessage(new Date(), "Success", "Department Deleted Successfully!"));
    }
}
