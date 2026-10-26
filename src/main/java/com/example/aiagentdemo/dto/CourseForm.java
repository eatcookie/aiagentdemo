package com.example.aiagentdemo.dto;

/** 课程新增/编辑表单（id 由路径参数提供，不放这里）。 */
public class CourseForm {
    private String name;
    private String teacher;
    private String category;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTeacher() { return teacher; }
    public void setTeacher(String teacher) { this.teacher = teacher; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}