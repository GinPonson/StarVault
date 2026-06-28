package com.starvault.data.remote.cloud115

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 115 proapi open 域文件端点(base URL = proapi.115.com/)。
 *
 * **OAuth Bearer 鉴权专用**(参考 p115client/client.py:2541 / 2935 / 4256 + OpenListTeam/115-sdk-go/const.go)。
 *
 *  全部端点统一走 Bearer 鉴权,调用 proapi.115.com 域。
 *
 *  端点列表(全部 proapi):
 *  - /open/ufile/files          GET   目录文件列表(混合)
 *  - /open/ufile/search         GET   文件名搜索(全账号 substring)
 *  - /open/folder/get_info      GET   单文件/夹 metadata(Preview 入口)
 *  - /open/folder/add           POST  新建空文件夹(Files 屏 AddMenu)
 *  - /open/ufile/downurl        POST  原图/文件直链(**必须传 UA,见 downloadUrl 注释**)
 *  - /open/video/play           GET   视频 m3u8(多清晰度,取 video_url[0])
 *  - /open/ufile/update         POST  星标 / 重命名(form 字段二选一,Retrofit null 自动省略)
 *  - /open/ufile/delete         POST  批量删除(逗号分隔 file_ids,M5 CRUD)
 *  - /open/ufile/move           POST  批量移动(逗号分隔 file_ids + to_cid,M5 CRUD)
 *
 *  所有端点需登录态(Bearer 由 [AuthHeaderInterceptor] 注入)。
 */
interface OpenFileApiService {

    /**
     * GET /open/ufile/files — 列出某目录的子项(文件夹 + 文件 混合)。
     *
     * 关键参数(proapi 域):
     *  - `aid=1` (固定)
     *  - `cid`  父目录 id,根目录为 0
     *  - `o`    排序字段(proapi 接受 `user_ptime` / `file_name` 等)
     *  - `asc`  0=降序 1=升序
     *  - `offset` / `limit`
     *  - `show_dir` 固定 1(取目录+文件混合)
     *  - `fc_mix` 固定 1(混合排序)
     *  - `count_folders` 固定 1(返回目录计数)
     *  - `nf` 0=显示文件,null=显示全部
     *
     * 响应: 顶层 { state, count, data, path, ... }(**没有 data 包装壳**),
     * 复用 [FileListResponse](字段名与 webapi 同款,115 没改)。
     *
     * 参考:p115client/client.py:2541 / OpenListTeam/115-sdk-go/fs.go。
     */
    @GET("open/ufile/files")
    suspend fun listFiles(
        @Query("aid") aid: Int = 1,
        @Query("cid") cid: String,
        @Query("o") order: String = "user_ptime",
        @Query("asc") asc: Int = 0,
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 50,
        @Query("show_dir") showDir: Int = 1,
        @Query("fc_mix") fcMix: Int = 1,
        @Query("count_folders") countFolders: Int = 1,
        @Query("nf") nf: Int? = null,
    ): Response<FileListResponse>

    /**
     * GET /open/ufile/search — 文件名搜索(全账号 substring 匹配)。
     *
     * 与 listFiles 同 DTO,只是 path 不同。
     *
     * 参考:p115client/client.py:2935 / OpenListTeam/115-sdk-go/fs.go(SearchFiles)。
     */
    @GET("open/ufile/search")
    suspend fun searchFiles(
        @Query("search_value") searchValue: String,
        @Query("aid") aid: Int = 1,
        @Query("cid") cid: String = "0",
        @Query("o") order: String = "user_ptime",
        @Query("asc") asc: Int = 0,
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 50,
        @Query("show_dir") showDir: Int = 1,
        @Query("fc_mix") fcMix: Int = 1,
        @Query("count_folders") countFolders: Int = 1,
    ): Response<FileListResponse>

    /**
     * GET /open/folder/get_info — 单文件(夹)详情(Preview 入口拿 metadata)。
     *
     * 官方文档:https://www.yuque.com/115yun/open/rl8zrhe2nag21dfw
     * OpenList 同步实现:OpenListTeam/115-sdk-go/fs.go:187-211(GetFolderInfo)。
     *
     * 参数:
     *  - file_id : 文件/夹 ID(必填,与 path 二选一;MVP 只用 file_id)
     *
     * 响应:[OpenFolderInfoResponse]:
     *  - state / message / code
     *  - data : 单 object(不是 array!),含 file_id / file_name / pick_code / sha1
     *    / file_size / file_type / file_category / play_long / ptime / utime / paths[]
     *
     * 字段名跟 webapi /files/get_info 完全不同,见 [OpenFolderInfoData] 注释里的映射表。
     */
    @GET("open/folder/get_info")
    suspend fun getInfo(
        @Query("file_id") fileId: String,
    ): Response<OpenFolderInfoResponse>

