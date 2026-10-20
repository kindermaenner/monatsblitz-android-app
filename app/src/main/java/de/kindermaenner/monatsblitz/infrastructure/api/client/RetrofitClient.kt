package de.kindermaenner.monatsblitz.infrastructure.api.client

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

object RetrofitClient {

    const val DEFAULT_BASE_URL = "https://dynamic.monatsblitz.local/wp-json/monatsblitz/v1/"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    private val contentType = "application/json".toMediaType()

    fun createDynamicApi(
        serverUrlProvider: () -> String,
        apiKeyProvider: () -> String
    ): MonatsblitzApi {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(DynamicBackendInterceptor(serverUrlProvider, apiKeyProvider))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(DEFAULT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        return retrofit.create(MonatsblitzApi::class.java)
    }

    fun createApi(apiKey: String, baseUrl: String = "https://www.sg-koenigslutter.de/wp-json/monatsblitz/v1/"): MonatsblitzApi {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(ApiKeyInterceptor(apiKey))
            .build()

        val normalizedBaseUrl = UrlNormalizer.normalize(baseUrl).ifEmpty {
            if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        return retrofit.create(MonatsblitzApi::class.java)
    }
}
