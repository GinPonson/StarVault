# StarVault Android M2 — 上传功能设计文档

| 字段 | 内容 |
|---|---|
| 文档版本 | v1.1 |
| 撰写日期 | 2026-06-25 |
| 状态 | 待用户复核 |
| 范围 | M2：单文件上传(115 OSS 直传 + WorkManager 后台 + Transfers 屏联动) |
| 实现目标 | 用户在 Files / Album / Preview 任一屏选本地文件 → 后台上传到当前目录 → Transfers 屏看到进度 → 完成后文件出现在云端列表 |
| 不在范围 | 秒传 / 断点续传 / 多文件并行 / 文件夹递归 / 重复检测 / 离线缓存(均推迟 M3+) |

---

## 1. 背景与定位

M1 已交付 9 屏 UI 骨架 + OAuth 设备码流(对齐 OpenList 115-sdk-go)+ 401 自动 refresh + ToastBus 全局错误通知,但**所有屏数据均为 mock / fixtures**(已在 `6d12ec9` 中删除)。

M2 的目标是接入**第一个真实写路径——单文件上传**,跑通"客户端触发 → 后台执行 → 进度可见 → 完成回流"完整链路。下载 / Player / 批量操作推迟到 M3+。

**为何优先做上传(而非下载)**:
- 下载路径已在 M1 通过 `downurl` 验证(commit `4ee434f`),架构骨架已就绪;上传是新链路,趁早暴露 WorkManager / 后台服务 / 文件 IO 等新风险面
- 上传是 115 用户日常高频操作(相册备份 / 微信文件归类),体验痛点更明显
- 与 Transfers 屏天然联动,验证"实时状态机" 真实可行性(而非 mock)

---

## 2. 范围

### 2.1 M2 包含

- **单文件选择**:`ActivityResultContracts.GetContent` 触发系统文件选择器(任何类型)
- **上传执行**:115 OSS 直传(走 `UploadGetToken` → `UploadInit` → OSS `PutObject`/`UploadPart`),对齐 OpenList `drivers/115_open/upload.go`
- **进度上报**:WorkManager `setProgress`,前台监听(Transfers 屏 + 通知栏)
- **后台保证**:`ForegroundInfo` + `Notification` 通知(用户切到其它 App / 锁屏时继续)
- **失败重试**:单分片失败自动重试 3 次(backoff),整体上传失败 → 标记 `TransferStatus.FAILED`
- **完成回调**:成功后 `RefreshFiles` 触发 Files 屏列表 reload
- **Transfers 屏联动**:新增 `Direction.UP` 的 Transfer row 显示在 Active tab,进度条 / 速度 / 状态实时更新

### 2.2 M2 不包含

- 秒传(走 `UploadResume` + SHA1 区间校验,推迟 M3)
- 断点续传(WorkManager 任务被 kill 后重新启动接续,推迟 M3)
- 多文件并行(`UP` 同时只允许 1 个,M3+ 改 `parallelism = 3`)
- 文件夹递归上传(目录遍历 + 树形进度,推迟 M4)
- 重复检测(上传前比对 SHA1,推迟 M3)
- 离线缓存(网络断开时排队等待,推迟 M3)
- 拖拽上传(从系统分享菜单触发,推迟 M4)
- 压缩 / 分卷(上传前预处理,推迟 M4)

---

## 3. 协议对齐(深度对齐 OpenList 115-sdk-go)

所有 115 协议端点严格按 `https://github.com/OpenListTeam/115-sdk-go` 的 Go SDK 行为移植;OSS 操作按 `https://github.com/aliyun/aliyun-oss-go-sdk` Go SDK 移植到 Kotlin/Java SDK。

### 3.1 115 Open API 端点(走 `proapi.115.com` + Bearer)

完整 endpoint 全名(`const.go` 中定义):`https://proapi.115.com/open/upload/{get_token|init|resume}`。

| 端点 | Method | Body | OpenList Go 参考 |
|---|---|---|---|
| `open/upload/get_token` | GET | — | `115-sdk-go/upload.go:21 UploadGetToken` |
| `open/upload/init` | POST | **form-urlencoded** | `115-sdk-go/upload.go:58 UploadInit` |
| `open/upload/resume` | POST | form-urlencoded | M3 才用,本 spec 不实现 |

