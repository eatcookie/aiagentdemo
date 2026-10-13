package com.example.aiagentdemo.mapper;

import com.example.aiagentdemo.entity.Course;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CourseMapper {
    List<Course> findAll();

    List<Course> search(@Param("keyword") String keyword);
}