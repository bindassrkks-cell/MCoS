package com.mcos

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class Article(
    val id: String,
    val title: String,
    val summary: String,
    val content: String,
    val category: String,
    val timeAgo: String,
    val readTime: String,
    val author: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            McosAppRoot()
        }
    }
}

@Composable
fun McosAppRoot() {
    var isDarkMode by remember { mutableStateOf(true) }
    var currentScreen by remember { mutableStateOf("splash") }
    var userEmail by remember { mutableStateOf("") }
    var selectedArticle by remember { mutableStateOf<Article?>(null) }

    val bg = if (isDarkMode) Color(0xFF0B0F19) else Color(0xFFF8FAFC)
    val cardBg = if (isDarkMode) Color(0xFF111827) else Color(0xFFFFFFFF)
    val textPrimary = if (isDarkMode) Color(0xFFF9FAFB) else Color(0xFF0F172A)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF64748B)
    val accent = Color(0xFF00E5FF)
    val border = if (isDarkMode) Color(0xFF1F2937) else Color(0xFFE2E8F0)

    Surface(modifier = Modifier.fillMaxSize(), color = bg) {
        when (currentScreen) {
            "splash" -> SplashScreen(accent) { currentScreen = "auth" }
            "auth" -> AuthScreen(
                bg = bg, cardBg = cardBg, textPrimary = textPrimary,
                textMuted = textMuted, accent = accent, border = border,
                isDarkMode = isDarkMode, onToggleTheme = { isDarkMode = !isDarkMode },
                onLoginSuccess = { email ->
                    userEmail = email
                    currentScreen = "home"
                }
            )
            "home" -> HomeScreen(
                userEmail = userEmail, bg = bg, cardBg = cardBg, textPrimary = textPrimary,
                textMuted = textMuted, accent = accent, border = border,
                isDarkMode = isDarkMode, onToggleTheme = { isDarkMode = !isDarkMode },
                onSelectArticle = { article ->
                    selectedArticle = article
                    currentScreen = "details"
                },
                onLogout = { currentScreen = "auth" }
            )
            "details" -> ArticleDetailScreen(
                article = selectedArticle, bg = bg, cardBg = cardBg, textPrimary = textPrimary,
                textMuted = textMuted, accent = accent, border = border,
                onBack = { currentScreen = "home" }
            )
        }
    }
}

@Composable
fun SplashScreen(accent: Color, onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2200)
        onTimeout()
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(Color(0xFF111827), RoundedCornerShape(24.dp))
                    .border(2.dp, accent, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "MC", color = accent, fontSize = 38.sp, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "MCoS", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp)
            Text(text = "Next-Gen Compose & Native C++ Core", color = Color(0xFF9CA3AF), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(28.dp))
            CircularProgressIndicator(color = accent, strokeWidth = 3.dp)
        }
    }
}

