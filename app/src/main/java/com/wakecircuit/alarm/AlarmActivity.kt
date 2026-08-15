package com.wakecircuit.alarm

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private const val PASSING_SCORE = 78

class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        setContent {
            WakeCircuitTheme {
                AlarmTaskScreen(onComplete = {
                    AlarmSoundService.stop(this)
                    finishAndRemoveTask()
                })
            }
        }
    }
}

private enum class WakeStage { MATH, PHOTO, SPEECH, COMPLETE }

@Composable
private fun AlarmTaskScreen(onComplete: () -> Unit) {
    var stageIndex by rememberSaveable { mutableIntStateOf(0) }
    val stage = WakeStage.entries[stageIndex]
    val math = remember { MathChallengeFactory.random() }
    val photoTask = remember { PhotoTasks.random() }
    val english = remember { EnglishChallenges.random() }
    val scope = rememberCoroutineScope()

    if (stage == WakeStage.COMPLETE) {
        CompletionScreen(onFinish = {
            scope.launch {
                delay(900)
                onComplete()
            }
        })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        AlarmHeader(stageIndex)
        TaskProgress(stageIndex)
        when (stage) {
            WakeStage.MATH -> MathTask(math, onPassed = { stageIndex = 1 })
            WakeStage.PHOTO -> PhotoTaskScreen(photoTask, onPassed = { stageIndex = 2 })
            WakeStage.SPEECH -> SpeechTask(english, onPassed = { stageIndex = 3 })
            WakeStage.COMPLETE -> Unit
        }
        Spacer(Modifier.weight(1f))
        Text("铃声将在全部 3 个任务通过后自动停止", color = TextMuted, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun AlarmHeader(stageIndex: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Signal.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.Timer, null, tint = Signal) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("醒醒，现在开始", fontSize = 21.sp, fontWeight = FontWeight.Black)
            Text("第 ${stageIndex + 1} / 3 个任务", color = TextMuted, fontSize = 13.sp)
        }
        Text("进行中", color = Signal, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun TaskProgress(stageIndex: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        LinearProgressIndicator(
            progress = { stageIndex / 3f },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50)),
            color = Signal,
            trackColor = PanelAlt
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("数学", color = if (stageIndex == 0) TextMain else TextMuted, fontSize = 12.sp)
            Text("拍摄", color = if (stageIndex == 1) TextMain else TextMuted, fontSize = 12.sp)
            Text("跟读", color = if (stageIndex == 2) TextMain else TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MathTask(challenge: MathChallenge, onPassed: () -> Unit) {
    var answer by rememberSaveable { mutableStateOf("") }
    var feedback by rememberSaveable { mutableStateOf<String?>(null) }
    TaskCard("高数热身", "保持清醒。答对这道随机题，才可开启镜头。") {
        Text(challenge.question, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 31.sp)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it; feedback = null },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("输入数值答案") },
            singleLine = true,
            supportingText = { Text("小数题允许约 1% 误差") }
        )
        feedback?.let { FeedbackText(it, it.startsWith("正确")) }
        Button(
            onClick = {
                if (challenge.accepts(answer)) {
                    feedback = "正确，进入拍摄任务。"
                    onPassed()
                } else {
                    feedback = "还不对。${challenge.hint}"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = answer.isNotBlank()
        ) { Text("提交答案") }
    }
}

@Composable
private fun PhotoTaskScreen(task: PhotoTask, onPassed: () -> Unit) {
    val context = LocalContext.current
    val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var status by rememberSaveable { mutableStateOf("拍摄包含目标物品的清晰照片。") }
    var verifying by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    TaskCard("镜头取证", "联网识图会核验目标物品是否确实出现在画面中。") {
        Text("请拍摄：${task.label}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Mint)
        Spacer(Modifier.height(12.dp))
        if (hasCamera) {
            CameraPreview(onCaptureReady = { imageCapture = it })
            Spacer(Modifier.height(12.dp))
            Text(status, color = if (status.startsWith("通过")) Mint else TextMuted, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    val capture = imageCapture ?: run {
                        status = "相机正在准备，请稍候。"
                        return@Button
                    }
                    val file = File(context.cacheDir, "photos/${System.currentTimeMillis()}.jpg").apply { parentFile?.mkdirs() }
                    verifying = true
                    status = "正在保存照片…"
                    capture.takePicture(
                        ImageCapture.OutputFileOptions.Builder(file).build(),
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                status = "正在联网核验…"
                                scope.launch {
                                    val result = VisionVerifier(AlarmStore(context).vision()).verify(file, task)
                                    verifying = false
                                    if (result.matched) {
                                        status = "通过：${result.reason}"
                                        onPassed()
                                    } else {
                                        status = "未通过：${result.reason}"
                                    }
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                verifying = false
                                status = "拍摄失败：${exception.message ?: "请重试"}"
                            }
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !verifying
            ) {
                Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (verifying) "正在核验" else "拍照并核验")
            }
        } else {
            Text("需要相机权限才能完成本关。", color = Signal)
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.fillMaxWidth()) { Text("允许使用相机") }
        }
    }
}

@Composable
private fun CameraPreview(onCaptureReady: (ImageCapture) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    DisposableEffect(lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            runCatching {
                val provider = future.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val capture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                onCaptureReady(capture)
            }
        }
        future.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {
            runCatching { future.get().unbindAll() }
        }
    }
    AndroidView(
        factory = { previewView },
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(18.dp))
    )
}

@Composable
private fun SpeechTask(challenge: EnglishChallenge, onPassed: () -> Unit) {
    val context = LocalContext.current
    val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    var status by rememberSaveable { mutableStateOf("点击开始后，清晰读出上面的英文。") }
    var listening by rememberSaveable { mutableStateOf(false) }
    val speech = remember { SpeechRecognizer.createSpeechRecognizer(context) }

    DisposableEffect(speech) { onDispose { speech.destroy() } }

    fun startRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            status = "此设备没有可用的语音识别服务。"
            return
        }
        speech.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { listening = true; status = "正在聆听…" }
            override fun onBeginningOfSpeech() { status = "继续朗读…" }
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() { listening = false; status = "正在判定…" }
            override fun onError(error: Int) { listening = false; status = "未能听清，请在安静环境下再试一次。" }
            override fun onResults(results: Bundle?) {
                listening = false
                val heard = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                val score = PronunciationJudge.score(challenge.sentence, heard)
                status = if (score >= PASSING_SCORE) {
                    "通过，识别相似度 $score%。"
                } else {
                    "相似度 $score%，需要达到 $PASSING_SCORE%。识别到：$heard"
                }
                if (score >= PASSING_SCORE) onPassed()
            }
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        speech.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        })
    }

    TaskCard("开口清醒", "使用设备的联网语音识别服务，发音接近原句即可通过。") {
        Text("“${challenge.sentence}”", fontSize = 23.sp, fontWeight = FontWeight.Black, lineHeight = 32.sp)
        Spacer(Modifier.height(9.dp))
        Text(challenge.chinese, color = TextMuted, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        Text(status, color = if (status.startsWith("通过")) Mint else TextMuted, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        if (hasMic) {
            Button(onClick = ::startRecognition, enabled = !listening, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Mic, null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (listening) "正在聆听" else "开始跟读")
            }
        } else {
            Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }, modifier = Modifier.fillMaxWidth()) { Text("允许使用麦克风") }
        }
    }
}

@Composable
private fun TaskCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text(subtitle, color = TextMuted, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun FeedbackText(message: String, positive: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(if (positive) Icons.Default.Check else Icons.Default.Close, null, tint = if (positive) Mint else Signal, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        Text(message, color = if (positive) Mint else Signal, fontSize = 13.sp)
    }
}

@Composable
private fun CompletionScreen(onFinish: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Ink), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(36.dp)) {
            Box(Modifier.size(86.dp).clip(RoundedCornerShape(28.dp)).background(Mint.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, null, tint = Mint, modifier = Modifier.size(48.dp))
            }
            Text("全部完成", fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("很好。今天从清醒开始。", color = TextMuted)
            Button(onClick = onFinish) { Text("停止铃声") }
        }
    }
}