    /**
     * POST /open/folder/add — 新建空文件夹(Files 屏 AddMenu → "新建文件夹")。
     *
     * 官方文档:https://www.yuque.com/115yun/open/qur839kyx9cgxpxi
     * OpenList 同步实现:OpenListTeam/115-sdk-go/fs.go:17-25(Mkdir)。
     *
     * **必须走 form-urlencoded body**(对齐 OpenList `ReqWithForm(SetFormData(...))`):
     *  - field `pid`       : 父目录 cid;根目录传 "0"
     *  - field `file_name` : 新文件夹名(由 UI 层 trim 完再传,Retrofit 自动 URL encode)
     *
     * 响应:[OpenFolderAddResponse]:
     *  - state / message / code
     *  - data : { file_name, file_id } — 创建后的文件夹元信息(回显名可能含 115 端转码)
     *
     * 错误场景(state=false):
     *  - 重名同层目录 → 115 端拒
     *  - 父目录不存在 / 无权限 → 40140123 / 990001 等
     *  - file_name 含 `/` 或其他保留字符 → 115 端返回 file_name 已替换为安全名
     */
    @FormUrlEncoded
    @POST("open/folder/add")
    suspend fun addFolder(
        @Field("pid") pid: String,
        @Field("file_name") fileName: String,
    ): Response<OpenFolderAddResponse>

    /**
     * POST /open/ufile/downurl — 文件直链(图片原图 / 任意文件下载 URL)。
     *
     * 官方文档:https://www.yuque.com/115yun/open/um8whr91bxb5997o
     * OpenList 同步实现:OpenListTeam/115-sdk-go/fs.go:308-319(DownURL)。
     *
     * **关键对齐 OpenList**:
     *  - `pick_code` 必须放在 **form-urlencoded body**(对齐 OpenList `ReqWithForm(SetFormData(...))`)。
     *    之前用 `@Query` 把它塞 URL query string,115 CDN 端一直挂死不响应(直到 30s callTimeout)。
     *  - **必须传 Android UA**(对齐 OpenList `ReqWithUA(base.UserAgent)`):115 CDN 按 UA 签发不同 URL,
     *    移动端 UA 才能拿到 CDN 直链。我们用 OkHttp 拦截器自动注入(`browserLikeHeaderInterceptor`),
     *    调用方**不用**显式传 UA——但要确保 OkHttp client 走的是带拦截器的那条链路。
     *
     * 参数:
     *  - pick_code : 文件提取码(必填,从 getInfo 拿),作为 form field 提交
     *
     * 响应:[OpenDownUrlResponse]:
     *  - data : `Map<file_id, OpenDownUrlItem>`,key 是 file_id(string)
     *  - OpenDownUrlItem.url.url : **文件直链**(对图片 = 原图 CDN URL)
     *
     * **取数据方式**:OpenList 用 `resp[obj.GetID()]`,我们先有 file_id 再调,直接 `resp.data[fid]`。
     * 如果只有 pickCode 没有 fileId,fallback 取第一个 entry。
     */
    @FormUrlEncoded
    @POST("open/ufile/downurl")
    suspend fun downloadUrl(
        @Field("pick_code") pickCode: String,
    ): Response<OpenDownUrlResponse>

    /**
     * GET /open/video/play — 视频 m3u8(多清晰度,Preview 用)。
     *
     * 官方文档:https://www.yuque.com/115yun/open/hqglxv3cedi3p9dz
     * OpenList 同步实现:OpenListTeam/115-sdk-go/video.go:32-44(VideoPlay)。
     *
     * 参数:
     *  - pick_code : 文件提取码(必填,从 getInfo 拿)
     *
     * 响应:[OpenVideoPlayResponse]:
     *  - data.video_url : **array**[{url, definition, desc}],取第一个 url 即可
     *  - data.duration  : 视频时长(秒),字段名跟 webapi 的 `play_long` 不同
     *
     * **与 webapi /files/video 的差异**:
     *  - video_url 从 string 变 array(选 [videoUrl] 第一项即可,服务端按可用性排序)
     *  - duration 替代 play_long
     *  - 没有 thumbnail_url 字段(缩略图走 Files 列表的 `u` 字段)
     */
    @GET("open/video/play")
    suspend fun videoPlay(
        @Query("pick_code") pickCode: String,
    ): Response<OpenVideoPlayResponse>

