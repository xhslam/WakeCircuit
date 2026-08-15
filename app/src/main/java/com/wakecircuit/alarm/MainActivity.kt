package com.wakecircuit.alarm

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WakeCircuitTheme { MainScreen() }
        }
    }
}

@Composable
private fun MainScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { AlarmStore(context) }
    var alarm by remember { mutableStateOf(store.alarm()) }
    var showVisionSettings by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()

    val ringtonePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            alarm = alarm.copy(ringtoneUri = uri?.toString())
            store.saveAlarm(alarm)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        val okay = granted[Manifest.permission.CAMERA] == true && granted[Manifest.permission.RECORD_AUDIO] == true
        context.toast(if (okay) "相机和麦克风已可用于任务。" else "请允许相机和麦克风，否则无法完成任务链。")
    }

    Scaffold(containerColor = Ink) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Header(onSettings = { showVisionSettings = true })
            AlarmControlCard(
                alarm = alarm,
                onEnabled = { enabled ->
                    alarm = alarm.copy(enabled = enabled)
                    store.saveAlarm(alarm)
                    if (enabled) {
                        if (AlarmScheduler.schedule(context)) context.toast("闹钟已设为每天 ${alarm.timeLabel}。")
                        else context.toast("无法设定精确闹钟，请检查系统的闹钟权限。")
                    } else {
                        AlarmScheduler.cancel(context)
                        context.toast("闹钟已关闭。")
                    }
                },
                onTime = {
                    TimePickerDialog(context, { _, hour, minute ->
                        alarm = alarm.copy(hour = hour, minute = minute)
                        store.saveAlarm(alarm)
                        if (alarm.enabled) AlarmScheduler.schedule(context)
                    }, alarm.hour, alarm.minute, true).show()
                },
                onRingtone = {
                    ringtonePicker.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, alarm.ringtoneUri?.let(Uri::parse))
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                    })
                }
            )
            TaskRouteCard()
            PermissionCard(
                onRequest = {
                    val permissions = buildList {
                        add(Manifest.permission.CAMERA)
                        add(Manifest.permission.RECORD_AUDIO)
                        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(permissions.toTypedArray())
                },
                onExactAlarm = {
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                    })
                },
                onFullScreen = {
                    if (Build.VERSION.SDK_INT >= 34) {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                            data = Uri.parse("package:${context.packageName}")
                        })
                    }
                }
            )
            PrivacyCard(visionReady = store.vision().isReady, onOpenSettings = { showVisionSettings = true })
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showVisionSettings) {
        VisionSettingsDialog(
            initial = store.vision(),
            onDismiss = { showVisionSettings = false },
            onSave = {
                store.saveVision(it)
                showVisionSettings = false
                context.toast("识图配置已保存到设备加密存储。")
            }
        )
    }
}

@Composable
private fun Header(onSettings: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text("醒醒", fontSize = 30.sp, fontWeight = FontWeight.Black, color = TextMain)
            Text("完成任务，才有资格继续睡。", color = TextMuted, fontSize = 14.sp)
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Default.Settings, contentDescription = "识图设置", tint = TextMain)
        }
    }
}

@Composable
private fun AlarmControlCard(
    alarm: AlarmConfig,
    onEnabled: (Boolean) -> Unit,
    onTime: () -> Unit,
    onRingtone: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Alarm, null, tint = Signal, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("每日唤醒", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(if (alarm.enabled) "已开启 · 下一次按计划触发" else "未开启", color = TextMuted, fontSize = 13.sp)
                }
                Switch(checked = alarm.enabled, onCheckedChange = onEnabled)
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onTime),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("唤醒时间", color = TextMuted, fontSize = 13.sp)
                    Text(alarm.timeLabel, fontSize = 44.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
                Icon(Icons.Default.ArrowForward, null, tint = Signal)
            }
            OutlinedButton(onClick = onRingtone, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.GraphicEq, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("选择系统铃声")
            }
        }
    }
}

@Composable
private fun TaskRouteCard() {
    Card(colors = CardDefaults.cardColors(containerColor = PanelAlt), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("解锁路径", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            RouteRow("01", "高数热身", "答对随机微积分题", Icons.Default.CheckCircle, Signal)
            RouteRow("02", "镜头取证", "拍摄随机指定物品并核验", Icons.Default.CameraAlt, Mint)
            RouteRow("03", "开口清醒", "英语跟读相似度达到 78%", Icons.Default.GraphicEq, Color(0xFF95B8FF))
        }
    }
}

@Composable
private fun RouteRow(number: String, title: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) { Text(number, color = accent, fontWeight = FontWeight.Black, fontSize = 12.sp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, color = TextMuted, fontSize = 12.sp)
        }
        Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun PermissionCard(onRequest: () -> Unit, onExactAlarm: () -> Unit, onFullScreen: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("首次使用准备", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("请完成以下系统授权。它们决定闹钟能否准时响起，并在锁屏时显示任务界面。", color = TextMuted, fontSize = 13.sp)
            OutlinedButton(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("授权相机、麦克风和通知")
            }
            OutlinedButton(onClick = onExactAlarm, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Alarm, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("检查精确闹钟权限")
            }
            if (Build.VERSION.SDK_INT >= 34) {
                OutlinedButton(onClick = onFullScreen, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.NotificationsActive, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("允许锁屏全屏响铃")
                }
            }
        }
    }
}

@Composable
private fun PrivacyCard(visionReady: Boolean, onOpenSettings: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, null, tint = Mint, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("联网识图", fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(if (visionReady) "已配置" else "待配置", color = if (visionReady) Mint else Signal, fontSize = 12.sp)
            }
            Text("照片只会在你提交当前挑战时发送到你配置的识图服务；密钥仅保存在本机加密存储中。", color = TextMuted, fontSize = 13.sp)
            TextButton(onClick = onOpenSettings) { Text(if (visionReady) "更新识图配置" else "配置识图服务") }
        }
    }
}

@Composable
private fun VisionSettingsDialog(initial: VisionConfig, onDismiss: () -> Unit, onSave: (VisionConfig) -> Unit) {
    var endpoint by remember { mutableStateOf(initial.endpoint) }
    var apiKey by remember { mutableStateOf(initial.apiKey) }
    var model by remember { mutableStateOf(initial.model) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("配置联网识图") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("填写兼容 Chat Completions 图片输入的服务地址、密钥和视觉模型。默认示例使用 OpenAI 兼容地址。", color = TextMuted, fontSize = 13.sp)
                OutlinedTextField(endpoint, { endpoint = it }, label = { Text("服务地址") }, singleLine = true)
                OutlinedTextField(apiKey, { apiKey = it }, label = { Text("API 密钥") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                OutlinedTextField(model, { model = it }, label = { Text("视觉模型") }, singleLine = true)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(VisionConfig(endpoint, apiKey, model)) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private fun Context.toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
