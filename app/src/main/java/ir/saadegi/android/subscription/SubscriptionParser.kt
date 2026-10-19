package ir.saadegi.android.subscription

import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import java.util.Locale

/** These are display-only descriptors; never keep secrets/URIs in Compose state or logs. */
data class SubscriptionNode(
    val id: String,
    val title: String,
    val protocol: String,
    val transport: String,
    val location: String,
    val compatibleWithCurrentEngine: Boolean,
)

data class SubscriptionUsage(
    val usedBytes: Long,
    val totalBytes: Long,
    val expiresAtSeconds: Long?,
)

data class ParsedSubscription(
    val nodes: List<SubscriptionNode>,
    val usage: SubscriptionUsage?,
    val updatedAtMillis: Long,
)

/** Parser for standard URI subscription formats; no network or Android dependency. */
object SubscriptionParser {
    private const val MAX_BYTES = 1_048_576
    private const val MAX_NODES = 1500
    private val prefixes = listOf("vless://", "vmess://", "trojan://", "ss://", "hysteria2://", "hy2://", "tuic://", "wireguard://")
    private val protocolsXray = setOf("vless", "vmess", "trojan", "shadowsocks")

    fun parse(raw: String, userInfoHeader: String? = null, nowMillis: Long = System.currentTimeMillis()): ParsedSubscription {
        require(raw.toByteArray(StandardCharsets.UTF_8).size <= MAX_BYTES) { "SUBSCRIPTION_TOO_LARGE" }
        val payload = raw.trim().removePrefix("\uFEFF")
        val text = if (payload.lineSequence().any { line -> prefixes.any { line.trim().startsWith(it, ignoreCase = true) } }) {
            payload
        } else {
            val decoded = decodeBase64(payload.replace("\r", "").replace("\n", ""))
                ?: throw IllegalArgumentException("SUBSCRIPTION_FORMAT_UNSUPPORTED")
            require(decoded.toByteArray(StandardCharsets.UTF_8).size <= MAX_BYTES) { "SUBSCRIPTION_TOO_LARGE" }
            decoded
        }

        val urls = text.lineSequence().map { it.trim() }
            .filter { line -> prefixes.any { line.startsWith(it, ignoreCase = true) } }
            .take(MAX_NODES + 1).toList()
        require(urls.size <= MAX_NODES) { "TOO_MANY_NODES" }

        val nodes = urls.mapNotNull(::parseNode).distinctBy { it.id }
        require(nodes.isNotEmpty()) { "NO_VALID_NODES" }
        return ParsedSubscription(nodes, parseUserInfo(userInfoHeader), nowMillis)
    }

    private fun parseNode(link: String): SubscriptionNode? {
      return try {
        if (link.length > 12_000 || link.any { it == '\u0000' }) return null
        val scheme = link.substringBefore("://").lowercase(Locale.ROOT)
        val remark: String
        val protocol: String
        val transport: String
        var hostLabel = ""
        if (scheme == "vmess") {
            val encoded = link.substringAfter("://").substringBefore("#")
            val json = JSONObject(decodeBase64(encoded) ?: return null)
            val host = json.optString("add").trim()
            val port = json.optString("port").toIntOrNull()
            if (host.isEmpty() || port == null || port !in 1..65535) return null
            hostLabel = host
            remark = json.optString("ps").take(140)
            protocol = "VMess"
            transport = json.optString("net").ifBlank { "tcp" }
        } else {
            // Only URI syntax validation and publicly displayable metadata. Never expose userInfo.
            val uri = URI(link)
            val host = uri.host ?: if (scheme == "ss") parseSsHost(link) else null
            if (host.isNullOrBlank()) return null
            hostLabel = host
            if (scheme != "ss" && uri.port !in 1..65535) return null
            remark = decodeUrlPart(uri.rawFragment ?: "").take(140)
            protocol = when (scheme) {
                "ss" -> "Shadowsocks"
                "hy2" -> "Hysteria2"
                else -> scheme.replaceFirstChar { it.titlecase(Locale.ROOT) }
            }
            transport = parseQueryValue(uri.rawQuery, "type")
                ?: if (scheme == "ss") "tcp" else "default"
        }
        val label = remark.ifBlank { hostLabel }.take(140)
        SubscriptionNode(
            id = MessageDigest.getInstance("SHA-256").digest(link.toByteArray(StandardCharsets.UTF_8))
                .take(12).joinToString("") { "%02x".format(it) },
            title = label,
            protocol = protocol,
            transport = transport.take(30),
            location = locationFromRemark(label),
            compatibleWithCurrentEngine = protocol.lowercase(Locale.ROOT) in protocolsXray,
        )
    } catch (_: Exception) { null }
    }

