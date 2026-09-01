package com.mcos

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
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
import java.util.concurrent.TimeUnit

const val GEMINI_API_KEY = "AIzaSyA1IulxGnWIz0RGynl4-h3pL-pjlnd04jY"

class MainActivity : ComponentActivity() {

    companion object {
        init {
            System.loadLibrary("mcore")
        }
    }

    external fun getNativeCoreVersion(): String

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val database: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainAppRoot(auth, database, getNativeCoreVersion())
        }
    }
}

@Composable
fun MainAppRoot(auth: FirebaseAuth, database: FirebaseDatabase, nativeVersion: String) {
    val context = LocalContext.current
    var isDarkMode by remember { mutableStateOf(true) }
    var currentScreen by remember { mutableStateOf("splash") }
    var userCoins by remember { mutableIntStateOf(0) }
    var isAiDialogVisible by remember { mutableStateOf(false) }
    var isVaultPinDialogVisible by remember { mutableStateOf(false) }

    val bg = if (isDarkMode) Color(0xFF080B11) else Color(0xFFF8FAFC)
    val cardBg = if (isDarkMode) Color(0xFF111827) else Color(0xFFFFFFFF)
    val textPrimary = if (isDarkMode) Color(0xFFF9FAFB) else Color(0xFF0F172A)
    val textMuted = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    val accent = Color(0xFF00E5FF)
    val border = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0)

    val currentUid = auth.currentUser?.uid ?: ""
    val currentEmail = auth.currentUser?.email ?: ""

    // Realtime User Coins Sync
    LaunchedEffect(currentUid) {
        if (currentUid.isNotEmpty()) {
            val ref = database.getReference("users").child(currentUid).child("coins")
            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    userCoins = snapshot.getValue(Int::class.java) ?: 0
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = bg) {
        when (currentScreen) {
            "splash" -> SplashScreen(accent, nativeVersion) {
                currentScreen = if (auth.currentUser != null) "home" else "auth"
            }
            "auth" -> AuthScreen(
                auth = auth, bg = bg, cardBg = cardBg, textPrimary = textPrimary,
                textMuted = textMuted, accent = accent, border = border,
                isDarkMode = isDarkMode, onToggleTheme = { isDarkMode = !isDarkMode },
                onLoginSuccess = { currentScreen = "home" }
            )
            "home" -> HomeScreen(
                auth = auth, database = database, userCoins = userCoins,
                bg = bg, cardBg = cardBg, textPrimary = textPrimary,
                textMuted = textMuted, accent = accent, border = border,
                isDarkMode = isDarkMode, onToggleTheme = { isDarkMode = !isDarkMode },
                onTriggerVault = { isVaultPinDialogVisible = true },
                onOpenMcosFeed = { context.startActivity(Intent(context, McosActivity::class.java)) },
                onOpenAi = { isAiDialogVisible = true },
                onOpenWallet = { currentScreen = "wallet" },
                onOpenRewards = { currentScreen = "rewards" },
                onLogout = {
                    auth.signOut()
                    currentScreen = "auth"
                }
            )
            "wallet" -> WalletScreen(
                userId = currentUid, userEmail = currentEmail, userCoins = userCoins,
                database = database, bg = bg, cardBg = cardBg, textPrimary = textPrimary,
                textMuted = textMuted, accent = accent, border = border,
                onBack = { currentScreen = "home" }
            )
            "rewards" -> RewardsOfferScreen(
                userId = currentUid, userCoins = userCoins, database = database,
                bg = bg, cardBg = cardBg, textPrimary = textPrimary,
                textMuted = textMuted, accent = accent, border = border,
                onBack = { currentScreen = "home" }
            )
        }

        if (isVaultPinDialogVisible) {
            VaultPinDialog(
                cardBg = cardBg, textPrimary = textPrimary, textMuted = textMuted,
                accent = accent, border = border,
                onUnlocked = {
                    isVaultPinDialogVisible = false
                    context.startActivity(Intent(context, VaultActivity::class.java))
                },
                onDismiss = { isVaultPinDialogVisible = false }
            )
        }

        if (isAiDialogVisible) {
            RealGeminiAiDialog(
                cardBg = cardBg, textPrimary = textPrimary, textMuted = textMuted,
                accent = accent, border = border,
                onDismiss = { isAiDialogVisible = false }
            )
        }
    }
}

