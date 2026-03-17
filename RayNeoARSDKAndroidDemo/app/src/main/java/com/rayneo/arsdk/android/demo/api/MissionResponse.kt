package com.rayneo.arsdk.android.demo.api

import com.google.gson.annotations.SerializedName

/**
 * 任务响应数据类
 */
data class MissionResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("task") val task: String,
    @SerializedName("name") val name: String,
    @SerializedName("state") val state: String
) 