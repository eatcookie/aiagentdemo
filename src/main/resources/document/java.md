# Java 零基础入门课程知识库

## 第 1 章 Java 简介与环境搭建

### 1.1 什么是 Java

Java 是一种广泛使用的编程语言，由 Sun Microsystems（现属于 Oracle 公司）于 1995 年推出。它的设计目标是"一次编写，到处运行"（Write Once, Run Anywhere），通过 Java 虚拟机（JVM）实现跨平台特性。

### 1.2 Java 的版本

- **Java SE（Standard Edition）**：标准版，用于开发桌面应用和命令行工具，是学习 Java 的基础。
- **Java EE（Enterprise Edition）**：企业版，用于开发 Web 应用和企业级系统，包含 Servlet、JSP、EJB 等技术。
- **Java ME（Micro Edition）**：微型版，用于嵌入式设备和移动设备开发。

### 1.3 JDK、JRE、JVM 的关系

- **JVM（Java Virtual Machine）**：Java 虚拟机，负责运行 Java 字节码，实现跨平台的核心。
- **JRE（Java Runtime Environment）**：Java 运行时环境，包含 JVM 和 Java 核心类库，用于运行 Java 程序。
- **JDK（Java Development Kit）**：Java 开发工具包，包含 JRE 以及编译器（javac）、调试器等开发工具。

> 简单记忆：JDK > JRE > JVM。开发需要 JDK，运行只需要 JRE。

### 1.4 安装 JDK

1. 访问 Oracle 官网或 OpenJDK 下载页面。
2. 选择对应操作系统的安装包（Windows 选 .exe，macOS 选 .dmg，Linux 选 .tar.gz）。
3. 双击安装，一路下一步，记住安装路径（如 `C:\Program Files\Java\jdk-17`）。
4. 配置环境变量：
    - 新建 `JAVA_HOME`，值为 JDK 安装路径。
    - 编辑 `Path`，追加 `%JAVA_HOME%\bin`。
5. 验证：打开命令行，输入 `java -version`，能看到版本号即成功。

### 1.5 第一个 Java 程序

```java
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
```

编译：`javac HelloWorld.java`
运行：`java HelloWorld`

---

## 第 2 章 基础语法

### 2.1 变量与数据类型

Java 是强类型语言，每个变量必须声明类型。

**基本数据类型（8 种）：**

| 类型       | 关键字  | 占用字节 | 取值范围             |
| ---------- | ------- | -------- | -------------------- |
| 字节型     | byte    | 1        | -128 ~ 127           |
| 短整型     | short   | 2        | -32768 ~ 32767       |
| 整型       | int     | 4        | -2^31 ~ 2^31-1       |
| 长整型     | long    | 8        | -2^63 ~ 2^63-1       |
| 单精度浮点 | float   | 4        | 约 7 位有效数字      |
| 双精度浮点 | double  | 8        | 约 15 位有效数字     |
| 字符型     | char    | 2        | 0 ~ 65535（Unicode） |
| 布尔型     | boolean | 1        | true 或 false        |

**引用数据类型：** 类（String）、接口、数组、枚举等。

### 2.2 运算符

- **算术运算符**：`+ - * / % ++ --`
- **赋值运算符**：`= += -= *= /=`
- **比较运算符**：`== != > < >= <=`
- **逻辑运算符**：`&& || !`
- **三元运算符**：`条件 ? 值1 : 值2`

### 2.3 流程控制

**if-else：**

```java
if (score >= 90) {
    System.out.println("优秀");
} else if (score >= 60) {
    System.out.println("及格");
} else {
    System.out.println("不及格");
}
```

**switch：**

```java
switch (day) {
    case 1: System.out.println("周一"); break;
    case 2: System.out.println("周二"); break;
    default: System.out.println("其他");
}
```

**for 循环：**

```java
for (int i = 0; i < 10; i++) {
    System.out.println(i);
}
```

**while 循环：**

```java
while (count < 5) {
    System.out.println(count);
    count++;
}
```

---

## 第 3 章 面向对象编程

### 3.1 类与对象

- **类（Class）**：是对象的模板，定义了属性和行为。
- **对象（Object）**：是类的实例，具体存在的个体。

```java
public class Student {
    // 属性（成员变量）
    String name;
    int age;
    
    // 方法（行为）
    public void study() {
        System.out.println(name + "正在学习");
    }
}

// 创建对象
Student s = new Student();
s.name = "小明";
s.study();
```

