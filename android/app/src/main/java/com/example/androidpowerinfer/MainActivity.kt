package com.example.androidpowerinfer

import android.app.ActivityManager
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.text.format.Formatter
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import com.example.androidpowerinfer.ui.theme.LlamaAndroidTheme
import java.io.File

/**
 * This Activity sets up the UI, logs memory stats, and initializes downloads and models.
 * It uses lazy-initialized system services and a MainViewModel to handle model loading and interaction.
 */
class MainActivity(
    activityManager: ActivityManager? = null,
    downloadManager: DownloadManager? = null,
    clipboardManager: ClipboardManager? = null,
) : ComponentActivity() {

    private val activityManager by lazy { activityManager ?: getSystemService<ActivityManager>()!! }
    private val downloadManager by lazy { downloadManager ?: getSystemService<DownloadManager>()!! }
    private val clipboardManager by lazy { clipboardManager ?: getSystemService<ClipboardManager>()!! }

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable strict mode for detecting leaked closable objects.
        StrictMode.setVmPolicy(
            VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build()
        )

        logMemoryInfo()
        logDownloadDirectory()

        val models = listOf(
            Downloadable(
                name = "Bamboo 7B (Q4)",
                Uri.parse(
                    "https://huggingface.co/PowerInfer/Bamboo-base-v0.1-gguf/" +
                        "resolve/main/bamboo-7b-v0.1.Q4_0.powerinfer.gguf?download=true"
                ),
                File(getExternalFilesDir(null), "bamboo-7b-v0.1.Q4_0.powerinfer.gguf")
            )
        )

        setContent {
            LlamaAndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainCompose(
                        viewModel = viewModel,
                        clipboard = clipboardManager,
                        dm = downloadManager,
                        models = models,
                    )
                }
            }
        }
    }

    /**
     * Logs the current device memory information using the ViewModel.
     */
    private fun logMemoryInfo() {
        val memoryInfo = ActivityManager.MemoryInfo().also {
            activityManager.getMemoryInfo(it)
        }
        val free = Formatter.formatFileSize(this, memoryInfo.availMem)
        val total = Formatter.formatFileSize(this, memoryInfo.totalMem)
        viewModel.log("Current memory: $free / $total")
    }

    /**
     * Logs the directory where downloads will be stored.
     */
    private fun logDownloadDirectory() {
        val extFilesDir = getExternalFilesDir(null)
        viewModel.log("Downloads directory: $extFilesDir")
    }
}

@Composable
fun MainCompose(
    viewModel: MainViewModel,
    clipboard: ClipboardManager,
    dm: DownloadManager,
    models: List<Downloadable>
) {
    // true : chat, false: Download
    var isChat by remember { mutableStateOf(true) }

    Column(Modifier.fillMaxSize()) {
        Button(
            onClick = { isChat = !isChat },
            modifier = Modifier.padding(8.dp)
        ) {
            Text(if (isChat) "Switch to Downloads" else "Switch to Chat")
        }

        if (isChat) {
            ChatScreen(viewModel, clipboard)
        } else {
            DownloadScreen(viewModel, dm, models)
        }
    }
}

@Composable
fun ChatScreen(viewModel: MainViewModel, clipboard: ClipboardManager) {
    Column(Modifier.fillMaxSize()) {
        val scrollState = rememberLazyListState()

        // Messages display
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(state = scrollState) {
                items(viewModel.messages) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge.copy(color = LocalContentColor.current),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // Message input and actions
        OutlinedTextField(
            value = viewModel.message,
            onValueChange = viewModel::updateMessage,
            label = { Text("Message") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Row(Modifier.padding(16.dp)) {
            Button(onClick = viewModel::send, modifier = Modifier.padding(end = 8.dp)) {
                Text("Send")
            }
            Button(onClick = viewModel::clear, modifier = Modifier.padding(end = 8.dp)) {
                Text("Clear")
            }
            Button(onClick = {
                viewModel.messages.joinToString("\n").let {
                    clipboard.setPrimaryClip(ClipData.newPlainText("messages", it))
                }
            }) {
                Text("Copy")
            }
        }
    }
}

@Composable
fun DownloadScreen(
    viewModel: MainViewModel,
    dm: DownloadManager,
    models: List<Downloadable>
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Instructions or title
        Text(
            "Download Models",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // List of models to download
        Column(modifier = Modifier.weight(1f)) {
            for (model in models) {
                Downloadable.Button(viewModel, dm, model)
            }
        }

        // Unload model button at the bottom
        Button(
            onClick = { viewModel.unload() },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Unload Model")
        }
    }
}
