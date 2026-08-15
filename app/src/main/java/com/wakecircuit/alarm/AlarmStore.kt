package com.wakecircuit.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.Calendar

data class AlarmConfig(
    val hour: Int,
    val minute: Int,
    val enabled: Boolean,
    val ringtoneUri: String?
) {
    val timeLabel: String get() = "%02d:%02d".format(hour, minute)
}

data class VisionConfig(
    val endpoint: String,
    val apiKey: String,
    val model: String
) {
    val isReady: Boolean get() = endpoint.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank()
}

class AlarmStore(context: Context) {
    private val prefs = runCatching {
        val key = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "wake_circuit_settings",
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrElse {
        context.getSharedPreferences("wake_circuit_settings", Context.MODE_PRIVATE)
    }

    fun alarm(): AlarmConfig = AlarmConfig(
        hour = prefs.getInt(KEY_HOUR, 7),
        minute = prefs.getInt(KEY_MINUTE, 0),
        enabled = prefs.getBoolean(KEY_ENABLED, false),
        ringtoneUri = prefs.getString(KEY_RINGTONE, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString())
    )

    fun saveAlarm(config: AlarmConfig) {
        prefs.edit()
            .putInt(KEY_HOUR, config.hour)
            .putInt(KEY_MINUTE, config.minute)
            .putBoolean(KEY_ENABLED, config.enabled)
            .putString(KEY_RINGTONE, config.ringtoneUri)
            .apply()
    }

    fun vision(): VisionConfig = VisionConfig(
        endpoint = prefs.getString(KEY_VISION_ENDPOINT, DEFAULT_VISION_ENDPOINT).orEmpty(),
        apiKey = prefs.getString(KEY_VISION_API_KEY, "").orEmpty(),
        model = prefs.getString(KEY_VISION_MODEL, DEFAULT_VISION_MODEL).orEmpty()
    )

    fun saveVision(config: VisionConfig) {
        prefs.edit()
            .putString(KEY_VISION_ENDPOINT, config.endpoint.trim().trimEnd('/'))
            .putString(KEY_VISION_API_KEY, config.apiKey.trim())
            .putString(KEY_VISION_MODEL, config.model.trim())
            .apply()
    }

    companion object {
        const val KEY_HOUR = "hour"
        const val KEY_MINUTE = "minute"
        const val KEY_ENABLED = "enabled"
        const val KEY_RINGTONE = "ringtone"
        const val KEY_VISION_ENDPOINT = "vision_endpoint"
        const val KEY_VISION_API_KEY = "vision_api_key"
        const val KEY_VISION_MODEL = "vision_model"
        const val DEFAULT_VISION_ENDPOINT = "https://api.openai.com/v1"
        const val DEFAULT_VISION_MODEL = "gpt-4o-mini"
    }
}

object AlarmScheduler {
    private const val REQUEST_CODE = 4101

    fun schedule(context: Context): Boolean {
        val config = AlarmStore(context).alarm()
        if (!config.enabled) return false
        val manager = context.getSystemService(AlarmManager::class.java)
        val triggerAt = nextTriggerMillis(config.hour, config.minute)
        val fireIntent = pendingIntent(context)
        val showIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE + 1,
            Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val info = AlarmManager.AlarmClockInfo(triggerAt, showIntent)
        return try {
            manager.setAlarmClock(info, fireIntent)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
    }

    fun rescheduleTomorrow(context: Context) {
        val config = AlarmStore(context).alarm()
        if (config.enabled) schedule(context)
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, AlarmReceiver::class.java).setAction("com.wakecircuit.alarm.FIRE"),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
    }
}
