// FILE: app/src/main/java/com/masud/moneymanager/ui/UpdateCard.kt
package com.masud.moneymanager.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masud.moneymanager.update.AppUpdateManager
import com.masud.moneymanager.update.UpdateInfo
import kotlinx.coroutines.launch
import java.io.File

private val UpdateGreen = Color(0xFF1ED760)

private sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val info: UpdateInfo) : UpdateUiState
    data class Downloading(val progress: Int) : UpdateUiState
    data class Downloaded(val file: File, val info: UpdateInfo) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

@Composable
fun UpdateCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<UpdateUiState>(UpdateUiState.Idle) }
    val currentVersion = remember { AppUpdateManager.currentVersionName(context) }

    fun check() {
        state = UpdateUiState.Checking
        scope.launch {
            val result = AppUpdateManager.checkForUpdate(context)
            state = result.fold(
                onSuccess = { info ->
                    if (info == null) UpdateUiState.UpToDate else UpdateUiState.Available(info)
                },
                onFailure = { UpdateUiState.Error(it.message ?: "Check failed") },
            )
        }
    }

    fun download(info: UpdateInfo) {
        state = UpdateUiState.Downloading(0)
        scope.launch {
            val result = AppUpdateManager.downloadApk(context, info) { p ->
                state = UpdateUiState.Downloading(p)
            }
            state = result.fold(
                onSuccess = { UpdateUiState.Downloaded(it, info) },
                onFailure = { UpdateUiState.Error(it.message ?: "Download failed") },
            )
        }
    }

    fun install(file: File) {
        if (!AppUpdateManager.canInstall(context)) {
            AppUpdateManager.openUnknownSourcesSettings(context)
            return
        }
        AppUpdateManager.installApk(context, file)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("App updates", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "Current version $currentVersion",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            when (val s = state) {
                is UpdateUiState.Idle -> {
                    Text(
                        "Check GitHub releases for a newer version.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                is UpdateUiState.Checking -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            "Checking for updates…",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                is UpdateUiState.UpToDate -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = UpdateGreen,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "You're on the latest version.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                is UpdateUiState.Available -> {
                    Text(
                        "Version ${s.info.versionLabel} is available",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (s.info.releaseNotes.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            s.info.releaseNotes.lines().take(4).joinToString("\n"),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 4,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { download(s.info) },
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("Download & install", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                is UpdateUiState.Downloading -> {
                    Text(
                        "Downloading update… ${s.progress}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { s.progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }

                is UpdateUiState.Downloaded -> {
                    Text(
                        "Version ${s.info.versionLabel} downloaded.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { install(s.file) },
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(
                            if (AppUpdateManager.canInstall(context)) "Install update"
                            else "Allow installs & continue",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        )
                    }
                    if (!AppUpdateManager.canInstall(context)) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Android needs your permission to install updates from this app — you'll be taken to settings once.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                is UpdateUiState.Error -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            s.message,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            AnimatedVisibility(visible = state !is UpdateUiState.Checking && state !is UpdateUiState.Downloading) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { check() },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Check for updates", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