### 3.2 封装

封装是将对象的属性和实现细节隐藏，仅对外提供公共访问方式。

- 使用 `private` 修饰属性。
- 提供 `getter` 和 `setter` 方法。

```java
public class Person {
    private String name;
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
}
```

### 3.3 继承

继承是子类继承父类的属性和方法，实现代码复用。

- 使用 `extends` 关键字。
- Java 只支持单继承（一个类只能直接继承一个父类）。

```java
public class Animal {
    public void eat() {
        System.out.println("吃东西");
    }
}

public class Dog extends Animal {
    public void bark() {
        System.out.println("汪汪汪");
    }
}
```

### 3.4 多态

多态是同一行为具有多种表现形式。

- **编译时多态**：方法重载（Overload）。
- **运行时多态**：方法重写（Override）+ 父类引用指向子类对象。

```java
Animal a = new Dog();  // 父类引用指向子类对象
a.eat();               // 调用的是 Animal 的 eat
// a.bark();           // 编译错误，Animal 类型没有 bark 方法
```

### 3.5 抽象类与接口

**抽象类：**

- 使用 `abstract` 修饰。
- 不能实例化。
- 可以包含抽象方法（只有声明没有实现）和具体方法。

```java
public abstract class Shape {
    public abstract double area();  // 抽象方法
    
    public void print() {           // 具体方法
        System.out.println("面积：" + area());
    }
}
```

**接口：**

- 使用 `interface` 定义。
- 方法默认是 `public abstract`。
- 变量默认是 `public static final`。
- 一个类可以实现多个接口。

```java
public interface Flyable {
    void fly();
}

public class Bird implements Flyable {
    @Override
    public void fly() {
        System.out.println("鸟儿展翅飞翔");
    }
}
```

---

## 第 4 章 常用 API

### 4.1 String 类

String 是不可变的字符序列。

```java
String s1 = "hello";
String s2 = new String("hello");

// 常用方法
s1.length();          // 长度
s1.charAt(0);         // 取字符
s1.substring(0, 3);   // 截取
s1.equals(s2);        // 比较内容（不要用 ==）
s1.contains("ell");   // 是否包含
s1.split(",");        // 分割
```

### 4.2 StringBuilder

StringBuilder 是可变的字符序列，适合频繁修改字符串的场景。

```java
StringBuilder sb = new StringBuilder();
sb.append("Hello");
sb.append(" World");
String result = sb.toString();  // "Hello World"
```

### 4.3 集合框架

**List（有序可重复）：**

```java
List<String> list = new ArrayList<>();
list.add("Java");
list.add("Python");
list.get(0);  // "Java"
```

**Set（无序不重复）：**

```java
Set<String> set = new HashSet<>();
set.add("Java");
set.add("Java");  // 不会重复添加
```

**Map（键值对）：**

```java
Map<String, Integer> map = new HashMap<>();
map.put("Java", 1);
map.put("Python", 2);
map.get("Java");  // 1
```

---

## 第 5 章 异常处理

### 5.1 异常体系

- **Throwable**
    - **Error**：严重错误，程序无法处理（如 OutOfMemoryError）。
    - **Exception**：异常，程序可以处理。
        - **RuntimeException**：运行时异常（如 NullPointerException、ArrayIndexOutOfBoundsException）。
        - **Checked Exception**：编译时异常（如 IOException、SQLException）。

### 5.2 try-catch

```java
try {
    int result = 10 / 0;
} catch (ArithmeticException e) {
    System.out.println("除数不能为 0");
} finally {
    System.out.println("无论是否异常，都会执行");
}
```

### 5.3 throws 与 throw

- **throws**：声明方法可能抛出的异常。
- **throw**：手动抛出一个异常对象。

```java
public void checkAge(int age) throws IllegalArgumentException {
    if (age < 0) {
        throw new IllegalArgumentException("年龄不能为负数");
    }
}
```

---

## 第 6 章 多线程

### 6.1 创建线程的三种方式

**方式一：继承 Thread 类**

```java
class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println("线程执行中");
    }
}
MyThread t = new MyThread();
t.start();
```

**方式二：实现 Runnable 接口**

```java
class MyRunnable implements Runnable {
    @Override
    public void run() {
        System.out.println("线程执行中");
    }
}
Thread t = new Thread(new MyRunnable());
t.start();
```

**方式三：实现 Callable 接口（有返回值）**

