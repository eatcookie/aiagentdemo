package com.example.aiagentdemo.mapper;

import com.example.aiagentdemo.entity.Course;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CourseMapper {
    List<Course> findAll();

    List<Course> search(@Param("keyword") String keyword);

    // 管理后台：分页 + 关键词查询 / 计数 / 单条 / 增删改
    List<Course> searchPaged(@Param("keyword") String keyword,
                             @Param("offset") int offset,
                             @Param("limit") int limit);

    long countSearch(@Param("keyword") String keyword);

    Course findById(@Param("id") Long id);

    int insert(Course course);

    int updateById(Course course);

    int deleteById(@Param("id") Long id);
}