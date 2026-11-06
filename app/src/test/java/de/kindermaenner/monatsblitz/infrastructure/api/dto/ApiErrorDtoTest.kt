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

    @Test
    fun `toDetailedErrorMessage formats HttpException with plugin message`() {
        val json = """{"message":"Turnier ist bereits beendet"}"""
        val response = retrofit2.Response.error<Any>(400, okhttp3.ResponseBody.create(null, json))
        val exception = retrofit2.HttpException(response)

        val result = exception.toDetailedErrorMessage()

        assertEquals("HTTP 400 Response.error(): Turnier ist bereits beendet", result)
    }

    @Test
    fun `toDetailedErrorMessage formats HttpException with raw body when not structured json`() {
        val raw = "Bad Gateway"
        val response = retrofit2.Response.error<Any>(502, okhttp3.ResponseBody.create(null, raw))
        val exception = retrofit2.HttpException(response)

        val result = exception.toDetailedErrorMessage()

        assertEquals("HTTP 502 Response.error(): Bad Gateway", result)
    }

    @Test
    fun `toDetailedErrorMessage formats HttpException without body`() {
        val response = retrofit2.Response.error<Any>(404, okhttp3.ResponseBody.create(null, ""))
        val exception = retrofit2.HttpException(response)

        val result = exception.toDetailedErrorMessage()

        assertEquals("HTTP 404 Response.error()", result)
    }

    @Test
    fun `toDetailedErrorMessage formats generic exception with message`() {
        val exception = IllegalArgumentException("Ungültige Rundenanzahl")
        assertEquals("Ungültige Rundenanzahl", exception.toDetailedErrorMessage())
    }

    @Test
    fun `toDetailedErrorMessage falls back to simpleName when message is blank or null`() {
        val exceptionWithBlank = RuntimeException("   ")
        assertEquals("RuntimeException", exceptionWithBlank.toDetailedErrorMessage())

        val exceptionWithNull = NullPointerException()
        assertEquals("NullPointerException", exceptionWithNull.toDetailedErrorMessage())
    }
}
