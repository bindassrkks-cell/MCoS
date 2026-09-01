package com.mcos

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class EncryptedVaultItem(
    val file: File,
    val originalName: String,
    val formattedSize: String,
    val type: String
)

class VaultActivity : ComponentActivity() {

    companion object {
        init {
            System.loadLibrary("vault")
        }
    }

    external fun nativeEncryptDecrypt(data: ByteArray, pinKey: String): ByteArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VaultSecureApp(
                onEncryptDecrypt = { bytes, pin -> nativeEncryptDecrypt(bytes, pin) },
                onFinish = { finish() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSecureApp(
    onEncryptDecrypt: (ByteArray, String) -> ByteArray,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("mcos_vault_prefs", Context.MODE_PRIVATE) }
    val savedPin = remember { prefs.getString("vault_pin", null) }

    var isUnlocked by remember { mutableStateOf(false) }
    var currentPin by remember { mutableStateOf("") }
    var vaultItems by remember { mutableStateOf<List<EncryptedVaultItem>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }

    val bg = Color(0xFF080B11)
    val cardBg = Color(0xFF111827)
    val textPrimary = Color(0xFFF9FAFB)
    val textMuted = Color(0xFF94A3B8)
    val accent = Color(0xFF10B981)
    val border = Color(0xFF1E293B)

    // Hidden Private Directory (.nomedia ensures 100% hidden from Gallery & File Managers)
    val vaultDirectory = remember {
        val dir = File(context.filesDir, ".mcos_secret_encrypted_vault")
        if (!dir.exists()) dir.mkdirs()
        val noMedia = File(dir, ".nomedia")
        if (!noMedia.exists()) noMedia.createNewFile()
        dir
    }

    val refreshVaultList = {
        val list = vaultDirectory.listFiles { f -> f.name != ".nomedia" }?.map { file ->
            val sizeMb = String.format("%.2f MB", file.length() / (1024.0 * 1024.0))
            val ext = file.name.substringAfterLast(".", "FILE").uppercase()
            EncryptedVaultItem(file, file.name, sizeMb, ext)
        } ?: emptyList()
        vaultItems = list
    }

    LaunchedEffect(isUnlocked) {
        if (isUnlocked) refreshVaultList()
    }

    // Pick ANY Document / Media File Launcher
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            isProcessing = true
            scope.launch(Dispatchers.IO) {
                uris.forEach { uri ->
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val rawBytes = inputStream?.readBytes() ?: byteArrayOf()
                        inputStream?.close()

                        // Encrypt bytes via C++ libvault.so
                        val encryptedBytes = onEncryptDecrypt(rawBytes, currentPin)

                        val targetFile = File(vaultDirectory, "enc_${System.currentTimeMillis()}_${(100..999).random()}.mcos")
                        FileOutputStream(targetFile).use { it.write(encryptedBytes) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                withContext(Dispatchers.Main) {
                    isProcessing = false
                    refreshVaultList()
                    Toast.makeText(context, "Encrypted & Hidden in Vault! 🔐", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = bg) {
        if (!isUnlocked) {
            // PIN Lock / Setup Screen
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.EnhancedEncryption, contentDescription = "Lock", tint = accent, modifier = Modifier.size(38.dp))
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text = if (savedPin == null) "Setup Master Vault PIN" else "Enter Master PIN", color = textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(text = "AES Native Encrypted Storage (.nomedia)", color = textMuted, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = { if (it.length <= 4) currentPin = it },
                        placeholder = { Text("••••", color = textMuted, textAlign = TextAlign.Center) },
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.width(180.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent, unfocusedBorderColor = border,
                            focusedTextColor = textPrimary, unfocusedTextColor = textPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (currentPin.length == 4) {
                                if (savedPin == null) {
                                    prefs.edit().putString("vault_pin", currentPin).apply()
                                    isUnlocked = true
                                } else if (currentPin == savedPin) {
                                    isUnlocked = true
                                } else {
                                    Toast.makeText(context, "Invalid PIN!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Enter 4 digits PIN", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (savedPin == null) "Create & Open" else "Unlock Vault", color = Color(0xFF080B11), fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Unlocked Vault Screen
            Scaffold(
                containerColor = bg,
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = bg),
                        title = { Text("Encrypted Vault", color = textPrimary, fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onFinish) { Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = accent) }
                        }
                    )
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = { documentPickerLauncher.launch("*/*") },
                        containerColor = accent,
                        contentColor = Color(0xFF080B11),
                        shape = RoundedCornerShape(16.dp),
                        icon = { Icon(imageVector = Icons.Default.AddModerator, contentDescription = "Add") },
                        text = { Text("Hide Any Document", fontWeight = FontWeight.Bold) }
                    )
                }
            ) { padding ->
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                    if (isProcessing) {
                        LinearProgressIndicator(color = accent, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (vaultItems.isEmpty() && !isProcessing) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Outlined.FolderSpecial, contentDescription = "Empty", tint = textMuted, modifier = Modifier.size(54.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Vault is Empty", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Pick PDF, Videos, Photos, or Docs to Encrypt", color = textMuted, fontSize = 12.sp)
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(vaultItems) { item ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = cardBg),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, border)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                                Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = accent)
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(text = item.originalName, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(text = "${item.type} • ${item.formattedSize} • Encrypted", color = textMuted, fontSize = 11.sp)
                                            }
                                        }

                                        Row {
                                            // Decrypt & Restore / Backup Button
                                            IconButton(
                                                onClick = {
                                                    scope.launch(Dispatchers.IO) {
                                                        try {
                                                            val encBytes = FileInputStream(item.file).readBytes()
                                                            val decrypted = onEncryptDecrypt(encBytes, currentPin)
                                                            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                                            val restoredFile = File(downloadDir, "RESTORED_${item.originalName}")
                                                            FileOutputStream(restoredFile).use { it.write(decrypted) }
                                                            withContext(Dispatchers.Main) {
                                                                Toast.makeText(context, "Decrypted & Saved to Downloads!", Toast.LENGTH_SHORT).show()
                                                            }
                                                        } catch (e: Exception) {
                                                            withContext(Dispatchers.Main) {
                                                                Toast.makeText(context, "Decryption error", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                }
                                            ) {
                                                Icon(imageVector = Icons.Default.CloudDownload, contentDescription = "Backup", tint = accent)
                                            }

                                            // Delete Button
                                            IconButton(
                                                onClick = {
                                                    item.file.delete()
                                                    refreshVaultList()
                                                    Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show()
                                                }
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
    }
}
