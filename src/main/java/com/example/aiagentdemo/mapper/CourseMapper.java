package com.example.aiagentdemo.mapper;

import com.example.aiagentdemo.entity.Course;
import java.util.List;

public interface CourseMapper {
    List<Course> findAll();
}