// 1. SPLASH SCREEN
@Composable
fun SplashScreen(accent: Color, nativeVersion: String, onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1600)
        onTimeout()
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .background(Color(0xFF111827), RoundedCornerShape(22.dp))
                    .border(2.dp, Brush.linearGradient(listOf(accent, Color(0xFF6366F1))), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "MC", color = accent, fontSize = 36.sp, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(text = "MCoS", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp)
            Text(text = nativeVersion, color = Color(0xFF94A3B8), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(26.dp))
            CircularProgressIndicator(color = accent, strokeWidth = 3.dp, modifier = Modifier.size(26.dp))
        }
    }
}

// 2. AUTH SCREEN
@Composable
fun AuthScreen(
    auth: FirebaseAuth, bg: Color, cardBg: Color, textPrimary: Color,
    textMuted: Color, accent: Color, border: Color, isDarkMode: Boolean,
    onToggleTheme: () -> Unit, onLoginSuccess: () -> Unit
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "MCoS Core", color = accent, fontWeight = FontWeight.Black, fontSize = 22.sp)
            IconButton(onClick = onToggleTheme) {
                Icon(imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "Theme", tint = accent)
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text(text = if (isSignUp) "Create Account" else "Welcome Back", color = textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text(text = "Watch Stream Ads & Earn Instant Wallet Cash", color = textMuted, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            placeholder = { Text("user@mcos.io", color = textMuted) },
            label = { Text("Email", color = textMuted) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent, unfocusedBorderColor = border,
                focusedTextColor = textPrimary, unfocusedTextColor = textPrimary
            )
        )
        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = password, onValueChange = { password = it },
            placeholder = { Text("••••••••••••", color = textMuted) },
            label = { Text("Password", color = textMuted) },
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent, unfocusedBorderColor = border,
                focusedTextColor = textPrimary, unfocusedTextColor = textPrimary
            )
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isNotBlank() && password.length >= 6) {
                    loading = true
                    val task = if (isSignUp) auth.createUserWithEmailAndPassword(email.trim(), password) else auth.signInWithEmailAndPassword(email.trim(), password)
                    task.addOnSuccessListener {
                        loading = false
                        onLoginSuccess()
                    }.addOnFailureListener {
                        loading = false
                        Toast.makeText(context, it.message ?: "Auth failed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Enter valid credentials", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = accent),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(color = Color(0xFF080B11), modifier = Modifier.size(22.dp))
            } else {
                Text(text = if (isSignUp) "Sign Up" else "Sign In", color = Color(0xFF080B11), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(text = if (isSignUp) "Already have an account? " else "Don't have an account? ", color = textMuted, fontSize = 13.sp)
            Text(text = if (isSignUp) "Sign In" else "Sign Up", color = accent, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { isSignUp = !isSignUp })
        }
    }
}

