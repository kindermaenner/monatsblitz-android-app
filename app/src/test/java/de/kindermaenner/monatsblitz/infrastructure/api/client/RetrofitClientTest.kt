package de.kindermaenner.monatsblitz.infrastructure.api.client

import org.junit.Assert.assertNotNull
import org.junit.Test

class RetrofitClientTest {

    @Test
    fun `createApi returns non-null MonatsblitzApi instance`() {
        val api = RetrofitClient.createApi("test-api-key")
        assertNotNull(api)
    }
}
