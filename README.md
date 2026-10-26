# Spring AI RAG Demo — Java Course Q&A with Elasticsearch Hybrid Retrieval

> 基于 **Spring AI + Alibaba DashScope + Elasticsearch** 的课程文档问答（RAG）教学项目。
> 亮点：**中文无插件全文检索**（内置 ngram bigram）+ **向量/全文混合检索**（RRF 融合）+ **Function Call 轻量工具路由** + **全参数可配置**。

## Status · 当前进度

本项目仍在持续开发中，README 采用**活文档**方式随代码一起提交演进。

| 状态 | 模块 |
| --- | --- |
| ✅ Done | 文档按标题结构化切块入库、DashScope 向量化、ES 向量存储（自定义 mapping）、混合检索（向量 + BM25）、RRF 融合去重、`/api/ai/ask` 返回答案与引用来源、一键入库自测脚本 |
| ✅ Done | **Function Call 轻量工具路由**：模型自主决定调用 `query_courses`（MySQL 按课程名/分类查询）或 `query_question_history`（Redis 按用户查提问历史） |
| ✅ Done | **管理后台（Vue SPA + Token 鉴权）**：登录/注册/登出、课程增删改查分页、AI 答疑、检索调试、知识库入库、提问历史、用户管理，入口 `/admin` |
| 🚧 In Progress | 检索质量调优（阈值/权重）、多文档类型支持（PDF 依赖已引入）、回答流式输出 |
| 📋 Planned | 多轮对话记忆、重排（rerank）、检索效果评估集、简易前端页面、Docker 化部署 |

