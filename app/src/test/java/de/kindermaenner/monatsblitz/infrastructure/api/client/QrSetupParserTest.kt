package de.kindermaenner.monatsblitz.infrastructure.api.client

import de.kindermaenner.monatsblitz.domain.model.ModePreselectionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class QrSetupParserTest {

    @Test
    fun `parses valid json setup payload`() {
        val json = """{"url":"https://sg-koenigslutter.de","key":"secret-key-123"}"""
        val config = QrSetupParser.parse(json)

        assertNotNull(config)
        assertEquals("https://sg-koenigslutter.de", config?.serverUrl)
        assertEquals("secret-key-123", config?.apiKey)
        assertNull(config?.modePreselectionType)
        assertNull(config?.fixedMode)
        assertNull(config?.rotationModes)
    }

    @Test
    fun `parses json setup payload with fixed mode preselection`() {
        val json = """{
            "url": "https://sg-koenigslutter.de",
            "key": "secret-key-123",
            "mode_type": "fixed",
            "fixed_mode": "3+2"
        }"""
        val config = QrSetupParser.parse(json)

        assertNotNull(config)
        assertEquals("https://sg-koenigslutter.de", config?.serverUrl)
        assertEquals("secret-key-123", config?.apiKey)
        assertEquals(ModePreselectionType.FIXED, config?.modePreselectionType)
        assertEquals("3+2", config?.fixedMode)
        assertNull(config?.rotationModes)
    }

    @Test
    fun `parses json setup payload with rotation mode preselection`() {
        val json = """{
            "url": "https://sg-koenigslutter.de",
            "key": "secret-key-123",
            "mode_type": "rotation",
            "rotation_modes": ["3+2", "5+0"]
        }"""
        val config = QrSetupParser.parse(json)

        assertNotNull(config)
        assertEquals("https://sg-koenigslutter.de", config?.serverUrl)
        assertEquals("secret-key-123", config?.apiKey)
        assertEquals(ModePreselectionType.ROTATION, config?.modePreselectionType)
        assertEquals(listOf("3+2", "5+0"), config?.rotationModes)
        assertNull(config?.fixedMode)
    }

    @Test
    fun `parses uri setup payload with mode preselection`() {
        val uri = "monatsblitz://setup?url=https%3A%2F%2Fsg-koenigslutter.de&key=secret-key-123&mode_type=rotation&rotation_modes=3%2B2%2C5%2B0"
        val config = QrSetupParser.parse(uri)

        assertNotNull(config)
        assertEquals("https://sg-koenigslutter.de", config?.serverUrl)
        assertEquals("secret-key-123", config?.apiKey)
        assertEquals(ModePreselectionType.ROTATION, config?.modePreselectionType)
        assertEquals(listOf("3+2", "5+0"), config?.rotationModes)
    }

    @Test
    fun `parses valid json with whitespace and extra fields`() {
        val json = """
            {
               "url": "  https://mein-schachverein.de/  ",
               "key": "  abc-xyz-789  ",
               "version": 1
            }
        """.trimIndent()
        val config = QrSetupParser.parse(json)

        assertNotNull(config)
        assertEquals("https://mein-schachverein.de/", config?.serverUrl)
        assertEquals("abc-xyz-789", config?.apiKey)
    }

    @Test
    fun `parses valid uri setup payload`() {
        val uri = "monatsblitz://setup?url=https%3A%2F%2Fsg-koenigslutter.de&key=secret-key-123"
        val config = QrSetupParser.parse(uri)

        assertNotNull(config)
        assertEquals("https://sg-koenigslutter.de", config?.serverUrl)
        assertEquals("secret-key-123", config?.apiKey)
    }

    @Test
    fun `returns null for empty or blank input`() {
        assertNull(QrSetupParser.parse(""))
        assertNull(QrSetupParser.parse("   "))
    }

    @Test
    fun `returns null for json with missing key or url`() {
        assertNull(QrSetupParser.parse("""{"url":"https://example.com"}"""))
        assertNull(QrSetupParser.parse("""{"key":"secret"}"""))
        assertNull(QrSetupParser.parse("""{"url":"","key":""}"""))
    }

    @Test
    fun `returns null for uri with missing parameters`() {
        assertNull(QrSetupParser.parse("monatsblitz://setup?url=https%3A%2F%2Fexample.com"))
        assertNull(QrSetupParser.parse("monatsblitz://setup?key=secret"))
        assertNull(QrSetupParser.parse("monatsblitz://setup"))
    }

    @Test
    fun `returns null for non-setup random text`() {
        assertNull(QrSetupParser.parse("Hello World"))
        assertNull(QrSetupParser.parse("WIFI:S:MyNetwork;T:WPA;P:MyPassword;;"))
    }
}
