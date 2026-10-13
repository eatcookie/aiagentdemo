package com.example.aiagentdemo.controller;

import com.example.aiagentdemo.entity.Course;
import com.example.aiagentdemo.service.CourseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class CourseController {
    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/course/list")
    public List<Course> list() {
        return courseService.listCourses();
    }
}

