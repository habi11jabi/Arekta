package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AppleBlue
import com.example.util.GitHubBackupConfig
import com.example.util.GitHubBackupManager
import com.example.util.ParsedBackupData
import kotlinx.coroutines.launch

@Composable
fun GitHubSyncDialog(
    lastSyncTime: Long,
    lastCommitSha: String,
    lastSyncStatus: String,
    isSyncing: Boolean,
    onDismiss: () -> Unit,
    onPushNow: () -> Unit,
    onFetchAndRestore: () -> Unit,
    onSaveConfig: (GitHubBackupConfig) -> Unit,
    onRestoreRawJson: (ParsedBackupData) -> Unit,
    currentFullBackupJson: () -> String
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val initialConfig = remember { GitHubBackupManager.loadConfig(context) }
    var token by remember { mutableStateOf(initialConfig.token) }
    var owner by remember { mutableStateOf(initialConfig.owner) }
    var repo by remember { mutableStateOf(initialConfig.repo) }
    var branch by remember { mutableStateOf(initialConfig.branch) }
    var path by remember { mutableStateOf(initialConfig.path) }
    var autoSyncEnabled by remember { mutableStateOf(initialConfig.autoSyncEnabled) }
    var isTokenVisible by remember { mutableStateOf(false) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testResultMsg by remember { mutableStateOf<String?>(null) }
    var testResultSuccess by remember { mutableStateOf<Boolean?>(null) }

    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showManualJsonSection by remember { mutableStateOf(false) }
    var manualJsonInput by remember { mutableStateOf("") }
    var manualJsonError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF24292F), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "GitHub",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "GitHub.com ক্লাউড ব্যাকআপ",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "স্বয়ংক্রিয় ডাটা সংরক্ষণ ও সিঙ্ক",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_github_sync_dialog")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Sync Status Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (lastSyncTime > 0L) Color(0xFFECFDF5) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (lastSyncTime > 0L) Icons.Default.CheckCircle else Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = if (lastSyncTime > 0L) Color(0xFF059669) else AppleBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "সর্বশেষ GitHub ব্যাকআপ:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = GitHubBackupManager.formatSyncTime(lastSyncTime, isBangla = true),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (lastSyncTime > 0L) Color(0xFF065F46) else MaterialTheme.colorScheme.onSurface
                            )
                            if (lastCommitSha.isNotBlank()) {
                                Text(
                                    text = "Commit: ${if (lastCommitSha.length >= 7) lastCommitSha.substring(0, 7) else lastCommitSha}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (lastSyncStatus.isNotBlank()) {
                                Text(
                                    text = lastSyncStatus,
                                    fontSize = 11.sp,
                                    color = if (lastSyncStatus.contains("সফল")) Color(0xFF059669) else Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }

                // GitHub Settings Form Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "GitHub একাউন্ট ও রিপোসিটরি কনফিগারেশন:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Personal Access Token
                        OutlinedTextField(
                            value = token,
                            onValueChange = {
                                token = it
                                testResultMsg = null
                            },
                            label = { Text("Personal Access Token (PAT)", fontSize = 12.sp) },
                            placeholder = { Text("ghp_... অথবা github_pat_...", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Key, contentDescription = "Token", tint = AppleBlue)
                            },
                            trailingIcon = {
                                IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                                    Icon(
                                        imageVector = if (isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle visibility"
                                    )
                                }
                            },
                            visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("github_token_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Owner / Username
                        OutlinedTextField(
                            value = owner,
                            onValueChange = {
                                owner = it
                                testResultMsg = null
                            },
                            label = { Text("GitHub Username / Owner", fontSize = 12.sp) },
                            placeholder = { Text("যেমন: habi11jabi", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Person, contentDescription = "Owner", tint = AppleBlue)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("github_owner_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Repository Name
                        OutlinedTextField(
                            value = repo,
                            onValueChange = {
                                repo = it
                                testResultMsg = null
                            },
                            label = { Text("Repository Name", fontSize = 12.sp) },
                            placeholder = { Text("যেমন: apple-area-backup", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Folder, contentDescription = "Repo", tint = AppleBlue)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("github_repo_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Branch & Path
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = branch,
                                onValueChange = { branch = it },
                                label = { Text("Branch", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(0.4f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = path,
                                onValueChange = { path = it },
                                label = { Text("Backup File Path", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(0.6f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        // Auto-sync switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "স্বয়ংক্রিয় ব্যাকআপ (Auto-Save)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "ডাটা পরিবর্তন হলেই GitHub এ স্বয়ংক্রিয়ভাবে পুশ হবে",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoSyncEnabled,
                                onCheckedChange = { autoSyncEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = AppleBlue)
                            )
                        }

                        // Connection test feedback
                        testResultMsg?.let { msg ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (testResultSuccess == true) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = msg,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (testResultSuccess == true) Color(0xFF166534) else Color(0xFF991B1B),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        // Buttons: Test Connection & Save Config
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val cfg = GitHubBackupConfig(
                                        token = token,
                                        owner = owner,
                                        repo = repo,
                                        path = path,
                                        branch = branch,
                                        autoSyncEnabled = autoSyncEnabled
                                    )
                                    isTestingConnection = true
                                    testResultMsg = null
                                    coroutineScope.launch {
                                        val res = GitHubBackupManager.testConnection(cfg)
                                        isTestingConnection = false
                                        res.onSuccess {
                                            testResultSuccess = true
                                            testResultMsg = it
                                        }.onFailure {
                                            testResultSuccess = false
                                            testResultMsg = it.message ?: "সংযোগ ব্যর্থ হয়েছে।"
                                        }
                                    }
                                },
                                enabled = !isTestingConnection && token.isNotBlank() && owner.isNotBlank() && repo.isNotBlank(),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_test_github_connection"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isTestingConnection) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text("সংযোগ পরীক্ষা", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    val cfg = GitHubBackupConfig(
                                        token = token,
                                        owner = owner,
                                        repo = repo,
                                        path = path,
                                        branch = branch,
                                        autoSyncEnabled = autoSyncEnabled
                                    )
                                    onSaveConfig(cfg)
                                    Toast.makeText(context, "GitHub সেটিংস সংরক্ষিত হয়েছে", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_save_github_config"),
                                colors = ButtonDefaults.buttonColors(containerColor = AppleBlue),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("সেটিংস সেভ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Direct Push & Restore Actions Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "ম্যানুয়াল পুশ ও রিস্টোর:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Push Now Button
                        Button(
                            onClick = onPushNow,
                            enabled = !isSyncing && token.isNotBlank() && owner.isNotBlank() && repo.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_push_github_now"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24292F)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("GitHub এ সেভ হচ্ছে...", fontSize = 13.sp)
                            } else {
                                Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("এখনই GitHub এ ব্যাকআপ পাঠান", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Restore from GitHub Button
                        OutlinedButton(
                            onClick = { showRestoreConfirmDialog = true },
                            enabled = !isSyncing && token.isNotBlank() && owner.isNotBlank() && repo.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_restore_from_github"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp), tint = AppleBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("GitHub থেকে ডাটা রিস্টোর করুন", fontSize = 13.sp, color = AppleBlue, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Offline & Manual JSON Section Toggle
                OutlinedButton(
                    onClick = { showManualJsonSection = !showManualJsonSection },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (showManualJsonSection) "▲ অফলাইন JSON ব্যাকআপ বন্ধ করুন" else "▼ অফলাইন JSON কপি / ম্যানুয়াল রিস্টোর",
                        fontSize = 12.sp
                    )
                }

                if (showManualJsonSection) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "ম্যানুয়াল JSON ব্যাকআপ কপি ও পেস্ট:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // Copy current JSON to clipboard
                            Button(
                                onClick = {
                                    val json = currentFullBackupJson()
                                    clipboardManager.setText(AnnotatedString(json))
                                    Toast.makeText(context, "সম্পূর্ণ ব্যাকআপ JSON ক্লিপবোর্ডে কপি হয়েছে!", Toast.LENGTH_LONG).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                            ) {
                                Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("বর্তমান ব্যাকআপ JSON কপি করুন", fontSize = 11.sp)
                            }

                            HorizontalDivider()

                            Text(
                                text = "JSON টেক্সট থেকে ডাটা রিস্টোর করুন:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = manualJsonInput,
                                onValueChange = {
                                    manualJsonInput = it
                                    manualJsonError = null
                                },
                                placeholder = { Text("এখানে ব্যাকআপ JSON পেস্ট করুন...", fontSize = 11.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .testTag("github_manual_json_input"),
                                shape = RoundedCornerShape(8.dp)
                            )

                            manualJsonError?.let { err ->
                                Text(text = err, color = Color(0xFFDC2626), fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    if (manualJsonInput.isBlank()) {
                                        manualJsonError = "অনুগ্রহ করে JSON টেক্সট পেস্ট করুন।"
                                        return@Button
                                    }
                                    val parsed = GitHubBackupManager.parseBackupJson(manualJsonInput.trim())
                                    if (parsed == null) {
                                        manualJsonError = "JSON ফরম্যাট সঠিক নয় বা ডাটা করাপ্ট।"
                                    } else {
                                        onRestoreRawJson(parsed)
                                        Toast.makeText(context, "ডাটা সফলভাবে রিস্টোর হয়েছে!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB45309))
                            ) {
                                Text("পেস্টকৃত JSON রিস্টোর করুন", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog before restoring from GitHub
    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626)) },
            title = { Text("GitHub থেকে ডাটা রিস্টোর করবেন?") },
            text = {
                Text("রিস্টোর করলে অ্যাপের বর্তমান তথ্যের সাথে GitHub এর ব্যাকআপ প্রতিস্থাপিত হবে। আপনি কি নিশ্চিত?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        onFetchAndRestore()
                    }
                ) {
                    Text("হ্যাঁ, রিস্টোর করুন", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}
