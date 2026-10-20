package de.kindermaenner.monatsblitz.infrastructure.api.client

import android.util.Log
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

class DynamicBackendInterceptor(
    private val serverUrlProvider: () -> String,
    private val apiKeyProvider: () -> String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        val apiKey = apiKeyProvider().trim()
        if (apiKey.isNotEmpty()) {
            requestBuilder.header("X-MB-Key", apiKey)
        }

        val rawUrl = serverUrlProvider().trim()
        if (rawUrl.isNotEmpty()) {
            val normalized = UrlNormalizer.normalize(rawUrl)
            val targetBaseUrl = normalized.toHttpUrlOrNull()
            if (targetBaseUrl != null) {
                val originalPath = originalRequest.url.encodedPath
                val prefix = "/wp-json/monatsblitz/v1/"
                val subPath = if (originalPath.startsWith(prefix)) {
                    originalPath.removePrefix(prefix)
                } else {
                    originalPath.removePrefix("/")
                }

                val newUrlBuilder = targetBaseUrl.resolve(subPath)?.newBuilder()
                if (originalRequest.url.encodedQuery != null) {
                    newUrlBuilder?.encodedQuery(originalRequest.url.encodedQuery)
                }

                val newUrl = newUrlBuilder?.build()
                if (newUrl != null) {
                    requestBuilder.url(newUrl)
                    Log.i("DynamicBackendInterceptor", "Rewrote URL to: $newUrl")
                }
            }
        }

        return chain.proceed(requestBuilder.build())
    }
}
