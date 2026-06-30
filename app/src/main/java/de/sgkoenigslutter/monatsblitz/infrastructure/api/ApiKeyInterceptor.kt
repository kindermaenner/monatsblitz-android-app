package de.sgkoenigslutter.monatsblitz.infrastructure.api

import okhttp3.Interceptor
import okhttp3.Response

class ApiKeyInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestWithApiKey = originalRequest.newBuilder()
            .header("X-API-Key", apiKey)
            .build()
        return chain.proceed(requestWithApiKey)
    }
}
