package com.example.aiagentdemo.entity;
//import lombok.Data;
//@Data


public class Course {
    private Long id;
    private String name;
    private String teacher;
    private String category;

    // 省略 getter 和 setter（你可以让 IDEA 自动生成：右键 -> Generate -> Getter and Setter）
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTeacher() { return teacher; }
    public void setTeacher(String teacher) { this.teacher = teacher; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

}

