package com.rayneo.arsdk.android.demo.api

import com.google.gson.annotations.SerializedName

/**
 * CSRF令牌响应数据类
 */
data class CsrfTokenResponse(
    @SerializedName("csrfToken")
    val csrfToken: String
) 