    /**
     * POST /open/ufile/update — 更新文件(夹)元数据(星标 / 重命名 / 备注 等)。
     *
     * 官方文档:https://www.yuque.com/115yun/open/gyrpw5a0zc4sengm
     * OpenList 同步实现:OpenListTeam/115-sdk-go/fs.go:25-35(Update)。
     * p115client 封装:client.py:3386 fs_star_set → fs_update_open (client.py:3673 fs_update)。
     *
     * **必须走 form-urlencoded body**(对齐 OpenList `ReqWithForm(SetFormData(...))`)。
     *
     * 参数(对齐 115 文档,所有可更新字段都 optional,但 115 端要求至少传一个):
     *  - file_id  : 文件(夹)ID(必填);**注意单选,多选用 `file_id[0]` / `file_id[1]`**(p115client 支持,MVP 用不到)
     *  - star     : 0=取消星标,1=设置星标(null 时不更新星标)
     *  - file_name: 新文件名(null 时不更新文件名)
     *
     * **Retrofit null 行为**(2.6+):可空字段值为 null 时**自动从 form body 省略**,不会发送
     * 字符串 "null"。所以 `updateFile(fileId, star = 1)` 只发 file_id + star,`updateFile(
     * fileId, fileName = "新名")` 只发 file_id + file_name — 不会互相覆盖,不会误清星标。
     *
     * 响应:[OpenFileUpdateResponse] — 顶层 `{state, code, message}` 三件套,无 data 字段(115 update 端点响应不带 data)。
     *
     * **对齐 115 文档**:star 操作在文件已被删除的情况下**仍可成功**(p115client 文档明确),
     * 所以此处失败语义只有"网络错" / "鉴权错"(401) / "参数错"(code 非 0)。
     *
     * **重命名单文件限制**:115 update 端点**单文件**(不接 `file_ids` 多选,多选 rename 要循环
     * 串行调)。Repository 层封装了循环(见 [FilesRepository.renameFile])。
     */
    @FormUrlEncoded
    @POST("open/ufile/update")
    suspend fun updateFile(
        @Field("file_id") fileId: String,
        @Field("star") star: Int? = null,
        @Field("file_name") fileName: String? = null,
    ): Response<OpenFileUpdateResponse>

    /**
     * POST /open/ufile/delete — 批量删除文件/夹(走回收站,可恢复 7 天内)。
     *
     * OpenList 同步实现:OpenListTeam/115-sdk-go/fs.go:36-46(Delete)。
     *
     * **必须走 form-urlencoded body**;`file_ids` 是**逗号分隔的字符串**(对齐 OpenList
     * `SetFormData(map[string]string{"file_ids": strings.Join(ids, ",")})`),Retrofit @Field
     * 不能直接接 List<String>。Repository 层负责 join。
     *
     * 参数:
     *  - file_ids : 逗号分隔的 fid 列表,e.g. `"123,456,789"`,空字符串 115 端会拒
     *
     * 响应:[OpenFileDeleteResponse] — 顶层 `{state, code, message}`,无 data 字段。
     *
     * **错误场景**:
     *  - 部分 id 不存在 / 已删除 → 115 端会成功(回收站幂等)或 state=false + message 非空
     *  - 越权(删他人分享的文件)→ 40140123 等 401 家族
     *  - 网络错 / 鉴权错 → Retrofit 层 / Token401Interceptor
     *
     * **删除不可逆语义**:虽然 115 有 7 天回收站,但客户端 UI 不提供"恢复"功能;Files 屏
     * 调用前必须弹 [ConfirmDialog] 让用户确认(见 FilesViewModel.bulk DELETE 路径)。
     */
    @FormUrlEncoded
    @POST("open/ufile/delete")
    suspend fun deleteFiles(
        @Field("file_ids") fileIds: String,
    ): Response<OpenFileDeleteResponse>

    /**
     * POST /open/ufile/move — 批量移动文件/夹到目标目录。
     *
     * OpenList 同步实现:OpenListTeam/115-sdk-go/fs.go:48-72(Move)。
     *
     * **必须走 form-urlencoded body**;`file_ids` 同 [deleteFiles] 是逗号分隔字符串。
     *
     * 参数:
     *  - file_ids : 逗号分隔的 fid 列表
     *  - to_cid   : 目标目录 cid;根目录传 "0",不能传文件 fid(会报错 code=990001)
     *
     * 响应:[OpenFileMoveResponse] — 顶层 `{state, code, message}`,无 data 字段。
     *
     * **错误场景**:
     *  - to_cid 指向文件 / 不存在 → state=false + message 非空(990001 家族)
     *  - file_ids 包含目标目录的子项(自循环)→ 115 端拒
     *  - 跨盘 / 权限不足 → 40140123
     *
     * **FolderPicker excludeIds** :UI 层(见 [FolderPickerRoute])需把"不允许选中的目录
     * id 集合"(当前已选 + 其所有祖先)传给 FilesViewModel 过滤行,避免移到自身/祖先造成
     * 自循环;此端点本身无 exclude 机制。
     */
    @FormUrlEncoded
    @POST("open/ufile/move")
    suspend fun moveFiles(
        @Field("file_ids") fileIds: String,
        @Field("to_cid") toCid: String,
    ): Response<OpenFileMoveResponse>
}