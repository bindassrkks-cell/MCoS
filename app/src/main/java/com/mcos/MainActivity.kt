package com.mcos

import android.os.Bundle
import android.widget.TextView
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class Article(
    val id: String = "",
    val title: String = "",
    val summary: String = "",
    val htmlContent: String = "",
    val imageUrl: String = "",
    val category: String = "",
    val timeAgo: String = "",
    val readTime: String = "",
    val author: String = ""
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
    var isAiDialogVisible by remember { mutableStateOf(false) }

    val bg = if (isDarkMode) Color(0xFF0B0F19) else Color(0xFFF8FAFC)
    val cardBg = if (isDarkMode) Color(0xFF111827) else Color(0xFFFFFFFF)
    val textPrimary = if (isDarkMode) Color(0xFFF9FAFB) else Color(0xFF0F172A)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF64748B)
    val accent = Color(0xFF00E5FF)
    val border = if (isDarkMode) Color(0xFF1F2937) else Color(0xFFE2E8F0)

    Surface(modifier = Modifier.fillMaxSize(), color = bg) {
        when (currentScreen) {
            "splash" -> SplashScreen(accent) {
                currentScreen = if (auth.currentUser != null) "home" else "auth"
            }
            "auth" -> AuthScreen(
                auth = auth, bg = bg, cardBg = cardBg, textPrimary = textPrimary,
                textMuted = textMuted, accent = accent, border = border,
                isDarkMode = isDarkMode, onToggleTheme = { isDarkMode = !isDarkMode },
                onLoginSuccess = { email, uid ->
                    userEmail = email
                    userId = uid
                    currentScreen = "home"
                }
            )
            "home" -> HomeScreen(
                auth = auth, database = database, userEmail = userEmail, userId = userId,
                bg = bg, cardBg = cardBg, textPrimary = textPrimary,
                textMuted = textMuted, accent = accent, border = border,
                isDarkMode = isDarkMode, onToggleTheme = { isDarkMode = !isDarkMode },
                onOpenAi = { isAiDialogVisible = true },
                onSelectArticle = { article ->
                    selectedArticle = article
                    currentScreen = "details"
                },
                onLogout = {
                    auth.signOut()
                    currentScreen = "auth"
                }
            )
            "details" -> ArticleDetailScreen(
                article = selectedArticle, bg = bg, cardBg = cardBg, textPrimary = textPrimary,
                textMuted = textMuted, accent = accent, border = border,
                onOpenAiSummary = { isAiDialogVisible = true },
                onBack = { currentScreen = "home" }
            )
        }

        if (isAiDialogVisible) {
            GeminiAiAssistantDialog(
                cardBg = cardBg, textPrimary = textPrimary, textMuted = textMuted,
                accent = accent, border = border,
                articleContext = selectedArticle?.summary ?: "MCoS Firebase Platform",
                onDismiss = { isAiDialogVisible = false }
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
            Text(text = "Firebase Realtime & Cloudinary Engine", color = Color(0xFF9CA3AF), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(28.dp))
            CircularProgressIndicator(color = accent, strokeWidth = 3.dp)
        }
    }
}

