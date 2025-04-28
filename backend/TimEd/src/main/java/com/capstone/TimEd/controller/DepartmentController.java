package com.capstone.TimEd.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.capstone.TimEd.model.Department;
import com.capstone.TimEd.service.DepartmentService;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @PostMapping
    public String createDepartment(@RequestBody Department department) throws ExecutionException, InterruptedException {
        return departmentService.createDepartment(department);
    }

    @GetMapping("/{id}")
    public Department getDepartment(@PathVariable String id) throws ExecutionException, InterruptedException {
        return departmentService.getDepartment(id);
    }

    @PutMapping("/{id}")
    public String updateDepartment(@PathVariable String id, @RequestBody Department department) throws ExecutionException, InterruptedException {
        return departmentService.updateDepartment(id, department);
    }

    @DeleteMapping("/{id}")
    public String deleteDepartment(@PathVariable String id) {
        return departmentService.deleteDepartment(id);
    }

    @GetMapping
    public List<Department> getAllDepartments() throws ExecutionException, InterruptedException {
        return departmentService.getAllDepartments();
    }
}
