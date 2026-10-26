package com.example.aiagentdemo.controller;

import com.example.aiagentdemo.common.PageResult;
import com.example.aiagentdemo.common.Result;
import com.example.aiagentdemo.dto.CourseForm;
import com.example.aiagentdemo.entity.Course;
import com.example.aiagentdemo.service.AdminCourseService;
import org.springframework.web.bind.annotation.*;

/** 管理后台课程接口（受 AdminAuthInterceptor 保护，登录接口除外）。 */
@RestController
@RequestMapping("/api/admin/courses")
public class AdminCourseController {

    private final AdminCourseService adminCourseService;

    public AdminCourseController(AdminCourseService adminCourseService) {
        this.adminCourseService = adminCourseService;
    }

    @GetMapping
    public Result<PageResult<Course>> list(@RequestParam(required = false) String keyword,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return Result.ok(adminCourseService.page(keyword, page, size));
    }

    @GetMapping("/{id}")
    public Result<Course> get(@PathVariable Long id) {
        return Result.ok(adminCourseService.get(id));
    }

    @PostMapping
    public Result<Course> create(@RequestBody CourseForm form) {
        return Result.ok(adminCourseService.create(form));
    }

    @PutMapping("/{id}")
    public Result<Course> update(@PathVariable Long id, @RequestBody CourseForm form) {
        return Result.ok(adminCourseService.update(id, form));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminCourseService.delete(id);
        return Result.ok();
    }
}