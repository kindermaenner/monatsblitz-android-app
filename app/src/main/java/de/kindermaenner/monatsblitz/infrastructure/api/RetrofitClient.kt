package de.kindermaenner.monatsblitz.infrastructure.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.java8.Java8CallAdapterFactory

object RetrofitClient {
    private const val BASE_URL = "https://www.sg-koenigslutter.de/wp-json/monatsblitz/v1/"

    fun createApi(apiKey: String): MonatsblitzApi {
        val httpClient = OkHttpClient.Builder()
            .addInterceptor(ApiKeyInterceptor(apiKey))
            .build()

        val json = Json { ignoreUnknownKeys = true }
        val contentType = "application/json".toMediaType()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .addCallAdapterFactory(Java8CallAdapterFactory.create())
            .build()

        return retrofit.create(MonatsblitzApi::class.java)
    }
}
