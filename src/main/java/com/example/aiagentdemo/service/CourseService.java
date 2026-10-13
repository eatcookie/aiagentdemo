package com.example.aiagentdemo.service;

import com.example.aiagentdemo.entity.Course;
import com.example.aiagentdemo.mapper.CourseMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CourseService {
    private final CourseMapper courseMapper;

    public CourseService(CourseMapper courseMapper) {
        this.courseMapper = courseMapper;
    }

    public List<Course> listCourses() {
        return courseMapper.findAll();
    }
}