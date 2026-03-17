package com.rayneo.arsdk.android.demo.api

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * API服务接口
 */
interface ApiService {
    
    /**
     * 获取CSRF令牌
     * @return CSRF令牌响应
     */
    @GET("/csrf/api/get-csrf-token/")
    suspend fun getCsrfToken(): Response<CsrfTokenResponse>
    
    /**
     * 上传照片到服务器
     * @param csrfToken CSRF令牌(作为HTTP头部)
     * @param name 照片名称
     * @param imageFile 照片文件
     * @return 响应体
     */
    @Multipart
    @POST("/image/image_info/")
    suspend fun uploadPhoto(
        @Header("X-CSRFToken") csrfToken: String,
        @Part name: MultipartBody.Part,
        @Part imageFile: MultipartBody.Part
    ): Response<ResponseBody>

    /**
     * 上传照片到服务器（带place参数）
     * @param csrfToken CSRF令牌(作为HTTP头部)
     * @param name 照片名称
     * @param place 位置信息
     * @param imageFile 照片文件
     * @return 响应体
     */
    @Multipart
    @POST("/image/image_info/")
    suspend fun uploadPhotoWithPlace(
        @Header("X-CSRFToken") csrfToken: String,
        @Part name: MultipartBody.Part,
        @Part place: MultipartBody.Part,
        @Part imageFile: MultipartBody.Part
    ): Response<ResponseBody>
    
    /**
     * 上传照片到服务器（带完整任务信息）
     * @param csrfToken CSRF令牌(作为HTTP头部)
     * @param name 照片名称
     * @param place 位置信息
     * @param taskName 任务名称
     * @param executorName 执行人名称
     * @param imageFile 照片文件
     * @return 响应体
     */
    @Multipart
    @POST("/image/image_info/")
    suspend fun uploadPhotoWithTaskInfo(
        @Header("X-CSRFToken") csrfToken: String,
        @Part name: MultipartBody.Part,
        @Part place: MultipartBody.Part,
        @Part task_name: MultipartBody.Part,
        @Part executor: MultipartBody.Part,
        @Part imageFile: MultipartBody.Part
    ): Response<ResponseBody>
    
    /**
     * 获取未完成的任务列表
     * @return 未完成任务列表
     */
    @GET("/mission/api/missions/unfinished/")
    suspend fun getUnfinishedMissions(): Response<List<MissionResponse>>
    
    /**
     * 将任务标记为已完成
     * @param missionId 任务ID
     * @return 响应体
     */
    @POST("/mission/api/missions/complete/{missionId}/")
    suspend fun completeMission(
        @Path("missionId") missionId: Int,
        @Header("X-CSRFToken") csrfToken: String
    ): Response<ResponseBody>
} 