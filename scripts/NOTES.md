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
7. **代理型加速器会拖垮 `git push`**：报错写着 `github.com port 443` ，但真凶可能是本机 Watt Toolkit/Clash 的代理节点（详见 **8. Git 推不上去的排查**）。

---

## 7. Git：提交与推送（自查 + 常用操作）

> 核心概念：**Push 只上传「已 commit」的内容**。磁盘上有 `target/`、`.idea/` 不等于会被推上去。

### 7.1 推送前三查（命令行，最可靠）

```powershell
git status                                  # 期望：nothing to commit, working tree clean
git ls-files                                # 会被推到公开仓库的完整清单：不应出现 target/ .idea/ *.iml .env
git grep -I -e Admin@ -e Root@ $(git ls-files)   # 无输出 = 已跟踪文件里无明文密钥
git check-ignore -v target .idea aiagentdemo.iml  # 反查忽略规则是否真生效（会打印命中的 .gitignore 行号）
git log --oneline -3; git branch -vv        # 确认提交内容与 origin 跟踪关系
```

> 注：本备忘录第 7/8 节写着 `Admin@`/`Root@` 这两个关键词本身，所以上面的 grep 会命中 `scripts/NOTES.md` 的**示例行**，属预期；
> 只要命中的不是真实口令面且不是业务文件，就是干净。想要零命中，可只搜具体密码片段。

首次初始化：

```powershell
git init
git add .
git status            # 人工过目，确认没有意外文件
git commit -m "feat: initial Spring AI RAG project"
git remote add origin https://github.com/<user>/<repo>.git
git ls-remote --heads origin              # 先验通道：报错=网络/代理问题；空输出=远程无分支
git push -u origin main
```

### 7.2 IDEA 2026 手动入口（界面改版，靠记菜单不如用搜索）

| 目的 | 操作 |
| --- | --- |
| 查待提交/工作区 | `Ctrl+K` 打开 Commit 窗口：**列表为空就是干净**（有改动才会出现颜色标记） |
| 查某条提交的文件 | 双击 `Shift` 输 `log` → Git Log → 选中提交 → 看 Changed Files 面板 |
| 全库搜明文 | `Ctrl+Shift+F`（Find in Files）搜 `Admin@` / `Root@`，Scope 选 **Project Files** |
| 登记远程 | 菜单 `Git → Remotes… → +` 粘仓库地址（结尾带 `.git`） |
| 推送 | `Ctrl+Shift+K`（或双击 `Shift` 输 `push`）；首次会自动带 `--set-upstream` |
| 查忽略是否生效 | Settings → Version Control → Ignored Files；或 Project 面板 ⚙ → Show Excluded Files |
| GitHub 账号 | File → Settings → Version Control → GitHub → `+` → **Login via GitHub**（一次性） |

### 7.3 改提交消息

```powershell
# 尚未推送（只改本地，安全）
git commit --amend -m "feat: Spring AI RAG demo with Elasticsearch hybrid retrieval"

# 已推送：改完后必须覆盖远程（仅适用于无他人协作的分支）
git commit --amend -m "feat: ..."
git push --force-with-lease origin main     # 永远优先用 --force-with-lease，不用 --force
```

> 带正文的写法（公开仓库更像正常开源项目）：
> `git commit --amend -m "feat: 标题行" -m "- 要点一`n- 要点二"`
> GitHub 列表只显示标题行，正文在点开提交后可见。

---

## 8. Git 推不上去（GitHub 连不上）排查流程

报错常是 `Failed to connect to github.com port 443` 或 `Recv failure: Connection was reset`，
但**真凶往往是本机加速器（Watt Toolkit / Clash）的代理挂了**——Git for Windows 会跟随 Windows 系统代理。

```powershell
# ① 加速器代理端口是否在监听（Watt Toolkit 常见 26561，具体看注册表 ProxyServer）
@(Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object LocalPort -eq 26561).Count
# ② 系统代理是否开着（1 = git 会被拐进代理）
(Get-ItemProperty 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Internet Settings').ProxyEnable
# ③ 关键对比：直连 vs 走加速器（一个 200 一个 000 就找到病因了）
curl.exe -s -o NUL -w 'direct=%{http_code}'   --max-time 15 https://api.github.com
curl.exe -s -o NUL -w 'via_watt=%{http_code}'  --max-time 15 -x http://127.0.0.1:26561 https://api.github.com
# ④ 单独验证 git 这条通道（比反复点 Push 快得多）
git ls-remote --heads origin
```

| 探测结果 | 结论 | 处置 |
| --- | --- | --- |
| 直连 200、走代理 000 | **加速器节点坏了**，把 git 一起拖死 | 单条命令绕过（不改任何配置）：`git -c http.proxy= -c https.proxy= push origin main`；或在加速器里换节点/停止加速 |
| 直连 000、走代理 200 | 本机出口到 GitHub 被阻断 | 保持加速开启；或改走 SSH（`ssh.github.com:443`） |
| 两者都 000 | 全网异常 | 检查是否 `ProxyEnable=1` 但端口无人监听（浏览器会一起断网）；在加速器界面点「停止加速」让它自己清理干净 |
| 403 / Unauthorized | 凭据问题 | Settings → Version Control → GitHub → `+` 重新授权；或删掉「凭据管理器」里 `git:https://github.com` 条目 |

其他实测经验：

- 建库时勾了 README → 首次推会报 `fetch first / non-fast-forward`：先 `Git → Pull`（Merge 方式）再推。
- `LF will be replaced by CRLF` 警告**无害**，也不会破坏 `.ps1` 的 UTF-8 BOM。
- 反复点「Commit and Push」会带 `--amend` 重写提交哈希；远程尚空时无害，一旦推过就别再随机 amend。
- Watt Toolkit 会写入 `git config --global http.sslcainfo=D:\cert\github.crt`、hosts 条目与系统代理；如果卸载它，记得把这些还原，否则 git 可能证书校验失败。
- IDEA 的 Push 走的是同一个 git，所以**代理坏着时图形界面必然失败**；要么修/关加速器，要么用上面的临时绕过。
