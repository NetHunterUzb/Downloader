package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.TemplateCode
import com.example.ui.viewmodel.*

// --- Customized Cyberpunk Color Tokens ---
val CyberBackground = Color(0xFF07070B)
val CyberDarkCard = Color(0x6612121E)
val CyberCyan = Color(0xFF00F2FE)
val CyberPurple = Color(0xFF9E00FF)
val CyberEmerald = Color(0xFF10B981)
val CyberTextSecondary = Color(0xFF94A3B8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloaderMainLayout(viewModel: DownloaderViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    // Fetch live reactive state flows
    val downloadLogs by viewModel.downloadLogs.collectAsState()
    val telegramUsers by viewModel.telegramUsers.collectAsState()
    val proxies by viewModel.proxies.collectAsState()
    val nodeLogs by viewModel.systemNodeLogs.collectAsState()
    val botChatHistory by viewModel.botChatHistory.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        bottomBar = {
            CustomBottomNavigationBar(
                activeTab = viewModel.activeTab,
                onTabSelected = { viewModel.activeTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberBackground)
                .padding(innerPadding)
                .drawBehind {
                    // Aesthetic backdrop glowing neon radial dots/blobs
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(CyberCyan.copy(alpha = 0.12f), Color.Transparent),
                            center = Offset(0f, 0f),
                            radius = size.width * 0.8f
                        ),
                        radius = size.width * 0.8f,
                        center = Offset(0f, 0f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(CyberPurple.copy(alpha = 0.12f), Color.Transparent),
                            center = Offset(size.width, size.height),
                            radius = size.width * 0.8f
                        ),
                        radius = size.width * 0.8f,
                        center = Offset(size.width, size.height)
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Top Brand Status bar
                TopStatusBar(proxies = proxies)

                Spacer(modifier = Modifier.height(12.dp))

                // Content render switches based on tabs state
                Box(modifier = Modifier.weight(1f)) {
                    when (viewModel.activeTab) {
                        "client" -> ClientWebView(
                            viewModel = viewModel,
                            downloadCount = downloadLogs.size
                        )
                        "bot" -> TelegramBotView(
                            viewModel = viewModel,
                            chatHistory = botChatHistory,
                            onClipboardPaste = {
                                clipboardManager.setText(AnnotatedString(it))
                                Toast.makeText(context, "Command link copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        )
                        "admin" -> AdminSettingsView(
                            viewModel = viewModel,
                            telegramUsers = telegramUsers,
                            downloadLogs = downloadLogs,
                            proxies = proxies,
                            nodeLogs = nodeLogs,
                            onBanClicked = { viewModel.executeBanUser(it) },
                            onUnbanClicked = { viewModel.executeUnbanUser(it) }
                        )
                        "exporter" -> CodeExporterView(
                            viewModel = viewModel,
                            onCopyClicked = { text, name ->
                                clipboardManager.setText(AnnotatedString(text))
                                Toast.makeText(context, "Successfully copied $name file to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        )
                        "ai_playground" -> AiPlaygroundView(
                            viewModel = viewModel,
                            onCopyOutput = {
                                clipboardManager.setText(AnnotatedString(it))
                                Toast.makeText(context, "Modified AI code copied!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

// --- Top Brand & Status Component ---
@Composable
fun TopStatusBar(proxies: List<com.example.data.ProxyStatus>) {
    val onlineProxiesCount = proxies.count { it.isOnline }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp)
            .border(1.dp, Brush.horizontalGradient(listOf(CyberCyan.copy(alpha = 0.2f), CyberPurple.copy(alpha = 0.2f))), RoundedCornerShape(12.dp))
            .background(CyberDarkCard, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "NEURA_CORE",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.6.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(CyberCyan.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "V2.2-PROD",
                        fontSize = 9.sp,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Text(
                text = "UPTIME_RECORD: 99.997% (STABLE)",
                fontSize = 10.sp,
                color = CyberTextSecondary,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(CyberEmerald)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$onlineProxiesCount NODES ACTIVE",
                fontSize = 10.sp,
                color = CyberEmerald,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// --- Client WebView (Web Downloader Simulation Dashboard) ---
@Composable
fun ClientWebView(
    viewModel: DownloaderViewModel,
    downloadCount: Int
) {
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalContext.current
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "UNIVERSAL STREAM EXTRACTOR",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Multi-source failover scraper fueled by python yt-dlp pipelines",
                    fontSize = 11.sp,
                    color = CyberTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        item {
            // High Tech Glass Search / Paste Form card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberCyan.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .background(CyberDarkCard, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "DESKTOP SCRAPER VIEWPORT (WEB)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyberCyan,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "URL Link",
                        tint = CyberCyan.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                    
                    TextField(
                        value = viewModel.clientUrlInput,
                        onValueChange = { viewModel.clientUrlInput = it },
                        placeholder = { Text("Paste YouTube, TikTok, or Instagram link...", fontSize = 12.sp, color = Color.Gray) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("url_text_input"),
                        textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = CyberCyan
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            viewModel.extractUrlClientSide()
                        })
                    )

                    Button(
                        onClick = {
                            val text = clipboardManager.getText()?.text
                            if (text != null && text.startsWith("http")) {
                                viewModel.clientUrlInput = text
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("PASTE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberCyan, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action extracting button with cyber gradient glow
                Button(
                    onClick = { viewModel.extractUrlClientSide() },
                    enabled = !viewModel.clientExtracting && viewModel.clientUrlInput.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("extract_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(CyberCyan, CyberPurple)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (viewModel.clientExtracting) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ANALYZING SOURCE METADATA...", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        } else {
                            Text("EXTRACT VIDEO STREAM ☄️", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                viewModel.clientErrorState?.let { err ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x33EF4444), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0x66EF4444), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = "Error", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = err, color = Color(0xFFFCA5A5), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // Processing Result Box
        viewModel.clientMetaResult?.let { meta ->
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .background(CyberDarkCard, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(CyberCyan.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(meta.platform.uppercase(java.util.Locale.getDefault()), fontSize = 10.sp, color = CyberCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Text(
                            text = "${meta.durationSec / 60}:${(meta.durationSec % 60).toString().padStart(2, '0')} MIN",
                            fontSize = 11.sp,
                            color = CyberTextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Thumbnail image
                        AsyncImage(
                            model = meta.thumbnail,
                            contentDescription = "Thumbnail",
                            modifier = Modifier
                                .size(width = 110.dp, height = 75.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f)),
                            contentScale = ContentScale.Crop
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = meta.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Verified Status", tint = CyberCyan, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("DAEMON SCRAPE STABLE", color = CyberCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("CHOOSE AUDIO/VIDEO STREAM PIPELINE", fontSize = 11.sp, color = CyberCyan, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Resolutions List Table
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.2f))
                    ) {
                        meta.formats.forEachIndexed { index, format ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = format.resolution, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(text = "${format.ext} File format", fontSize = 10.sp, color = CyberTextSecondary)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "${format.sizeMb} MB", fontSize = 12.sp, color = CyberTextSecondary, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(end = 12.dp))
                                    Button(
                                        onClick = { viewModel.submitSimulatedDownload(format, meta) },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.15f)),
                                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("PULL", fontSize = 11.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            if (index < meta.formats.size - 1) {
                                Divider(color = Color.White.copy(alpha = 0.05f))
                            }
                        }
                    }
                }
            }
        }

        // Global telemetry highlights
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(
                    Triple("EST_CLIENT_DLS", "$downloadCount units", CyberCyan),
                    Triple("DB_AGGREGATE", "Online (Mongo)", CyberPurple),
                    Triple("PING_uptime", "99.9%", CyberEmerald)
                ).forEach { items ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .background(CyberDarkCard, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = items.first, fontSize = 9.sp, color = CyberTextSecondary, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = items.second, fontSize = 11.sp, color = items.third, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

// --- Bot View (Telegram Chat Simulator UX inside App) ---
@Composable
fun TelegramBotView(
    viewModel: DownloaderViewModel,
    chatHistory: List<ChatMessage>,
    onClipboardPaste: (String) -> Unit
) {
    var isInputValidLink by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "TELEGRAM BOT INTERACTION HUB",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = "Simulate start triggers, callback clicks, and automated video parsing",
                fontSize = 11.sp,
                color = CyberTextSecondary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chat Container Box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(10.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                reverseLayout = false,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(chatHistory) { message ->
                    val isBot = message.sender == "Bot"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isBot) Arrangement.Start else Arrangement.End
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .background(
                                    if (isBot) Color(0xFF1E1E2F) else CyberPurple.copy(alpha = 0.3f),
                                    RoundedCornerShape(
                                        topStart = 12.dp,
                                        topEnd = 12.dp,
                                        bottomStart = if (isBot) 0.dp else 12.dp,
                                        bottomEnd = if (isBot) 12.dp else 0.dp
                                    )
                                )
                                .border(
                                    1.dp,
                                    if (isBot) Color.White.copy(alpha = 0.05f) else CyberPurple.copy(alpha = 0.4f),
                                    RoundedCornerShape(
                                        topStart = 12.dp,
                                        topEnd = 12.dp,
                                        bottomStart = if (isBot) 0.dp else 12.dp,
                                        bottomEnd = if (isBot) 12.dp else 0.dp
                                    )
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = if (isBot) "🤖 DOWNLOAD_BOT" else "👤 USER_CLIENT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isBot) CyberCyan else CyberPurple,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            
                            // Supports simple HTML formatting rendering inside bot chat (simulated)
                            Text(
                                text = message.text.replace("<b>", "").replace("</b>", "").replace("<i>", "").replace("</i>", "").replace("<code>", "`").replace("</code>", "`"),
                                fontSize = 12.sp,
                                color = Color.White,
                                lineHeight = 16.sp
                            )
                            
                            // Render inline callback buttons if populated
                            if (message.inlineButtons.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    message.inlineButtons.forEach { pair ->
                                        Button(
                                            onClick = { viewModel.handleBotCallback(pair.second) },
                                            modifier = Modifier.fillMaxWidth().height(32.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(pair.first, fontSize = 11.sp, color = CyberCyan, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                            
                            Text(
                                text = message.timestamp,
                                fontSize = 8.sp,
                                color = CyberTextSecondary, 
                                modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Custom Quick Commands Trigger pills
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                "/start" to "Register bot",
                "/admin" to "Control board",
                "https://youtu.be/dQw4w9" to "Test Stream URL"
            ).forEach { pair ->
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .clickable { viewModel.botUserInputMessage = pair.first }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(pair.first, fontSize = 9.sp, color = CyberCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Send Input Bar Form
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .background(CyberDarkCard, RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = viewModel.botUserInputMessage,
                onValueChange = { viewModel.botUserInputMessage = it },
                placeholder = { Text("Send bot message or paste clip URL...", fontSize = 12.sp, color = Color.Gray) },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = CyberPurple
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    viewModel.sendBotMessage()
                })
            )

            IconButton(
                onClick = { viewModel.sendBotMessage() },
                enabled = viewModel.botUserInputMessage.isNotEmpty(),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Message",
                    tint = if (viewModel.botUserInputMessage.isNotEmpty()) CyberCyan else CyberTextSecondary
                )
            }
        }
    }
}

// --- Admin Panel Settings View (Admin ID Control Suite) ---
@Composable
fun AdminSettingsView(
    viewModel: DownloaderViewModel,
    telegramUsers: List<com.example.data.TelegramUser>,
    downloadLogs: List<com.example.data.DownloadLog>,
    proxies: List<com.example.data.ProxyStatus>,
    nodeLogs: List<String>,
    onBanClicked: (Long) -> Unit,
    onUnbanClicked: (Long) -> Unit
) {
    val loggedIn = viewModel.loggedInAdminId != null
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(text = "ADMIN CONTROLLERS (SECURE)", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text(text = "Enter ADMIN_TELEGRAM_ID to unlock metrics overlays & toggles", fontSize = 11.sp, color = CyberTextSecondary)
            }
        }

        if (!loggedIn) {
            // Log in Box
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberPurple.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .background(CyberDarkCard, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Security Unlock", tint = CyberPurple, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("UNAUTHORIZED HOOK DETECTED", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Provide simulated or real Admin ID (example: 123456789 or any numbers) to authenticate central connection nodes.", fontSize = 10.sp, color = CyberTextSecondary, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = viewModel.adminTelegramIdInput,
                        onValueChange = { viewModel.adminTelegramIdInput = it },
                        label = { Text("ADMIN_TELEGRAM_ID", color = CyberTextSecondary, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            cursorColor = CyberCyan
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    if (viewModel.customAdminId.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Or touch here to use configured Admin ID: ${viewModel.customAdminId}",
                            fontSize = 11.sp,
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .clickable {
                                    viewModel.adminTelegramIdInput = viewModel.customAdminId
                                    viewModel.submitAdminLogin()
                                }
                                .padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.submitAdminLogin() },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("AUTHENTICATE DEEP SECURE LINE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        } else {
            // Fully Authenticated Dashboard
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberEmerald.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .background(CyberEmerald.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Active", tint = CyberEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AUTHENTICATED: ID ${viewModel.loggedInAdminId}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                    }
                    Text(
                        "DISCONNECT",
                        fontSize = 10.sp,
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clickable { viewModel.logoutAdmin() }
                            .padding(4.dp)
                    )
                }
            }

            // Realtime Admin Control Metrics Box Grid
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .background(CyberDarkCard, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text("TOTAL REGISTERED", fontSize = 9.sp, color = CyberTextSecondary, fontFamily = FontFamily.Monospace)
                        Text("${telegramUsers.size} Users", fontSize = 16.sp, fontWeight = FontWeight.Black, color = CyberCyan, fontFamily = FontFamily.Monospace)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .background(CyberDarkCard, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text("STREAM EXTRACTION", fontSize = 9.sp, color = CyberTextSecondary, fontFamily = FontFamily.Monospace)
                        Text("${downloadLogs.size} Scrapes", fontSize = 16.sp, fontWeight = FontWeight.Black, color = CyberPurple, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // Global System Flags (Toggles)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .background(CyberDarkCard, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text("GLOBAL ENGINE CONFIGURATION", fontSize = 11.sp, color = CyberCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Maintenance Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Maintenance Mode", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Switches Downloader Web & Bot systems offline", color = CyberTextSecondary, fontSize = 10.sp)
                        }
                        Switch(
                            checked = viewModel.isMaintenanceMode,
                            onCheckedChange = { viewModel.toggleMaintenance(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberCyan,
                                checkedTrackColor = CyberCyan.copy(alpha = 0.4f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = Color.White.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Disable YouTube downloads
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Disable YouTube Scrapes", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Restrict YouTube URL parsing with direct warning feedback", color = CyberTextSecondary, fontSize = 10.sp)
                        }
                        Switch(
                            checked = viewModel.isDisableYoutube,
                            onCheckedChange = { viewModel.toggleYoutubeRestrict(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberPurple,
                                checkedTrackColor = CyberPurple.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }

            // Ban/Unban user section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .background(CyberDarkCard, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text("SECURITY FIREWALL: IP & ID SHIELD", fontSize = 11.sp, color = CyberCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextField(
                            value = viewModel.testUserToBanInput,
                            onValueChange = { viewModel.testUserToBanInput = it },
                            placeholder = { Text("Enter Target ID to black/whitelist...", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                            shape = RoundedCornerShape(8.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Black.copy(alpha = 0.4f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )

                        Button(
                            onClick = {
                                val target = viewModel.testUserToBanInput.toLongOrNull()
                                if (target != null) {
                                    viewModel.executeBanUser(target)
                                    viewModel.testUserToBanInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("BAN", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Render simulated database user list
                    Text("REGISTERED SHIELD DIRECTORY (REDIS/MONGO)", fontSize = 9.sp, color = CyberTextSecondary, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                    ) {
                        telegramUsers.forEach { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("@${user.username} (${user.id})", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("Downloaded: ${user.downloadedCount} | Join: ${user.joinDate}", fontSize = 9.sp, color = CyberTextSecondary)
                                }
                                Box {
                                    if (user.isBanned) {
                                        Text(
                                            "BANNED",
                                            color = Color.Red,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .border(1.dp, Color.Red, RoundedCornerShape(4.dp))
                                                .clickable { onUnbanClicked(user.id) }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    } else {
                                        Text(
                                            "BAN",
                                            color = CyberTextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                .clickable { onBanClicked(user.id) }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Interactive Announce Broadcaster (Rate limiting visualizer)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .background(CyberDarkCard, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text("SYSTEM BROADCAST QUEUE ENGINE", fontSize = 11.sp, color = CyberCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Pre-configulates async scheduling algorithms to avoid Telegram speed block limits (30 actions/sec limit).", fontSize = 9.sp, color = CyberTextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))

                    TextField(
                        value = viewModel.adminLogBroadcastingText,
                        onValueChange = { viewModel.adminLogBroadcastingText = it },
                        placeholder = { Text("E.g., Platform updates node maintenance complete trigger...", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black.copy(alpha = 0.4f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (viewModel.isBroadcastingRunning) {
                        LinearProgressIndicator(
                            progress = viewModel.broadcastProgress,
                            color = CyberCyan,
                            trackColor = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("BROADCASTING QUEUE RUNNING: ${(viewModel.broadcastProgress * 100).toInt()}% COMPLETE", fontSize = 10.sp, color = CyberCyan, fontFamily = FontFamily.Monospace)
                    } else {
                        Button(
                            onClick = { viewModel.runBroadcastQueue() },
                            enabled = viewModel.adminLogBroadcastingText.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("RUN ASYNC DISPATCH QUEUE 📢", fontSize = 11.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Centralized Node Logs Output viewport (Terminal-style)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .background(Color.Black, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Text("CORE_DAEMON_OUTPUT_STREAM", fontSize = 10.sp, color = CyberEmerald, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column {
                            nodeLogs.forEach { log ->
                                Text(log, fontSize = 9.sp, color = CyberEmerald, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(bottom = 2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Dynamic Exporter Panel View ---
@Composable
fun CodeExporterView(viewModel: DownloaderViewModel, onCopyClicked: (String, String) -> Unit) {
    var activeLangTab by remember { mutableStateOf("FastAPI") } // "FastAPI", "Bot", "React"

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(text = "REFERENCE CODE EXPORTER", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text(text = "Production-ready, ready-to-copy source code blocks for system components", fontSize = 11.sp, color = CyberTextSecondary)
        }

        // Custom Configuration Fields Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberCyan.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberDarkCard.copy(alpha = 0.8f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "⚙️ SOZLAMALAR / CONFIGURATION",
                    fontSize = 11.sp,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Eksport qilinayotgan kodda sizning haqiqiy Telegram Bot tokeningiz va Admin IDingizni avtomatik sozlash uchun quyidagilarni kiriting:",
                    fontSize = 10.sp,
                    color = CyberTextSecondary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // BOT TOKEN Input
                    OutlinedTextField(
                        value = viewModel.customBotToken,
                        onValueChange = { viewModel.saveCustomBotToken(it) },
                        label = { Text("Telegram Bot Token", fontSize = 10.sp, color = CyberCyan.copy(alpha = 0.7f)) },
                        placeholder = { Text("E.g., 123456789:ABC_...", fontSize = 10.sp, color = Color.Gray) },
                        modifier = Modifier.weight(1.3f).testTag("custom_bot_token_input"),
                        textStyle = TextStyle(color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedLabelColor = CyberCyan,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.4f)
                        )
                    )

                    // ADMIN ID Input
                    OutlinedTextField(
                        value = viewModel.customAdminId,
                        onValueChange = { viewModel.saveCustomAdminId(it) },
                        label = { Text("Admin Telegram ID", fontSize = 10.sp, color = CyberCyan.copy(alpha = 0.7f)) },
                        placeholder = { Text("E.g., 98210398", fontSize = 10.sp, color = Color.Gray) },
                        modifier = Modifier.weight(0.7f).testTag("custom_admin_id_input"),
                        textStyle = TextStyle(color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedLabelColor = CyberCyan,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }

        // Language Tabs Toggle Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("FastAPI", "Bot", "React").forEach { tab ->
                val selected = activeLangTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selected) CyberCyan.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { activeLangTab = tab }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (tab == "React") "App.jsx (React)" else if (tab == "Bot") "bot.py (Aiogram)" else "main.py (FastAPI)",
                        color = if (selected) CyberCyan else CyberTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        val baseCode = when (activeLangTab) {
            "FastAPI" -> TemplateCode.FASTAPI_CODE
            "Bot" -> TemplateCode.TELEGRAM_BOT_CODE
            else -> TemplateCode.FRONTEND_CODE
        }

        // Dynamically replace with custom bot token/admin id if provided
        val codeValue = remember(baseCode, viewModel.customBotToken, viewModel.customAdminId) {
            var temp = baseCode
            if (viewModel.customBotToken.isNotEmpty()) {
                temp = temp.replace("000000000:AAE_DummyTokenForTesting", viewModel.customBotToken)
            }
            if (viewModel.customAdminId.isNotEmpty()) {
                temp = temp.replace("123456789", viewModel.customAdminId)
            }
            temp
        }

        // Code editor viewport with terminal scrolling
        Column(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                .background(Color(0xFF0D0D15), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (activeLangTab == "React") "REACT CLIENT APP" else "PYTHON COMPONENT SOURCE",
                    fontSize = 9.sp,
                    color = CyberTextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                
                Button(
                    onClick = { onCopyClicked(codeValue, activeLangTab) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text("COPY CODE CODE", fontSize = 9.sp, color = CyberCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = codeValue,
                    color = Color(0xFFA5B4FC),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

// --- AI Playroom Screen (Dynamic Customizer Module) ---
@Composable
fun AiPlaygroundView(viewModel: DownloaderViewModel, onCopyOutput: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(text = "AI CODE RE-WRITER (GEMINI)", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text(text = "Direct REST call to Gemini model to alter/extend production code in real-time!", fontSize = 11.sp, color = CyberTextSecondary)
        }

        // Selection of active source file
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                .padding(4.dp)
        ) {
            listOf("FastAPI", "Bot", "React").forEach { tab ->
                val isSelected = viewModel.selectedPlaygroundPlatform == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) CyberPurple.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { viewModel.selectedPlaygroundPlatform = tab }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (tab == "React") "App.jsx (React)" else if (tab == "Bot") "bot.py (Aiogram)" else "main.py (FastAPI)",
                        color = if (isSelected) CyberPurple else CyberTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Prompt input area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                .background(CyberDarkCard, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Text("WHAT CUSTOM RE-FACTORS DO YOU WANT TO ADD?", fontSize = 11.sp, color = CyberPurple, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(2.dp))
            Text("E.g., 'Add basic token auth to endpoints' or 'Add logging commands and database cleanup'", fontSize = 9.sp, color = CyberTextSecondary)
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = viewModel.playgroundPromptInput,
                onValueChange = { viewModel.playgroundPromptInput = it },
                placeholder = { Text("E.g., Add secure endpoints validation checks and custom route logging variables...", fontSize = 12.sp, color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().testTag("ai_prompt_input"),
                textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                    unfocusedContainerColor = Color.Black.copy(alpha = 0.15f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // AI Action Button
            Button(
                onClick = { viewModel.runGeminiModificationCode() },
                enabled = !viewModel.isAiWorking && viewModel.playgroundPromptInput.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(42.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (viewModel.isAiWorking) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("GEMINI IS WRITING NEW PIPELINE KERNEL...", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Run AI", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("DISPATCH AI ENGINE MODIFICATIONS 🚀", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // AI Output code viewer
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                .background(Color(0xFF06060B), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("GEMINI-GENERATED PIPELINE CODE OUTPUT", fontSize = 9.sp, color = CyberTextSecondary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                
                if (viewModel.aiEditedCodeResult.isNotEmpty() && !viewModel.aiEditedCodeResult.startsWith("API Key")) {
                    Button(
                        onClick = { onCopyOutput(viewModel.aiEditedCodeResult) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPurple.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("COPY NEW CODE CODE", fontSize = 8.sp, color = CyberPurple, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (viewModel.aiEditedCodeResult.isEmpty()) "AI-customized output code files will appear here... Prompt Gemini to optimize any code snippet above." else viewModel.aiEditedCodeResult,
                    color = if (viewModel.aiEditedCodeResult.startsWith("API Key") || viewModel.aiEditedCodeResult.startsWith("Error")) Color.Red else Color(0xFFFBBF24),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

// --- Custom Cyberpunk Navigation Bar Components ---
@Composable
fun CustomBottomNavigationBar(activeTab: String, onTabSelected: (String) -> Unit) {
    Surface(
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)))
            .windowInsetsPadding(WindowInsets.navigationBars), // Prevent layout underlap on system keys/pills
        color = Color(0xEA07070B)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val navItems = listOf(
                Quadruple("client", "Scraper", Icons.Default.Home, "Scraper screen"),
                Quadruple("bot", "BotSim", Icons.Default.Send, "Bot screen"),
                Quadruple("admin", "Admin", Icons.Default.Settings, "Admin panel"),
                Quadruple("exporter", "Export Code", Icons.Default.List, "Code panel"),
                Quadruple("ai_playground", "AI Custom", Icons.Default.Star, "AI customizer")
            )

            navItems.forEach { item ->
                val selected = activeTab == item.first
                Column(
                    modifier = Modifier
                        .clickable { onTabSelected(item.first) }
                        .padding(horizontal = 6.dp, vertical = 8.dp)
                        .minimumInteractiveComponentSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.third,
                        contentDescription = item.fourth,
                        tint = if (selected) { if (item.first == "ai_playground") CyberPurple else CyberCyan } else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.second,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) Color.White else Color.White.copy(alpha = 0.4f),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// --- Custom helper tuples ---
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
