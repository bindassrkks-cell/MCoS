package com.mcos

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

const val GEMINI_API_KEY = "AIzaSyA1IulxGnWIz0RGynl4-h3pL-pjlnd04jY"

// Models
data class Article(
    val id: String = "",
    val title: String = "",
    val summary: String = "",
    val htmlContent: String = "",
    val imageUrl: String = "",
    val videoUrl: String = "", // YouTube/MP4/HLS direct video URL support
    val category: String = "",
    val timeAgo: String = "",
    val readTime: String = "",
    val author: String = ""
)

data class OfferTask(
    val id: String,
    val title: String,
    val description: String,
    val rewardCoins: Int,
    val type: String, // "AD", "DAILY", "TASK"
    val durationSeconds: Int = 10
)

data class VaultFile(
    val file: File,
    val isVideo: Boolean,
    val name: String,
    val sizeFormatted: String
)

class MainActivity : ComponentActivity() {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val database: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            McosMainApp(auth, database)
        }
    }
}

@Composable
fun McosMainApp(auth: FirebaseAuth, database: FirebaseDatabase) {
    var isDarkMode by remember { mutableStateOf(true) }
    var currentScreen by remember { mutableStateOf("splash") }
    var userEmail by remember { mutableStateOf(auth.currentUser?.email ?: "") }
    var userId by remember { mutableStateOf(auth.currentUser?.uid ?: "") }
    var selectedArticle by remember { mutableStateOf<Article?>(null) }
    var activeVideoUrl by remember { mutableStateOf<String?>(null) }
    var isAiDialogVisible by remember { mutableStateOf(false) }
    var isVaultAuthVisible by remember { mutableStateOf(false) }
    var userCoins by remember { mutableIntStateOf(0) }

    val bg = if (isDarkMode) Color(0xFF080B11) else Color(0xFFF8FAFC)
    val cardBg = if (isDarkMode) Color(0xFF111827) else Color(0xFFFFFFFF)
    val textPrimary = if (isDarkMode) Color(0xFFF9FAFB) else Color(0xFF0F172A)
    val textMuted = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    val accent = Color(0xFF00E5FF)
    val border = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0)

    // Realtime User Coins Listener
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            val coinRef = database.getReference("users").child(userId).child("coins")
            coinRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    userCoins = snapshot.getValue(Int::class.java) ?: 0
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = bg) {
        when (currentScreen) {
            "splash" -> SplashScreen(accent) {
                currentScreen = if (auth.currentUser != null) "home" else "auth"
            }
            "auth" -> AuthScreen(
                auth = auth, bg = bg, cardBg = cardBg, textPrimary = textPrimary,
                textMuted = textMuted, accent = accent, border = border,
                isDarkMode = isDarkMode, onToggleTheme = { isDarkMode = !isDarkMode },
                onOpenVaultTrigger = { isVaultAuthVisible = true },
                onLoginSuccess = { email, uid ->
                    userEmail = email
                    userId = uid
                    currentScreen = "home"
                }
            )
            "home" -> HomeScreen(
                database = database, userEmail = userEmail, userCoins = userCoins,
                bg = bg, cardBg = cardBg, textPrimary = textPrimary,
                textMuted = textMuted, accent = accent, border = border,
                isDarkMode = isDarkMode, onToggleTheme = { isDarkMode = !isDarkMode },
                onOpenVault = { isVaultAuthVisible = true },
                onOpenAi = { isAiDialogVisible = true },
                onOpenRewards = { currentScreen = "rewards" },
                onSelectArticle = { article ->
                    selectedArticle = article
                    currentScreen = "details"
                },
                onPlayDirectVideo = { url ->
                    activeVideoUrl = url
                    currentScreen = "player"
                },
                onLogout = {
                    auth.signOut()
                    currentScreen = "auth"
                }
            )
            "details" -> ArticleDetailScreen(
                article = selectedArticle, bg = bg, cardBg = cardBg, textPrimary = textPrimary,
                textMuted = textMuted, accent = accent, border = border,
                onPlayVideo = { url ->
                    activeVideoUrl = url
                    currentScreen = "player"
                },
                onOpenAiSummary = { isAiDialogVisible = true },
                onBack = { currentScreen = "home" }
            )
            "player" -> VideoPlayerScreen(
                videoUriString = activeVideoUrl ?: "",
                bg = bg, accent = accent,
                onBack = { currentScreen = "home" }
            )
            "rewards" -> RewardsOfferScreen(
                userId = userId, userCoins = userCoins, database = database,
                bg = bg, cardBg = cardBg, textPrimary = textPrimary,
                textMuted = textMuted, accent = accent, border = border,
                onBack = { currentScreen = "home" }
            )
            "vault" -> PrivateVaultScreen(
                bg = bg, cardBg = cardBg, textPrimary = textPrimary,
                textMuted = textMuted, accent = accent, border = border,
                onPlayVaultVideo = { file ->
                    activeVideoUrl = file.absolutePath
                    currentScreen = "player"
                },
                onBack = { currentScreen = "home" }
            )
        }

        if (isVaultAuthVisible) {
            VaultPinDialog(
                cardBg = cardBg, textPrimary = textPrimary, textMuted = textMuted,
                accent = accent, border = border,
                onUnlocked = {
                    isVaultAuthVisible = false
                    currentScreen = "vault"
                },
                onDismiss = { isVaultAuthVisible = false }
            )
        }

        if (isAiDialogVisible) {
            RealGeminiAiDialog(
                cardBg = cardBg, textPrimary = textPrimary, textMuted = textMuted,
                accent = accent, border = border,
                articleContext = selectedArticle?.summary ?: selectedArticle?.title ?: "MCoS Platform",
                onDismiss = { isAiDialogVisible = false }
            )
        }
    }
}

