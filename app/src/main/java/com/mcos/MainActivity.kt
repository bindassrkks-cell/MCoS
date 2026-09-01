package com.mcos

import android.content.Context
import android.content.Intent
import android.os.Bundle
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

data class PostItem(
    val id: String = "",
    val title: String = "",
    val summary: String = "",
    val htmlContent: String = "",
    val imageUrl: String = "",
    val videoUrl: String = "",
    val category: String = "",
    val timeAgo: String = "",
    val readTime: String = "",
    val author: String = ""
)

data class RewardOffer(
    val id: String,
    val title: String,
    val description: String,
    val rewardCoins: Int,
    val type: String,
    val timerSec: Int = 10
)

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

    val bg = if (isDarkMode) Color(0xFF080B11) else Color(0xFFF8FAFC)
    val cardBg = if (isDarkMode) Color(0xFF111827) else Color(0xFFFFFFFF)
    val textPrimary = if (isDarkMode) Color(0xFFF9FAFB) else Color(0xFF0F172A)
    val textMuted = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    val accent = Color(0xFF00E5FF)
    val border = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0)

    val currentUid = auth.currentUser?.uid ?: ""

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
                onOpenVault = {
                    context.startActivity(Intent(context, VaultActivity::class.java))
                },
                onOpenMcosFeed = {
                    context.startActivity(Intent(context, McosActivity::class.java))
                },
                onOpenAi = { isAiDialogVisible = true },
                onOpenRewards = { currentScreen = "rewards" },
                onLogout = {
                    auth.signOut()
                    currentScreen = "auth"
                }
            )
            "rewards" -> RewardsOfferScreen(
                userId = currentUid, userCoins = userCoins, database = database,
                bg = bg, cardBg = cardBg, textPrimary = textPrimary,
                textMuted = textMuted, accent = accent, border = border,
                onBack = { currentScreen = "home" }
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
        delay(1800)
        onTimeout()
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(90.dp)
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "MCoS Core", color = accent, fontWeight = FontWeight.Black, fontSize = 20.sp)
            IconButton(onClick = onToggleTheme) {
                Icon(imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "Theme", tint = accent)
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text(text = if (isSignUp) "Create Account" else "Welcome Back", color = textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text(text = "Cloud Sync & Native Cryptographic Security", color = textMuted, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it },
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
                    val task = if (isSignUp) {
                        auth.createUserWithEmailAndPassword(email.trim(), password)
                    } else {
                        auth.signInWithEmailAndPassword(email.trim(), password)
                    }
                    task.addOnSuccessListener {
                        loading = false
                        onLoginSuccess()
                    }.addOnFailureListener {
                        loading = false
                        Toast.makeText(context, it.message ?: "Authentication error", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Enter valid email and 6+ character password", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = accent),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(color = Color(0xFF080B11), modifier = Modifier.size(22.dp))
            } else {
                Text(text = if (isSignUp) "Sign Up" else "Sign In", color = Color(0xFF080B11), fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(text = if (isSignUp) "Already have an account? " else "Don't have an account? ", color = textMuted, fontSize = 13.sp)
            Text(text = if (isSignUp) "Sign In" else "Sign Up", color = accent, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { isSignUp = !isSignUp })
        }
    }
}

// 3. HOME DASHBOARD
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    auth: FirebaseAuth, database: FirebaseDatabase, userCoins: Int,
    bg: Color, cardBg: Color, textPrimary: Color, textMuted: Color,
    accent: Color, border: Color, isDarkMode: Boolean,
    onToggleTheme: () -> Unit, onOpenVault: () -> Unit,
    onOpenMcosFeed: () -> Unit, onOpenAi: () -> Unit,
    onOpenRewards: () -> Unit, onLogout: () -> Unit
) {
    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(cardBg)
                                .border(1.5.dp, accent, RoundedCornerShape(10.dp))
                                .clickable { onOpenVault() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("MC", color = accent, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("MCoS Core", color = textPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Text("Dashboard", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                actions = {
                    Surface(
                        onClick = onOpenRewards,
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = "Coins", tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "$userCoins", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    IconButton(onClick = onOpenAi) { Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI", tint = accent) }
                    IconButton(onClick = onToggleTheme) { Icon(imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "Theme", tint = textMuted) }
                    IconButton(onClick = onLogout) { Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color(0xFFEF4444)) }
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
            // Card 1: Video Feed & Articles (McosActivity Navigation)
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onOpenMcosFeed() },
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, accent)
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(54.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.PlayCircle, contentDescription = "Stream", tint = accent, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "MCoS Video & News Feed", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(text = "YouTube Player & Tech Articles Stream", color = textMuted, fontSize = 12.sp)
                    }
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Open", tint = accent)
                }
            }

            // Card 2: Secret Vault (VaultActivity Navigation)
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onOpenVault() },
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF10B981))
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(54.dp).clip(CircleShape).background(Color(0xFF10B981).copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Vault", tint = Color(0xFF10B981), modifier = Modifier.size(30.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Secret Storage Locker", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(text = "Native encrypted hide photos, videos & docs", color = textMuted, fontSize = 12.sp)
                    }
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Open", tint = Color(0xFF10B981))
                }
            }

            // Card 3: Ads, Offers & Rewards
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onOpenRewards() },
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFF59E0B))
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(54.dp).clip(CircleShape).background(Color(0xFFF59E0B).copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = "Offers", tint = Color(0xFFF59E0B), modifier = Modifier.size(30.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Rewards & Offerwall", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(text = "Complete offers and watch ads to win coins", color = textMuted, fontSize = 12.sp)
                    }
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Open", tint = Color(0xFFF59E0B))
                }
            }
        }
    }
}

