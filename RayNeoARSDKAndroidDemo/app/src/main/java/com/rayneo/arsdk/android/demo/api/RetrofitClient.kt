package com.rayneo.arsdk.android.demo.api

import android.util.Log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit客户端
 */
object RetrofitClient {
    
    // 日志标签
    private const val TAG = "RetrofitClient"
    
    // 服务器基础URL
    const val BASE_URL = "http://192.168.137.75:8000/"
    
    // Cookie容器，用于保存会话Cookie
    private val cookieStore = HashMap<String, List<Cookie>>()
    
    // 创建Cookie处理器
    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            Log.d(TAG, "Saving Cookie: domain=${url.host}, cookie count=${cookies.size}")
            cookieStore[url.host] = cookies
        }
        
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val cookies = cookieStore[url.host] ?: ArrayList()
            Log.d(TAG, "Loading Cookie: domain=${url.host}, cookie count=${cookies.size}")
            return cookies
        }
    }
    
    // 创建日志拦截器
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // 记录完整的请求和响应信息
        Log.d(TAG, "Creating logging interceptor: level=${level}")
    }
    
    // 添加自定义网络拦截器，用于记录更详细的网络请求和响应
    private val networkInterceptor = okhttp3.Interceptor { chain ->
        val request = chain.request()
        val requestBody = request.body
        
        Log.d(TAG, "发送请求: ${request.method} ${request.url}")
        Log.d(TAG, "请求头: ${request.headers}")
        
        if (requestBody != null) {
            Log.d(TAG, "请求体类型: ${requestBody.contentType()}")
            // 无法直接打印请求体内容，因为它可能是流式数据
            Log.d(TAG, "请求体大小: ${if (requestBody.contentLength() > 0) "${requestBody.contentLength()} bytes" else "未知"}")
        }
        
        val startTime = System.currentTimeMillis()
        val response = chain.proceed(request)
        val endTime = System.currentTimeMillis()
        
        Log.d(TAG, "收到响应: ${response.code} ${response.message}, 耗时: ${endTime - startTime}ms")
        Log.d(TAG, "响应头: ${response.headers}")
        
        response
    }

    // 创建OkHttpClient
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .cookieJar(cookieJar) // 添加Cookie处理器
        .addInterceptor(loggingInterceptor)
        .addNetworkInterceptor(networkInterceptor) // 添加网络拦截器
        .build().also {
            Log.d(TAG, "Creating OkHttpClient: connect timeout=${it.connectTimeoutMillis}ms, read timeout=${it.readTimeoutMillis}ms")
        }
    
    // 创建Retrofit实例
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build().also {
            Log.d(TAG, "Creating Retrofit client: server address=${BASE_URL}")
        }
    
    // 创建API服务
    val apiService: ApiService = retrofit.create(ApiService::class.java).also {
        Log.d(TAG, "Creating API service interface")
    }
    
    init {
        Log.d(TAG, "RetrofitClient initialization completed")
    }
} 