#### 3.1.1 `UploadGetTokenResp` 字段(全 5 字段,必填)

来源 `115-sdk-go/upload.go:12-18`:

| JSON 字段 | 类型 | 说明 |
|---|---|---|
| `endpoint` | `string` | OSS endpoint,例 `https://oss-cn-shanghai.aliyuncs.com` |
| `AccessKeyId` | `string` | **注意大小写**:服务端驼峰,不是 `access_key_id` |
| `AccessKeySecret` | `string` | **注意大小写**:服务端驼峰,不是 `access_key_secret` |
| `SecurityToken` | `string` | **注意大小写**:服务端驼峰,不是 `security_token`(STS 临时凭证) |
| `expiration` | `string` | ISO 8601 时间字符串,**不是** Long 时间戳 |

DTO 命名严格按服务端(Kotlin):
```kotlin
@Serializable data class UploadGetTokenResp(
    val endpoint: String,
    val AccessKeyId: String,        // 不要 rename
    val AccessKeySecret: String,    // 不要 rename
    val SecurityToken: String,      // 不要 rename
    val expiration: String,
)
```
SDK 用 `@SerialName` 也行,但默认就是 camelCase 直接对接。

#### 3.1.2 `UploadInitReq` 字段(全 10 字段,**全部必填**)

来源 `115-sdk-go/upload.go:30-40`:

| JSON 字段 | 类型 | 说明 |
|---|---|---|
| `file_name` | `string` | 上传后的文件名 |
| `file_size` | `string` | **服务端是 string**(`strconv.FormatInt(fileSize, 10)`),不是 `Long` — Kotlin 端要 `fileSize.toString()` |
| `target` | `string` | 目标目录 CID,**带前缀** `U_1_<cid>`(见下方说明) |
| `fileid` | `string` | **整文件 SHA1**(十六进制,大写) |
| `preid` | `string` | **前 128KB SHA1**(十六进制,大写) |
| `pick_code` | `string` | 提取码,M2 首次上传时为空字符串 |
| `topupload` | `string` | 固定填 `"1"`(其它值待 OpenList 后续版本定义) |
| `sign_key` | `string` | **首次上传时为空字符串**;two-way verify 时填上次响应里的 `SignKey` |
| `sign_val` | `string` | **首次上传时为空字符串**;two-way verify 时填区间 SHA1(大写) |

**target 字段前缀**:OpenList `upload.go:63` 写的是 `fmt.Sprintf("U_1_%s", req.Target)`,所以 Kotlin 端要 `"U_1_${cid}"` 拼接后提交,**不能**只传原始 cid。

#### 3.1.3 `UploadInitResp` 字段(全 9 字段,部分有条件)

来源 `115-sdk-go/upload.go:42-55`:

| JSON 字段 | 类型 | 说明 |
|---|---|---|
| `pick_code` | `string` | 提取码;若 Status == 2(秒传)仍返回,跟真正上传同一字段 |
| `status` | `int` | **关键字段,见 §3.1.4 语义表** |
| `sign_key` | `string` | two-way verify 时使用,与下次 `sign_val` 配对 |
| `sign_check` | `string` | 格式 `"start-end"`(字节偏移),two-way verify 时服务端指定的区间 |
| `file_id` | `string` | 服务端文件 ID,秒传时已分配;真正上传完成后才有 |
| `target` | `string` | 服务端的目标目录 CID(可能与请求的 target 不同,**以响应为准**) |
| `bucket` | `string` | OSS bucket 名,真正上传时用 |
| `object` | `string` | OSS object key,真正上传时用 |
| `callback` | `object \| array<object>` | **`StructOrArray[T]`**:可能是 `{"callback":"...", "callback_var":"..."}` 或 `[{...}]`。**Kotlin 端必须用 sealed/union 类型兼容两种 shape**(见下方 DTO) |

**Callback DTO(sealed 兼容对象/数组)**:
```kotlin
@Serializable data class CallbackInfo(val callback: String, val callback_var: String)

@Serializable
sealed class UploadCallback {
    @Serializable @SerialName("object") data class Single(val value: CallbackInfo) : UploadCallback()
    @Serializable @SerialName("array")  data class Multi(val items: List<CallbackInfo>) : UploadCallback()
}
// 服务端实际只回 Single,但 DTO 设计要防御(参考 115-sdk-go `json_types.StructOrArray[T]` 泛型)
```

