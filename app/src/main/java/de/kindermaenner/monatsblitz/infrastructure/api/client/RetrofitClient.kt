package de.kindermaenner.monatsblitz.infrastructure.api.client

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

object RetrofitClient {

    private const val BASE_URL =
        "https://www.sg-koenigslutter.de/wp-json/monatsblitz/v1/"

    fun createApi(apiKey: String): MonatsblitzApi {

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(ApiKeyInterceptor(apiKey))
            .build()

        val json = Json {
            ignoreUnknownKeys = true
        }

        val contentType = "application/json".toMediaType()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        return retrofit.create(MonatsblitzApi::class.java)
    }
}