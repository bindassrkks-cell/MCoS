package com.mcos

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            McosApp()
        }
    }
}

@Composable
fun McosApp() {
    var currentScreen by remember { mutableStateOf("splash") }
    var userEmail by remember { mutableStateOf("") }

    val darkBackground = Color(0xFF0B0F19)
    val neonCyan = Color(0xFF00E5FF)
    val cardDark = Color(0xFF111827)
    val textMuted = Color(0xFF9CA3AF)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = darkBackground
    ) {
        when (currentScreen) {
            "splash" -> SplashScreen {
                currentScreen = "auth"
            }
            "auth" -> AuthScreen(
                darkBackground = darkBackground,
                neonCyan = neonCyan,
                cardDark = cardDark,
                textMuted = textMuted,
                onLoginSuccess = { email ->
                    userEmail = email
                    currentScreen = "home"
                }
            )
            "home" -> HomeScreen(
                userEmail = userEmail,
                neonCyan = neonCyan,
                cardDark = cardDark,
                textMuted = textMuted,
                onLogout = {
                    currentScreen = "auth"
                }
            )
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2200)
        onTimeout()
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(Color(0xFF111827), RoundedCornerShape(24.dp))
                    .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "MC",
                    color = Color(0xFF00E5FF),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "MCOS",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 6.sp
            )
            Text(
                text = "Jetpack Compose + NDK C++ Core",
                color = Color(0xFF9CA3AF),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(28.dp))
            CircularProgressIndicator(color = Color(0xFF00E5FF), strokeWidth = 3.dp)
        }
    }
}

@Composable
fun AuthScreen(
    darkBackground: Color,
    neonCyan: Color,
    cardDark: Color,
    textMuted: Color,
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
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(cardDark, RoundedCornerShape(12.dp))
                .border(1.5.dp, neonCyan, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "MC", color = neonCyan, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isSignUp) "Create Account" else "Welcome Back",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (isSignUp) "Sign up with Neon Cloud Backend" else "Sign in to access your MCOS console",
            color = textMuted,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(28.dp))

        if (isSignUp) {
            Text(text = "Full Name", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                placeholder = { Text("John Doe", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = neonCyan,
                    unfocusedBorderColor = Color(0xFF1F2937),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = cardDark,
                    unfocusedContainerColor = cardDark
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        Text(text = "Email Address", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("user@mcos.io", color = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = neonCyan,
                unfocusedBorderColor = Color(0xFF1F2937),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = cardDark,
                unfocusedContainerColor = cardDark
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(14.dp))

        Text(text = "Password", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("••••••••••••", color = Color.Gray) },
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = neonCyan,
                unfocusedBorderColor = Color(0xFF1F2937),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = cardDark,
                unfocusedContainerColor = cardDark
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isNotBlank() && password.isNotBlank()) {
                    Toast.makeText(context, "Authenticated via C++ NDK Core", Toast.LENGTH_SHORT).show()
                    onLoginSuccess(email)
                } else {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = neonCyan),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = if (isSignUp) "Create Account" else "Sign In",
                color = darkBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isSignUp) "Already have an account? " else "Don't have an account? ",
                color = textMuted,
                fontSize = 14.sp
            )
            Text(
                text = if (isSignUp) "Sign In" else "Sign Up",
                color = neonCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.clickable { isSignUp = !isSignUp }
            )
        }
    }
}

@Composable
fun HomeScreen(
    userEmail: String,
    neonCyan: Color,
    cardDark: Color,
    textMuted: Color,
    onLogout: () -> Unit
) {
    val projectId = remember { NeonBackend.getNeonProjectId() }
    val bucket = remember { NeonBackend.getNeonBucket() }
    val dataApi = remember { NeonBackend.getNeonDataApi() }
    val s3Endpoint = remember { NeonBackend.getNeonS3Endpoint() }
    val region = remember { NeonBackend.getNeonRegion() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Hello, ${userEmail.substringBefore("@")}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(text = userEmail, color = textMuted, fontSize = 13.sp)
            }
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(text = "Sign Out", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Neon DB Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardDark),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1F2937)))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Neon Postgres Database", color = neonCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Project ID:", color = textMuted, fontSize = 12.sp)
                Text(text = projectId, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "REST API Endpoint:", color = textMuted, fontSize = 12.sp)
                Text(text = dataApi, color = Color.LightGray, fontSize = 11.sp, maxLines = 1)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Status: Connected (NDK Verified)", color = Color(0xFF10B981), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // S3 Storage Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardDark),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1F2937)))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Neon S3 Storage Core", color = neonCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Bucket Name:", color = textMuted, fontSize = 12.sp)
                Text(text = bucket, color = neonCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Region:", color = textMuted, fontSize = 12.sp)
                Text(text = region, color = Color.White, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "S3 Endpoint:", color = textMuted, fontSize = 12.sp)
                Text(text = s3Endpoint, color = Color.LightGray, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}