#### 3.1.4 `status` 字段语义(关键控制流)

来源 `OpenList/drivers/115_open/driver.go:302-335` + `upload.go:60-75`:

| Status 值 | 含义 | M2 行为 |
|---|---|---|
| `2` | **秒传成功** — 服务端检测到已有相同 SHA1 文件,无需上传 | M2 **不处理**(推迟 M3),M2 直接报错"暂不支持秒传" |
| `6` / `7` / `8` | **two-way verify** — 服务端要求客户端算 `sign_check` 区间 SHA1,再用 `sign_key` + `sign_val` 重传 Init | M2 **必须实现**:OpenList driver.go:309-330 完整逻辑 |
| 其它(典型 `0` / `1`) | 服务端要求客户端真正上传,继续走 `UploadGetToken` + OSS PUT | M2 主路径 |

**字段映射大小写**:所有 field 名(`fileid` / `preid` / `pick_code` / `topupload` / `sign_key` / `sign_val` / `sign_check`)服务端是下划线 / 全小写,服务端区分大小写,**不能** rename 成 camelCase。
参考 §12 #5。

### 3.2 OSS 操作(走 `tokenResp.Endpoint` + STS)

参考 OpenList `drivers/115_open/upload.go`:
- **单 PUT**(`bucket.PutObject(object, file, Callback, CallbackVar)`)— 文件 ≤ 20MB 直接走
- **Multipart**(`bucket.InitiateMultipartUpload` → 循环 `bucket.UploadPart` → `bucket.CompleteMultipartUpload`)— 文件 > 20MB 分片
- **分片大小** `calPartSize`:
  ```
  fileSize ≤ 20MB             → 整文件 1 分片
  20MB  < fileSize ≤ 128GB    → 20MB / 片
  128GB < fileSize ≤ 256GB    → 约 26.2MB / 片
  256GB < fileSize ≤ 384GB    → 约 39.3MB / 片
  384GB < fileSize ≤ 512GB    → 约 52.4MB / 片
  512GB < fileSize ≤ 768GB    → 约 78.6MB / 片
  768GB < fileSize ≤ 1TB      → 约 104.9MB / 片(1TB 切成 10000 片)
  fileSize > 1TB              → 5GB / 片
  ```
  M2 文件大小阈值**实际**远低于 128GB(Android 单文件通常 < 10GB),但算法 1:1 移植,保证未来扩展性
- **重试**:每分片 `retry.Attempts(3)` + `retry.BackOffDelay`(起始 1s,指数退避)
- **回调**:`Callback` / `CallbackVar` 字段必须 `base64.StdEncoding.EncodeToString` 后传给 OSS SDK
- **进度**:分片循环中每片完成 `up(offset * 100 / fileSize)`,由 `driver.UpdateProgress` 透传到 WorkManager `setProgress`

### 3.3 Android OSS SDK 选型

- **首选**:`com.aliyun.oss:oss-android-sdk:2.x`(阿里云官方 Android 优化版,体积 ~2MB)
- **备选**:`okhttp` 手写 OSS PUT/Multipart(对齐 downurl 风格,但分片签名逻辑需自实现 ~300 行,不推荐)
- **理由**:115 OSS 端是标准 Aliyun OSS,直接用官方 SDK 比分自实现少 80% 代码,且自动支持断点 / 重试 / 进度回调

---

## 4. 技术栈

| 维度 | 选择 | 备注 |
|---|---|---|
| 后台调度 | androidx.work:work-runtime-ktx 2.10.x | `CoroutineWorker` + `setProgress` + `getWorkInfoByIdLiveData` |
| 前台服务 | `ForegroundInfo` + `NotificationCompat` | API 34+ 必须 `FOREGROUND_SERVICE_TYPE_DATA_SYNC` |
| OSS SDK | com.aliyun.oss:oss-android-sdk | 见 §3.3 |
| 文件 IO | `ContentResolver.openInputStream(uri)` | 走 SAF,不需要 `READ_EXTERNAL_STORAGE` |
| SHA1 计算 | `java.security.MessageDigest` | 小文件整文件算;大文件 `FileInputStream` 流式 |
| 进度单位 | Bytes(存 `Transfer.transferredBytes`),UI 层 `progress = transferredBytes / totalBytes` | 与 M1 `TransferRow.progress` 一致 |

