package dev.folio.news.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

data class UpdateInfo(val version: String, val notes: String, val apkUrl: String, val size: Long, val page: String)

/** Self-updater: looks at the latest GitHub release and installs its APK through the system installer. */
object Updater {
    const val REPO = "fspecii/folio-news-reader"

    /** Returns the newest release if it is newer than [current], else null. */
    fun check(client: OkHttpClient, current: String): UpdateInfo? {
        val req = Request.Builder().url("https://api.github.com/repos/$REPO/releases/latest")
            .header("Accept", "application/vnd.github+json").header("User-Agent", "Folio-Updater").build()
        client.newBuilder().cache(null).build().newCall(req).execute().use { r ->
            if (!r.isSuccessful) error("GitHub answered ${r.code}")
            val o = JSONObject(r.body!!.string())
            val tag = o.getString("tag_name")
            val assets = o.getJSONArray("assets")
            var apk: JSONObject? = null
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.getString("name").endsWith(".apk")) { apk = a; break }
            }
            if (apk == null || !isNewer(tag, current)) return null
            return UpdateInfo(tag.removePrefix("v"), o.optString("body", "").trim(), apk.getString("browser_download_url"), apk.optLong("size"), o.optString("html_url"))
        }
    }

    fun isNewer(remote: String, local: String): Boolean {
        fun parts(v: String) = v.removePrefix("v").split('.', '-').map { it.takeWhile { c -> c.isDigit() }.toIntOrNull() ?: 0 }
        val a = parts(remote); val b = parts(local)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /** Downloads the APK into the cache, reporting 0..1 progress. */
    fun download(client: OkHttpClient, ctx: Context, info: UpdateInfo, onProgress: (Float) -> Unit): File {
        val dir = File(ctx.cacheDir, "update").apply { deleteRecursively(); mkdirs() }
        val out = File(dir, "Folio-${info.version}.apk")
        val req = Request.Builder().url(info.apkUrl).header("User-Agent", "Folio-Updater").build()
        client.newBuilder().cache(null).readTimeout(60, java.util.concurrent.TimeUnit.SECONDS).build().newCall(req).execute().use { r ->
            if (!r.isSuccessful) error("Download failed (${r.code})")
            val total = r.body!!.contentLength().takeIf { it > 0 } ?: info.size
            r.body!!.byteStream().use { input ->
                out.outputStream().use { o ->
                    val buf = ByteArray(64 * 1024); var done = 0L; var last = 0f
                    while (true) {
                        val n = input.read(buf); if (n < 0) break
                        o.write(buf, 0, n); done += n
                        if (total > 0) { val p = done.toFloat() / total; if (p - last > 0.01f) { last = p; onProgress(p) } }
                    }
                }
            }
        }
        onProgress(1f)
        return out
    }

    fun canInstall(ctx: Context) = Build.VERSION.SDK_INT < 26 || ctx.packageManager.canRequestPackageInstalls()

    /** Sends the user to the "allow installs from Folio" screen. */
    fun askPermission(ctx: Context) {
        ctx.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun install(ctx: Context, apk: File) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", apk)
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
