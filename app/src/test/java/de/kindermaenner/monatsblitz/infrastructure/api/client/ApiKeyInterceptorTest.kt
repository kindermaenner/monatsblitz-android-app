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

class ApiKeyInterceptorTest {

    private val apiKey = "test-api-key"
    private val interceptor = ApiKeyInterceptor(apiKey)

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
    }

    @Test
    fun `intercept adds X-MB-Key header to request`() {
        val request = Request.Builder()
            .url("https://example.com")
            .build()
        
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.proceed(any()) } answers {
            val interceptedRequest = arg<Request>(0)
            assertEquals(apiKey, interceptedRequest.header("X-MB-Key"))
            mockk<Response>()
        }

        interceptor.intercept(chain)
    }
}
