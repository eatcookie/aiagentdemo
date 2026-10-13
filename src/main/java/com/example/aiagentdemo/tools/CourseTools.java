package com.example.aiagentdemo.tools;

import com.example.aiagentdemo.entity.Course;
import com.example.aiagentdemo.mapper.CourseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/** 课程查询工具：数据来自 MySQL（t_course），由模型按需调用。 */
@Component
@RequiredArgsConstructor
public class CourseTools {

    private final CourseMapper courseMapper;

    @Tool(name = "query_courses",
            description = "按课程名或课程分类查询课程列表，返回课程 id、名称、讲师、分类。"
                    + "当用户询问有哪些课程、某分类下有哪些课、某课程的信息时调用。"
                    + "返回空列表表示没有匹配的课程。")
    public List<Course> queryCourses(
            @ToolParam(description = "课程名或分类关键词，例如 Java、数据库、编程语言；不传则返回全部课程",
                    required = false)
            String keyword) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return courseMapper.search(kw);
    }
}