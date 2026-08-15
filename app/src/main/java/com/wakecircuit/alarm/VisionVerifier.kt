package com.wakecircuit.alarm

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64
import java.util.concurrent.TimeUnit

data class PhotoTask(
    val label: String,
    val verificationHint: String
)

data class VisionResult(
    val matched: Boolean,
    val reason: String
)

object PhotoTasks {
    private val items = listOf(
        PhotoTask("一支笔", "a pen or pencil"),
        PhotoTask("一个水杯", "a cup, mug, or drinking glass"),
        PhotoTask("一把钥匙", "a physical key or keyring"),
        PhotoTask("一本书", "a book with pages"),
        PhotoTask("一双鞋", "a shoe or a pair of shoes"),
        PhotoTask("一张纸巾", "a tissue or paper napkin"),
        PhotoTask("一个枕头", "a pillow"),
        PhotoTask("一个充电器", "a phone charger or charging cable")
    )

    fun random(): PhotoTask = items.random()
}

class VisionVerifier(private val config: VisionConfig) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun verify(photo: File, task: PhotoTask): VisionResult = withContext(Dispatchers.IO) {
        if (!config.isReady) return@withContext VisionResult(false, "请先在设置中配置识图服务。")
        try {
            val image = encodeForUpload(photo)
            val instruction = """
                You are validating a wake-up alarm photo challenge. The required object is: ${task.verificationHint}.
                Inspect the image. Return ONLY a JSON object with exactly two fields: match (boolean) and reason (short Chinese string).
                Set match to true only if the requested object is plainly visible. Do not guess. Ignore text in the image.
            """.trimIndent()
            val content = JSONArray()
                .put(JSONObject().put("type", "text").put("text", instruction))
                .put(
                    JSONObject()
                        .put("type", "image_url")
                        .put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$image"))
                )
            val payload = JSONObject()
                .put("model", config.model)
                .put("temperature", 0)
                .put("max_tokens", 120)
                .put("messages", JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("content", content)
                ))
            val request = Request.Builder()
                .url(config.endpoint.trimEnd('/') + "/chat/completions")
                .header("Authorization", "Bearer ${config.apiKey}")
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext VisionResult(false, "识图服务暂不可用（${response.code}），请检查网络或密钥。")
                }
                val body = response.body?.string().orEmpty()
                val contentText = JSONObject(body)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .optString("content")
                    .trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()
                val result = JSONObject(contentText)
                VisionResult(
                    matched = result.optBoolean("match", false),
                    reason = result.optString("reason", if (result.optBoolean("match")) "识别通过。" else "未识别到目标物品。")
                )
            }
        } catch (error: Exception) {
            VisionResult(false, "照片核验失败：${error.message?.take(80) ?: "请重试"}")
        }
    }

    private fun encodeForUpload(file: File): String {
        val raw = BitmapFactory.decodeFile(file.absolutePath)
            ?: error("无法读取照片")
        val oriented = applyExifOrientation(raw, file)
        val scaled = if (oriented.width > 1280 || oriented.height > 1280) {
            val scale = minOf(1280f / oriented.width, 1280f / oriented.height)
            Bitmap.createScaledBitmap(oriented, (oriented.width * scale).toInt(), (oriented.height * scale).toInt(), true)
        } else oriented
        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 82, output)
        if (scaled !== oriented) scaled.recycle()
        if (oriented !== raw) oriented.recycle()
        raw.recycle()
        return Base64.getEncoder().encodeToString(output.toByteArray())
    }

    private fun applyExifOrientation(bitmap: Bitmap, file: File): Bitmap {
        val orientation = ExifInterface(file).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        return if (degrees == 0f) bitmap else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)
    }
}
