package com.example.androidpowerinfer

import com.example.androidpowerinfer.ui.theme.*
import android.app.DownloadManager
import android.content.ClipboardManager
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import com.example.androidpowerinfer.ui.theme.LlamaAndroidTheme
import java.io.File

/**
 * This Activity sets up the UI, logs memory stats, and initializes downloads and models.
 * It uses lazy-initialized system services and a MainViewModel to handle model loading and interaction.
 */
class MainActivity(
//    activityManager: ActivityManager? = null,
    downloadManager: DownloadManager? = null,
    clipboardManager: ClipboardManager? = null,
) : ComponentActivity() {

//    private val activityManager by lazy { activityManager ?: getSystemService<ActivityManager>()!! }
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

//        logMemoryInfo()
//        logDownloadDirectory()

        val models = listOf(
            Downloadable(
                name = "Bamboo 7B (Q4)",
                Uri.parse(
                    "https://huggingface.co/PowerInfer/Bamboo-base-v0.1-gguf/" +
                        "resolve/main/bamboo-7b-v0.1.Q4_0.powerinfer.gguf?download=true"
                ),
                File(getExternalFilesDir(null), "bamboo-7b-v0.1.Q4_0.powerinfer.gguf")
            ),
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
//    private fun logMemoryInfo() {
//        val memoryInfo = ActivityManager.MemoryInfo().also {
//            activityManager.getMemoryInfo(it)
//        }
//        val free = Formatter.formatFileSize(this, memoryInfo.availMem)
//        val total = Formatter.formatFileSize(this, memoryInfo.totalMem)
//        viewModel.log("Current memory: $free / $total")
//    }
//
//    /**
//     * Logs the directory where downloads will be stored.
//     */
//    private fun logDownloadDirectory() {
//        val extFilesDir = getExternalFilesDir(null)
//        viewModel.log("Downloads directory: $extFilesDir")
//    }
}

@Composable
fun MainCompose(
    viewModel: MainViewModel,
    clipboard: ClipboardManager,
    dm: DownloadManager,
    models: List<Downloadable>
) {
    // true : chat, false: Download
    var isChat by remember { mutableStateOf(false) }
    val onModelLoaded: () -> Unit = { isChat = true }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = isChat,
                    onClick = { isChat = true },
                    icon = {},
                    label = { Text("Chat") }
                )

                NavigationBarItem(
                    selected = !isChat,
                    onClick = { isChat = false },
                    icon = {},
                    label = { Text("Models") }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (isChat) {
                ChatScreen(viewModel, clipboard)
            } else {
                DownloadScreen(viewModel, dm, models, onModelLoaded)
            }
        }
    }
}

@Composable
fun ChatScreen(viewModel: MainViewModel, clipboard: ClipboardManager) {
    Column(Modifier.fillMaxSize()) {
        val scrollState = rememberLazyListState()

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(state = scrollState, reverseLayout = false) {
                items(viewModel.messages) { message ->
                    val isUser = message.sender == "User"
                    val bubbleShape = RoundedCornerShape(BubbleCornerShape)

                    val backgroundColor = when (message.sender) {
                        "User" -> UserBubbleColor
                        "Assistant" -> AssistantBubbleColor
                        else -> OtherBubbleColor
                    }

                    val alignment = if (isUser) Arrangement.End else Arrangement.Start

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = alignment
                    ) {
                        Column(
                            modifier = Modifier
                                .shadow(elevation = 2.dp, shape = bubbleShape)
                                .background(backgroundColor, shape = bubbleShape)
                                .padding(8.dp)
                                .widthIn(max = 300.dp)
                        ) {

                            if (!isUser) {
                                Text(
                                    text = message.sender,
                                    style = MaterialTheme.typography.senderTextStyle,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }

                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.messageTextStyle
                            )
                        }
                    }
                }
            }
        }

        Row(
            Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = viewModel.message,
                onValueChange = viewModel::updateMessage,
                label = { Text("Message") },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
            )

            Button(
                onClick = viewModel::send,
                modifier = Modifier.padding(2.dp)
            ) {
                Text(">")
            }
        }

//        Row(Modifier.padding(BubbleCornerShape)) {
//            Button(onClick = viewModel::send, modifier = Modifier.padding(end = SmallBubbleCornerShape)) {
//                Text("Send")
//            }
//            Button(onClick = viewModel::clear, modifier = Modifier.padding(end = SmallBubbleCornerShape)) {
//                Text("Clear")
//            }
//            Button(onClick = {
//                val copiedText = viewModel.messages.joinToString("\n") {
//                    "${it.sender}: ${it.content}"
//                }
//                clipboard.setPrimaryClip(ClipData.newPlainText("messages", copiedText))
//            }) {
//                Text("Copy")
//            }
//        }
    }
}

@Composable
fun DownloadScreen(
    viewModel: MainViewModel,
    dm: DownloadManager,
    models: List<Downloadable>,
    onModelLoaded: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Instructions or title
        Text(
            "Download Model Lists",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // List of models to download
        Column(modifier = Modifier.weight(1f)) {
            for (model in models) {
                Downloadable.Button(viewModel, dm, model, onModelLoaded = onModelLoaded)
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