@Composable
fun AuthScreen(
    bg: Color, cardBg: Color, textPrimary: Color, textMuted: Color,
    accent: Color, border: Color, isDarkMode: Boolean, onToggleTheme: () -> Unit,
    onLoginSuccess: (String) -> Unit
) {
    var isSignUp by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(cardBg, RoundedCornerShape(12.dp))
                    .border(1.5.dp, accent, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "MC", color = accent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            IconButton(onClick = onToggleTheme) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Theme", tint = accent
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = if (isSignUp) "Join MCoS" else "Welcome Back", color = textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(text = "Neon Database & Supabase Realtime Gateway", color = textMuted, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(28.dp))

        if (isSignUp) {
            Text(text = "Full Name", color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = fullName, onValueChange = { fullName = it },
                placeholder = { Text("Alex Mason", color = textMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent, unfocusedBorderColor = border,
                    focusedTextColor = textPrimary, unfocusedTextColor = textPrimary,
                    focusedContainerColor = cardBg, unfocusedContainerColor = cardBg
                ),
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        Text(text = "Email Address", color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            placeholder = { Text("admin@mcos.io", color = textMuted) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent, unfocusedBorderColor = border,
                focusedTextColor = textPrimary, unfocusedTextColor = textPrimary,
                focusedContainerColor = cardBg, unfocusedContainerColor = cardBg
            ),
            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(14.dp))

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
            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isNotBlank() && password.isNotBlank()) {
                    Toast.makeText(context, "Authenticated via C++ Native Engine", Toast.LENGTH_SHORT).show()
                    onLoginSuccess(email)
                } else {
                    Toast.makeText(context, "Please enter credentials", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = accent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(text = if (isSignUp) "Create Account" else "Sign In", color = Color(0xFF0B0F19), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(text = if (isSignUp) "Already registered? " else "Don't have an account? ", color = textMuted, fontSize = 14.sp)
            Text(
                text = if (isSignUp) "Sign In" else "Sign Up", color = accent,
                fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.clickable { isSignUp = !isSignUp }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userEmail: String, bg: Color, cardBg: Color, textPrimary: Color,
    textMuted: Color, accent: Color, border: Color, isDarkMode: Boolean,
    onToggleTheme: () -> Unit, onSelectArticle: (Article) -> Unit, onLogout: () -> Unit
) {
    val systemStatus = remember { McosNativeCore.getSystemStatus() }
    val projectId = remember { McosNativeCore.getNeonProjectId() }
    val bucket = remember { McosNativeCore.getNeonBucket() }

    val articles = remember {
        listOf(
            Article("1", "MCoS Native Kernel v1.0 Released", "High performance C++ NDK layer with direct Postgres and Supabase synchronization.", "MCoS Native v1.0 integrates modular C++ layers directly with Android Jetpack Compose. This enables sub-millisecond execution speeds for data validation and cryptographic security tokens without JavaScript overhead.", "Core Tech", "10m ago", "3 min read", "MCoS Team"),
            Article("2", "Neon Database Serverless & S3 Architecture", "Scaling serverless Postgres with instant branching and object storage.", "Neon brings serverless Postgres architecture to mobile applications. With S3-compatible cloud storage buckets and REST data APIs, files and datasets scale automatically.", "Cloud & DB", "1h ago", "5 min read", "Data Lead"),
            Article("3", "Jetpack Compose Modern UI Paradigms", "Designing fluid, hardware-accelerated dark/light user interfaces.", "Jetpack Compose replaces legacy XML layouts with declarative Kotlin state management. Combined with Material 3 theming, applications respond dynamically to system accents and theme preferences.", "UI / UX", "3h ago", "4 min read", "Design Core"),
            Article("4", "Supabase Realtime WebSockets on Android", "Streaming database row-level changes with low latency.", "Supabase Realtime allows instant push updates over secure WebSockets. Live collaborative events and feed changes reflect automatically on client devices.", "Realtime", "5h ago", "6 min read", "Network Eng")
        )
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
                                .size(36.dp)
                                .background(cardBg, RoundedCornerShape(10.dp))
                                .border(1.dp, accent, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("MC", color = accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("MCoS Feed", color = textPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "Theme", tint = accent)
                    }
                    IconButton(onClick = onLogout) {
                        Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color(0xFFEF4444))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                // Native Cloud Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(border))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Engine Core & Cloud", color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = systemStatus, color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Project: $projectId | Bucket: $bucket", color = textMuted, fontSize = 11.sp)
                    }
                }
            }

            item {
                Text(text = "Top Stories & Articles", color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            items(articles) { article ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectArticle(article) },
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(border))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = article.category.uppercase(),
                                color = accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(accent.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                            Text(text = article.timeAgo, color = textMuted, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = article.title, color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = article.summary, color = textMuted, fontSize = 13.sp, maxLines = 2)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "By ${article.author}", color = textMuted, fontSize = 12.sp)
                            Text(text = article.readTime, color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    article: Article?, bg: Color, cardBg: Color, textPrimary: Color,
    textMuted: Color, accent: Color, border: Color, onBack: () -> Unit
) {
    if (article == null) return

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg),
                title = { Text("Article View", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = article.category.uppercase(),
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(accent.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = article.title, color = textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Published by ${article.author}", color = textMuted, fontSize = 13.sp)
                Text(text = "${article.timeAgo} • ${article.readTime}", color = textMuted, fontSize = 13.sp)
            }
            Divider(modifier = Modifier.padding(vertical = 16.dp), color = border)
            Text(
                text = article.content,
                color = textPrimary,
                fontSize = 15.sp,
                lineHeight = 24.sp
            )
        }
    }
}
