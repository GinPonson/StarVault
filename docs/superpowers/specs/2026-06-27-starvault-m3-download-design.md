# StarVault Android — M3 Download Design

**Status**: Draft v0.1 · 2026-06-27
**Author**: StarVault Agent
**Milestone**: M3 (Download)
**Dependencies**: M1 (UI 9 屏) ✓ · M2 (Upload) ✓ · createFolder 接入 (M1.x) ✓

---

## 1. 背景与定位

M2 上传链路已完整跑通(WorkManager + OSS Multipart + ForegroundService)。M3 **补齐下载方向**,闭环"用户把云端文件取回本地"的最常用操作:

- Files 屏 / Preview 屏 / Search 结果,均可触发单文件下载
- 原图 / 原文件 走 115 CDN 直链(签名 URL,**5 分钟过期**)
- 保存到 Android **Download 目录**(SAF 公共目录,免用户选路径)
- 与 M2 上传对称:走 WorkManager + ForegroundService + 通知栏进度

> ⚠️ M3 **不做批量下载**(Files 屏多选全下)+ **不做断点续传**(超出 5 分钟签名 TTL 后让用户重下)+ **不做下载限速**(115 CDN 全速)

---

## 2. 范围

### 2.1 M3 包含

| # | 项 | 备注 |
|---|---|---|
| 1 | `DownloadRepository.download(item, saveName)` | 单文件下载主流程 |
| 2 | `DownloadWorker`(CoroutinesWorker) | 协程骨架复用 M2 UploadWorker |
| 3 | SAF 写入 Download 公共目录 | `MediaStore.Downloads` API 30+,免 SAF picker |
| 4 | Files 屏 / Preview 屏 / Search 屏 长按菜单加 "下载" | 解 TODO 钩子 |
| 5 | `TransferRepository` 复用 | 上传 / 下载统一在 Transfers 屏展示(下行) |
| 6 | ForegroundService + 通知栏进度 | 与 M2 对称 |
| 7 | `OssDownloader`(OkHttp 流式 GET) | 替代 M2 的 `OssUploader`,签名 URL 拉文件 |

### 2.2 M3 不包含

| # | 项 | 推到后续 |
|---|---|---|
| 1 | 多文件批量下载 | Files 屏 BulkAction.Download 留给 M4+ |
| 2 | 断点续传(Range 续下) | 5 分钟签名过期后重下,不算 DoD |
| 3 | 下载限速 / Wi-Fi only | 系统级,放后续优化 |
| 4 | 自定义保存路径 | 用 `MediaStore.Downloads`,用户不选路径 |
| 5 | 预览直存(下载同时打开) | 走通用 download → 通知栏 → 用户点开 |

---

## 3. 协议对齐(深度对齐 OpenList 115-sdk-go)

### 3.1 115 Open API 端点(走 `proapi.115.com` + Bearer)

**`POST /open/ufile/downurl`** — 拿 CDN 签名直链

- 官方文档:https://www.yuque.com/115yun/open/um8whr91bxb5997o
- OpenList 实现:OpenListTeam/115-sdk-go/fs.go:300-321(DownURL)+ OpenList drivers/115_open/driver.go:147-165(Link)
- **请求**(form-urlencoded):
  - `pick_code` : 文件提取码(必填,从 getInfo 拿)
- **响应**:
  ```json
  {
    "state": true,
    "code": 0,
    "message": "ok",
    "data": {
      "<file_id>": {
        "file_name": "...",
        "file_size": 57671680,
        "pick_code": "...",
        "sha1": "...",
        "url": { "url": "https://cdn-cf-thumb.115.com/...?t=...&sign=..." }
      }
    }
  }
  ```

### 3.2 CDN 下载(走 `data.url.url` + Android UA)

- 拿到 `data[file_id].url.url` 后,直接 OkHttp GET 该 URL
- **必须带 Android UA**(`Mozilla/5.0 (Linux; Android 14) ... Chrome/120.0`)
  - 115 CDN 按 UA 签发不同节点,移动 UA 才能拿到 CDN 直链
  - 我们 OkHttp 客户端已统一注入 `browserLikeHeaderInterceptor`,**不用**每次显式传