详见 [Roadmap](#roadmap-后续计划)。

## What's New · 本次改动（Function Call 轻量版）

> 已本地实测通过：课程查询、提问历史、RAG 知识问答三条链路均正常。

- **两个工具**（`@Tool` 声明，注册在 `/api/ai/ask` 的同一次模型调用中，不引入多 Agent / ReAct）：
    - `query_courses`：按课程名或分类查 MySQL `t_course`（`name` / `category` 模糊匹配；空关键词返回全部，最多 20 条）。
    - `query_question_history`：查 Redis List `ai:history:{user}`（最新在前，最多 20 条）。
- **提问自动留痕**：每次 `/api/ai/ask` 在回答生成后写入 Redis（保留最近 50 条），避免「查历史」类问题把本次提问也算进去。
- **`user` 字段**：请求体新增可选 `user`（缺省 `default`），经 `UserContext`（ThreadLocal）传给历史工具；后续接入登录 Token 只需替换取值来源。
- **提示词升级**：`SYSTEM_PROMPT` 明确「课程列表 → 必须调 `query_courses`；提问历史 → 必须调 `query_question_history`；其余知识题严格依据【课程片段】」。
- **新增代码**：`tools/CourseTools`、`tools/HistoryTools`、`support/UserContext`；`Course` / `CourseMapper` / `CourseMapper.xml` 增加 `category` 支持。
- **数据库变更**（必须执行一次）：

        ALTER TABLE t_course ADD COLUMN category VARCHAR(50) DEFAULT NULL COMMENT '课程分类' AFTER teacher;

- **降级策略**：Redis 异常时历史记录/查询只打 warn 日志，不影响问答主流程。

工具型提问示例（`user` 可省略）：

    $body = '{"question":"帮我看Java分类有哪些课","user":"tom"}'
    Invoke-RestMethod -Uri 'http://localhost:8080/api/ai/ask' -Method POST `
      -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($body))
    # 中文乱码时参考 scripts/NOTES.md 第 2 节（PS 5.1 需手动按 UTF-8 解码）


## Highlights · 核心特性

- **结构优先切块**：自行解析 Markdown 的 `#` / `##` / `###` 标题层级，标题既写入 `metadata.title`（供检索加权），也保留在正文中；超长小节再按 `max-chars + overlap` 二次切分。
- **无插件的中文 BM25**：ES 索引使用内置 `ngram` bigram 分析器（本机 ES 不支持 `cjk` filter，也无需安装 IK 插件），中文关键词检索真正生效。
- **混合检索 + RRF 融合**：向量语义召回与全文精确召回按 `score(d) = Σ 1/(rrfK + rank)` 融合，规避两路分数量纲不一致的问题；任一路失败自动降级为单路结果。
- **轻量 Function Call**：`@Tool` 声明「查课程（MySQL）」与「查提问历史（Redis）」两个工具，与 RAG 在同一次 `ChatClient` 调用中注册，由模型按问题自主判断是否调用；不引入多 Agent / ReAct 框架。
- **去噪门槛**：`minimum_should_match=50%` 过滤只蹭到「什么/是什」等泛词 bigram 的离题小节。
- **可溯源**：响应除答案外，返回引用片段的标题、文件名与摘要，便于核对。
- **零硬编码密钥**：所有密钥走环境变量占位符，`application.yml` 可安全公开（模板见 [.env.example](.env.example)）。

## Tech Stack · 技术栈

| 组件 | 版本 / 说明 |
| --- | --- |
| Java | 17（Temurin） |
| Spring Boot | 3.2.5 |
| Spring AI | 1.0.0 GA |
| Spring AI Alibaba DashScope | 1.0.0.2 · chat: `qwen-plus`，embedding: `text-embedding-v4`（1024 维） |
| Elasticsearch | 8.x，客户端 `elasticsearch-java` 8.17.0，向量字段 `dense_vector` + `int8_hnsw` + cosine |
| MyBatis + MySQL | `mybatis-spring-boot-starter` 3.0.3（示例业务接口） |
| Spring Security | 演示/健康/AI 接口放行；后台 `/api/admin/**` 由 `AdminAuthInterceptor` 校验 Token（详见 API Endpoints） |
| Redis | 本地 `6379`：提问历史 List（Function Call 工具）+ 登录 Token |


## Architecture · 架构与流程

```text
┌──────────────── Ingest 入库（手动触发 /api/ai/reingest） ────────────────┐
document/java.md → 按标题结构切块(+二次切分) → 注入 title/filename
                → DashScope Embedding(1024d) → Elasticsearch 索引(向量 + ngram 全文)

┌──────────────── Query 问答（POST /api/ai/ask） ─────────────────────────┐
question ─┬─→ VectorStore.similaritySearch(向量 top-k, 阈值)   ┐
          └─→ ES multi_match(BM25, content + title^2, msm=50%) ┴─→ RRF 融合 → 去重截断 max-sources
                                                                       ↓
                     【课程片段】+【问题】+ 两个 @Tool → qwen-plus 自主决策
                                                                       ↓
        ├─ 实时数据类问题 → 调 query_courses / query_question_history → 据工具结果作答
        └─ 知识类问题     → 严格依据【课程片段】作答（片段不足则说明未找到）
                                                                       ↓
                                                                    answer + sources
```

启动时 `ElasticsearchIndexInitializer` 会用 `src/main/resources/es/document-index-mapping.json` 预建索引（已存在则跳过），因此 `spring.ai.vectorstore.elasticsearch.initialize-schema=false`。

## Prerequisites · 环境要求

- JDK 17、Maven（或直接用仓库自带的 `mvnw` / `mvnw.cmd`）
- Elasticsearch 8.x 运行于 `http://localhost:9200`（开启 Basic 认证）
- MySQL 8 运行于 `localhost:3306`，库名 `aiagent`
- MySQL 的 `t_course` 表需包含 `category` 列（Function Call 课程查询依赖，DDL 见 What's New）
- Redis 运行于 `localhost:6379`
- 阿里云百炼（DashScope）API Key

## Quick Start · 快速开始

### 1. 准备环境变量

密钥一律不写入仓库，本地按下述变量提供（完整清单与写法见 [.env.example](.env.example)）：

| 变量 | 用途 |
| --- | --- |
| `DASHSCOPE_API_KEY` | 大模型与向量化 |
| `MYSQL_PASSWORD` | MySQL 密码 |
| `SPRING_ELASTICSEARCH_PASSWORD` | Elasticsearch 密码（用户名默认 `newadmin`） |
| `AI_ES_USER` / `AI_ES_PASS` | 仅 `scripts/reingest-and-test.ps1 -RecreateIndex` 删索引时需要 |

IDEA 中在 Run/Debug Configuration → Environment variables 填入即可。

### 2. 启动应用

```powershell
.\mvnw.cmd spring-boot:run
```

启动日志出现「已创建索引 spring-ai-document-index-v2…」表示 ES mapping 就绪（首次会打印创建成功，之后为已存在跳过）。

### 3. 文档入库

```powershell
# 覆盖式入库：先清空索引再写入，重复执行不会产生脏数据
Invoke-RestMethod -Uri 'http://localhost:8080/api/ai/reingest' -Method POST
# → @{success=True; ingestedChunks=56}
```

### 4. 提问

```powershell
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$body = '{"question":"类和对象有什么区别？"}'
Invoke-RestMethod -Uri 'http://localhost:8080/api/ai/ask' -Method POST `
  -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($body))
