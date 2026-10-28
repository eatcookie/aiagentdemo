CREATE TABLE IF NOT EXISTS t_course (
    id       BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '课程ID',
    name     VARCHAR(100) NOT NULL COMMENT '课程名称',
    teacher  VARCHAR(50)  COMMENT '讲师',
    category VARCHAR(50)  COMMENT '课程分类（Function Call query_courses 模糊匹配用）',
    UNIQUE KEY uk_course_name (name)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '课程表';

CREATE TABLE IF NOT EXISTS t_user (
    id       BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50)  NOT NULL COMMENT '登录名',
    password VARCHAR(100) NOT NULL COMMENT 'BCrypt 哈希，禁止明文',
    role     VARCHAR(20)  DEFAULT 'USER' COMMENT '角色：ADMIN / USER',
    UNIQUE KEY uk_user_username (username)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

INSERT INTO t_course (name, teacher, category) VALUES
    ('Java 基础', '张老师', '编程语言'),
    ('数据结构与算法', '李老师', '计算机基础'),
    ('MySQL 数据库', '王老师', '数据库'),
    ('Spring Boot 实战', '赵老师', '编程语言');