- 响应体:**二进制文件流**(image/jpeg / video/mp4 / application/octet-stream ...)
- Content-Length 头携带文件总字节数,用来算 progress 百分比
- **签名 URL 5 分钟过期**:拿到后立刻开始下载,中间失败不要重试(重下整文件)

### 3.3 OpenList 行为对齐

- OpenList 把 `DownURL` + UA 打包成 `model.Link { URL, Header }`,上层 streaming GET 时套上 `Link.Header["User-Agent"]`
- 我们:把 UA 放到 OkHttp 客户端拦截器(已就绪),下载时**不**额外传 Header,避免与拦截器冲突

### 3.4 Android SAF 写入(API 29+)

- **目标目录**:系统 `Downloads/`(用户最熟悉的位置)
- **API 30+**:`MediaStore.Downloads.EXTERNAL_CONTENT_URI` 公共目录写入,**免 SAF picker**(用户不选路径,直接落)
- **API 29**:仍可用 `MediaStore`,但需要 `WRITE_EXTERNAL_STORAGE` 权限(我们的 minSdk=34 不踩)
- MIME type 从 file_id → downurl response 的 `file_name` 扩展名推断
- 文件名冲突:115 文件名可能有重复,加 `_${timestamp}` 后缀兜底

---

## 4. 技术栈

| 依赖 | 版本(对齐 M2) | 用途 |
|---|---|---|
| `kotlinx-coroutines-android` | (libs.versions.toml) | ViewModelScope + WorkManager 协程 |
| `androidx.work:work-runtime-ktx` | (M2 同款) | DownloadWorker |
| `okhttp3` | (M2 同款) | OkHttp client 拉签名 URL |
| `MediaStore.Downloads` | 系统 API | 写入 Download 公共目录(免 SAF picker) |
| `ContentResolver.openOutputStream` | 系统 API | 流式写入 |

**新增依赖:无**(全是 Android / OkHttp / Coroutines 现有 API)。

---

## 5. 项目结构(M3 新增)

```
app/src/main/kotlin/com/starvault/
├── data/
│   └── download/                          # ← NEW
│       ├── OssDownloader.kt               # OkHttp GET 签名 URL + 进度回调
│       ├── DownloadSaveUri.kt             # MediaStore.Downloads 写入 + MIME 推断
│       └── DownloadWorker.kt              # CoroutinesWorker,与 UploadWorker 同骨架
├── data/repository/
│   └── DownloadRepository.kt              # ← NEW,组合 downurl + OssDownloader + DownloadSaveUri
├── core/
│   └── ServiceLocator.kt                  # 加 downloadRepository 字段
└── ui/files/
    ├── FilesScreen.kt                     # row "更多"菜单加 "下载"
    ├── FilesViewModel.kt                  # 加 download(item: ParsedFileItem) 方法
    └── FilesRoute.kt                      # +1 callback
```

**不新增任何 5-file screen**(下载是单文件操作,没有"下载列表屏",复用 Transfers 屏显示进度)。

---

## 6. 数据流(端到端)

### 6.1 用户路径

```
Files 屏 row 点击 "···" → 弹菜单 → "下载"
   │
   ▼
FilesViewModel.download(item)
   │  • item.id → OpenFolderInfoResponse.file_id(已缓存?没就 re-fetch)
   │  • item.pickCode(从 list 已带,免一次 getInfo)
   ▼
DownloadRepository.download(item, saveName=item.name)
   │  • api.downloadUrl(pickCode) → OpenDownUrlResponse → data[fid].url.url
   │  • transferRepo.add(Transfer(direction=DOWN, status=RUNNING, ...))
   │  • workerManager.enqueue(DownloadWorker(...))
   ▼
DownloadWorker.doWork()
   │  • setForeground(ForegroundInfo(notificationId, progressNotification))
   │  • ossDownloader.download(cdnUrl, destUri, onProgress = { transferred, total -> ... })
   │      • ContentResolver.openOutputStream(destUri) → BufferedOutputStream
   │      • OkHttp GET cdnUrl → ResponseBody.byteStream → 循环 read(buf) → write(buf)
   │      • 每 1MB / 200ms 调 onProgress 一次(避免 IPC 风暴)
   │  • transferRepo.markDone(workId)
   ▼
Transfers 屏:列表状态从 RUNNING → SUCCESS
通知栏:从进度条 → "下载完成 · tap 打开"(action = Intent.ACTION_VIEW + destUri)
```