// 1. SPLASH SCREEN
@Composable
fun SplashScreen(accent: Color, onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1800)
        onTimeout()
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(Color(0xFF111827), RoundedCornerShape(24.dp))
                    .border(2.dp, Brush.linearGradient(listOf(accent, Color(0xFF6366F1))), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "MC", color = accent, fontSize = 38.sp, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "MCoS", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp)
            Text(text = "Realtime Video & Reward Engine", color = Color(0xFF94A3B8), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(28.dp))
            CircularProgressIndicator(color = accent, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
        }
    }
}

// 2. AUTH SCREEN
@Composable
fun AuthScreen(
    auth: FirebaseAuth, bg: Color, cardBg: Color, textPrimary: Color,
    textMuted: Color, accent: Color, border: Color, isDarkMode: Boolean,
    onToggleTheme: () -> Unit, onOpenVaultTrigger: () -> Unit,
    onLoginSuccess: (String, String) -> Unit
) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBg)
                    .border(1.5.dp, accent, RoundedCornerShape(14.dp))
                    .clickable { onOpenVaultTrigger() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "MC", color = accent, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            IconButton(onClick = onToggleTheme) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Theme", tint = accent
                )
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text(text = if (isSignUp) "Create Account" else "Welcome Back", color = textPrimary, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        Text(text = "Join MCoS to watch streams, complete offers & earn rewards", color = textMuted, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(28.dp))

        Text(text = "Email Address", color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            placeholder = { Text("user@mcos.io", color = textMuted) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent, unfocusedBorderColor = border,
                focusedTextColor = textPrimary, unfocusedTextColor = textPrimary,
                focusedContainerColor = cardBg, unfocusedContainerColor = cardBg
            ),
            shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Password", color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            placeholder = { Text("••••••••••••", color = textMuted) },
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent, unfocusedBorderColor = border,
                focusedTextColor = textPrimary, unfocusedTextColor = textPrimary,
                focusedContainerColor = cardBg, unfocusedContainerColor = cardBg
            ),
            shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(26.dp))

        Button(
            onClick = {
                if (email.isNotBlank() && password.length >= 6) {
                    loading = true
                    if (isSignUp) {
                        auth.createUserWithEmailAndPassword(email.trim(), password)
                            .addOnSuccessListener { res ->
                                loading = false
                                Toast.makeText(context, "Account Created!", Toast.LENGTH_SHORT).show()
                                onLoginSuccess(res.user?.email ?: email, res.user?.uid ?: "")
                            }
                            .addOnFailureListener { err ->
                                loading = false
                                Toast.makeText(context, err.message ?: "Sign up failed", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        auth.signInWithEmailAndPassword(email.trim(), password)
                            .addOnSuccessListener { res ->
                                loading = false
                                onLoginSuccess(res.user?.email ?: email, res.user?.uid ?: "")
                            }
                            .addOnFailureListener { err ->
                                loading = false
                                Toast.makeText(context, err.message ?: "Auth failed", Toast.LENGTH_LONG).show()
                            }
                    }
                } else {
                    Toast.makeText(context, "Enter valid credentials (min 6 chars)", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = accent),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(color = Color(0xFF080B11), modifier = Modifier.size(22.dp))
            } else {
                Text(text = if (isSignUp) "Sign Up" else "Sign In", color = Color(0xFF080B11), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        Spacer(modifier = Modifier.height(18.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(text = if (isSignUp) "Already have an account? " else "Don't have an account? ", color = textMuted, fontSize = 14.sp)
            Text(
                text = if (isSignUp) "Sign In" else "Sign Up", color = accent,
                fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.clickable { isSignUp = !isSignUp }
            )
        }
    }
}

// 3. HOME SCREEN (With Direct Video Badges & Rewards Access)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    database: FirebaseDatabase, userEmail: String, userCoins: Int,
    bg: Color, cardBg: Color, textPrimary: Color, textMuted: Color, accent: Color, border: Color,
    isDarkMode: Boolean, onToggleTheme: () -> Unit, onOpenVault: () -> Unit,
    onOpenAi: () -> Unit, onOpenRewards: () -> Unit,
    onSelectArticle: (Article) -> Unit, onPlayDirectVideo: (String) -> Unit, onLogout: () -> Unit
) {
    var articlesList by remember { mutableStateOf<List<Article>>(emptyList()) }

    DisposableEffect(Unit) {
        val articlesRef = database.getReference("articles")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Article>()
                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val item = child.getValue(Article::class.java)
                        if (item != null) list.add(0, item)
                    }
                }
                if (list.isEmpty()) {
                    list.add(
                        Article(
                            id = "vid_demo_1",
                            title = "Next-Gen Android Compose & ExoPlayer Engine",
                            summary = "Play high performance streaming videos and complete offers to unlock premium rewards.",
                            htmlContent = "<h2>Seamless Video Streaming</h2><p>Integrated YouTube style custom ExoPlayer media renderer with hardware acceleration.</p>",
                            imageUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&q=80",
                            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                            category = "STREAMING",
                            timeAgo = "Live",
                            readTime = "4 min watch",
                            author = "MCoS Core"
                        )
                    )
                }
                articlesList = list
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        articlesRef.addValueEventListener(listener)
        onDispose { articlesRef.removeEventListener(listener) }
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(cardBg)
                                .border(1.5.dp, accent, RoundedCornerShape(12.dp))
                                .clickable { onOpenVault() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("MC", color = accent, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("MCoS Stream", color = textPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Text("Realtime Engine", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                actions = {
                    // Reward Coins Chip in Top Bar
                    Surface(
                        onClick = onOpenRewards,
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = "Coins", tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "$userCoins", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    IconButton(onClick = onOpenAi) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI", tint = accent)
                    }
                    IconButton(onClick = onToggleTheme) {
                        Icon(imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "Theme", tint = textMuted)
                    }
                    IconButton(onClick = onLogout) {
                        Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color(0xFFEF4444))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenRewards,
                containerColor = Color(0xFFF59E0B),
                contentColor = Color(0xFF080B11),
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = "Rewards") },
                text = { Text("Ads & Offers", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Banner for Offerwall
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenRewards() },
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(Color(0xFFF59E0B), accent)))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "🎁 Complete Offers & Earn Coins", color = textPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            Text(text = "Watch short ads & complete tasks to redeem rewards", color = textMuted, fontSize = 12.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF59E0B))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Win Rewards", color = Color(0xFF080B11), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            item {
                Text(text = "Latest Feeds & Media", color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            items(articlesList) { article ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onSelectArticle(article) },
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, border)
                ) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                            AsyncImage(
                                model = article.imageUrl.ifBlank { "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&q=80" },
                                contentDescription = article.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            if (article.videoUrl.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0x55000000)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IconButton(
                                        onClick = { onPlayDirectVideo(article.videoUrl) },
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(accent)
                                    ) {
                                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", tint = Color(0xFF080B11), modifier = Modifier.size(32.dp))
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = article.category.ifBlank { "MEDIA" }.uppercase(),
                                    color = accent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(accent.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                                Text(text = article.timeAgo.ifBlank { "Just now" }, color = textMuted, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = article.title, color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = article.summary, color = textMuted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

// 4. PROFESSIONAL YOUTUBE-STYLE VIDEO PLAYER SCREEN (ExoPlayer)
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    videoUriString: String,
    bg: Color,
    accent: Color,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.parse(videoUriString))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Back Button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(top = 40.dp, start = 16.dp)
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0x88000000))
        ) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
    }
}

// 5. REWARDS & ADS OFFERWALL SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsOfferScreen(
    userId: String,
    userCoins: Int,
    database: FirebaseDatabase,
    bg: Color,
    cardBg: Color,
    textPrimary: Color,
    textMuted: Color,
    accent: Color,
    border: Color,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isWatchingAd by remember { mutableStateOf(false) }
    var adRemainingTime by remember { mutableIntStateOf(10) }
    var currentOffer by remember { mutableStateOf<OfferTask?>(null) }
    val scope = rememberCoroutineScope()

    val offers = remember {
        listOf(
            OfferTask("ad_1", "Watch Sponsored Video Ad", "Watch 10 seconds commercial to win instant coins", 50, "AD", 10),
            OfferTask("ad_2", "Special Partner Offer", "Watch 15 seconds showcase video", 75, "AD", 15),
            OfferTask("daily_1", "Daily Login Streak", "Claim daily login bonus rewards", 100, "DAILY", 0),
            OfferTask("task_1", "Gemini AI Query Task", "Use Gemini AI to summarize 2 topics", 150, "TASK", 0),
            OfferTask("task_2", "Secret Vault Security Check", "Store a photo in the private locker", 200, "TASK", 0)
        )
    }

    val completeReward = { task: OfferTask ->
        if (userId.isNotEmpty()) {
            val userRef = database.getReference("users").child(userId).child("coins")
            userRef.setValue(userCoins + task.rewardCoins).addOnSuccessListener {
                Toast.makeText(context, "+${task.rewardCoins} Coins Added to Wallet! 🎉", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Simulated Ad Watch Timer
    LaunchedEffect(isWatchingAd) {
        if (isWatchingAd && currentOffer != null) {
            adRemainingTime = currentOffer!!.durationSeconds
            while (adRemainingTime > 0) {
                delay(1000)
                adRemainingTime--
            }
            isWatchingAd = false
            completeReward(currentOffer!!)
        }
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg),
                title = { Text("Rewards & Offerwall", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = accent)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Wallet Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(Color(0xFFF59E0B), accent)))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Total Reward Balance", color = textMuted, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = "Coins", tint = Color(0xFFF59E0B), modifier = Modifier.size(34.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "$userCoins Coins", color = textPrimary, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "1000 Coins = $1.00 USD / ₹85 Instant Voucher", color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Available Offers & Tasks", color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(14.dp))

            offers.forEach { offer ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, border)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (offer.type == "AD") Color(0xFFF59E0B).copy(alpha = 0.15f) else accent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (offer.type == "AD") Icons.Default.PlayCircleFilled else Icons.Default.TaskAlt,
                                    contentDescription = null,
                                    tint = if (offer.type == "AD") Color(0xFFF59E0B) else accent
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(text = offer.title, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(text = offer.description, color = textMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "+${offer.rewardCoins} Coins", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = {
                                if (offer.type == "AD") {
                                    currentOffer = offer
                                    isWatchingAd = true
                                } else {
                                    completeReward(offer)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (offer.type == "AD") Color(0xFFF59E0B) else accent),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (offer.type == "AD") "Watch Ad" else "Claim",
                                color = Color(0xFF080B11),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Ad Playing Overlay Dialog
        if (isWatchingAd) {
            AlertDialog(
                onDismissRequest = {},
                containerColor = cardBg,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.SlowMotionVideo, contentDescription = "Ad", tint = Color(0xFFF59E0B))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Playing Sponsored Video Ad", color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFFF59E0B), strokeWidth = 4.dp, modifier = Modifier.size(52.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Reward unlocks in: $adRemainingTime seconds", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Please don't close the app to win +${currentOffer?.rewardCoins} coins", color = textMuted, fontSize = 12.sp)
                    }
                },
                confirmButton = {}
            )
        }
    }
}

// 6. ARTICLE DETAIL SCREEN (With Video & AI Summary)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    article: Article?, bg: Color, cardBg: Color, textPrimary: Color,
    textMuted: Color, accent: Color, border: Color,
    onPlayVideo: (String) -> Unit, onOpenAiSummary: () -> Unit, onBack: () -> Unit
) {
    if (article == null) return

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg),
                title = { Text("Article Detail", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = accent)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenAiSummary) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI Summary", tint = accent)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                AsyncImage(
                    model = article.imageUrl.ifBlank { "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&q=80" },
                    contentDescription = article.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (article.videoUrl.isNotBlank()) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color(0x66000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { onPlayVideo(article.videoUrl) },
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", tint = Color(0xFF080B11))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play Stream Video", color = Color(0xFF080B11), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = article.category.ifBlank { "TECH" }.uppercase(),
                    color = accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accent.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = article.title, color = textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "By ${article.author}", color = textMuted, fontSize = 13.sp)
                    Text(text = "${article.timeAgo} • ${article.readTime}", color = textMuted, fontSize = 13.sp)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = border)

                AndroidView(
                    factory = { ctx ->
                        TextView(ctx).apply {
                            setTextColor(textPrimary.toArgb())
                            textSize = 15f
                            setLineSpacing(8f, 1.2f)
                        }
                    },
                    update = { textView ->
                        textView.setTextColor(textPrimary.toArgb())
                        textView.text = HtmlCompat.fromHtml(article.htmlContent, HtmlCompat.FROM_HTML_MODE_COMPACT)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = onOpenAiSummary,
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI", tint = Color(0xFF080B11))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Summarize with Gemini AI", color = Color(0xFF080B11), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

// 7. SECRET PRIVATE VAULT (With Direct Player Hook)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateVaultScreen(
    bg: Color, cardBg: Color, textPrimary: Color,
    textMuted: Color, accent: Color, border: Color,
    onPlayVaultVideo: (File) -> Unit, onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var vaultFiles by remember { mutableStateOf<List<VaultFile>>(emptyList()) }
    var isImporting by remember { mutableStateOf(false) }

    val vaultDir = remember {
        val dir = File(context.filesDir, ".mcos_vault_hidden")
        if (!dir.exists()) dir.mkdirs()
        val noMedia = File(dir, ".nomedia")
        if (!noMedia.exists()) noMedia.createNewFile()
        dir
    }

    val refreshVault = {
        val files = vaultDir.listFiles { file -> file.name != ".nomedia" }?.map { f ->
            val isVid = f.name.endsWith(".mp4", true) || f.name.endsWith(".mkv", true) || f.name.endsWith(".mov", true)
            val sizeMb = String.format("%.1f MB", f.length() / (1024.0 * 1024.0))
            VaultFile(f, isVid, f.name, sizeMb)
        } ?: emptyList()
        vaultFiles = files
    }

    LaunchedEffect(Unit) {
        refreshVault()
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            isImporting = true
            scope.launch(Dispatchers.IO) {
                uris.forEach { uri ->
                    try {
                        val mime = context.contentResolver.getType(uri) ?: ""
                        val ext = if (mime.contains("video")) "mp4" else "jpg"
                        val targetFile = File(vaultDir, "vault_${System.currentTimeMillis()}_${(100..999).random()}.$ext")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(targetFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                withContext(Dispatchers.Main) {
                    isImporting = false
                    refreshVault()
                    Toast.makeText(context, "Encrypted & Stored in Secret Vault!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Vault", tint = accent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Private Vault", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = accent)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { mediaPickerLauncher.launch("*/*") },
                containerColor = accent,
                contentColor = Color(0xFF080B11),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = "Hide Media")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Hide Photo/Video", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, border)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = "Secure", tint = accent)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Hidden Directory (.nomedia)", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${vaultFiles.size} Hidden Files (Invisible to Gallery)", color = textMuted, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (vaultFiles.isEmpty() && !isImporting) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Outlined.FolderSpecial, contentDescription = "Empty", tint = textMuted, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Vault is Empty", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Tap 'Hide Photo/Video' to store securely", color = textMuted, fontSize = 13.sp)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(vaultFiles) { vFile ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clickable {
                                    if (vFile.isVideo) onPlayVaultVideo(vFile.file)
                                },
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, border)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(vFile.file)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = vFile.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC080B11))))
                                )

                                if (vFile.isVideo) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = "Play Video",
                                        tint = accent,
                                        modifier = Modifier.align(Alignment.Center).size(36.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = vFile.sizeFormatted, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    IconButton(
                                        onClick = {
                                            vFile.file.delete()
                                            refreshVault()
                                            Toast.makeText(context, "Removed from Vault", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 8. VAULT PIN DIALOG
@Composable
fun VaultPinDialog(
    cardBg: Color, textPrimary: Color, textMuted: Color,
    accent: Color, border: Color, onUnlocked: () -> Unit, onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mcos_vault_prefs", Context.MODE_PRIVATE) }
    val savedPin = remember { prefs.getString("vault_pin", null) }

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val isSettingUp = savedPin == null

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardBg,
        shape = RoundedCornerShape(22.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.EnhancedEncryption, contentDescription = "Lock", tint = accent)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isSettingUp) "Setup Vault PIN" else "Private Vault Security",
                    color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isSettingUp) "Create a 4-digit PIN to secure hidden files." else "Enter your 4-digit PIN to unlock media.",
                    color = textMuted, fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = { if (it.length <= 4) enteredPin = it },
                    placeholder = { Text("••••", color = textMuted, textAlign = TextAlign.Center) },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent, unfocusedBorderColor = border,
                        focusedTextColor = textPrimary, unfocusedTextColor = textPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMessage, color = Color(0xFFEF4444), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (enteredPin.length == 4) {
                        if (isSettingUp) {
                            prefs.edit().putString("vault_pin", enteredPin).apply()
                            Toast.makeText(context, "Vault PIN Configured!", Toast.LENGTH_SHORT).show()
                            onUnlocked()
                        } else {
                            if (enteredPin == savedPin) {
                                onUnlocked()
                            } else {
                                errorMessage = "Incorrect PIN"
                            }
                        }
                    } else {
                        errorMessage = "Enter 4 digits"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text(if (isSettingUp) "Save & Open" else "Unlock", color = Color(0xFF080B11), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textMuted)
            }
        }
    )
}

// 9. GEMINI AI REALTIME DIALOG
@Composable
fun RealGeminiAiDialog(
    cardBg: Color, textPrimary: Color, textMuted: Color,
    accent: Color, border: Color, articleContext: String, onDismiss: () -> Unit
) {
    var prompt by remember { mutableStateOf("Explain in simple terms: $articleContext") }
    var responseText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardBg,
        shape = RoundedCornerShape(22.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI", tint = accent)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Google Gemini AI Free", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = { Text("Ask Gemini anything...", color = textMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent, unfocusedBorderColor = border,
                        focusedTextColor = textPrimary, unfocusedTextColor = textPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(90.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (isGenerating) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = accent, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Gemini is generating response...", color = textMuted, fontSize = 12.sp)
                    }
                } else if (responseText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .background(Color(0xFF080B11), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(text = responseText, color = textPrimary, fontSize = 13.sp, lineHeight = 20.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (prompt.isNotBlank()) {
                        isGenerating = true
                        scope.launch(Dispatchers.IO) {
                            try {
                                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$GEMINI_API_KEY"
                                val jsonBody = JSONObject().apply {
                                    val contentsArr = JSONArray().apply {
                                        val contentObj = JSONObject().apply {
                                            val partsArr = JSONArray().apply {
                                                put(JSONObject().put("text", prompt))
                                            }
                                            put("parts", partsArr)
                                        }
                                        put(contentObj)
                                    }
                                    put("contents", contentsArr)
                                }

                                val request = Request.Builder()
                                    .url(url)
                                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                                    .build()

                                val response = client.newCall(request).execute()
                                val bodyStr = response.body?.string() ?: ""

                                if (response.isSuccessful) {
                                    val respJson = JSONObject(bodyStr)
                                    val text = respJson.getJSONArray("candidates")
                                        .getJSONObject(0)
                                        .getJSONObject("content")
                                        .getJSONArray("parts")
                                        .getJSONObject(0)
                                        .getString("text")
                                    withContext(Dispatchers.Main) {
                                        responseText = text.trim()
                                        isGenerating = false
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        responseText = "Error from Gemini API (${response.code})."
                                        isGenerating = false
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    responseText = "Connection failed: ${e.localizedMessage}"
                                    isGenerating = false
                                }
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text("Generate", color = Color(0xFF080B11), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = textMuted)
            }
        }
    )
}
