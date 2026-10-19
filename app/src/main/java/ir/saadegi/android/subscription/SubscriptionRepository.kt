package ir.saadegi.android.subscription

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.net.ssl.HttpsURLConnection

data class SubscriptionSnapshot(val sourceHost: String, val parsed: ParsedSubscription)

/**
 * One manual subscription for the initial no-login pilot.
 * Raw subscription content (including proxy credentials) and its URL are persisted only in
 * Android Keystore AES-GCM encrypted app-private storage. No telemetry or log printing.
 */
class SubscriptionRepository(context: Context) {
    private val file = File(context.applicationContext.filesDir, "subscription-v1.bin")
    private val alias = "saadegi_subscription_key_v1"

    suspend fun load(): SubscriptionSnapshot? = withContext(Dispatchers.IO) {
        val stored = decryptRead() ?: return@withContext null
        snapshot(stored)
    }

    suspend fun importSubscription(inputUrl: String): SubscriptionSnapshot = withContext(Dispatchers.IO) {
        val url = validateUrl(inputUrl)
        val remote = download(url)
        val parsed = SubscriptionParser.parse(remote.body, remote.userInfo)
        val stored = Stored(url.toString(), remote.body, remote.userInfo, parsed.updatedAtMillis)
        encryptWrite(stored)
        SubscriptionSnapshot(url.host, parsed)
    }

    suspend fun refresh(): SubscriptionSnapshot = withContext(Dispatchers.IO) {
        val saved = decryptRead() ?: throw IllegalStateException("SUBSCRIPTION_NOT_SET")
        val url = validateUrl(saved.url)
        val remote = download(url)
        val parsed = SubscriptionParser.parse(remote.body, remote.userInfo)
        val updated = Stored(url.toString(), remote.body, remote.userInfo, parsed.updatedAtMillis)
        encryptWrite(updated)
        SubscriptionSnapshot(url.host, parsed)
    }

    suspend fun delete() = withContext(Dispatchers.IO) {
        if (file.exists() && !file.delete()) throw IllegalStateException("SUBSCRIPTION_REMOVE_FAILED")
    }

    private fun snapshot(stored: Stored): SubscriptionSnapshot {
        val uri = validateUrl(stored.url)
        return SubscriptionSnapshot(uri.host, SubscriptionParser.parse(stored.body, stored.userInfo, stored.updatedAt))
    }

    private data class Stored(val url: String, val body: String, val userInfo: String?, val updatedAt: Long)
    private data class Remote(val body: String, val userInfo: String?)

    /** Never accept HTTP or non-web schemes; block local/loopback origins, embedded passwords and redirects. */
    private fun validateUrl(value: String): URI {
        if (value.length > 2048 || value != value.trim()) throw IllegalArgumentException("INVALID_SUBSCRIPTION_URL")
        val uri = try { URI(value) } catch (_: Exception) { throw IllegalArgumentException("INVALID_SUBSCRIPTION_URL") }
        val host = uri.host?.lowercase() ?: throw IllegalArgumentException("INVALID_SUBSCRIPTION_URL")
        if (uri.scheme != "https" || uri.userInfo != null || uri.fragment != null ||
            uri.port !in -1..65535 || uri.port == 0 || host == "localhost" ||
            host.endsWith(".localhost") || host.endsWith(".local") ||
            host.endsWith(".internal") || host == "0.0.0.0" || host == "::1" ||
            host.startsWith("127.") || host.startsWith("10.") || host.startsWith("192.168.") ||
            host.startsWith("169.254.") || host.startsWith("172.") && host.substringAfter("172.")
                .substringBefore(".").toIntOrNull() in 16..31 ||
            host.startsWith("[")
        ) throw IllegalArgumentException("INVALID_SUBSCRIPTION_URL")
        return uri
    }

    private fun download(url: URI): Remote {
        val connection = (url.toURL().openConnection() as HttpsURLConnection)
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 9_000
        connection.readTimeout = 14_000
        connection.setRequestProperty("Accept", "text/plain, application/octet-stream;q=0.9, */*;q=0.5")
        connection.setRequestProperty("User-Agent", "SaadegiAndroid/0.1")
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("SUBSCRIPTION_HTTP_ERROR")
            val length = connection.contentLengthLong
            if (length > 1_048_576) throw IllegalStateException("SUBSCRIPTION_TOO_LARGE")
            val out = ByteArrayOutputStream()
            connection.inputStream.use { stream ->
                val chunk = ByteArray(8192)
                while (true) {
                    val n = stream.read(chunk)
                    if (n < 0) break
                    out.write(chunk, 0, n)
                    if (out.size() > 1_048_576) throw IllegalStateException("SUBSCRIPTION_TOO_LARGE")
                }
            }
            val body = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(out.toByteArray())).toString()
            return Remote(body, connection.getHeaderField("subscription-userinfo"))
        } finally { connection.disconnect() }
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build()
        )
        return generator.generateKey()
    }

    private fun encryptWrite(stored: Stored) {
        val objectJson = JSONObject().put("url", stored.url).put("body", stored.body)
            .put("userInfo", stored.userInfo ?: JSONObject.NULL).put("updatedAt", stored.updatedAt)
        val bytes = objectJson.toString().toByteArray(StandardCharsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.doFinal(bytes)
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.outputStream().use { output ->
            output.write(cipher.iv)
            output.write(encrypted)
            output.fd.sync()
        }
        if (!tmp.renameTo(file)) {
            tmp.delete()
            throw IllegalStateException("SUBSCRIPTION_SAVE_FAILED")
        }
    }

    private fun decryptRead(): Stored? {
        if (!file.exists()) return null
        val bytes = file.readBytes()
        if (bytes.size !in 29..1_300_000) throw IllegalStateException("SUBSCRIPTION_STORAGE_INVALID")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        val json = JSONObject(
            String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), StandardCharsets.UTF_8)
        )
        return Stored(
            url = json.getString("url"),
            body = json.getString("body"),
            userInfo = if (json.isNull("userInfo")) null else json.getString("userInfo"),
            updatedAt = json.getLong("updatedAt"),
        )
    }
}