### 6.2 Transfer 状态机复用

```
TransferStatus.RUNNING  →  setProgress(workInfo, transferredBytes, totalBytes)
TransferStatus.SUCCESS  →  markDone(workId)
TransferStatus.FAILED   →  markFailed(workId, errorMessage)
```

`TransferRepository`(M2 已有)统一处理上行 + 下行,Transfers 屏按 `Direction.DOWN` 过滤。

---

## 7. 状态机

```
DownloadWorker:
  START ─→ setForeground ─→ RUNNING(progress) ─→ SUCCESS
                │                  │
                │                  └──→ FAILED (ToastBus.error + 通知栏失败)
                │
                └──→ cancel(by user) ─→ CANCELLED
```

UI 层(FilesViewModel.download):
- 调 repo.download() 同步返回 `WorkInfo.id`(用于取消 / 跟踪)
- 不阻塞调用方,失败 / 完成靠 TransferRepository events flow 通知

---

## 8. 错误处理

| 错误 | 检测点 | 处理 |
|---|---|---|
| `downurl` 返回 state=false | `DownloadRepository` | `Result.failure(IllegalStateException(message))` + ToastBus.error |
| HTTP 4xx/5xx(签名过期 / 文件不存在) | `OssDownloader` | 同上 + markFailed |
| **签名 URL 5 分钟过期**(网络慢导致) | `OssDownloader` 收到 403/410 | markFailed,message "下载超时,请重试" |
| OkHttp 网络断 | `OssDownloader` IOException | markFailed,message 含 IOException cause.message |
| ContentResolver 写入失败(磁盘满 / 权限) | `DownloadSaveUri` | markFailed,message 含 IOException |
| **Worker.runAttemptCount > 3** | `DownloadWorker` return Result.failure() | markFailed,**不重试**(签名 URL 已过期,重试无意义) |
| 用户主动取消 | `WorkManager.cancelWorkById` | markFailed("已取消") |

---

## 9. 与 Transfers 屏联动

Transfers 屏(M2 已实现)有 3 个 tab:Active / Done / Offline。下行传输直接走同一套:
- `Transfer.direction = DOWN` ← UploadWorker 已用 `UP`,DownloadWorker 用 `DOWN`
- `Transfer.status` 用 M2 已定义的 RUNNING / PAUSED / SUCCESS / FAILED

UI 复用度:
- TransfersScreen Preview 不变(M2 已 mock 5 条 transfer,其中 `t02 song.flac` 是 UP SUCCESS,可再加 1 条 DOWN 类 case)
- `transfers_uploading` Paparazzi baseline 已存在(M2 已加),**M3 不需要新 baseline**(状态形态不变,只是多 DOWN 类型)

---

## 10. 测试策略

### 10.1 JVM 单元测试(JUnit + MockK,继承 M2 风格)

| 测试类 | 覆盖 |
|---|---|
| `OssDownloaderTest` | Mock OkHttp Response,验证 byte 流正确写到 OutputStream + 进度回调次数 |
| `DownloadSaveUriTest` | Robolectric mock ContentResolver,验证 MediaStore insert + openOutputStream 写入 |
| `DownloadRepositoryTest` | Mock OpenFileApiService,downurl 失败 / 成功两条路径 |

### 10.2 Paparazzi(仅 UI 截图回归)

- **无新 baseline**(FilesScreen / TransfersScreen 不变)
- 复用 M1 + M2 已有的 27 PNG

### 10.3 AVD 端到端验证(必做,与 M2 同流程)

按 M3 DoD 走。

---

## 11. DoD(M3 完成定义)