// 4. REWARDS & OFFERWALL SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsOfferScreen(
    userId: String, userCoins: Int, database: FirebaseDatabase,
    bg: Color, cardBg: Color, textPrimary: Color, textMuted: Color,
    accent: Color, border: Color, onBack: () -> Unit
) {
    val context = LocalContext.current
    var isWatchingAd by remember { mutableStateOf(false) }
    var adRemainingTime by remember { mutableIntStateOf(10) }
    var activeOffer by remember { mutableStateOf<RewardOffer?>(null) }

    val offers = remember {
        listOf(
            RewardOffer("ad_1", "Sponsored Video Ad", "Watch full 10-sec ad to earn instant coins", 50, "AD", 10),
            RewardOffer("ad_2", "Partner Video Stream", "Watch 15-sec promotional video", 80, "AD", 15),
            RewardOffer("daily_1", "Daily Streak Bonus", "Claim free daily attendance reward", 100, "DAILY", 0),
            RewardOffer("task_1", "Secret Vault Task", "Encrypt and hide a document in vault", 150, "TASK", 0)
        )
    }

    val completeReward = { task: RewardOffer ->
        if (userId.isNotEmpty()) {
            val userRef = database.getReference("users").child(userId).child("coins")
            userRef.setValue(userCoins + task.rewardCoins).addOnSuccessListener {
                Toast.makeText(context, "+${task.rewardCoins} Coins Added! 🎉", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(isWatchingAd) {
        if (isWatchingAd && activeOffer != null) {
            adRemainingTime = activeOffer!!.timerSec
            while (adRemainingTime > 0) {
                delay(1000)
                adRemainingTime--
            }
            isWatchingAd = false
            completeReward(activeOffer!!)
        }
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg),
                title = { Text("Ads & Offerwall", color = textPrimary, fontWeight = FontWeight.Bold) },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, Color(0xFFF59E0B))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Total Coins Balance", color = textMuted, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "$userCoins Coins", color = textPrimary, fontSize = 32.sp, fontWeight = FontWeight.Black)
                }
            }

            Text("Available Offers", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)

            offers.forEach { offer ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, border)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = offer.title, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = offer.description, color = textMuted, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "+${offer.rewardCoins} Coins", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                if (offer.type == "AD") {
                                    activeOffer = offer
                                    isWatchingAd = true
                                } else {
                                    completeReward(offer)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (offer.type == "AD") "Watch Ad" else "Claim", color = Color(0xFF080B11), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (isWatchingAd) {
            AlertDialog(
                onDismissRequest = {},
                containerColor = cardBg,
                shape = RoundedCornerShape(20.dp),
                title = { Text("Playing Sponsored Ad", color = textPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(color = Color(0xFFF59E0B), modifier = Modifier.size(46.dp))
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Unlocking reward in: $adRemainingTime s", color = textPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                confirmButton = {}
            )
        }
    }
}

// 5. GEMINI AI DIALOG
@Composable
fun RealGeminiAiDialog(
    cardBg: Color, textPrimary: Color, textMuted: Color,
    accent: Color, border: Color, onDismiss: () -> Unit
) {
    var prompt by remember { mutableStateOf("Explain how to build Android Native C++ applications.") }
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
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = border, focusedTextColor = textPrimary, unfocusedTextColor = textPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(90.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (isGenerating) {
                    CircularProgressIndicator(color = accent, modifier = Modifier.size(24.dp))
                } else if (responseText.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).background(Color(0xFF080B11), RoundedCornerShape(10.dp)).padding(10.dp).verticalScroll(rememberScrollState())) {
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
                                responseText = "Gemini Response Error: ${e.message}"
                                isGenerating = false
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text("Ask AI", color = Color(0xFF080B11), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close", color = textMuted) } }
    )
}
