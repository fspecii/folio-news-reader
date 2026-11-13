package dev.folio.news.data

data class Source(val id: String, val name: String, val url: String, val enabled: Boolean = true, val topic: String? = null)

data class Article(
    val id: String,
    val sourceId: String,
    val sourceName: String,
    val title: String,
    val summary: String,
    val image: String?,
    val link: String,
    val published: Long,
    val contentHtml: String?,
    val rank: Int = 0,
)

enum class ThemeMode { System, Light, Dark }

enum class TextKind { Title, Byline, Heading, Para, Quote, Bullet }

data class SpanInfo(val start: Int, val end: Int, val bold: Boolean, val italic: Boolean, val url: String? = null)

sealed interface Block {
    data class Text(val kind: TextKind, val text: String, val spans: List<SpanInfo> = emptyList()) : Block
    data class Image(val url: String, val caption: String? = null) : Block
}