---

## 5. 项目结构(M2 新增)

```
app/src/main/kotlin/com/starvault/
├── data/
│   ├── remote/cloud115/
│   │   ├── OpenUploadApiService.kt       # 新增：115 Open upload 端点
│   │   ├── OpenUploadModels.kt           # 新增：UploadInitReq / UploadInitResp / UploadGetTokenResp
│   │   └── Cloud115ApiClient.kt          # 修改：新增 uploadApiRetrofit
│   ├── upload/
│   │   ├── OssUploader.kt                # 新增：单 PUT / Multipart 编排 + 重试 + 进度
│   │   ├── UploadInitClient.kt           # 新增：调用 115 Open upload 端点
│   │   └── Sha1Hashing.kt                # 新增：整文件 + 128KB 首段 SHA1
│   └── uploadworker/
│       └── UploadWorker.kt              # 新增：CoroutineWorker,调度 OssUploader + 上报 progress
├── ui/
│   ├── upload/
│   │   ├── UploadFilePicker.kt           # 新增：ActivityResultContracts.GetContent 封装
│   │   └── UploadRoute.kt                # 新增：FAB → 选文件 → enqueue UploadWorker
│   └── transfers/
│       ├── TransfersViewModel.kt         # 修改：监听 WorkManager getWorkInfoById,合并到 UiState
│       └── TransfersUiState.kt           # 复用：M1 已定义 Transfer(Direction.UP)
└── core/
    └── NotificationChannel.kt            # 新增：upload 渠道
```

---

## 6. 数据流(端到端)

```
用户点 FAB("上传")
    ↓
UploadRoute.kt:ActivityResultContracts.GetContent("*/*")
    ↓
用户选文件,得到 content:// URI + DisplayName + Size
    ↓
UploadWorker.enqueue(OneTimeWorkRequest, INPUT_DATA: {uri, targetDirCid, filename, size})
    ↓
[后台,ForegroundInfo + Notification] UploadWorker.doWork():
    1. ContentResolver.openInputStream(uri) → 流式 SHA1(整文件 + 前 128KB)
       - 整文件 SHA1 用 `MessageDigest.getInstance("SHA1")` 全文件 hash
       - 前 128KB SHA1 用同一个 MessageDigest,只读前 131072 字节
    2. uploadInitClient.init(fileName, fileSize, "U_1_${targetCid}", sha1, preSha1, pickCode="", topupload="1", signKey="", signVal="")
       → resp {status, bucket, object, callback, callbackVar, signCheck, signKey, fileId, pickCode, target}
    3. **Status 分支判断**(关键控制流,见 §3.1.4):
       - Status == 2 → 秒传成功,M2 不支持 → Result.failure() + ToastBus("暂不支持秒传")
       - Status ∈ {6, 7, 8} → two-way verify:
           a. parse `signCheck = "start-end"` 拿字节区间
           b. ContentResolver.openInputStream(uri) → RandomAccessFile.seek(start) → 读 end-start+1 字节 → SHA1 → `signVal` (大写)
           c. uploadInitClient.init(...) 第二次调用,带 signKey + signVal,其它字段不变
           d. 再次判断 Status:
              - Status == 2 → 秒传成功(M2 不支持)
              - Status ∈ {6, 7, 8} → 区间反复不匹配,Result.failure() + ToastBus
              - 其它 → 继续走 OSS 上传
       - 其它(典型 0/1) → 继续走 OSS 上传
    4. uploadGetTokenClient.getToken()
       → resp {endpoint, AccessKeyId, AccessKeySecret, SecurityToken, expiration}
    5. ossUploader.upload(endpoint, sts, initResp, inputStream, totalBytes, progressCb):
         - ≤ 20MB: bucket.PutObject + Callback/CallbackVar headers(base64 编码)
         - > 20MB: InitiateMultipartUpload + UploadPart×N + CompleteMultipartUpload
         - 每分片: progressCb(offset) → setProgress(workDataOf("transferred" to offset))
         - 每分片 retry 3 次 + backoff(reference §3.2)
    6. setProgress(workDataOf("phase" to "DONE", "fileId" to resp.fileId))
    7. Result.success()
    ↓
TransfersViewModel.observeWorkInfoById(workId):
    - 每 200ms 拉 workInfo.progress → 累加到对应 Transfer row
    - phase == DONE → TransfersRepository.refresh() 触发 Files 屏 reload
    - phase == FAILED → TransferStatus.FAILED + ToastBus.error
```

