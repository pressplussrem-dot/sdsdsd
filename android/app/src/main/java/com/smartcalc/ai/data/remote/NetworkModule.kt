package com.smartcalc.ai.data.remote

import com.smartcalc.ai.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Small hand-rolled DI. No API keys here: only the address of our own backend,
 * which is supplied at build time through the SMARTCALC_BACKEND_URL Gradle property.
 */
object NetworkModule {

    private const val TIMEOUT_SECONDS = 45L

    private val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .retryOnConnectionFailure(true)
            .build()
    }

    val solverApi: SolverApi by lazy {
        Retrofit.Builder()
            .baseUrl(normalizedBaseUrl())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SolverApi::class.java)
    }

    private fun normalizedBaseUrl(): String {
        val url = BuildConfig.BACKEND_BASE_URL
        return if (url.endsWith("/")) url else "$url/"
    }
}
