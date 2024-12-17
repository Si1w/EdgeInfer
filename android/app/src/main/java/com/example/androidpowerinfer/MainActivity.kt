package com.example.androidpowerinfer

//import android.app.ActivityManager
import com.example.androidpowerinfer.ui.theme.*
import android.app.DownloadManager
import android.content.ClipboardManager
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import com.example.androidpowerinfer.ui.theme.LlamaAndroidTheme
import java.io.File

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

        /**
         * List of models to available to download and use
         * @param name: name of the model
         * @param uri: uri of the model to download
         * @param file: file to save the model to
         */
        val models = listOf(
            Downloadable(
                name = "Bamboo 7B (Q4)",
                Uri.parse(
                    "https://huggingface.co/PowerInfer/Bamboo-base-v0.1-gguf/" +
                        "resolve/main/bamboo-7b-v0.1.Q4_0.powerinfer.gguf?download=true"
                ),
                File(getExternalFilesDir(null), "bamboo-7b-v0.1.Q4_0.powerinfer.gguf")
            ),
            Downloadable(
                name = "Bamboo DPO (Q4)",
                Uri.parse(
                    "https://huggingface.co/PowerInfer/Bamboo-DPO-v0.1-gguf/" +
                        "resolve/main/bamboo-7b-dpo-v0.1.Q4_0.powerinfer.gguf?download=true"
                ),
                File(getExternalFilesDir(null), "bamboo-7b-dpo-v0.1.Q4_0.powerinfer.gguf")
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
//                        clipboard = clipboardManager,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainCompose(
    viewModel: MainViewModel,
//    clipboard: ClipboardManager,
    dm: DownloadManager,
    models: List<Downloadable>
) {
    // true : chat, false: Download
    var isChat by remember { mutableStateOf(false) }
    val onModelLoaded: () -> Unit = { isChat = true }
    val context = LocalContext.current

    // NavBar
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PowerInfer") },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.exportChatToPdf(context)
                        },
                        enabled = !viewModel.isExporting
                    ) {
                        if (viewModel.isExporting) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export Chat"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                )
            )
        },
        bottomBar = {
            NavigationBar (
                containerColor = MaterialTheme.colorScheme.onPrimary
            ) {
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
                ChatScreen(viewModel)
            } else {
                DownloadScreen(viewModel, dm, models, onModelLoaded)
            }
        }
    }
}

@Composable
fun ChatScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scrollState = rememberLazyListState()
    val messages by remember { derivedStateOf { viewModel.messages } }
    val isParsing by remember { derivedStateOf { viewModel.isParsing } }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                viewModel.uploadPDF(context, it)
            }
        }
    )

    Column(Modifier.fillMaxSize()) {
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

                if (isParsing) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp,vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { pdfPickerLauncher.launch("application/pdf") },
                modifier = Modifier.size(35.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add PDF",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            OutlinedTextField(
                value = viewModel.message,
                onValueChange = viewModel::updateMessage,
                label = { Text("Message") },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            )

            IconButton(
                onClick = viewModel::send,
                modifier = Modifier.size(35.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Send Message",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        LaunchedEffect(key1 = messages.size, key2 = isParsing) {
            if (messages.isNotEmpty()) {
                scrollState.animateScrollToItem(messages.size)
            }
        }
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

        Text(
            "Download Model Lists",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            for (model in models) {
                Downloadable.Button(viewModel, dm, model, onModelLoaded = onModelLoaded)
            }
        }

        Button(
            onClick = { viewModel.unload() },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Unload Model")
        }
    }
}
