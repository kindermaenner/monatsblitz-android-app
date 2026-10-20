package de.kindermaenner.monatsblitz.infrastructure.api.client

import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DynamicBackendInterceptorTest {

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
    }

    @Test
    fun `rewrites URL and adds api key header`() {
        val interceptor = DynamicBackendInterceptor(
            serverUrlProvider = { "https://sg-koenigslutter.de" },
            apiKeyProvider = { "secret-key-123" }
        )

        val request = Request.Builder()
            .url("https://dynamic.monatsblitz.local/wp-json/monatsblitz/v1/players")
            .build()

        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.proceed(any()) } answers {
            val intercepted = arg<Request>(0)
            assertEquals("https://sg-koenigslutter.de/wp-json/monatsblitz/v1/players", intercepted.url.toString())
            assertEquals("secret-key-123", intercepted.header("X-MB-Key"))
            mockk<Response>()
        }

        interceptor.intercept(chain)
    }

    @Test
    fun `preserves query parameters during URL rewrite`() {
        val interceptor = DynamicBackendInterceptor(
            serverUrlProvider = { "https://sg-koenigslutter.de" },
            apiKeyProvider = { "secret-key-123" }
        )

        val request = Request.Builder()
            .url("https://dynamic.monatsblitz.local/wp-json/monatsblitz/v1/games/42?limit=10")
            .build()

        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.proceed(any()) } answers {
            val intercepted = arg<Request>(0)
            assertEquals("https://sg-koenigslutter.de/wp-json/monatsblitz/v1/games/42?limit=10", intercepted.url.toString())
            mockk<Response>()
        }

        interceptor.intercept(chain)
    }

    @Test
    fun `leaves URL unchanged if serverUrlProvider is empty`() {
        val interceptor = DynamicBackendInterceptor(
            serverUrlProvider = { "" },
            apiKeyProvider = { "" }
        )

        val request = Request.Builder()
            .url("https://dynamic.monatsblitz.local/wp-json/monatsblitz/v1/players")
            .build()

        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.proceed(any()) } answers {
            val intercepted = arg<Request>(0)
            assertEquals("https://dynamic.monatsblitz.local/wp-json/monatsblitz/v1/players", intercepted.url.toString())
            mockk<Response>()
        }

        interceptor.intercept(chain)
    }
}
