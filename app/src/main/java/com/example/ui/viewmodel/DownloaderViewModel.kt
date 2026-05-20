package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiManager
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// --- Auxiliary Structs ---
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "User" or "Bot" Or "AdminSystem"
    val text: String,
    val timestamp: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
    val isPhoto: Boolean = false,
    val photoUrl: String = "",
    val inlineButtons: List<Pair<String, String>> = emptyList() // Label to CallbackData
)

data class SimulatedMeta(
    val title: String,
    val thumbnail: String,
    val durationSec: Int,
    val platform: String,
    val formats: List<SimulatedFormat>
)

data class SimulatedFormat(
    val formatId: String,
    val resolution: String,
    val ext: String,
    val sizeMb: Double
)

class DownloaderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DownloaderRepository
    
    // --- Tabs & Active UI State ---
    var activeTab by mutableStateOf("client") // "client", "bot", "admin", "exporter", "ai_playground"
    
    // --- Admin Settings & Authentication ---
    var adminTelegramIdInput by mutableStateOf("")
    var loggedInAdminId by mutableStateOf<Long?>(null)
    var isMaintenanceMode by mutableStateOf(false)
    var isDisableYoutube by mutableStateOf(false)
    
    // --- Custom Bot Token & Admin ID configs ---
    var customBotToken by mutableStateOf("")
    var customAdminId by mutableStateOf("")
    
    // --- Client States ---
    var clientUrlInput by mutableStateOf("")
    var clientExtracting by mutableStateOf(false)
    var clientMetaResult by mutableStateOf<SimulatedMeta?>(null)
    var clientErrorState by mutableStateOf<String?>(null)
    
    // --- Bot Simulator States ---
    var botUserInputMessage by mutableStateOf("")
    var botChatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    var isBotProcessingLink by mutableStateOf(false)
    
    // --- Admin Dashboard States ---
    var adminLogBroadcastingText by mutableStateOf("")
    var isBroadcastingRunning by mutableStateOf(false)
    var broadcastProgress by mutableStateOf(0f)
    var testUserToBanInput by mutableStateOf("")
    
    // --- AI Code Playground States ---
    var selectedPlaygroundPlatform by mutableStateOf("FastAPI") // "FastAPI", "Bot", "React"
    var playgroundPromptInput by mutableStateOf("")
    var isAiWorking by mutableStateOf(false)
    var aiEditedCodeResult by mutableStateOf("")
    
    // --- Real-time Node Logs ---
    val systemNodeLogs = MutableStateFlow<List<String>>(emptyList())
    
    // --- Flows from DB ---
    val downloadLogs: StateFlow<List<DownloadLog>>
    val telegramUsers: StateFlow<List<TelegramUser>>
    val proxies: StateFlow<List<ProxyStatus>>
    
    init {
        val database = DownloaderDatabase.getDatabase(application)
        repository = DownloaderRepository(database.downloaderDao())
        
        // Connect Live Flows
        downloadLogs = repository.downloadLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        telegramUsers = repository.telegramUsers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        proxies = repository.systemProxies.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
        // Hydrate default entities securely if first launch
        viewModelScope.launch {
            repository.systemProxies.first().let { currentList ->
                if (currentList.isEmpty()) {
                    seedDefaultData()
                }
            }
            
            // Read settings from Database if they exist
            isMaintenanceMode = repository.getSetting("maintenance_mode", "false").toBoolean()
            isDisableYoutube = repository.getSetting("disable_youtube", "false").toBoolean()
            loggedInAdminId = repository.getSetting("logged_admin_id", "").toLongOrNull()
            customBotToken = repository.getSetting("custom_bot_token", "")
            customAdminId = repository.getSetting("custom_admin_id", "")
            
            initBotWelcomeHistory()
            appendLog("🟢 Universal Downloader Node Daemon successfully loaded.")
            appendLog("📡 Connecting to shared PostgreSQL/MongoDB simulated gateways.")
        }
    }

    private fun appendLog(log: String) {
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        systemNodeLogs.value = (systemNodeLogs.value + "[$time] $log").takeLast(50)
    }

    private suspend fun seedDefaultData() {
        appendLog("📦 Database hydrate check: Empty. Injecting mock master schema records.")
        // Add Proxies
        repository.addProxy(ProxyStatus(address = "192.168.10.42:3128", country = "Uzbekistan", speedMs = 15, isOnline = true))
        repository.addProxy(ProxyStatus(address = "198.51.100.12:8880", country = "Germany", speedMs = 74, isOnline = true))
        repository.addProxy(ProxyStatus(address = "203.0.113.43:8080", country = "United States", speedMs = 124, isOnline = true))
        repository.addProxy(ProxyStatus(address = "185.190.141.51:443", country = "Singapore", speedMs = 999, isOnline = false))

        // Add Telegram Users
        repository.addTelegramUser(TelegramUser(98210398, "momin_dev", "2026-05-18 10:20", 12))
        repository.addTelegramUser(TelegramUser(123456789, "cyber_admin", "2026-05-10 08:15", 3))
        repository.addTelegramUser(TelegramUser(50239103, "ux_tester_88", "2026-05-19 14:40", 25))
        repository.addTelegramUser(TelegramUser(88203928, "spammer_link", "2026-05-20 01:10", 0, isBanned = true))

        // Add Download logs
        repository.addDownloadLog(DownloadLog(url = "https://youtube.com/watch?v=dQw4w9WgXcQ", title = "Never Gonna Give You Up", platform = "YouTube", resolution = "1080p", format = "MP4", sizeMb = 34.2, status = "Completed"))
        repository.addDownloadLog(DownloadLog(url = "https://instagram.com/p/C9hV0fSae09", title = "Neon Tokyo Drifting Aesthetic", platform = "Instagram", resolution = "720p", format = "MP4", sizeMb = 14.8, status = "Completed"))
        repository.addDownloadLog(DownloadLog(url = "https://tiktok.com/@user/video/728103", title = "Spaghetti Tutorial Loop", platform = "TikTok", resolution = "480p", format = "MP4", sizeMb = 5.2, status = "Completed"))
        repository.addDownloadLog(DownloadLog(url = "https://youtube.com/watch?v=y6120QOlsfU", title = "Cyberpunk Music Mix", platform = "YouTube", resolution = "MP3", format = "MP3", sizeMb = 8.4, status = "Completed"))
    }

    private fun initBotWelcomeHistory() {
        botChatHistory.value = listOf(
            ChatMessage(sender = "Bot", text = "🤖 <b>UNIVERSAL HYPERENGINE SYSTEM</b>\n\nWelcome to the official simulator chat. Submit a command like /start, /admin, or simply paste a video url to see structural processing outputs in real time.")
        )
    }

    // --- Interactive Settings Control ---
    fun toggleMaintenance(enabled: Boolean) {
        isMaintenanceMode = enabled
        viewModelScope.launch {
            repository.saveSetting("maintenance_mode", enabled.toString())
            appendLog("⚙️ Admin Settings Changed: Maintenance mode = $enabled")
        }
    }

    fun toggleYoutubeRestrict(enabled: Boolean) {
        isDisableYoutube = enabled
        viewModelScope.launch {
            repository.saveSetting("disable_youtube", enabled.toString())
            appendLog("⚙️ Admin Settings Changed: Disable YouTube = $enabled")
        }
    }

    // --- Admin Panel Authentication ---
    fun submitAdminLogin(): Boolean {
        val parsedId = adminTelegramIdInput.toLongOrNull()
        if (parsedId != null) {
            loggedInAdminId = parsedId
            viewModelScope.launch {
                repository.saveSetting("logged_admin_id", parsedId.toString())
                // Ensure this user exists in list
                repository.addTelegramUser(TelegramUser(parsedId, "active_admin", "2026-05-20 18:29", 0))
                appendLog("🔐 Telegram Admin ID authenticated: $parsedId")
            }
            return true
        }
        return false
    }

    fun logoutAdmin() {
        loggedInAdminId = null
        viewModelScope.launch {
            repository.saveSetting("logged_admin_id", "")
            appendLog("🔓 Admin console logged out.")
        }
    }

    fun executeBanUser(userId: Long) {
        viewModelScope.launch {
            repository.updateBanStatus(userId, true)
            appendLog("🔒 Admin Command Executed: Ban user ID $userId")
        }
    }

    fun executeUnbanUser(userId: Long) {
        viewModelScope.launch {
            repository.updateBanStatus(userId, false)
            appendLog("🔑 Admin Command Executed: Unban user ID $userId")
        }
    }

    fun saveCustomBotToken(token: String) {
        customBotToken = token
        viewModelScope.launch {
            repository.saveSetting("custom_bot_token", token)
            appendLog("⚙️ Custom Bot Token configured: $token")
        }
    }

    fun saveCustomAdminId(id: String) {
        customAdminId = id
        viewModelScope.launch {
            repository.saveSetting("custom_admin_id", id)
            appendLog("⚙️ Custom Admin ID configured: $id")
        }
    }

    // --- Interactive Downloader Extractor ---
    fun extractUrlClientSide() {
        if (clientUrlInput.isEmpty()) return
        
        clientExtracting = true
        clientErrorState = null
        clientMetaResult = null
        
        appendLog("🚀 Client WebApp triggering: Extract metadata for URL '$clientUrlInput'")

        viewModelScope.launch {
            delay(1500) // Simulate processing delay
            
            // Checks for restriction flags
            if (isMaintenanceMode) {
                clientErrorState = "503 Daemon Error: System in maintenance mode toggled by supervisor."
                clientExtracting = false
                appendLog("❌ Client WebApp block: Maintenance mode active.")
                return@launch
            }
            
            val isYt = clientUrlInput.contains("youtube.com") || clientUrlInput.contains("youtu.be")
            if (isDisableYoutube && isYt) {
                clientErrorState = "400 Client Error: YouTube downloads are temporarily disabled by platform administration."
                clientExtracting = false
                appendLog("❌ Client WebApp block: YouTube restricted.")
                return@launch
            }

            // Successfully map dummy items based on URL
            val resolvedPlatform = when {
                clientUrlInput.contains("youtube.com") || clientUrlInput.contains("youtu.be") -> "YouTube"
                clientUrlInput.contains("instagram.com") -> "Instagram"
                clientUrlInput.contains("tiktok.com") -> "TikTok"
                else -> "Generic Stream Client"
            }

            val randomExtractedTitle = when (resolvedPlatform) {
                "YouTube" -> "Lofi Coffee Shop Vibes - Coding chill stream"
                "Instagram" -> "Luxury Glassmorphic Dashboard Design Reveal"
                "TikTok" -> "Cooking Pasta in Rome Loop #shorts"
                else -> "Direct Link File Stream"
            }

            val randomImg = when (resolvedPlatform) {
                "YouTube" -> "https://images.unsplash.com/photo-1518495973542-4542c06a5843?w=500&auto=format&fit=crop"
                "Instagram" -> "https://images.unsplash.com/photo-1507238691740-187a5b1d37b8?w=500&auto=format&fit=crop"
                "TikTok" -> "https://images.unsplash.com/photo-1498837167922-ddd27525d352?w=500&auto=format&fit=crop"
                else -> "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=500&auto=format&fit=crop"
            }

            clientMetaResult = SimulatedMeta(
                title = randomExtractedTitle,
                thumbnail = randomImg,
                durationSec = 245,
                platform = resolvedPlatform,
                formats = listOf(
                    SimulatedFormat("yt-1080p", "1080p", "MP4", 45.2),
                    SimulatedFormat("yt-720p", "720p", "MP4", 25.4),
                    SimulatedFormat("yt-480p", "480p", "MP4", 12.1),
                    SimulatedFormat("yt-audio", "Audio Audio", "MP3", 5.6)
                )
            )
            clientExtracting = false
            appendLog("🟢 Client WebApp extraction completed: $randomExtractedTitle ($resolvedPlatform)")
        }
    }

    fun submitSimulatedDownload(format: SimulatedFormat, meta: SimulatedMeta) {
        viewModelScope.launch {
            appendLog("📥 Downloading Stream format [${format.resolution}] - Title: ${meta.title}")
            delay(1000)
            repository.addDownloadLog(
                DownloadLog(
                    url = clientUrlInput,
                    title = meta.title,
                    platform = meta.platform,
                    resolution = format.resolution,
                    format = format.ext,
                    sizeMb = format.sizeMb,
                    status = "Completed"
                )
            )
            appendLog("🟢 Stream Complete: saved record logs to shared sqlite indices.")
        }
    }

    // --- Interactive Telegram Bot Simulation ---
    fun sendBotMessage() {
        val userText = botUserInputMessage.trim()
        if (userText.isEmpty()) return
        
        botUserInputMessage = ""
        
        // Append user's action
        val userMsgObj = ChatMessage(sender = "User", text = userText)
        botChatHistory.value = botChatHistory.value + userMsgObj
        
        viewModelScope.launch {
            delay(800) // Human-like typing delay
            
            val cleanLower = userText.lowercase()
            when {
                cleanLower == "/start" -> {
                    // Register user mock
                    val newTgUser = TelegramUser(
                        id = 82193022, 
                        username = "tester_telegram", 
                        joinDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()), 
                        downloadedCount = 0
                    )
                    repository.addTelegramUser(newTgUser)
                    appendLog("🤖 Bot command start: Registered user 'tester_telegram'")
                    
                    val welcomeText = """
                        ⚡ <b>UNIVERSAL HIGH-SPEED DOWNLOAD BOT</b> ⚡
                        
                        Hello user. Please select one of the core interactive functions below, or simply send me a video link.
                    """.trimIndent()
                    
                    botChatHistory.value = botChatHistory.value + ChatMessage(
                        sender = "Bot",
                        text = welcomeText,
                        inlineButtons = listOf(
                            "🌐 Open Web App" to "app_open",
                            "📊 My Stats" to "stats_open"
                        )
                    )
                }
                
                cleanLower == "/admin" -> {
                    val matchingAdminId = loggedInAdminId ?: 123456789
                    val adminPanelText = """
                        ⚙️ <b>BOT ADMIN CONTROL CENTER v3.5</b> ⚙️
                        
                        Unlocked admin modules for authorized Telegram ID. Activate settings right inside the chat.
                    """.trimIndent()
                    
                    botChatHistory.value = botChatHistory.value + ChatMessage(
                        sender = "Bot",
                        text = adminPanelText,
                        inlineButtons = listOf(
                            "📊 Real-time Stats" to "admin_action_stats",
                            "📢 Broadcast Message" to "admin_action_broadcast"
                        )
                    )
                }
                
                cleanLower.startsWith("/ban ") -> {
                    val split = userText.split(" ")
                    val targetId = split.getOrNull(1)?.toLongOrNull()
                    if (targetId != null) {
                        repository.updateBanStatus(targetId, true)
                        botChatHistory.value = botChatHistory.value + ChatMessage(sender = "Bot", text = "🔒 Ban state synced: locked user <code>$targetId</code> successfully inside DB database.")
                        appendLog("📝 Telegram Command [/ban] logged.")
                    } else {
                        botChatHistory.value = botChatHistory.value + ChatMessage(sender = "Bot", text = "Usage format: <code>/ban user_id</code>")
                    }
                }

                // Match a generic URL
                cleanLower.contains("http://") || cleanLower.contains("https://") -> {
                    botChatHistory.value = botChatHistory.value + ChatMessage(sender = "Bot", text = "⚙️ <b>PROFILING HOST... Analyzing stream pipelines</b> ⚡")
                    isBotProcessingLink = true
                    delay(1200)
                    isBotProcessingLink = false
                    
                    if (isMaintenanceMode) {
                        botChatHistory.value = botChatHistory.value + ChatMessage(sender = "Bot", text = "⛔ Downloader global server toggled OFFLINE by maintenance. Try later.")
                        return@launch
                    }

                    val isYoutube = cleanLower.contains("youtube.com") || cleanLower.contains("youtu.be")
                    if (isDisableYoutube && isYoutube) {
                        botChatHistory.value = botChatHistory.value + ChatMessage(sender = "Bot", text = "⛔ Download Block: Youtube module is restricted globally of admin decisions.")
                        return@launch
                    }

                    val resolvedPlatform = when {
                        cleanLower.contains("youtube.com") || cleanLower.contains("youtu.be") -> "YouTube"
                        cleanLower.contains("instagram.com") -> "Instagram"
                        cleanLower.contains("tiktok.com") -> "TikTok"
                        else -> "Generic Stream"
                    }
                    val title = "Simulated Social Extract ($resolvedPlatform)"
                    
                    val buttons = listOf(
                        "1080p (MP4)" to "dl_sim|1080p|$resolvedPlatform",
                        "720p (MP4)" to "dl_sim|720p|$resolvedPlatform",
                        "480p (MP4)" to "dl_sim|480p|$resolvedPlatform",
                        "High Bitrate MP3" to "dl_sim|MP3|$resolvedPlatform"
                    )

                    botChatHistory.value = botChatHistory.value + ChatMessage(
                        sender = "Bot",
                        text = "🎬 <b>TITLE:</b> $title\n<b>🌐 REPO:</b> $resolvedPlatform\n\nSelect video grade resolution format to send video inside Chat:",
                        inlineButtons = buttons
                    )
                }
                
                else -> {
                    // Simple bot parrot or fallback instructions
                    botChatHistory.value = botChatHistory.value + ChatMessage(
                        sender = "Bot",
                        text = "❓ Unknown Command. Send me a social media video URL (e.g. YouTube, TikTok, Instagram) or enter <code>/start</code> to initialize bot panels."
                    )
                }
            }
        }
    }

    fun handleBotCallback(callbackData: String) {
        val msgUuid = UUID.randomUUID().toString()
        viewModelScope.launch {
            when {
                callbackData == "app_open" -> {
                    // Switches screen simulation back directly
                    activeTab = "client"
                    appendLog("🤖 Telegram callback app_open click: Routing emulator web viewport.")
                }
                callbackData == "stats_open" -> {
                    // Display stats mock
                    val logs = downloadLogs.value
                    botChatHistory.value = botChatHistory.value + ChatMessage(
                        sender = "Bot",
                        text = "📊 <b>MY DOWNLOAD STATISTICS</b>\n\n- Downloads count: <b>${logs.size}</b> videos\n- Account Rank: Elite Downloader Miner\n- Cache Cluster Node: Active"
                    )
                }
                callbackData == "admin_action_stats" -> {
                    val totalU = telegramUsers.value.size
                    val totalD = downloadLogs.value.size
                    val inlineStatsText = """
                        📂 <b>LIVE SYSTEM METRICS (FASTAPI DB)</b>
                        
                        - Total Users: <b>$totalU</b>
                        - Download Records: <b>$totalD</b>
                        - Rotating Proxies: <b>${proxies.value.count { it.isOnline }} online</b>
                        - Engine Heartbeat: healthy 🔋
                    """.trimIndent()
                    botChatHistory.value = botChatHistory.value + ChatMessage(sender = "Bot", text = inlineStatsText)
                }
                callbackData == "admin_action_broadcast" -> {
                    botChatHistory.value = botChatHistory.value + ChatMessage(
                        sender = "Bot",
                        text = "✉️ Please prompt broadcast message directly inside input (e.g., <code>This is bot announcement!</code>) or go to Admin tab inside App shell to use custom queue rate-limiter."
                    )
                }
                callbackData.startsWith("dl_sim|") -> {
                    val parts = callbackData.split("|")
                    val res = parts.getOrNull(1) ?: "1080p"
                    val plat = parts.getOrNull(2) ?: "YouTube"
                    
                    botChatHistory.value = botChatHistory.value + ChatMessage(sender = "Bot", text = "⚡ <b>Extracting stream chunk pipelines... Downloading file</b>")
                    delay(1200)
                    botChatHistory.value = botChatHistory.value + ChatMessage(sender = "Bot", text = "📦 <b>Uploading stream chunk (Telegram high speed node)...</b>")
                    delay(1000)
                    
                    // Add download log record
                    val simTitle = "Simulated Chat Extract ($plat)"
                    repository.addDownloadLog(DownloadLog(url = "Telegram Bot Client", title = simTitle, platform = plat, resolution = res, format = "MP4", sizeMb = 18.2, status = "Completed"))
                    
                    botChatHistory.value = botChatHistory.value + ChatMessage(
                        sender = "Bot",
                        text = "✅ <b>STREAM TRANSMITTED SUCCESSFULLY!</b>\n\nFile <code>$simTitle - $res</code> has been natively loaded behind chats buffers."
                    )
                    appendLog("🤖 Bot Download callback logged to local Room indices.")
                }
            }
        }
    }

    // --- Admin Dashboard Broadcast Execution ---
    fun runBroadcastQueue() {
        val text = adminLogBroadcastingText.trim()
        if (text.isEmpty()) return
        
        isBroadcastingRunning = true
        broadcastProgress = 0f
        
        appendLog("📢 Admin Queue: Running Broadcast message across registered DB lines ($text)")

        viewModelScope.launch {
            val list = telegramUsers.value
            val totalCount = list.size
            if (totalCount == 0) {
                delay(1000)
                isBroadcastingRunning = false
                adminLogBroadcastingText = ""
                appendLog("📢 Admin Queue incomplete: zero registered targets found in Mongo/Postgres lists.")
                return@launch
            }

            for (i in list.indices) {
                delay(400) // Delay representation for Anti Flood Rate limit compliance (Async telegram queues)
                val peer = list[i]
                broadcastProgress = (i + 1).toFloat() / totalCount
                appendLog("📢 Successfully transmitted packet: User @${peer.username} (ID: ${peer.id})")
            }
            
            isBroadcastingRunning = false
            adminLogBroadcastingText = ""
            broadcastProgress = 1f
            appendLog("🟢 Broadcast daemon finished. Registered packets successfully reached all nodes.")
        }
    }

    // --- AI Gemini Playground Actions ---
    fun runGeminiModificationCode() {
        val userPrompt = playgroundPromptInput.trim()
        if (userPrompt.isEmpty()) return
        
        isAiWorking = true
        aiEditedCodeResult = ""
        appendLog("🤖 Connected to Gemini API node... Dispatching editing code requests.")

        viewModelScope.launch {
            val baseCode = when (selectedPlaygroundPlatform) {
                "FastAPI" -> TemplateCode.FASTAPI_CODE
                "Bot" -> TemplateCode.TELEGRAM_BOT_CODE
                else -> TemplateCode.FRONTEND_CODE
            }
            
            val systemInstructions = "You are a professional system architect. Return only code modified as python or javascript syntax with no extra descriptions format."
            
            val result = GeminiManager.generateModifiedCode(userPrompt, systemInstructions, baseCode)
            aiEditedCodeResult = result
            isAiWorking = false
            
            appendLog("🟢 Gemini Code generation task returned successfully.")
        }
    }
}
