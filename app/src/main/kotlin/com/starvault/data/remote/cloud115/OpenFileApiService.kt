package com.starvault.data.remote.cloud115

import retrofit2.Response
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
 *  - /open/ufile/downurl        POST  原图/文件直链(**必须传 UA,见 downloadUrl 注释**)
 *  - /open/video/play           GET   视频 m3u8(多清晰度,取 video_url[0])
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
     * POST /open/ufile/downurl — 文件直链(图片原图 / 任意文件下载 URL)。
     *
     * 官方文档:https://www.yuque.com/115yun/open/um8whr91bxb5997o
     * OpenList 同步实现:OpenListTeam/115-sdk-go/fs.go:308-319(DownURL)。
     *
     * **关键约束 — 必须传 Android UA**:
     *  115 CDN 端按 UA 签发不同 URL,移动端 UA 才能拿到 CDN 直链。
     *  OpenList driver 调用方式:`c.client.DownURL(ctx, pc, base.UserAgent)`(driver.go:151)。
     *  我们用 OkHttp 拦截器自动注入 Android UA(`browserLikeHeaderInterceptor`),
     *  调用方**不用**显式传 UA——但要确保 OkHttp client 走的是带拦截器的那条链路。
     *
     * 参数:
     *  - pick_code : 文件提取码(必填,从 getInfo 拿)
     *
     * 响应:[OpenDownUrlResponse]:
     *  - data : `Map<file_id, OpenDownUrlItem>`,key 是 file_id(string)
     *  - OpenDownUrlItem.url.url : **文件直链**(对图片 = 原图 CDN URL)
     *
     * **取数据方式**:OpenList 用 `resp[obj.GetID()]`,我们先有 file_id 再调,直接 `resp.data[fid]`。
     * 如果只有 pickCode 没有 fileId,fallback 取第一个 entry。
     */
    @POST("open/ufile/downurl")
    suspend fun downloadUrl(
        @Query("pick_code") pickCode: String,
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
}