**关键点**:
- 步骤 3 是 M2 唯一复杂的状态分支,必须按 `OpenList/drivers/115_open/driver.go:302-335` 1:1 移植
- 步骤 4 (UploadGetToken) 与 Init 并行无依赖,可以放协程并行;但 OpenList 是顺序,M2 也顺序(简单)
- 步骤 5 的 OSS Bucket 操作全部用 STS token,不能复用现有 Bearer token 链路(`Cloud115ApiClient.baseBuilder` 不挂这里)

---

## 7. 状态机

```
UploadWorker:
    ENQUEUED → RUNNING → (setProgress repeatedly) → SUCCEEDED / FAILED / CANCELLED

Transfer(UP):
    RUNNING (进度 0% → 100%) → SUCCESS (收到 phase=DONE)
                              → FAILED (上传异常,toast 提示)

ForegroundInfo:
    title: 正在上传 / 已暂停 / 上传完成
    text:  "12.3 MB / 100 MB (12%)"
    ongoing: true(用户不可左划清除)
    progress: 0..100
```

---

## 8. 错误处理

按 [[no-ui-state-error-placeholders]] 项目约定:**失败一律走 `ToastBus.error(msg)`,不引入 `TransferUiState.Error` 分支**。

| 错误源 | 处理 |
|---|---|
| `UploadGetToken` 返回失败 / 401 | Token401Interceptor 自动 refresh + retry 一次,再失败 → Result.failure() + ToastBus |
| `UploadInit` 返回 code != 0 | Result.failure() + ToastBus("初始化失败:{msg}") |
| `UploadInit` Status == 2(秒传) | M2 不支持秒传:Result.failure() + ToastBus("暂不支持秒传") |
| `UploadInit` Status ∈ {6, 7, 8} 反复触发 two-way verify | 第二次校验仍不匹配 → Result.failure() + ToastBus("文件校验失败,请重试") |
| OSS PutObject 5xx / 网络中断 | 单分片 retry 3 次 + backoff,整体失败 → Result.failure() + ToastBus |
| OSS PutObject 4xx (签名错 / token 过期) | 不重试(签名错重试无意义),立即 FAILED + ToastBus |
| 用户取消 | `setProgress(workDataOf("phase" to "CANCELED"))` + Result.failure() |
| 文件 IO 异常(uri 已失效 / SD 卡拔出) | Result.failure() + ToastBus("文件读取失败") |

#### 8.1 115 业务错误码(401 系列触发 refresh)

来源 [[android-debug-checklist]] §② + OpenList `Is401Started`(`utils.go:21-24` = `code 字符串以 "401" 开头即 refresh`)。

115 Open API 业务错误码(只列 M2 上传相关):

| Code | 含义 | M2 处理 |
|---|---|---|
| `0` / `1` | 业务成功 / Init 进入上传阶段 | 走 §3.1.4 表 |
| `2` | 秒传成功 | M2 报"暂不支持秒传"(M3 实现) |
| `6` / `7` / `8` | two-way verify 触发 | 走 §6 步骤 3 分支 |
| `99` | access_token 过期(业务层 401) | Token401Interceptor refresh + retry 1 次 |
| `401` / `40140123` / `40140124` / `40140126` | 鉴权失败 / 签名错 / 格式错 / 验证错 | Token401Interceptor refresh + retry 1 次(若 refresh 后仍 401 → Result.failure) |
| 其它 4xx / 5xx | 业务错 | 直接 FAILED + ToastBus |

**Token401Interceptor 复用**:`Cloud115ApiClient.baseBuilder` 已挂 Token401Interceptor(commit `8745482` + `0e6ec08`),`uploadApiRetrofit` 用同一条链路,M2 **不实现**额外 refresh 逻辑。