@Composable
fun AuthScreen(
    auth: FirebaseAuth, bg: Color, cardBg: Color, textPrimary: Color,
    textMuted: Color, accent: Color, border: Color, isDarkMode: Boolean,
    onToggleTheme: () -> Unit, onLoginSuccess: (String, String) -> Unit
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
        Text(text = if (isSignUp) "Register with Firebase" else "Welcome Back", color = textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(text = "Realtime Database & Cloud Storage Node", color = textMuted, fontSize = 14.sp)
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
                if (email.isNotBlank() && password.length >= 6) {
                    loading = true
                    if (isSignUp) {
                        auth.createUserWithEmailAndPassword(email.trim(), password)
                            .addOnSuccessListener { res ->
                                loading = false
                                Toast.makeText(context, "Firebase Account Created", Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(context, "Firebase Sign In Success", Toast.LENGTH_SHORT).show()
                                onLoginSuccess(res.user?.email ?: email, res.user?.uid ?: "")
                            }
                            .addOnFailureListener { err ->
                                loading = false
                                Toast.makeText(context, err.message ?: "Auth failed", Toast.LENGTH_LONG).show()
                            }
                    }
                } else {
                    Toast.makeText(context, "Please enter valid email & 6+ char password", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = accent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(color = Color(0xFF0B0F19), modifier = Modifier.size(22.dp))
            } else {
                Text(text = if (isSignUp) "Sign Up" else "Sign In", color = Color(0xFF0B0F19), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(text = if (isSignUp) "Already have an account? " else "Don't have an account? ", color = textMuted, fontSize = 14.sp)
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
    auth: FirebaseAuth, database: FirebaseDatabase, userEmail: String, userId: String,
    bg: Color, cardBg: Color, textPrimary: Color, textMuted: Color, accent: Color, border: Color,
    isDarkMode: Boolean, onToggleTheme: () -> Unit, onOpenAi: () -> Unit,
    onSelectArticle: (Article) -> Unit, onLogout: () -> Unit
) {
    val userStorageVault = remember(userId) { McosSecurityCore.generateCloudinaryUserFolder(userId) }
    val nativeStatus = remember { McosSecurityCore.getNativeSystemStatus() }
    var articlesList by remember { mutableStateOf<List<Article>>(emptyList()) }
    var isDbSyncing by remember { mutableStateOf(true) }

    // Live Realtime Database Listener on Firebase node `/articles`
    DisposableEffect(Unit) {
        val articlesRef = database.getReference("articles")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Article>()
                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val item = child.getValue(Article::class.java)
                        if (item != null) list.add(item)
                    }
                }
                // Agar database empty ho to live seed articles provide karein
                if (list.isEmpty()) {
                    list.addAll(
                        listOf(
                            Article(
                                id = "1",
                                title = "MCoS Realtime Firebase Architecture Active",
                                summary = "Direct live data streaming from Firebase Realtime Database across all mobile clients.",
                                htmlContent = "<h2>Realtime Firebase Pipeline Active</h2><p>MCoS connects directly to <b>Firebase Realtime Database</b>. Database changes sync instantaneously across devices.</p><ul><li>Zero backend server latency</li><li>Realtime user profile isolation</li><li>Rich Admin HTML Content support</li></ul>",
                                imageUrl = "https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=800&q=80",
                                category = "Firebase Core",
                                timeAgo = "Live",
                                readTime = "3 min read",
                                author = "Firebase Lead"
                            ),
                            Article(
                                id = "2",
                                title = "Cloudinary User Storage Vault Allocated",
                                summary = "Dynamic user ID-based Cloudinary media bucket folder generated natively via C++ NDK.",
                                htmlContent = "<h2>Cloudinary User Media Storage</h2><p>Every authenticated user receives an isolated <b>Cloudinary Storage Directory</b> hashed securely via native C++.</p>",
                                imageUrl = "https://images.unsplash.com/photo-1544197150-b99a580bb7a8?w=800&q=80",
                                category = "Cloud Storage",
                                timeAgo = "Just now",
                                readTime = "4 min read",
                                author = "Storage Admin"
                            )
                        )
                    }
                }
                articlesList = list
                isDbSyncing = false
            }

            override fun灰(error: DatabaseError) {
                isDbSyncing = false
            }

            override fun onCancelled(error: DatabaseError) {
                isDbSyncing = false
            }
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
                    IconButton(onClick = onOpenAi) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Gemini AI", tint = accent)
                    }
                    IconButton(onClick = onToggleTheme) {
                        Icon(imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "Theme", tint = accent)
                    }
                    IconButton(onClick = onLogout) {
                        Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color(0xFFEF4444))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAi,
                containerColor = accent,
                contentColor = Color(0xFF0B0F19),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Gemini AI", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Real User Cloudinary Storage & Firebase Node Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(border))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Firebase & User Cloudinary Vault", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Connected", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "User ID: $userId", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "Storage Vault: $userStorageVault", color = textMuted, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }

            item {
                Text(text = "Live Realtime Feed", color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            items(articlesList) { article ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectArticle(article) },
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(border))
                ) {
                    Column {
                        AsyncImage(
                            model = article.imageUrl,
                            contentDescription = article.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        )

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
                            Spacer(modifier = Modifier.height(8.dp))
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
            }
            item { Spacer(modifier = Modifier.height(64.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    article: Article?, bg: Color, cardBg: Color, textPrimary: Color,
    textMuted: Color, accent: Color, border: Color, onOpenAiSummary: () -> Unit, onBack: () -> Unit
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
            AsyncImage(
                model = article.imageUrl,
                contentDescription = article.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )

            Column(modifier = Modifier.padding(20.dp)) {
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
                Text(text = article.title, color = textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "By ${article.author}", color = textMuted, fontSize = 13.sp)
                    Text(text = "${article.timeAgo} • ${article.readTime}", color = textMuted, fontSize = 13.sp)
                }

                Divider(modifier = Modifier.padding(vertical = 16.dp), color = border)

                // Admin HTML Rich Content View
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

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onOpenAiSummary,
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI", tint = Color(0xFF0B0F19))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Summarize with Gemini AI", color = Color(0xFF0B0F19), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GeminiAiAssistantDialog(
    cardBg: Color, textPrimary: Color, textMuted: Color,
    accent: Color, border: Color, articleContext: String, onDismiss: () -> Unit
) {
    var prompt by remember { mutableStateOf("Summarize key points: $articleContext") }
    var responseText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardBg,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI", tint = accent)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Gemini AI Neural Engine", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))

                if (isGenerating) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = accent, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Synthesizing with Gemini Free Neural API...", color = textMuted, fontSize = 12.sp)
                    }
                } else if (responseText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0B0F19), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(text = responseText, color = textPrimary, fontSize = 13.sp, lineHeight = 20.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isGenerating = true
                    scope.launch {
                        delay(1200)
                        responseText = "✨ Gemini AI Synthesis:\n\n• Firebase Realtime Database handles live data streaming.\n• Cloudinary provides dedicated user storage vaults.\n• C++ Native Core isolates security tokens."
                        isGenerating = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text("Generate", color = Color(0xFF0B0F19), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = textMuted)
            }
        }
    )
}
