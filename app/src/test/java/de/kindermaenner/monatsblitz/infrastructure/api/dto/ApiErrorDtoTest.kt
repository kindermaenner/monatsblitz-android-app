package de.kindermaenner.monatsblitz.infrastructure.api.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ApiErrorDtoTest {

    @Test
    fun `extractMessage returns message from valid json`() {
        val json = """{"code":"rest_api_version_mismatch","message":"Inkompatible API-Version"}"""
        val result = ApiErrorDto.extractMessage(json)
        assertEquals("Inkompatible API-Version", result)
    }

    @Test
    fun `extractMessage returns null when message is empty or missing`() {
        assertNull(ApiErrorDto.extractMessage("""{"code":"error"}"""))
        assertNull(ApiErrorDto.extractMessage("""{"message":"   "}"""))
    }

    @Test
    fun `extractMessage returns null on invalid json or null`() {
        assertNull(ApiErrorDto.extractMessage(null))
        assertNull(ApiErrorDto.extractMessage(""))
        assertNull(ApiErrorDto.extractMessage("<html>Server Error</html>"))
    }
}
