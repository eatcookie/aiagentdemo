package com.example.aiagentdemo.mapper;

import com.example.aiagentdemo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    @Select("SELECT * FROM t_user WHERE username = #{username}")
    User findByUsername(String username);
}