- [ ] AVD:Files 屏 row 点击 "···" → "下载" → 选小文件(< 20MB,走 singlepart)→ Downloads 目录出现文件 + 通知栏进度 0→100% + Transfers 屏 SUCCESS
- [ ] AVD:下载 > 20MB 文件(走 OssDownloader 流式,非 Range)→ 通知栏进度实时 + 完成后能用系统文件管理器打开
- [ ] AVD:下载中按 Home 键 → app 进后台 → 通知栏持续显示进度 → WorkManager 续跑不杀进程
- [ ] AVD:`adb shell am force-stop com.starvault` → WorkManager 重启进程 → 续传(注:与 M2 不同,**M3 续传可能失败**,签名 5 分钟过期,失败属预期)
- [ ] JVM:`OssDownloaderTest` / `DownloadRepositoryTest` 绿
- [ ] 失败路径:网络断开中途 → markFailed + ToastBus.error + 通知栏"下载失败"action=重试
- [ ] CLAUDE.md 更新:§Upload pipeline 旁加 §Download pipeline 章节
- [ ] 提交记录全部英文 message(对齐 commit `b94bef7` / `e701a7e` 风格)

---

## 12. 风险与待定

| # | 项 | 状态 | 风险 |
|---|---|---|---|
| 1 | 签名 URL 5 分钟过期 | 已知 | 大文件慢网下可能中途过期 → 失败属预期,提示用户重下。**不**实现断点续传(超出 M3 范围) |
| 2 | `MediaStore.Downloads` 文件名冲突 | 已知 | 加 `_${System.currentTimeMillis()/1000}` 后缀兜底 |
| 3 | `WorkManager.runAttemptCount > 3` 策略 | 已知 | **不**重试(重试也是同一个签名 URL,过期一样失败) |
| 4 | `setProgress` 频率 | 已知 | 每 1MB / 200ms 一次,够细不爆 IPC |
| 5 | **SAF 公共目录写入兼容性** | 已知 | API 29+ MediaStore,API 34+ 我们 minSdk 全部支持 |
| 6 | **API 30-32 scoped storage** | 待验证 | MediaStore.Downloads 在 API 29+ 全部可用,无需 MANAGE_EXTERNAL_STORAGE |
| 7 | 与 FilesViewModel.bulk() 集成 | 后续 | M4 BatchDownload 才用,本次 download() 是单文件入口 |
| 8 | 通知栏 action Intent | 已知 | Intent.ACTION_VIEW + destUri,需要 `FileProvider`(M2 上传已加,复用) |
| 9 | 行点击 / 长按菜单的 "下载" 入口 | 设计 | 复用 FilesScreen 现有 "···" 菜单,加一个 menu item,不引入新交互 |

---

## 13. 关联文档

- **协议真相源**:
  - OpenListTeam/115-sdk-go/fs.go:298-321(DownURL)· drivers/115_open/driver.go:147-165(Link)
  - 115 开放平台官方文档:https://www.yuque.com/115yun/open/um8whr91bxb5997o
- **M2 复用的对称模块**:
  - `app/src/main/kotlin/com/starvault/data/uploadworker/UploadWorker.kt` — CoroutinesWorker 骨架
  - `app/src/main/kotlin/com/starvault/data/repository/TransferRepository.kt` — Direction.UP/DOWN 统一
  - `app/src/main/kotlin/com/starvault/core/ServiceLocator.kt:115-125` — ForegroundInfo + NotificationChannel

---

## 14. 待用户决策(本 spec 通过前需明确)

| # | 决策项 | 推荐 | 备选 |
|---|---|---|---|
| 1 | 下载保存路径 | **Download 公共目录**(`MediaStore.Downloads`,免 picker) | SAF picker 用户选(体验差) |
| 2 | 是否在 DownloadWorker 加重试 | **不加**(签名过期重试无用) | 加 1 次(过期前重拿 downurl) |
| 3 | 长按菜单 vs "···" 菜单入口 | **"···" 菜单**(已存在,加 item) | 长按弹出 BottomSheet |
| 4 | 是否支持下载文件夹(zip 一把) | **不支持**(M4 BatchDownload 范围) | M3 内加 zip 打包(复杂度高) |