```java
class MyCallable implements Callable<Integer> {
    @Override
    public Integer call() {
        return 123;
    }
}
FutureTask<Integer> task = new FutureTask<>(new MyCallable());
new Thread(task).start();
Integer result = task.get();
```

### 6.2 线程安全

当多个线程同时访问共享资源时，可能出现数据不一致问题。

**解决方案：**

- 使用 `synchronized` 关键字。
- 使用 `Lock` 接口（如 ReentrantLock）。
- 使用线程安全集合（如 ConcurrentHashMap）。

```java
public synchronized void add(int num) {
    count += num;  // 线程安全
}
```

---

## 第 7 章 IO 与文件操作

### 7.1 字节流

用于读写二进制数据（图片、视频等）。

- **InputStream / OutputStream**（抽象基类）
- **FileInputStream / FileOutputStream**

```java
// 读取文件
FileInputStream fis = new FileInputStream("test.txt");
int b;
while ((b = fis.read()) != -1) {
    System.out.print((char) b);
}
fis.close();
```

### 7.2 字符流

用于读写文本数据。

- **Reader / Writer**（抽象基类）
- **FileReader / FileWriter**

```java
// 写入文件
FileWriter fw = new FileWriter("test.txt");
fw.write("Hello, Java IO!");
fw.close();
```

### 7.3 try-with-resources

自动关闭资源，避免忘记 close()。

```java
try (FileInputStream fis = new FileInputStream("test.txt")) {
    // 使用 fis
} catch (IOException e) {
    e.printStackTrace();
}
// fis 自动关闭
```

---

## 第 8 章 数据库与 JDBC

### 8.1 什么是 JDBC

JDBC（Java Database Connectivity）是 Java 连接数据库的标准 API，通过它可以用统一的代码操作不同的数据库（MySQL、Oracle、PostgreSQL 等）。

### 8.2 JDBC 操作步骤

1. 加载驱动（MySQL 8.x 可省略）。
2. 获取连接（Connection）。
3. 创建 Statement / PreparedStatement。
4. 执行 SQL。
5. 处理结果集（ResultSet）。
6. 关闭资源。

```java
String url = "jdbc:mysql://localhost:3306/test?useSSL=false&serverTimezone=UTC";
String username = "root";
String password = "123456";

try (Connection conn = DriverManager.getConnection(url, username, password)) {
    String sql = "SELECT * FROM user WHERE id = ?";
    PreparedStatement ps = conn.prepareStatement(sql);
    ps.setInt(1, 1);
    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
        System.out.println(rs.getString("name"));
    }
} catch (SQLException e) {
    e.printStackTrace();
}
```

### 8.3 PreparedStatement 与 Statement 的区别

- **PreparedStatement**：预编译 SQL，防止 SQL 注入，性能更好。推荐使用。
- **Statement**：拼接 SQL 字符串，有 SQL 注入风险。不推荐使用。

---

## 第 9 章 Spring Boot 入门

### 9.1 什么是 Spring Boot

Spring Boot 是基于 Spring 框架的快速开发脚手架，核心理念是"约定优于配置"，通过自动配置（Auto Configuration）大幅简化 Spring 应用的搭建和开发。

### 9.2 创建 Spring Boot 项目

1. 访问 https://start.spring.io 。
2. 选择 Maven/Gradle、Java 版本。
3. 添加依赖（如 Spring Web、MyBatis、MySQL Driver）。
4. 下载解压，用 IDEA 打开。

### 9.3 项目结构

```
src/main/java/com/example/demo/
├── DemoApplication.java       # 启动类（含 main 方法）
├── controller/                # 控制器层（接收请求）
├── service/                   # 业务层（业务逻辑）
├── repository/ 或 mapper/     # 数据访问层
└── entity/ 或 model/          # 实体类
```

### 9.4 第一个 REST 接口

```java
@RestController
@RequestMapping("/api")
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello, Spring Boot!";
    }
}
```

### 9.5 常用注解

| 注解                       | 作用                                      |
| -------------------------- | ----------------------------------------- |
| `@SpringBootApplication`   | 启动类注解，包含 @EnableAutoConfiguration |
| `@RestController`          | 组合了 @Controller + @ResponseBody        |
| `@RequestMapping`          | 映射请求路径                              |
| `@GetMapping`              | 处理 GET 请求                             |
| `@PostMapping`             | 处理 POST 请求                            |
| `@Autowired`               | 自动注入依赖                              |
| `@Value`                   | 读取配置值                                |
| `@ConfigurationProperties` | 批量绑定配置                              |