    private fun parseSsHost(link: String): String? {
      return try {
        val authority = link.substringAfter("ss://").substringBefore("?").substringBefore("#")
        val decodedAuthority = if (authority.contains("@")) authority else decodeBase64(authority) ?: return null
        val hostPort = decodedAuthority.substringAfterLast("@")
        val uri = URI("http://$hostPort")
        if (uri.port !in 1..65535) null else uri.host
    } catch (_: Exception) { null }
    }

    private fun parseQueryValue(query: String?, name: String): String? =
        query?.split("&")?.firstOrNull { it.substringBefore("=") == name }
            ?.substringAfter("=", "")?.let(::decodeUrlPart)?.take(30)

    private fun decodeUrlPart(raw: String): String =
        runCatching { URLDecoder.decode(raw.replace("+", "%2B"), "UTF-8") }.getOrDefault("")

    private fun decodeBase64(raw: String): String? {
      return try {
        if (raw.length > MAX_BYTES * 2) return null
        val clean = raw.trim().replace('-', '+').replace('_', '/')
        val padded = clean + "=".repeat((4 - clean.length % 4) % 4)
        val decoded = Base64.getDecoder().decode(padded)
        if (decoded.size > MAX_BYTES) return null
        StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(decoded)).toString()
    } catch (_: Exception) { null }
    }

    private val countryMatchers: List<Pair<String, List<String>>> = listOf(
        "آلمان" to listOf("🇩🇪", "germany", "deutschland", "آلمان", "🇩🇪 de", "[de]", "(de)"),
        "هلند" to listOf("🇳🇱", "netherlands", "holland", "هلند", "[nl]", "(nl)"),
        "ترکیه" to listOf("🇹🇷", "turkey", "türkiye", "ترکیه", "[tr]", "(tr)"),
        "فرانسه" to listOf("🇫🇷", "france", "فرانسه", "[fr]", "(fr)"),
        "انگلیس" to listOf("🇬🇧", "united kingdom", "britain", "انگلیس", "[uk]", "(uk)"),
        "آمریکا" to listOf("🇺🇸", "united states", "usa", "آمریکا", "[us]", "(us)"),
        "فنلاند" to listOf("🇫🇮", "finland", "فنلاند", "[fi]", "(fi)"),
        "امارات" to listOf("🇦🇪", "emirates", "dubai", "امارات", "[ae]", "(ae)"),
    )

    /** Only infer from inbound / node remark; never present endpoint geography as verified. */
    fun locationFromRemark(remark: String): String {
        val normalized = remark.lowercase(Locale.ROOT)
        return countryMatchers.firstOrNull { (_, aliases) -> aliases.any { it in normalized } }?.first ?: "نامشخص"
    }

    fun parseUserInfo(header: String?): SubscriptionUsage? {
        if (header.isNullOrBlank()) return null
        val values = Regex("(upload|download|total|expire)\\s*=\\s*(\\d+)", RegexOption.IGNORE_CASE)
            .findAll(header).associate { it.groupValues[1].lowercase(Locale.ROOT) to it.groupValues[2].toLongOrNull() }
        val upload = values["upload"] ?: return null
        val download = values["download"] ?: return null
        val total = values["total"] ?: return null
        if (upload < 0 || download < 0 || total <= 0 || upload > Long.MAX_VALUE - download) return null
        return SubscriptionUsage(upload + download, total, values["expire"]?.takeIf { it > 0 })
    }
}
