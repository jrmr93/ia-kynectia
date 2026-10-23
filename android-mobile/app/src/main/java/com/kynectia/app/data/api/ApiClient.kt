package com.kynectia.app.data.api

import android.content.Context
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var serviceInstance: KynectiaApiService? = null
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun getService(context: Context): KynectiaApiService {
        return serviceInstance ?: synchronized(this) {
            serviceInstance ?: createService(context).also { serviceInstance = it }
        }
    }

    fun reset() {
        synchronized(this) {
            serviceInstance = null
        }
    }

    private fun createService(context: Context): KynectiaApiService {
        val sessionManager = SessionManager.getInstance(context)

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(2500, TimeUnit.MILLISECONDS)
            .readTimeout(2500, TimeUnit.MILLISECONDS)
            .writeTimeout(2500, TimeUnit.MILLISECONDS)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                sessionManager.authToken?.let { token ->
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
                requestBuilder.addHeader("Accept", "application/json")
                chain.proceed(requestBuilder.build())
            }
            .addInterceptor(loggingInterceptor)
            .build()

        val contentType = "application/json".toMediaType()

        val retrofit = Retrofit.Builder()
            .baseUrl(sessionManager.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        return retrofit.create(KynectiaApiService::class.java)
    }

    fun resolveMediaUrl(context: Context, urlOrPath: String?): String? {
        if (urlOrPath.isNullOrBlank()) return null
        val sessionManager = SessionManager.getInstance(context)
        val isEmulator = sessionManager.isEmulator

        if (urlOrPath.startsWith("http://") || urlOrPath.startsWith("https://")) {
            if (!isEmulator && (urlOrPath.contains("://localhost") || urlOrPath.contains("://127.0.0.1") || urlOrPath.contains("://10.0.2.2"))) {
                return urlOrPath
                    .replace("://localhost", "://192.168.0.24")
                    .replace("://127.0.0.1", "://192.168.0.24")
                    .replace("://10.0.2.2", "://192.168.0.24")
            }
            return urlOrPath
        }

        val baseUrl = sessionManager.baseUrl
        val root = try {
            val uri = java.net.URI(baseUrl)
            val host = if (!isEmulator && (uri.host == "localhost" || uri.host == "127.0.0.1" || uri.host == "10.0.2.2")) {
                "192.168.0.24"
            } else {
                uri.host
            }
            if (uri.port != -1) {
                "${uri.scheme}://$host:${uri.port}"
            } else {
                "${uri.scheme}://$host"
            }
        } catch (_: Exception) {
            baseUrl.substringBefore("/api/v1").removeSuffix("/")
        }
        val cleanPath = if (urlOrPath.startsWith("/")) urlOrPath else "/$urlOrPath"
        return "$root$cleanPath"
    }
}