```

### 5. 一条命令完成「入库 + 检索自测」

```powershell
powershell -ExecutionPolicy Bypass -File scripts\reingest-and-test.ps1 -Question "多态怎么实现？"
```

## API Endpoints · 接口说明

`/api/login`、`/health`、`/api/ai/**` 已在 Spring Security 中放行，可直接调用；`/admin/**` 与 `/api/admin/**`（除登录/注册外）由 `AdminAuthInterceptor` 校验 `X-Token`/`Authorization: Bearer`，登录质询未接入 Spring Security，其余路径 `authenticated()` 仅作文档声明。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/ai/ask` | RAG 问答 + Function Call 工具，返回答案 + 引用来源 |
| POST | `/api/ai/reingest` | 覆盖式重新入库，返回入库片段数 |
| GET | `/api/ai/test` | 纯模型连通性测试（不经过检索） |
| POST | `/api/login` | 登录示例（MyBatis + BCrypt 校验） |
| GET | `/course/list` | 课程列表示例 |
| GET | `/health` | 健康检查 |

## Admin Console · 管理后台（Vue SPA）

访问入口 `http://localhost:8080/admin`（`WebMvcConfig` 转发到 `static/admin/index.html`，Vue 已本地化，无需 CDN）。登录签发 Redis Token（`ai:token:{token}`，TTL 2h），后续请求带 `X-Token` 头，由 `AdminAuthInterceptor` 校验并写入 `UserContext`。所有响应统一封装为 `{ code, message, data }`（`code=0` 为成功，见 `common/Result`；异常由 `GlobalExceptionHandler` 兜底）。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/admin/login` | 后台登录（BCrypt 校验 + 签发 Token） |
| POST | `/api/admin/register` | 注册（用户名唯一校验 + BCrypt 加密） |
| POST | `/api/admin/logout` | 登出（作废 Redis Token，后端失效 + 前端清 localStorage 双清理） |
| GET | `/api/admin/courses?keyword=&page=&size=` | 课程分页（匹配名称/分类/讲师） |
| GET | `/api/admin/courses/{id}` | 课程详情 |
| POST | `/api/admin/courses` | 新增课程 |
| PUT | `/api/admin/courses/{id}` | 编辑课程 |
| DELETE | `/api/admin/courses/{id}` | 删除课程 |
| POST | `/api/admin/rag/ask` | AI 答疑（复用 `RagQaService`，user 取当前登录人） |
| GET | `/api/admin/rag/retrieval?question=` | 检索调试（只召回不调模型，查看向量+BM25+RRF） |
| POST | `/api/admin/rag/reingest` | 知识库覆盖式入库 |
| GET | `/api/admin/history?user=` | 查看提问历史（省略 user = 当前登录人） |
| GET | `/api/admin/users` | 用户列表（脱敏，不含密码） |

> 登出为「后端作废 Token + 前端清 localStorage」双清理：前端直接 `fetch('/api/admin/logout')` 携带旧 Token，`TokenStore.invalidate()` 删除 Redis 键；即使该请求失败，前端仍会清空本地存储兜底登出。

后台页面含 6 个屏：课程管理、AI 答疑、检索调试、知识库、提问历史、用户管理。

**`POST /api/ai/ask`**

```json
{ "question": "类和对象有什么区别？", "user": "tom" }
```

响应（`answer` 由模型生成，`snippet` 最多 200 字符，示例节选）：

```json
{
  "answer": "1) 类是抽象定义，描述属性与行为；对象是类的实例…",
  "sources": [
    {
      "filename": "java.md",
      "title": "3.1 类与对象",
      "snippet": "类是对某一类事物的抽象描述…"
    }
  ]
}
```

### Function Call 工具（轻量 Agent）

`/api/ai/ask` 请求体新增可选 `user` 字段（缺省 `default`）：

    { "question": "我最近都问了哪些问题？", "user": "tom" }

同一次模型调用内注册两个 `@Tool`，由 `qwen-plus` 根据问题自行决定是否调用；知识类问题仍走 RAG 片段：

| 工具名 | 数据源 | 触发示例 | 行为 |
| --- | --- | --- | --- |
| `query_courses` | MySQL `t_course` | 「帮我看 Java 分类有哪些课」 | 按 `name` / `category` 模糊匹配；空关键词返回全部，最多 20 条 |
| `query_question_history` | Redis `ai:history:{user}` | 「我最近都问了哪些问题？」 | 最新在前，最多 20 条；无记录时 `count=0` |

使用前为 `t_course` 增加分类列：

    ALTER TABLE t_course ADD COLUMN category VARCHAR(50) DEFAULT NULL COMMENT '课程分类' AFTER teacher;

工具型问题的 `sources` 为空属正常（不依赖 ES 检索）；Redis 异常时历史工具自动降级为空结果。


## Configuration · 配置项与调优

全部通过 `application.yml` 外置，无需改代码（`@Value` 注入，均带默认值）：

| 配置键 | 默认 | 作用 |
| --- | --- | --- |
| `app.rag.ingest-on-startup` | `false` | 启动是否自动入库（推荐手动触发，数据可持久复用） |
| `app.rag.batch-size` | `10` | 单次向量化文本块数（DashScope 上限 25） |
| `app.rag.chunking.max-chars` | `800` | 结构切分后单块最大字符数 |
| `app.rag.chunking.overlap` | `80` | 二次切分相邻重叠字符数 |
| `app.rag.retrieval.top-k` | `10` | 向量召回数量 |
| `app.rag.retrieval.text-top-k` | `10` | 全文 BM25 召回数量 |
| `app.rag.retrieval.similarity-threshold` | `0.5` | 向量相似度阈值 |
| `app.rag.retrieval.max-sources` | `5` | RRF 融合去重后保留的来源数 |
| `app.rag.retrieval.rrf-k` | `60` | RRF 平滑常数 |
| `app.rag.retrieval.text-min-should-match` | `50%` | 全文最小匹配门槛（去噪关键参数） |
| `app.http.read-timeout` | `300s` | 大模型长回答读取超时 |

调优口径：

- 召回过少 / 漏答案 → 降 `text-min-should-match`、升 `top-k`
- 来源离题 / 噪声多 → 升 `text-min-should-match`（如 `75%`）、升 `similarity-threshold`、降 `max-sources`
- 改了 `es/document-index-mapping.json`（分析器或向量维度）→ **必须删索引**：`.\scripts\reingest-and-test.ps1 -RecreateIndex` → 重启应用 → 再入库

## Project Layout · 目录结构

```text
src/main/java/com/example/aiagentdemo/
├── config/      ChatClientConfig · ElasticsearchIndexInitializer · RestClientConfig · Security* · WebMvcConfig · AdminAuthInterceptor
├── controller/  AiTestController(RAG + Function Call) · AdminAuthController(登录/注册/登出) · AdminCourseController · AdminRagController · AdminHistoryController · AdminUserController · AuthController · CourseController · HealthController
├── service/     RagIngestService(切分入库) · HybridRetrievalService(混合检索+RRF) · RagQaService · AdminCourseService · UserService · CourseService · AiService
├── tools/       CourseTools(按名称/分类查课程) · HistoryTools(记录/查询提问历史)
├── support/     UserContext(请求级用户上下文，ThreadLocal) · TokenStore(Redis Token 签发/校验/作废)
├── common/      Result(code=0 成功) · PageResult · BizException · GlobalExceptionHandler
├── entity/ mapper/  MyBatis 示例业务（User / Course，Course 含 category）
src/main/resources/
├── document/java.md              RAG 语料：Java 课程文档
├── es/document-index-mapping.json 自定义索引 mapping（ngram bigram + dense_vector）
├── static/admin/                 管理后台 Vue SPA（index.html + 本地化 vue.global.prod.js）
└── application.yml               全部密钥为环境变量占位符
scripts/
├── reingest-and-test.ps1         一键「入库 + 提问自测」，支持 -RecreateIndex
└── NOTES.md                      操作备忘录：常用命令、ES 运维、Git 自查与网络排查
```

## Troubleshooting · 已知坑（均已在代码中修复）

1. **中文全文检索失效**：ES 默认 `standard` 分析器不对中文分词 → 用内置 `ngram` bigram(2,2) + `lowercase`；`cjk` filter 在本机 ES 不可用；索引与查询必须使用同一分析器。
2. **改 mapping 不生效**：索引初始化器「已存在则跳过」，必须删除索引后重启应用。
3. **标题全变成文件名**：`MarkdownDocumentReader` 会剥离标题行 → 改为自写按标题切分逻辑。
4. **全文召回 `Failed to decode response`**：向量索引 `_source` 含 `embedding` 未知属性 → 检索时 `includes(content, metadata)` 并给映射记录加 `@JsonIgnoreProperties(ignoreUnknown = true)`。
5. **来源混入「什么是 X」离题小节**：bigram 泛词命中 → `minimum_should_match=50%`。
6. **长回答 `SocketTimeoutException`**：`app.http.read-timeout=300s`（`RestClientConfig`）。
7. **上下文含代码块导致 Prompt 解析异常**：不使用 `PromptTemplate`，改用字符串拼接。
8. **`git push` 报 `github.com:443` 连不上**：真凶常是本机代理型加速器（Watt Toolkit/Clash）节点失效，Git for Windows 会跟随系统代理 → 用 `curl` 对比「直连 / 走代理」定位，临时绕过：`git -c http.proxy= -c https.proxy= push`。
9. **工具类问题 `sources` 为空 / 工具不触发**：查课程、查历史走 MySQL/Redis，不经过 ES，`sources: []` 属正常；若该调工具却未调用，确认 `SYSTEM_PROMPT` 已包含工具规则且应用已重启（项目未引入 devtools，改代码必须重启）。

完整命令与排查步骤见 [scripts/NOTES.md](scripts/NOTES.md)。

## Roadmap · 后续计划

- [ ] 多文档类型入库（PDF 依赖 `spring-ai-pdf-document-reader` 已引入）
- [ ] 多轮对话与会话记忆（基于 Redis）
- [ ] 检索重排（rerank）与元数据过滤
- [ ] 检索/问答效果评估集，量化 `top-k`、阈值等参数
- [ ] 流式输出（SSE）与前端问答页面
- [ ] Docker Compose 一键起 MySQL / Redis / ES
- [x] Agent 化：Function Call 轻量工具路由（查课程、查提问历史）
- [x] 管理后台 SPA（课程 CRUD / AI 答疑 / 检索调试 / 知识库 / 历史 / 用户）
- [ ] Agent 扩展：成绩查询、多工具编排与参数校验

## Notes · 说明

- 这是一个**教学演示**项目，代码内保留较详细的中文注释以说明「为什么这样写」。
- 本仓库不包含任何真实密钥；克隆后请按 [Quick Start](#quick-start-快速开始) 配置自己的环境变量。
