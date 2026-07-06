package de.kindermaenner.monatsblitz.infrastructure.api.client

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class ApiKeyInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestWithApiKey = originalRequest.newBuilder()
            .header("X-MB-Key", apiKey)
            .build()
        Log.i("ApiKeyInterceptor", "Added API key to request: ${requestWithApiKey.url}: ${requestWithApiKey.headers}")
        return chain.proceed(requestWithApiKey)
    }
}