---

## 第 10 章 MyBatis 持久层框架

### 10.1 什么是 MyBatis

MyBatis 是一个优秀的持久层框架，它封装了 JDBC 的繁琐操作，通过 XML 或注解的方式将 SQL 语句与 Java 代码分离，实现了"SQL 与代码解耦"。

### 10.2 MyBatis 核心组件

- **SqlSessionFactory**：会话工厂，负责创建 SqlSession。
- **SqlSession**：数据库会话，执行 SQL 的入口。
- **Mapper**：映射器接口，定义 SQL 操作的方法。
- **Mapper XML**：SQL 映射文件，编写具体 SQL。

### 10.3 使用步骤

1. 引入依赖（mybatis-spring-boot-starter）。
2. 配置数据源（application.yml）。
3. 定义实体类（Entity）。
4. 定义 Mapper 接口。
5. 编写 Mapper XML（SQL 语句）。
6. 在启动类添加 `@MapperScan` 注解。

### 10.4 Mapper 示例

```java
@Mapper
public interface UserMapper {
    @Select("SELECT * FROM user WHERE id = #{id}")
    User selectById(Long id);
    
    @Insert("INSERT INTO user(name, age) VALUES(#{name}, #{age})")
    int insert(User user);
}
```

### 10.5 动态 SQL

MyBatis 提供动态 SQL 标签，解决条件拼接问题：

- `<if>`：条件判断。
- `<choose> <when> <otherwise>`：多路选择。
- `<foreach>`：循环遍历。
- `<set>`：更新时动态拼接 SET。
- `<where>`：动态拼接 WHERE。

```xml
<select id="selectByCondition" resultType="User">
    SELECT * FROM user
    <where>
        <if test="name != null">
            AND name = #{name}
        </if>
        <if test="age != null">
            AND age = #{age}
        </if>
    </where>
</select>
```

---

## 第 11 章 Redis 缓存

### 11.1 什么是 Redis

Redis（Remote Dictionary Server）是一个开源的内存数据结构存储系统，可以用作数据库、缓存和消息中间件。支持字符串、哈希、列表、集合、有序集合等多种数据结构。

### 11.2 为什么用 Redis 做缓存

- **速度快**：数据存储在内存中，读写速度极快（10万+ QPS）。
- **减轻数据库压力**：热点数据从缓存读取，减少数据库查询。
- **支持过期**：可以设置键的过期时间，自动淘汰。

### 11.3 缓存读写策略

**Cache-Aside（旁路缓存）模式：**

1. 读：先查缓存 → 命中返回 → 未命中查数据库 → 写入缓存 → 返回。
2. 写：先更新数据库 → 再删除缓存（不是更新缓存）。

### 11.4 Spring Boot 整合 Redis

引入 `spring-boot-starter-data-redis` 依赖，使用 `RedisTemplate` 或 `@Cacheable` 注解。

```java
@Autowired
private StringRedisTemplate redisTemplate;

// 写入缓存
redisTemplate.opsForValue().set("key", "value", 1, TimeUnit.HOURS);

// 读取缓存
String value = redisTemplate.opsForValue().get("key");
```

### 11.5 缓存问题

- **缓存穿透**：查询不存在的数据，绕过缓存打到数据库。解决：缓存空值、布隆过滤器。
- **缓存击穿**：热点 key 过期瞬间，大量请求打到数据库。解决：互斥锁、逻辑过期。
- **缓存雪崩**：大量 key 同时过期。解决：过期时间加随机值、Redis 集群。

---

## 第 12 章 Elasticsearch 搜索引擎

### 12.1 什么是 Elasticsearch

Elasticsearch（简称 ES）是一个分布式、RESTful 风格的搜索和分析引擎，基于 Apache Lucene 构建。它可以实现全文搜索、结构化搜索、数据分析等功能。

### 12.2 核心概念

| 概念             | 类比 MySQL                |
| ---------------- | ------------------------- |
| Index（索引）    | Database（数据库）        |
| Type（类型）     | Table（表，7.x 后已废弃） |
| Document（文档） | Row（行）                 |
| Field（字段）    | Column（列）              |
| Mapping（映射）  | Schema（表结构）          |

### 12.3 ES 8.x 新特性

- 默认开启安全认证（用户名密码 + HTTPS）。
- 引入向量字段（dense_vector），支持向量相似度搜索（KNN）。
- 推荐使用 Java API Client（取代已废弃的 High Level REST Client）。

