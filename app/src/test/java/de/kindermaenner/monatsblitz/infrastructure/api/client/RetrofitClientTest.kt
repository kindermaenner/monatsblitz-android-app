package de.kindermaenner.monatsblitz.infrastructure.api.client

import org.junit.Assert.assertNotNull
import org.junit.Test

class RetrofitClientTest {

    @Test
    fun `createApi returns non-null MonatsblitzApi instance`() {
        val api = RetrofitClient.createApi("test-api-key")
        assertNotNull(api)
    }

    @Test
    fun `createApi with custom baseUrl with and without trailing slash`() {
        val api1 = RetrofitClient.createApi("key", "https://example.com/api/")
        assertNotNull(api1)

        val api2 = RetrofitClient.createApi("key", "https://example.com/api")
        assertNotNull(api2)
    }

    @Test
    fun `createDynamicApi returns non-null MonatsblitzApi instance`() {
        val dynamicApi = RetrofitClient.createDynamicApi(
            serverUrlProvider = { "https://example.com" },
            apiKeyProvider = { "dyn-key" }
        )
        assertNotNull(dynamicApi)
    }
}