**判断逻辑**:与 M1 `Token401Interceptor.parseErrorCode` 一致 —— `code == 99 || code.toString().startsWith("401")`,详见 `app/src/main/kotlin/com/starvault/data/remote/cloud115/Token401Interceptor.kt:193`。

---

## 9. 与 Transfers 屏联动

M1 `Transfer` 模型已支持 `Direction.UP`,无需新增字段。`TransfersViewModel` 改造:

```kotlin
// 伪代码
val workManager = WorkManager.getInstance(context)

fun enqueueUpload(uri: Uri, targetCid: String, fileName: String, size: Long) {
    val workId = uploadWorker.enqueue(uri, targetCid, fileName, size)
    transfersRepository.add(
        Transfer(
            id = workId.toString(),
            fileName = fileName,
            direction = Direction.UP,
            totalBytes = size,
            transferredBytes = 0,
            speedBps = 0,
            status = TransferStatus.RUNNING,
            startedAt = System.currentTimeMillis() / 1000,
        )
    )
}

fun observeUpload(workId: UUID) {
    workManager.getWorkInfoByIdLiveData(workId).observeForever { info ->
        val progress = info.progress.getLong("transferred", 0L)
        val phase = info.progress.getString("phase")
        transfersRepository.updateProgress(workId.toString(), progress, phase)
    }
}
```

UI 层(`TransfersScreen`)无需改动,已有的 `TransferRow.progress` 直接渲染。

---

## 10. 测试策略

### 10.1 JVM 单元测试(JUnit + MockK)

- `OssUploaderTest`:mock `OSS` + `Bucket`,验证 PutObject / UploadPart 调用顺序、Callback header、重试次数
- `Sha1HashingTest`:固定输入验证 SHA1 十六进制输出
- `UploadWorkerTest`:Robolectric + WorkManagerTestInitHelper,验证 `doWork()` 在 mock 依赖下的成功/失败路径

### 10.2 Paparazzi(仅 UI 截图回归)

- `UploadFilePicker` 复用 M1 FAB 样式,**不新增 Paparazzi test**(系统文件选择器是 Android 系统组件,无法在 Paparazzi 渲染)
- `TransfersScreen` 已有的 Paparazzi test 中,**新增一个 `transfers_uploading` 状态**(在 `mockTransfers` 加一条 `Direction.UP` + `RUNNING` 的 transfer),与现有 3 个状态(active / done / offline)并列

### 10.3 AVD 端到端验证(必做)

按 [[android-debug-checklist]] §⑤:
1. `./gradlew installDebug`
2. AVD 上点 FAB → 选一个 > 5MB 的本地文件(如相册大图)
3. logcat 确认看到 `--> GET https://proapi.115.com/open/upload/get_token` + `<-- 200`
4. logcat 确认看到 `--> POST https://proapi.115.com/open/upload/init` + `<-- 200`
5. logcat 确认看到 `--> PUT https://<endpoint>/<bucket>/<object>`(OSS,不是 proapi)
6. Transfers 屏看到 UP 行进度条从 0 走到 100
7. 切到 Files 屏 → 看到刚上传的文件

---

## 11. DoD(M2 完成定义)

- [ ] `./gradlew installDebug` 在 AVD 上能跑通"选文件 → 上传 → 完成 → Files 列表出现新文件"
- [ ] AVD 上传一个 50MB 视频,进度条实时更新,通知栏持续显示
- [ ] AVD 杀掉 App 后上传继续(WorkManager 持久化)
- [ ] AVD 网络断开 5 秒后恢复,上传自动续传(单分片重试,不要求断点续传)
- [ ] AVD 上传一个 25MB 文件(> 20MB 阈值),确认走 Multipart 路径(logcat 看 InitiateMultipartUpload + 2× UploadPart)
- [ ] `OssUploaderTest` / `Sha1HashingTest` / `UploadWorkerTest` 全部绿
- [ ] `TransfersScreenshotTest` 新增 `transfers_uploading` 状态,4 张 Paparazzi PNG baseline 一致
- [ ] 失败路径测试:故意传入不存在的文件 URI → ToastBus 提示 + Transfer FAILED + 不留垃圾文件
- [ ] CLAUDE.md 更新:补充上传端点对齐条目(接 §Network 章节现有端点表)
- [ ] 提交记录全部英文 message(参考 commit `4ee434f` / `6d12ec9` 风格)