### 12.4 向量搜索（RAG 核心）

ES 8 支持 `dense_vector` 字段类型，可以存储文本的向量表示，实现语义搜索：

```json
PUT /knowledge_base
{
  "mappings": {
    "properties": {
      "content": { "type": "text" },
      "embedding": { "type": "dense_vector", "dims": 1536 }
    }
  }
}
```

查询时使用 KNN 检索：

```json
POST /knowledge_base/_search
{
  "knn": {
    "field": "embedding",
    "query_vector": [0.1, 0.2, ...],
    "k": 5
  }
}
```

---

## 第 13 章 RAG（检索增强生成）

### 13.1 什么是 RAG

RAG（Retrieval-Augmented Generation，检索增强生成）是一种将信息检索与大模型生成相结合的架构。流程：

1. **索引阶段**：将知识库文档切片 → 向量化 → 存入向量数据库（如 ES）。
2. **检索阶段**：用户提问 → 向量化 → 在向量数据库中检索 Top-K 相似文档。
3. **生成阶段**：将检索到的文档作为上下文 + 用户问题 → 发送给大模型 → 生成回答。

### 13.2 为什么需要 RAG

- 大模型的知识有截止日期，不知道最新信息。
- 大模型不了解你的私有数据（公司内部文档、课程资料等）。
- RAG 让大模型"有据可依"，减少幻觉。

### 13.3 文档切片策略

- **按固定长度切片**：每 500 字切一段，重叠 50 字。
- **按语义切片**：按段落、章节、句号切分。
- **按结构切片**：按 Markdown 标题层级切分。

### 13.4 Embedding 模型选择

- **通义千问 Embedding**：`text-embedding-v2`，1536 维。
- **OpenAI Embedding**：`text-embedding-3-small`，1536 维。
- **Hugging Face 本地模型**：`bge-large-zh`，1024 维。

### 13.5 RAG 接口设计

```java
@PostMapping("/api/ai/ask-rag")
public String askRag(@RequestBody AskRequest req) {
    // 1. 将用户问题向量化
    float[] queryVector = embeddingService.embed(req.getQuestion());
    
    // 2. 在 ES 中做 KNN 检索
    List<String> contexts = esService.knnSearch(queryVector, 5);
    
    // 3. 拼接 Prompt
    String prompt = buildPrompt(contexts, req.getQuestion());
    
    // 4. 调用大模型生成回答
    return aiService.chat(prompt);
}
```

### 13.6 Prompt 模板

```
你是一个专业的课程助教。请根据以下参考资料回答用户的问题。
如果参考资料中没有相关信息，请如实告知"抱歉，我的知识库中没有相关内容"。

参考资料：
{context}

用户问题：{question}

请基于参考资料，给出准确、简洁的回答：
```

---

## 第 14 章 项目实战：AI 智能答疑系统

### 14.1 系统架构

```
┌──────────┐     ┌──────────────┐     ┌─────────────┐     ┌──────────┐
│  前端/Apifox │────▶│  Spring Boot  │────▶│  Elasticsearch │────▶│  大模型  │
└──────────┘     │  (Controller) │     │  (向量检索)   │     │ (生成回答)│
                 │  (Service)    │     └─────────────┘     └──────────┘
                 │  (Redis缓存)  │
                 └──────────────┘
```

### 14.2 技术栈

- **后端**：Spring Boot 3.x
- **搜索**：Elasticsearch 8.x（dense_vector 向量检索）
- **缓存**：Redis 7.x（缓存高频问答）
- **大模型**：通义千问 / DeepSeek（OpenAI 兼容接口）
- **Embedding**：通义千问 text-embedding-v2
- **数据库**：MySQL 8.x（用户、课程数据）
- **ORM**：MyBatis

### 14.3 接口清单

| 方法 | 路径                  | 说明                       |
| ---- | --------------------- | -------------------------- |
| POST | /api/ai/ask           | 普通 AI 问答（直连大模型） |
| POST | /api/ai/ask-rag       | RAG 问答（检索 + 生成）    |
| POST | /api/knowledge/ingest | 文档入库（切片 + 向量化）  |
| GET  | /api/health           | 健康检查                   |

### 14.4 限流与兜底

- 使用 Redis 实现滑动窗口限流（每用户每分钟最多 10 次）。
- API Key 未配置时返回友好提示。
- 大模型调用失败时返回兜底话术。
- 缓存命中直接返回，减轻大模型压力。