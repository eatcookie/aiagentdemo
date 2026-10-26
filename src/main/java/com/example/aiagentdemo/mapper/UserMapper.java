package com.example.aiagentdemo.mapper;

import com.example.aiagentdemo.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper {
    @Select("SELECT * FROM t_user WHERE username = #{username}")
    User findByUsername(String username);

    @Select("SELECT COUNT(*) FROM t_user WHERE username = #{username}")
    long countByUsername(String username);

    @Insert("INSERT INTO t_user (username, password, role) VALUES (#{username}, #{password}, #{role})")
    int insert(User user);

    // 列表查询：显式不选 password，避免密码泄露到前端
    @Select("SELECT id, username, role FROM t_user ORDER BY id")
    List<User> listAll();
}
