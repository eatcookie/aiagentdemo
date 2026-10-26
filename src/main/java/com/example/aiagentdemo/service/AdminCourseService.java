package com.example.aiagentdemo.service;

import com.example.aiagentdemo.common.BizException;
import com.example.aiagentdemo.common.PageResult;
import com.example.aiagentdemo.dto.CourseForm;
import com.example.aiagentdemo.entity.Course;
import com.example.aiagentdemo.mapper.CourseMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/** 管理后台课程服务：分页查询 + 增删改，含入参校验。 */
@Service
public class AdminCourseService {

    private final CourseMapper courseMapper;

    public AdminCourseService(CourseMapper courseMapper) {
        this.courseMapper = courseMapper;
    }

    public PageResult<Course> page(String keyword, int page, int size) {
        int p = Math.max(1, page);
        int s = Math.min(Math.max(1, size), 100);
        int offset = (p - 1) * s;
        List<Course> list = courseMapper.searchPaged(keyword, offset, s);
        long total = courseMapper.countSearch(keyword);
        return new PageResult<>(list, total, p, s);
    }

    public Course get(Long id) {
        Course course = courseMapper.findById(id);
        if (course == null) {
            throw new BizException(404, "课程不存在：id=" + id);
        }
        return course;
    }

    public Course create(CourseForm form) {
        validate(form);
        Course course = new Course();
        course.setName(form.getName().trim());
        course.setTeacher(trimToNull(form.getTeacher()));
        course.setCategory(trimToNull(form.getCategory()));
        courseMapper.insert(course);
        return courseMapper.findById(course.getId());
    }

    public Course update(Long id, CourseForm form) {
        validate(form);
        Course exist = courseMapper.findById(id);
        if (exist == null) {
            throw new BizException(404, "课程不存在：id=" + id);
        }
        Course course = new Course();
        course.setId(id);
        course.setName(form.getName().trim());
        course.setTeacher(trimToNull(form.getTeacher()));
        course.setCategory(trimToNull(form.getCategory()));
        courseMapper.updateById(course);
        return courseMapper.findById(id);
    }

    public void delete(Long id) {
        if (courseMapper.deleteById(id) == 0) {
            throw new BizException(404, "课程不存在：id=" + id);
        }
    }

    private void validate(CourseForm form) {
        if (form == null || form.getName() == null || form.getName().isBlank()) {
            throw new BizException("课程名称不能为空");
        }
    }

    private String trimToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}