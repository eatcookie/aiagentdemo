# RAG 项目操作备忘录（aiagentdemo）

> 本目录汇总本项目常用运维/调试命令，均为实测可用。依赖：应用运行在 `http://localhost:8080`，
> Elasticsearch 运行在 `http://localhost:9200`（Basic 认证）。
> ⚠️ **本项目会推到公开仓库：本文件及任何将提交的文件中都禁止出现明文密码**，
> 请一律用环境变量（见根目录 `.env.example`）。
> 凭证说明：**脚本里的密码 = ES 密码**（仅删索引时用）；**DashScope API key 由应用从环境变量
> `DASHSCOPE_API_KEY` 读取**，与脚本无关。

---

## 0. 一键脚本（最常用）

```powershell
# 改了 java.md，或想测某个问题：覆盖式重新入库 + 检索自测（无需重启、无需删索引）
powershell -ExecutionPolicy Bypass -File scripts\reingest-and-test.ps1
powershell -ExecutionPolicy Bypass -File scripts\reingest-and-test.ps1 -Question "多态怎么实现？"

# 改了 es/document-index-mapping.json（分析器/维度）：
#   第1步 删旧索引 → 第2步 重启应用(自动按新mapping重建) → 第3步 再跑一次普通模式入库
powershell -ExecutionPolicy Bypass -File scripts\reingest-and-test.ps1 -RecreateIndex

# 省掉删索引时手输 ES 账密（值填你本机的，只在会话/环境变量里出现，勿写回文件）
$env:AI_ES_USER = '你的ES用户名'
$env:AI_ES_PASS = '你的ES密码'
```

---

## 1. 手动调用应用接口

```powershell
# 提问（中文必须按 UTF-8 字节发送）
$body = '{"question":"类和对象是什么？"}'
Invoke-RestMethod -Uri 'http://localhost:8080/api/ai/ask' -Method POST `
  -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($body))

# 重新入库（返回 success / ingestedChunks）
Invoke-RestMethod -Uri 'http://localhost:8080/api/ai/reingest' -Method POST

# 纯模型连通性测试（不走向量检索）
Invoke-RestMethod -Uri 'http://localhost:8080/api/ai/test'
```

## 2. PowerShell 中文显示乱码修复（会话级，执行一次）

```powershell
[Console]::OutputEncoding = [Text.Encoding]::UTF8

# 若 Invoke-RestMethod 仍乱码（PS 5.1 按错误编码解码响应体），用原始字节手动解码：
$resp = Invoke-WebRequest -Uri 'http://localhost:8080/api/ai/ask' -Method POST `
  -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($body)) -UseBasicParsing
$json = [Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray()) | ConvertFrom-Json
$json.answer
```

> 长期建议：改用 **PowerShell 7（`pwsh`）**，默认 UTF-8，无需以上技巧。
> 含中文的 `.ps1` 文件必须保存为 **UTF-8 with BOM**，否则 5.1 按 ANSI 读取会解析报错。

---

## 3. Elasticsearch 常用运维（本地开发）

```powershell
# 通用：先备好认证头（账密从环境变量读取，勿写明文）
if (-not $env:AI_ES_USER) { $env:AI_ES_USER = Read-Host 'ES 用户名' }
if (-not $env:AI_ES_PASS) { $env:AI_ES_PASS = Read-Host 'ES 密码' }
$auth = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("$($env:AI_ES_USER):$($env:AI_ES_PASS)"))
$H = @{ Authorization = $auth }
$IDX = 'spring-ai-document-index-v2'

# 集群健康 / 索引列表 / 文档数
Invoke-RestMethod -Uri 'http://localhost:9200/_cluster/health' -Headers $H
Invoke-RestMethod -Uri 'http://localhost:9200/_cat/indices?v' -Headers $H
Invoke-RestMethod -Uri "http://localhost:9200/$IDX/_count" -Headers $H

# 查看 mapping（确认 ngram bigram 分析器与 dims/cosine）
Invoke-RestMethod -Uri "http://localhost:9200/$IDX/_mapping" -Headers $H

# 删除索引（改 mapping/维度后；删完必须重启应用让初始化器重建）
Invoke-RestMethod -Uri "http://localhost:9200/$IDX" -Method DELETE -Headers $H

# 全文检索实验：对 Spring AI 向量索引的搜索务必用 _source filter 只取 content/metadata，
# 否则 _source 里的 embedding(1024维) 会撑爆输出/导致客户端反序列化失败
$q  = '{"size":5,"_source":["content","metadata"],' + '"query":{"multi_match":{"query":"类与对象","fields":["content","metadata.title^2"],"minimum_should_match":"50%"}}}'
$bytes = [Text.Encoding]::UTF8.GetBytes($q)
Invoke-RestMethod -Uri "http://localhost:9200/$IDX/_search" -Method POST -Headers $H `
  -ContentType 'application/json; charset=utf-8' -Body $bytes

# 验证分词器：看中文被切成怎样的二元组
$a = '{"analyzer":"default","text":"类与对象"}'
Invoke-RestMethod -Uri "http://localhost:9200/$IDX/_analyze" -Method POST -Headers $H `
  -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($a))
```

---

## 4. 关键配置项速查（application.yml）

```yaml
app.rag.chunking:    max-chars: 800, overlap: 80         # 结构切分后超长块的二次切分
app.rag.retrieval:
  top-k: 10            # 向量召回数量
  text-top-k: 10       # 全文(BM25)召回数量
  similarity-threshold: 0.5   # 向量相似度阈值（收紧弱相关可升到 0.6）
  max-sources: 5       # RRF 融合去重后保留的来源数
  rrf-k: 60            # RRF 平滑常数
  text-min-should-match: 50%  # BM25 最小匹配门槛（滤掉只蹭"什么"等泛词的离题片段）
```

调优口径：结果太少/漏答案 → 降 `text-min-should-match`、升 `top-k`；噪声多 → 升门槛、升 `similarity-threshold`、降 `max-sources`。

## 5. 改代码后的快速编译

```powershell
.\mvnw.cmd -o -q compile   # 静默模式：无输出即成功
```

## 6. 常见坑（都踩过，勿回退）

1. **ES 无中文插件时用内置 ngram bigram(2,2) 做中文 BM25**；`cjk` filter 本机 ES 不支持；索引与查询必须同一分析器。
2. **改 mapping 必须删索引重建**：初始化器「索引已存在则跳过」，不删不会生效。
3. **MarkdownDocumentReader 会剥离标题**：已改为自写按 `#/##/###` 切分，标题进正文+metadata。
4. **ES Java 客户端检索向量索引会 decode 失败**：`_source` 含 embedding 未知属性所致；已在检索中加 `includes(content, metadata)` + `@JsonIgnoreProperties`。
5. **DashScope 长回答超时**：`app.http.read-timeout: 300s`（RestClientCustomizer）。
6. **prompt 拼接勿用 PromptTemplate**：代码块里的 `{}` 会被当占位符解析，改用字符串拼接。
