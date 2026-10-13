<#
.SYNOPSIS
    RAG 重新入库 + 检索自测 一键脚本（本地开发用）。

.DESCRIPTION
    覆盖三种常见场景：
      A. 改了 java.md                → 直接跑本脚本（重新入库 + 自测）。无需重启、无需删索引。
      B. 改了 document-index-mapping.json（分析器/维度）
                                     → 先跑: .\reingest-and-test.ps1 -RecreateIndex  删旧索引；
                                       然后重启应用（ElasticsearchIndexInitializer 会按新 mapping 重建）；
                                       再跑一次本脚本做入库 + 自测。
      C. 只想测某个问题的检索与回答   → .\reingest-and-test.ps1 -Question "多态怎么实现？"




      重点：
      Powershell
      # 改了 java.md，或想测某个问题：
      powershell -ExecutionPolicy Bypass -File scripts\reingest-and-test.ps1 -Question "你的问题"

      # 改了 mapping/维度：先删索引 → 重启应用 → 再跑上面那条入库
      powershell -ExecutionPolicy Bypass -File scripts\reingest-and-test.ps1 -RecreateIndex




    步骤：
      [1] (可选 -RecreateIndex) 删除向量索引，删完即退出，等待你重启应用重建。
      [2] POST /api/ai/reingest 覆盖式重新入库（内部先清空再写，不会累积重复）。
      [3] POST /api/ai/ask 提问，并以 UTF-8 正确打印 answer 与 sources（规避 PS 5.1 中文乱码）。

.PARAMETER Question
    测试问题，默认「类与对象是什么？」。

.PARAMETER RecreateIndex
    开关：删除向量索引。仅在改了 mapping/维度时需要；删除后必须重启应用让索引按新 mapping 重建。

.PARAMETER AppBaseUrl / EsBaseUrl / IndexName
    应用与 ES 的连接参数，默认与 application.yml 一致。

.PARAMETER EsUser / EsPass
    ES 账号密码，仅 -RecreateIndex 用到。读取优先级：显式参数 > 环境变量 AI_ES_USER / AI_ES_PASS > 交互输入。
    （不在脚本内写死密码，方便安全地纳入版本库长期复用。）

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\reingest-and-test.ps1
    powershell -ExecutionPolicy Bypass -File scripts\reingest-and-test.ps1 -Question "多态怎么实现？"
    powershell -ExecutionPolicy Bypass -File scripts\reingest-and-test.ps1 -RecreateIndex

.NOTES
    依赖：应用已在 8080 运行、ES 已在 9200 运行。
    编码：本文件须为「UTF-8 with BOM」，否则 Windows PowerShell 5.1 会按 ANSI 读取导致中文乱码/解析报错。
    本文件不含任何明文密钥，可安全提交；如用环境变量持久化口令，请放到被 .gitignore 忽略的 .env 中再加载。
#>
param(
    [string]$Question = '类与对象是什么？',
    [switch]$RecreateIndex,
    [string]$AppBaseUrl = 'http://localhost:8080',
    [string]$EsBaseUrl  = 'http://localhost:9200',
    [string]$IndexName  = 'spring-ai-document-index-v2',
    [string]$EsUser     = $env:AI_ES_USER,
    [string]$EsPass     = $env:AI_ES_PASS
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.Encoding]::UTF8

# 统一发 JSON 请求并按 UTF-8 解码响应（Invoke-RestMethod 在 PS 5.1 会按错误编码解码中文，故用 RawContentStream 手动解码）
function Invoke-JsonApi {
    param([string]$Uri, $BodyBytes, [hashtable]$Headers)
    $params = @{ Uri = $Uri; Method = 'POST'; ContentType = 'application/json; charset=utf-8'; UseBasicParsing = $true }
    if ($BodyBytes) { $params.Body = $BodyBytes }
    if ($Headers)   { $params.Headers = $Headers }
    $resp = Invoke-WebRequest @params
    return [Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray())
}

# ---------- [1] 可选：删除索引（改 mapping/维度后才需要）----------
if ($RecreateIndex) {
    if ([string]::IsNullOrEmpty($EsUser)) { $EsUser = Read-Host 'ES 用户名' }
    if ([string]::IsNullOrEmpty($EsPass)) {
        $secure = Read-Host 'ES 密码' -AsSecureString
        $EsPass = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
            [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
    }
    $esAuth = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("${EsUser}:${EsPass}"))
    Write-Host "[1] 删除向量索引 $IndexName ..." -ForegroundColor Yellow
    try {
        Invoke-RestMethod -Uri "$EsBaseUrl/$IndexName" -Method DELETE -Headers @{ Authorization = $esAuth } | Out-Null
        Write-Host "    已删除。请【重启应用】让初始化器按新 mapping 重建索引，然后重新运行本脚本。" -ForegroundColor Green
    } catch {
        Write-Host "    索引不存在或删除失败（可忽略）：$($_.Exception.Message)" -ForegroundColor DarkGray
    }
    return
}

# ---------- [2] 覆盖式重新入库 ----------
Write-Host '[2] 触发重新入库 POST /api/ai/reingest ...' -ForegroundColor Yellow
$ingest = (Invoke-JsonApi -Uri "$AppBaseUrl/api/ai/reingest" -BodyBytes $null -Headers $null) | ConvertFrom-Json
Write-Host ("    入库结果：success={0}, ingestedChunks={1}" -f $ingest.success, $ingest.ingestedChunks) -ForegroundColor Green

# ---------- [3] 检索自测 ----------
Write-Host ("[3] 提问：{0}" -f $Question) -ForegroundColor Yellow
$bodyBytes = [Text.Encoding]::UTF8.GetBytes((@{ question = $Question } | ConvertTo-Json -Compress))
$result = (Invoke-JsonApi -Uri "$AppBaseUrl/api/ai/ask" -BodyBytes $bodyBytes -Headers $null) | ConvertFrom-Json

Write-Host "`n===== 回答 =====" -ForegroundColor Cyan
Write-Host $result.answer

Write-Host ("`n===== 引用来源（{0} 条）=====" -f @($result.sources).Count) -ForegroundColor Cyan
$result.sources | ForEach-Object {
    $snip = if ($_.snippet -and $_.snippet.Length -gt 60) { $_.snippet.Substring(0, 60) + '…' } else { $_.snippet }
    [pscustomobject]@{ title = $_.title; filename = $_.filename; snippet = $snip }
} | Format-Table -AutoSize -Wrap