// 3. HOME SCREEN
@Composable
fun HomeScreen(
    auth: FirebaseAuth, database: FirebaseDatabase, userCoins: Int,
    bg: Color, cardBg: Color, textPrimary: Color, textMuted: Color,
    accent: Color, border: Color, isDarkMode: Boolean,
    onToggleTheme: () -> Unit, onTriggerVault: () -> Unit,
    onOpenMcosFeed: () -> Unit, onOpenAi: () -> Unit,
    onOpenWallet: () -> Unit, onOpenRewards: () -> Unit, onLogout: () -> Unit
) {
    var articlesList by remember { mutableStateOf<List<ArticlePost>>(emptyList()) }
    var featuredPost by remember { mutableStateOf<ArticlePost?>(null) }

    DisposableEffect(Unit) {
        val ref = database.getReference("articles")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ArticlePost>()
                for (child in snapshot.children) {
                    child.getValue(ArticlePost::class.java)?.let { list.add(0, it) }
                }
                if (list.isEmpty()) {
                    val defaultItem = ArticlePost(
                        id = "yt_sample_1",
                        title = "Jetpack Compose & Media3 Video Core Engine",
                        summary = "Watch 4K streaming feeds, complete ads offers, and manage your private encrypted documents securely.",
                        imageUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&q=80",
                        videoUrl = "M7lc1UVf-VE",
                        category = "STREAMING",
                        timeAgo = "Live",
                        author = "MCoS Dev"
                    )
                    list.add(defaultItem)
                }
                featuredPost = list.firstOrNull()
                articlesList = if (list.size > 1) list.drop(1) else list
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        onDispose { ref.removeEventListener(listener) }
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bg)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(cardBg)
                                .border(1.5.dp, accent, RoundedCornerShape(12.dp))
                                .clickable { onTriggerVault() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("MC", color = accent, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("MCoS Core", color = textPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Text("Online Hub", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            onClick = onOpenWallet,
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B))
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = "Coins", tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "$userCoins", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        IconButton(onClick = onOpenAi, modifier = Modifier.size(36.dp)) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI", tint = accent, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onToggleTheme, modifier = Modifier.size(36.dp)) {
                            Icon(imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "Theme", tint = textMuted, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onLogout, modifier = Modifier.size(36.dp)) {
                            Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenRewards,
                containerColor = Color(0xFFF59E0B),
                contentColor = Color(0xFF080B11),
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = "Rewards") },
                text = { Text("Watch Ads & Offers", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (featuredPost != null) {
                item {
                    Text("🔥 Featured Stream", color = textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onOpenMcosFeed() },
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(accent, Color(0xFF6366F1))))
                    ) {
                        Column {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                                AsyncImage(
                                    model = featuredPost!!.imageUrl.ifBlank { "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&q=80" },
                                    contentDescription = featuredPost!!.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xDD080B11))))
                                )

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(accent.copy(alpha = 0.9f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", tint = Color(0xFF080B11), modifier = Modifier.size(34.dp))
                                }

                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = featuredPost!!.category.uppercase(),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFEF4444))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                    Text(
                                        text = "${featuredPost!!.readTime} • ${featuredPost!!.timeAgo}",
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = featuredPost!!.title, color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = featuredPost!!.summary, color = textMuted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📺 Video & Article Feeds", color = textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "View All",
                        color = accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onOpenMcosFeed() }
                    )
                }
            }

            items(articlesList) { article ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenMcosFeed() },
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, border)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 110.dp, height = 75.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = article.imageUrl.ifBlank { "https://images.unsplash.com/photo-1574717024653-61fd2cf4d44d?w=800&q=80" },
                                contentDescription = article.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (article.videoUrl.isNotBlank()) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color(0x44000000)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.PlayCircle, contentDescription = "Play", tint = accent, modifier = Modifier.size(24.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = article.title, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "By ${article.author} • ${article.timeAgo}", color = textMuted, fontSize = 11.sp)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

