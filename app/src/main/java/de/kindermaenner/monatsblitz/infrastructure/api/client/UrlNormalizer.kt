package de.kindermaenner.monatsblitz.infrastructure.api.client

object UrlNormalizer {

    private const val DEFAULT_SCHEME = "https://"
    private const val WP_API_PATH = "/wp-json/monatsblitz/v1"

    fun normalize(rawUrl: String): String {
        var url = rawUrl.trim()
        if (url.isEmpty()) return ""

        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            url = DEFAULT_SCHEME + url
        }

        while (url.endsWith("/")) {
            url = url.dropLast(1)
        }

        if (!url.endsWith(WP_API_PATH, ignoreCase = true)) {
            url = "$url$WP_API_PATH"
        }

        return "$url/"
    }
}
