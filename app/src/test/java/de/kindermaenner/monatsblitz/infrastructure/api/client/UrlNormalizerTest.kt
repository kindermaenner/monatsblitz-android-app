package de.kindermaenner.monatsblitz.infrastructure.api.client

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlNormalizerTest {

    @Test
    fun `normalizes bare domain without scheme`() {
        val result = UrlNormalizer.normalize("sg-koenigslutter.de")
        assertEquals("https://sg-koenigslutter.de/wp-json/monatsblitz/v1/", result)
    }

    @Test
    fun `normalizes https url with trailing slash`() {
        val result = UrlNormalizer.normalize("https://sg-koenigslutter.de/")
        assertEquals("https://sg-koenigslutter.de/wp-json/monatsblitz/v1/", result)
    }

    @Test
    fun `normalizes http url with port and subdirectory`() {
        val result = UrlNormalizer.normalize("http://192.168.1.50:8080/my-site")
        assertEquals("http://192.168.1.50:8080/my-site/wp-json/monatsblitz/v1/", result)
    }

    @Test
    fun `does not duplicate wp-json path when already present without trailing slash`() {
        val result = UrlNormalizer.normalize("https://sg-koenigslutter.de/wp-json/monatsblitz/v1")
        assertEquals("https://sg-koenigslutter.de/wp-json/monatsblitz/v1/", result)
    }

    @Test
    fun `does not duplicate wp-json path when already present with trailing slash`() {
        val result = UrlNormalizer.normalize("https://sg-koenigslutter.de/wp-json/monatsblitz/v1/")
        assertEquals("https://sg-koenigslutter.de/wp-json/monatsblitz/v1/", result)
    }

    @Test
    fun `returns empty string for empty or blank input`() {
        assertEquals("", UrlNormalizer.normalize(""))
        assertEquals("", UrlNormalizer.normalize("   "))
    }
}