// 4. WALLET & PAYOUT SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    userId: String, userEmail: String, userCoins: Int,
    database: FirebaseDatabase, bg: Color, cardBg: Color,
    textPrimary: Color, textMuted: Color, accent: Color, border: Color,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedMethod by remember { mutableStateOf("UPI") }
    var paymentAddress by remember { mutableStateOf("") }
    var withdrawCoinsInput by remember { mutableStateOf("500") }
    var isSubmitting by remember { mutableStateOf(false) }

    val coinsToInrRate = 0.01
    val requestedCoins = withdrawCoinsInput.toIntOrNull() ?: 0
    val convertedAmountInr = requestedCoins * coinsToInrRate

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg),
                title = { Text("Wallet & Payouts", color = textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = accent) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(Color(0xFFF59E0B), accent)))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Total Redeemable Balance", color = textMuted, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = "Coins", tint = Color(0xFFF59E0B), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "$userCoins Coins", color = textPrimary, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Estimated Value: ₹${String.format("%.2f", userCoins * coinsToInrRate)} INR",
                        color = Color(0xFF10B981),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text("Withdrawal Method", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { selectedMethod = "UPI" },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedMethod == "UPI") accent else cardBg
                    ),
                    border = BorderStroke(1.dp, if (selectedMethod == "UPI") accent else border)
                ) {
                    Text("UPI ID", color = if (selectedMethod == "UPI") Color(0xFF080B11) else textPrimary, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { selectedMethod = "PAYTM" },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedMethod == "PAYTM") accent else cardBg
                    ),
                    border = BorderStroke(1.dp, if (selectedMethod == "PAYTM") accent else border)
                ) {
                    Text("Paytm Number", color = if (selectedMethod == "PAYTM") Color(0xFF080B11) else textPrimary, fontWeight = FontWeight.Bold)
                }
            }

            OutlinedTextField(
                value = paymentAddress,
                onValueChange = { paymentAddress = it },
                label = { Text(if (selectedMethod == "UPI") "Enter UPI ID (e.g. name@okhdfcbank)" else "Enter 10-digit Paytm Mobile", color = textMuted) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent, unfocusedBorderColor = border,
                    focusedTextColor = textPrimary, unfocusedTextColor = textPrimary
                )
            )

            OutlinedTextField(
                value = withdrawCoinsInput,
                onValueChange = { withdrawCoinsInput = it },
                label = { Text("Coins to Withdraw (Min 500 Coins)", color = textMuted) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent, unfocusedBorderColor = border,
                    focusedTextColor = textPrimary, unfocusedTextColor = textPrimary
                )
            )

            Text("You will receive: ₹${String.format("%.2f", convertedAmountInr)} directly into $selectedMethod", color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

            Button(
                onClick = {
                    if (requestedCoins < 500) {
                        Toast.makeText(context, "Minimum withdrawal is 500 Coins (₹5)", Toast.LENGTH_SHORT).show()
                    } else if (requestedCoins > userCoins) {
                        Toast.makeText(context, "Insufficient Coins in Wallet!", Toast.LENGTH_SHORT).show()
                    } else if (paymentAddress.isBlank() || paymentAddress.length < 5) {
                        Toast.makeText(context, "Enter valid $selectedMethod address", Toast.LENGTH_SHORT).show()
                    } else {
                        isSubmitting = true
                        val newCoins = userCoins - requestedCoins
                        val reqId = database.getReference("withdrawals").push().key ?: "req_${System.currentTimeMillis()}"
                        val requestObj = WithdrawalRequest(
                            id = reqId,
                            userId = userId,
                            userEmail = userEmail,
                            paymentType = selectedMethod,
                            paymentAddress = paymentAddress.trim(),
                            coinsDebited = requestedCoins,
                            amountInInr = convertedAmountInr,
                            timestamp = System.currentTimeMillis(),
                            status = "PENDING"
                        )

                        database.getReference("withdrawals").child(reqId).setValue(requestObj).addOnSuccessListener {
                            database.getReference("users").child(userId).child("coins").setValue(newCoins)
                            isSubmitting = false
                            Toast.makeText(context, "Withdrawal Request Submitted! 🎉", Toast.LENGTH_LONG).show()
                            onBack()
                        }.addOnFailureListener {
                            isSubmitting = false
                            Toast.makeText(context, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color(0xFF080B11), modifier = Modifier.size(24.dp))
                } else {
                    Text("Redeem Payout Now", color = Color(0xFF080B11), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

// 5. ADS & REWARD OFFERS SCREEN (GOOGLE ADS STYLE 2-COLUMN GRID)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsOfferScreen(
    userId: String, userCoins: Int, database: FirebaseDatabase,
    bg: Color, cardBg: Color, textPrimary: Color, textMuted: Color,
    accent: Color, border: Color, onBack: () -> Unit
) {
    val context = LocalContext.current
    var isWatchingAd by remember { mutableStateOf(false) }
    var activeAd by remember { mutableStateOf<AdminAdOffer?>(null) }
    var adOffersList by remember { mutableStateOf<List<AdminAdOffer>>(emptyList()) }

    DisposableEffect(Unit) {
        val ref = database.getReference("admin_ads")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<AdminAdOffer>()
                for (child in snapshot.children) {
                    child.getValue(AdminAdOffer::class.java)?.let { list.add(it) }
                }
                if (list.isEmpty()) {
                    list.add(
                        AdminAdOffer(
                            id = "ad_1",
                            title = "GooDady Web Hosting",
                            description = "Watch promo video to win coins",
                            rewardCoins = 50,
                            durationSec = 10,
                            skipAfterSec = 5,
                            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                            bannerUrl = "https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=800&q=80",
                            targetUrl = "https://goodaddy.com",
                            type = "VIDEO_AD"
                        )
                    )
                }
                adOffersList = list
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        onDispose { ref.removeEventListener(listener) }
    }

    val completeReward = { task: AdminAdOffer ->
        if (userId.isNotEmpty()) {
            val userRef = database.getReference("users").child(userId).child("coins")
            userRef.setValue(userCoins + task.rewardCoins).addOnSuccessListener {
                Toast.makeText(context, "+${task.rewardCoins} Coins Added to Wallet! 🎉", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg),
                title = { Text("Ads & Reward Offers", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = accent) }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(adOffersList) { offer ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            activeAd = offer
                            isWatchingAd = true
                        },
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, border)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(115.dp)
                        ) {
                            AsyncImage(
                                model = offer.bannerUrl.ifBlank { "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&q=80" },
                                contentDescription = offer.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(listOf(Color(0x99000000), Color.Transparent, Color(0xAA080B11))))
                            )

                            // Google Ads Mohar / Badge
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFF59E0B))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Ad",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sponsored",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (offer.type == "VIDEO_AD") {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(accent.copy(alpha = 0.9f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", tint = Color(0xFF080B11), modifier = Modifier.size(22.dp))
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = offer.title,
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = offer.description.ifBlank { "Watch to claim reward coins" },
                                color = textMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "+${offer.rewardCoins}",
                                        color = Color(0xFF10B981),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    text = "${offer.durationSec}s",
                                    color = textMuted,
                                    fontSize = 10.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    activeAd = offer
                                    isWatchingAd = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier.fillMaxWidth().height(36.dp)
                            ) {
                                Text(
                                    text = if (offer.type == "VIDEO_AD") "Watch Ad" else "View Ad",
                                    color = Color(0xFF080B11),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isWatchingAd && activeAd != null) {
            FullscreenAdPlayer(
                ad = activeAd!!,
                onDismiss = { isWatchingAd = false },
                onRewardEarned = {
                    isWatchingAd = false
                    completeReward(activeAd!!)
                }
            )
        }
    }
}

// 6. FULLSCREEN YOUTUBE-STYLE VIDEO & BANNER AD PLAYER
@OptIn(UnstableApi::class)
@Composable
fun FullscreenAdPlayer(
    ad: AdminAdOffer,
    onDismiss: () -> Unit,
    onRewardEarned: () -> Unit
) {
    val context = LocalContext.current
    var remainingTime by remember { mutableIntStateOf(ad.durationSec.coerceAtLeast(6)) }
    var skipTimer by remember { mutableIntStateOf(ad.skipAfterSec.coerceAtLeast(3)) }
    var isRewardClaimed by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (remainingTime > 0) {
            delay(1000)
            remainingTime--
            if (skipTimer > 0) skipTimer--
        }
        if (!isRewardClaimed) {
            isRewardClaimed = true
            onRewardEarned()
        }
    }

    val formattedVideoUrl = remember(ad.videoUrl) {
        var url = ad.videoUrl.trim()
        if (url.contains("github.com") && url.contains("/blob/")) {
            url = url.replace("github.com", "raw.githubusercontent.com").replace("/blob/", "/")
        }
        url
    }

    val exoPlayer = remember(formattedVideoUrl) {
        if (ad.type == "VIDEO_AD" && formattedVideoUrl.isNotBlank()) {
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("MCoS-Player/1.0")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(15000)

            val mediaSourceFactory = ProgressiveMediaSource.Factory(httpDataSourceFactory)

            ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .build().apply {
                    val mediaItem = MediaItem.fromUri(Uri.parse(formattedVideoUrl))
                    setMediaItem(mediaItem)
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            isBuffering = (playbackState == Player.STATE_BUFFERING)
                        }
                    })
                    prepare()
                    playWhenReady = true
                }
        } else null
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer?.release()
        }
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (ad.type == "VIDEO_AD" && exoPlayer != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (isBuffering) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF00E5FF), modifier = Modifier.size(44.dp))
                    }
                }
            } else {
                AsyncImage(
                    model = ad.bannerUrl.ifBlank { "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&q=80" },
                    contentDescription = ad.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xCC000000), Color.Transparent, Color(0xDD000000))
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFF59E0B))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Ad", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = ad.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (ad.targetUrl.isNotBlank()) {
                    Button(
                        onClick = {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(ad.targetUrl))
                            context.startActivity(browserIntent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Visit Site ↗", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x99000000))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (remainingTime > 0) "Reward in ${remainingTime}s (+${ad.rewardCoins})" else "Reward Unlocked!",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                if (skipTimer > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xAA000000))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Skip in ${skipTimer}s",
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            if (!isRewardClaimed) {
                                isRewardClaimed = true
                                onRewardEarned()
                            } else {
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Skip Ad", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(imageVector = Icons.Default.FastForward, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// 7. VAULT PIN DIALOG
@Composable
fun VaultPinDialog(
    cardBg: Color, textPrimary: Color, textMuted: Color,
    accent: Color, border: Color, onUnlocked: () -> Unit, onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mcos_vault_prefs", Context.MODE_PRIVATE) }
    val savedPin = remember { prefs.getString("vault_pin", null) }

    var enteredPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    val isSettingUp = savedPin == null

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardBg,
        shape = RoundedCornerShape(22.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.EnhancedEncryption, contentDescription = "Lock", tint = accent)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isSettingUp) "Setup Vault PIN" else "Private Vault Access", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isSettingUp) "Create a 4-digit PIN for your secret documents." else "Enter your 4-digit PIN to unlock encrypted files.",
                    color = textMuted, fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = { if (it.length <= 4) enteredPin = it },
                    placeholder = { Text("••••", color = textMuted, textAlign = TextAlign.Center) },
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent, unfocusedBorderColor = border,
                        focusedTextColor = textPrimary, unfocusedTextColor = textPrimary
                    )
                )
                if (errorMsg.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = errorMsg, color = Color(0xFFEF4444), fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (enteredPin.length == 4) {
                        if (isSettingUp) {
                            prefs.edit().putString("vault_pin", enteredPin).apply()
                            onUnlocked()
                        } else if (enteredPin == savedPin) {
                            onUnlocked()
                        } else {
                            errorMsg = "Incorrect PIN. Try again."
                        }
                    } else {
                        errorMsg = "Enter 4 digits"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text(if (isSettingUp) "Save & Open" else "Unlock", color = Color(0xFF080B11), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = textMuted) }
        }
    )
}

