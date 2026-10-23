package com.kynectia.tv.data

import android.content.Context
import com.kynectia.tv.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface TvApiService {

    @POST("tv/session")
    suspend fun createSession(): ApiResponse<TvSessionData>

    @GET("tv/session/{sessionId}")
    suspend fun getSessionStatus(@Path("sessionId") sessionId: String): ApiResponse<TvSessionStatus>

    @POST("tv/disconnect/{sessionId}")
    suspend fun disconnect(@Path("sessionId") sessionId: String): ApiResponse<Unit>

    @POST("tv/command")
    suspend fun sendRemoteCommand(@Body body: TvCommandRequest): ApiResponse<TvCommandResponse>
}

object TvApiClient {
    private var service: TvApiService? = null
    var baseUrl: String = BuildConfig.DEFAULT_BASE_URL
        set(value) {
            field = if (value.endsWith("/")) value else "$value/"
            service = null
        }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun getService(): TvApiService {
        return service ?: synchronized(this) {
            service ?: createService().also { service = it }
        }
    }

    private fun createService(): TvApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(3000, TimeUnit.MILLISECONDS)
            .readTimeout(3000, TimeUnit.MILLISECONDS)
            .writeTimeout(3000, TimeUnit.MILLISECONDS)
            .addInterceptor(loggingInterceptor)
            .build()

        val contentType = "application/json".toMediaType()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        return retrofit.create(TvApiService::class.java)
    }

    fun resolveMediaUrl(urlOrPath: String?): String? {
        if (urlOrPath.isNullOrBlank()) return null
        if (urlOrPath.startsWith("http://") || urlOrPath.startsWith("https://")) {
            return urlOrPath
        }
        val root = try {
            val uri = java.net.URI(baseUrl)
            if (uri.port != -1) {
                "${uri.scheme}://${uri.host}:${uri.port}"
            } else {
                "${uri.scheme}://${uri.host}"
            }
        } catch (_: Exception) {
            baseUrl.substringBefore("/api/v1").removeSuffix("/")
        }
        val cleanPath = if (urlOrPath.startsWith("/")) urlOrPath else "/$urlOrPath"
        return "$root$cleanPath"
    }
}
