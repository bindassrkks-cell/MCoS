package com.mcos

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import coil.compose.AsyncImage
import com.google.firebase.database.*

class McosActivity : ComponentActivity() {

    companion object {
        init {
            System.loadLibrary("mcos")
        }
    }

    external fun getStreamEngineToken(): String

    private val database: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            McosFeedApp(database, onFinish = { finish() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McosFeedApp(database: FirebaseDatabase, onFinish: () -> Unit) {
    val bg = Color(0xFF080B11)
    val cardBg = Color(0xFF111827)
    val textPrimary = Color(0xFFF9FAFB)
    val textMuted = Color(0xFF94A3B8)
    val accent = Color(0xFF00E5FF)
    val border = Color(0xFF1E293B)

    var posts by remember { mutableStateOf<List<ArticlePost>>(emptyList()) }
    var selectedPost by remember { mutableStateOf<ArticlePost?>(null) }
    var activeYouTubeId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val ref = database.getReference("articles")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ArticlePost>()
                for (child in snapshot.children) {
                    child.getValue(ArticlePost::class.java)?.let { list.add(0, it) }
                }
                if (list.isEmpty()) {
                    list.add(
                        ArticlePost(
                            id = "yt_1",
                            title = "Jetpack Compose YouTube Stream Engine",
                            summary = "Hardware accelerated embedded video playback with realtime cloud feeds.",
                            htmlContent = "<h3>Streaming Engine</h3><p>MCoS Video feed powered by YouTube IFrame architecture.</p>",
                            imageUrl = "https://images.unsplash.com/photo-1574717024653-61fd2cf4d44d?w=800&q=80",
                            videoUrl = "M7lc1UVf-VE",
                            category = "STREAM",
                            timeAgo = "Live",
                            readTime = "5 min watch",
                            author = "MCoS Kernel"
                        )
                    )
                }
                posts = list
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        onDispose { ref.removeEventListener(listener) }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = bg) {
        if (activeYouTubeId != null) {
            // Fullscreen YouTube Player
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                YouTubeIFramePlayer(
                    videoId = activeYouTubeId!!,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = { activeYouTubeId = null },
                    modifier = Modifier
                        .padding(top = 40.dp, start = 16.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x88000000))
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            }
        } else if (selectedPost != null) {
            // Post Detail View
            Scaffold(
                containerColor = bg,
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = bg),
                        title = { Text("Post Details", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                        navigationIcon = {
                            IconButton(onClick = { selectedPost = null }) {
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
                        .verticalScroll(rememberScrollState())
                ) {
                    if (selectedPost!!.videoUrl.isNotBlank()) {
                        YouTubeIFramePlayer(
                            videoId = selectedPost!!.videoUrl,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        )
                    } else if (selectedPost!!.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = selectedPost!!.imageUrl,
                            contentDescription = selectedPost!!.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        )
                    }

                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = selectedPost!!.category.uppercase(),
                            color = accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accent.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = selectedPost!!.title, color = textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "By ${selectedPost!!.author} • ${selectedPost!!.timeAgo}", color = textMuted, fontSize = 12.sp)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = border)

                        AndroidView(
                            factory = { ctx ->
                                TextView(ctx).apply {
                                    setTextColor(textPrimary.toArgb())
                                    textSize = 15f
                                    setLineSpacing(6f, 1.2f)
                                }
                            },
                            update = { it.text = HtmlCompat.fromHtml(selectedPost!!.htmlContent, HtmlCompat.FROM_HTML_MODE_COMPACT) }
                        )
                    }
                }
            }
        } else {
            // Main Feed List
            Scaffold(
                containerColor = bg,
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = bg),
                        title = { Text("MCoS Media Stream", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                        navigationIcon = {
                            IconButton(onClick = onFinish) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = accent)
                            }
                        }
                    )
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(posts) { post ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { selectedPost = post },
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, border)
                        ) {
                            Column {
                                Box(modifier = Modifier.fillMaxWidth().height(190.dp)) {
                                    AsyncImage(
                                        model = post.imageUrl.ifBlank { "https://images.unsplash.com/photo-1574717024653-61fd2cf4d44d?w=800&q=80" },
                                        contentDescription = post.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (post.videoUrl.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x99000000)))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            IconButton(
                                                onClick = { activeYouTubeId = post.videoUrl },
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(CircleShape)
                                                    .background(accent)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Play",
                                                    tint = Color(0xFF080B11),
                                                    modifier = Modifier.size(34.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(
                                            text = post.category.uppercase(),
                                            color = accent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(text = post.timeAgo, color = textMuted, fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = post.title, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = post.summary, color = textMuted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(30.dp)) }
                }
            }
        }
    }
}

// Native WebView YouTube Player Bridge
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeIFramePlayer(videoId: String, modifier: Modifier = Modifier) {
    val html = """
        <!DOCTYPE html><html><head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>* {margin:0;padding:0;} body {background:#000;} .video-box{width:100vw;height:100vh;} iframe{width:100%;height:100%;border:none;}</style>
        </head><body><div class="video-box">
        <iframe src="https://www.youtube.com/embed/$videoId?autoplay=1&enablejsapi=1&rel=0&playsinline=1" allowfullscreen></iframe>
        </div></body></html>
    """.trimIndent()

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
            }
        },
        update = { it.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null) }
    )
}
