package ir.saadegi.android.diagnostics

import ir.saadegi.android.subscription.SubscriptionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import libXray.LibXray
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ServerSocket
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.math.roundToInt

/**
 * Network diagnostics genuinely routed through libXray, NOT direct phone-network ping.
 *
 * Tests run only while VPN/TUN is inactive. libXray managed methods are process-global and
 * pingBatch cannot overlap an active runXray; serialize speed, ping and connection lifecycle.
 */
class XrayNativeDiagnostics(private val repository: SubscriptionRepository) {
    companion object {
        private val nativeLock = Mutex()
        private const val LATENCY_URL = "https://cp.cloudflare.com/"
        private const val DOWNLOAD_URL = "https://speed.cloudflare.com/__down?bytes=5242880"
        private const val EXPECTED_BYTES = 5 * 1024 * 1024
        const val MAX_NODES_PER_RUN = 20
    }

    data class SpeedResult(
        val transferredBytes: Int,
        val seconds: Double,
        val mbps: Double,
    )

    /** Fail closed when the core is already in use; no direct-network fallback. */
    private fun assertCoreNotRunning() {
        val active = invoke("getXrayState", JSONObject())
            .optJSONObject("data")?.optBoolean("running", false) ?: false
        if (active) throw IllegalStateException("XRAY_ALREADY_RUNNING")
    }

    private fun invoke(method: String, payload: JSONObject): JSONObject {
        val request = JSONObject().put("apiVersion", 3).put("method", method).put("payload", payload)
        val output = try { LibXray.invoke(request.toString()) }
        catch (_: Exception) { throw IllegalStateException("XRAY_NATIVE_UNAVAILABLE") }
        val response = try { JSONObject(output) } catch (_: Exception) {
            throw IllegalStateException("XRAY_NATIVE_INVALID_RESPONSE")
        }
        if (!response.optBoolean("success", false)) {
            // Never show upstream raw errors: they could contain user config data.
            throw IllegalStateException("XRAY_METHOD_FAILED")
        }
        return response
    }

    private suspend fun outbound(profileId: String): JSONObject {
        val uri = repository.getProfileUri(profileId) ?: throw IllegalStateException("PROFILE_NOT_FOUND")
        val result = invoke("convertShareLinksToXrayJson", JSONObject().put("text", uri))
        val array = result.optJSONObject("data")?.optJSONArray("outbounds")
            ?: throw IllegalStateException("XRAY_CONFIG_UNSUPPORTED")
        if (array.length() != 1) throw IllegalStateException("XRAY_CONFIG_UNSUPPORTED")
        return JSONObject(array.getJSONObject(0).toString())
    }

    private fun outboundConfig(outbound: JSONObject): String =
        JSONObject().put("outbounds", JSONArray().put(outbound)).toString()

    private fun sample(config: String): Long? {
        val entry = JSONObject().put("xrayJson", config)
        val pingRequest = JSONObject()
            .put("configs", JSONArray().put(entry))
            .put("timeout", 5)
            .put("url", LATENCY_URL)
        val result = invoke("pingBatch", pingRequest)
            .optJSONObject("data")?.optJSONArray("results")?.optJSONObject(0) ?: return null
        if (!result.optBoolean("success")) return null
        return result.optLong("delay", -1L).takeIf { it in 1..9_999 }
    }

    /**
     * The user chooses one or three genuine requests per selected profile.
     * onProgress reports only completed samples; no fake/default milliseconds.
     */
    suspend fun measure(
        profileIds: List<String>,
        mode: ProbeMode,
        onProgress: suspend (QualityResult) -> Unit,
    ): List<QualityResult> = withContext(Dispatchers.IO) {
        require(profileIds.isNotEmpty() && profileIds.size <= MAX_NODES_PER_RUN) { "INVALID_TEST_SELECTION" }
        require(profileIds.distinct().size == profileIds.size) { "DUPLICATE_TEST_SELECTION" }
        nativeLock.lock()
        try {
            assertCoreNotRunning()
            val results = mutableListOf<QualityResult>()
            for (id in profileIds) {
                val samples = mutableListOf<Long?>()
                val config = try { outboundConfig(outbound(id)) } catch (_: Exception) { null }
                repeat(mode.samples) { index ->
                    if (index > 0) delay(350)
                    val measured = if (config == null) null else try { sample(config) } catch (_: Exception) { null }
                    samples += measured
                }
                val result = QualityScoring.aggregate(id, samples)
                results += result
                onProgress(result)
            }
            QualityScoring.rank(results)
        } finally { nativeLock.unlock() }
    }

    /**
     * Download exactly 5 MiB through an isolated Xray SOCKS outbound.
     * A broken SOCKS or TLS request fails. NEVER silently retry through direct internet.
     * 5 MiB counts against the customer's subscription usage.
     */
    suspend fun testFiveMiB(profileId: String): SpeedResult = withContext(Dispatchers.IO) {
        nativeLock.lock()
        var started = false
        try {
            assertCoreNotRunning()
            val outbound = outbound(profileId)
            val localPort = ServerSocket(0).use { it.localPort }
            val inbound = JSONObject()
                .put("tag", "diagnostic-socks")
                .put("listen", "127.0.0.1")
                .put("port", localPort)
                .put("protocol", "socks")
                .put("settings", JSONObject().put("auth", "noauth").put("udp", false))
            val coreConfig = JSONObject()
                .put("log", JSONObject().put("loglevel", "none"))
                .put("inbounds", JSONArray().put(inbound))
                .put("outbounds", JSONArray().put(outbound))
            invoke("runXray", JSONObject().put("xrayJson", coreConfig.toString()))
            started = true

            // Explicit SOCKS proxy. A failed proxy must never fall back to a direct connection.
            val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", localPort))
            val connection = (URL(DOWNLOAD_URL).openConnection(proxy) as HttpsURLConnection)
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 9_000
            connection.readTimeout = 15_000
            connection.useCaches = false
            connection.setRequestProperty("Cache-Control", "no-cache")
            val startedAt = System.nanoTime()
            try {
                if (connection.responseCode != 200) throw IllegalStateException("SPEED_ENDPOINT_UNAVAILABLE")
                var count = 0
                connection.inputStream.use { input ->
                    val buffer = ByteArray(32768)
                    while (count < EXPECTED_BYTES) {
                        val n = input.read(buffer, 0, minOf(buffer.size, EXPECTED_BYTES - count))
                        if (n <= 0) break
                        count += n
                    }
                }
                val seconds = (System.nanoTime() - startedAt) / 1_000_000_000.0
                if (count != EXPECTED_BYTES || seconds <= 0) throw IllegalStateException("INCOMPLETE_SPEED_TEST")
                SpeedResult(count, seconds, count * 8.0 / seconds / 1_000_000.0)
            } finally { connection.disconnect() }
        } finally {
            if (started) try { invoke("stopXray", JSONObject()) } catch (_: Exception) { /* safe stop attempt */ }
            nativeLock.unlock()
        }
    }
}
