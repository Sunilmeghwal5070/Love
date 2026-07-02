package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PermissionsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Audio Permission using Accompanist
    val recordAudioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    var hasAudioPermission by remember { mutableStateOf(recordAudioPermission.status.isGranted) }
    
    // Special Permissions States
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasWriteSettingsPermission by remember { mutableStateOf(Settings.System.canWrite(context)) }
    var hasFileAccessPermission by remember { mutableStateOf(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager() else true) }
    var hasNotificationAccess by remember { 
        mutableStateOf(NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)) 
    }

    // Update statuses on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAudioPermission = recordAudioPermission.status.isGranted
                hasOverlayPermission = Settings.canDrawOverlays(context)
                hasWriteSettingsPermission = Settings.System.canWrite(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    hasFileAccessPermission = Environment.isExternalStorageManager()
                }
                hasNotificationAccess = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Will refresh on resume anyway
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Permissions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color(0xFFFFF5F8)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Grant these permissions to allow Love to fully control the device based on your commands.",
                    color = Color.DarkGray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            item {
                PermissionItem(
                    title = "Microphone Access",
                    description = "Required to listen to your voice commands.",
                    isGranted = hasAudioPermission,
                    onRequest = { recordAudioPermission.launchPermissionRequest() }
                )
            }

            item {
                PermissionItem(
                    title = "Display over other apps",
                    description = "Allows the assistant popup to appear from anywhere.",
                    isGranted = hasOverlayPermission,
                    onRequest = {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                        launcher.launch(intent)
                    }
                )
            }

            item {
                PermissionItem(
                    title = "Modify System Settings",
                    description = "Required to change settings like brightness, Wi-Fi, etc.",
                    isGranted = hasWriteSettingsPermission,
                    onRequest = {
                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
                        launcher.launch(intent)
                    }
                )
            }
            
            item {
                PermissionItem(
                    title = "All File Access",
                    description = "Required to manage files and media on your device.",
                    isGranted = hasFileAccessPermission,
                    onRequest = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))
                            launcher.launch(intent)
                        }
                    }
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Default Assistant", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF4A4A4A))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Set Love as your default digital assistant app.", fontSize = 12.sp, color = Color.Gray, lineHeight = 16.sp)
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
                                    launcher.launch(intent)
                                } catch (e: Exception) {
                                    launcher.launch(Intent(Settings.ACTION_SETTINGS))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6FA5)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Settings")
                        }
                    }
                }
            }

            item {
                PermissionItem(
                    title = "Notification Access",
                    description = "Allows reading and replying to incoming messages.",
                    isGranted = hasNotificationAccess,
                    onRequest = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        launcher.launch(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF4A4A4A))
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, fontSize = 12.sp, color = Color.Gray, lineHeight = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isGranted) Color(0xFF4ADE80) else Color(0xFFFFB6C1),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isGranted) "Granted" else "Not Granted",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGranted) Color(0xFF4ADE80) else Color(0xFFFFB6C1)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            if (!isGranted) {
                Button(
                    onClick = onRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6FA5)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Grant")
                }
            }
        }
    }
}