---

## 12. 风险与待定

| # | 项 | 状态 | 风险 |
|---|---|---|---|
| 1 | `com.aliyun.oss:oss-android-sdk` 包体 | 待确认 | 2MB+,APK 增加 2MB;若不可接受,退回 okhttp 手写 ~300 行 |
| 2 | WorkManager `setProgress` 频率 | 已知 | 每分片完成一次,频率够(每 20MB 一次);不要每 chunk 报,徒增 IPC 开销 |
| 3 | `upload/sign_check` 区间 SHA1 校验 | 待验证 | OpenList Go SDK 行为是"区间不匹配时再算",我们的 Kotlin 实现要按此分支,不能直接拒 |
| 4 | ForegroundService 权限 | Android 14+ | 需要 `android.permission.FOREGROUND_SERVICE` + `android.permission.FOREGROUND_SERVICE_DATA_SYNC`,manifest 加 + run-time 不需要(普通权限) |
| 5 | `UploadInit` 字段命名大小写 | 已知 | `fileid`(小写)/ `preid`(小写)/ `pick_code`(下划线)/ `topupload`(小写连写),按 OpenList SDK 1:1,不能"想当然"改成 camelCase |
| 6 | 上传中切到后台 / 杀进程 | 已知 | **依赖 ForegroundInfo 提升优先级**(API 26+)+ WorkManager 持久化(API 23+),双保险才能扛住 OEM 后台杀进程(小米/华为)。M2 spec §4 已加 FOREGROUND_SERVICE_TYPE_DATA_SYNC,这是 Android 14+ 强制要求。 |
| 7 | M3+ 秒传依赖的 SHA1 完整链路 | 待定 | M2 复用 `Sha1Hashing.kt`(整文件 + 128KB),M3 加 `UploadResume` + 区间匹配即可,不重写 |
| 8 | `workInfo.progress` 跨进程读取限制 | 已知 | Data 字段 ≤ 10KB,文件 ID + 数字字节数足够;不传整个文件元数据 |

---

## 13. 关联文档

- **协议真相源**:
  - `https://github.com/OpenListTeam/115-sdk-go/upload.go`(upload.go:21-109 三端点签名)
  - `https://github.com/OpenListTeam/OpenList/drivers/115_open/upload.go`(单 PUT + Multipart + calPartSize + 重试 + 回调)
  - `https://github.com/OpenListTeam/OpenList/drivers/115_open/driver.go`(driver.go:272-349 SHA1 计算 + UploadInit + 区间重算)
- **M1 spec**: `docs/superpowers/specs/2026-06-14-starvault-android-skeleton-design.md`(§3 技术栈 / §4 项目结构 / §17 Phase 5 提纲)
- **M1 plan**: `docs/superpowers/plans/2026-06-14-starvault-android-skeleton.md`
- **M2 plan(下一步产物)**:本 spec 通过复核后由 `superpowers:writing-plans` 生成 TDD checklist plan

---

## 14. 待用户决策(本 spec 通过前需明确)

- [ ] **OSS SDK 选型**:用阿里云官方 SDK(包体 +2MB)还是 okhttp 手写(包体 +0 但代码 +300 行)?
- [ ] **前台通知样式**:系统默认还是自定义(进度条 + 暂停/取消按钮)?
- [ ] **失败重试策略**:仅单分片重试(简单,跟 OpenList 1:1)还是整体重试(用户体感更好但 WorkManager 状态机更复杂)?OpenList 默认前者,推荐一致。
- [ ] **目标目录默认值**:上传到"当前目录"(Files 屏选中状态)还是"根目录 / 我的文件"?
- [ ] **文件名冲突处理**:服务端同名文件默认行为是"(1)"后缀(M1 mock 行为),M2 是否保留?
- [ ] **Status == 2 秒传**:M2 遇到秒传成功是报"暂不支持"还是直接接受(M3 再补 UI 提示用户"该文件已存在,无需重复上传")?
- [ ] **`U_1_` target 前缀**:OpenList `upload.go:63` 强制加,服务端约定俗成。M2 是按 OpenList 1:1 加,还是反编译服务端确认这是必需前缀(如果服务端去前缀兼容,M2 可省)?推荐前者(跟 OpenList 一致)。