// 8. GEMINI AI DIALOG
@Composable
fun RealGeminiAiDialog(
    cardBg: Color, textPrimary: Color, textMuted: Color,
    accent: Color, border: Color, onDismiss: () -> Unit
) {
    var prompt by remember { mutableStateOf("Explain how to write native C++ JNI code for Android.") }
    var responseText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val client = remember { OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).build() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardBg,
        shape = RoundedCornerShape(22.dp),
        title = { Text("Google Gemini AI", color = textPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = prompt, onValueChange = { prompt = it },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(90.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent, unfocusedBorderColor = border,
                        focusedTextColor = textPrimary, unfocusedTextColor = textPrimary
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (isGenerating) {
                    CircularProgressIndicator(color = accent, modifier = Modifier.size(24.dp))
                } else if (responseText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .background(Color(0xFF080B11), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(text = responseText, color = textPrimary, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isGenerating = true
                    scope.launch(Dispatchers.IO) {
                        try {
                            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$GEMINI_API_KEY"
                            val body = JSONObject().put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt))))).toString()
                            val req = Request.Builder().url(url).post(body.toRequestBody("application/json".toMediaType())).build()
                            val resp = client.newCall(req).execute()
                            val respStr = resp.body?.string() ?: ""
                            val text = JSONObject(respStr).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                            withContext(Dispatchers.Main) {
                                responseText = text
                                isGenerating = false
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                responseText = "Gemini Error: ${e.message}"
                                isGenerating = false
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text("Generate", color = Color(0xFF080B11), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close", color = textMuted) } }
    )
}
