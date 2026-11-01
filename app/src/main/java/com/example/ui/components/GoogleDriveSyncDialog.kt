package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppleBlue
import com.example.util.GoogleDriveSyncManager
import com.example.util.ParsedBackupData

@Composable
fun GoogleDriveSyncDialog(
    lastSyncTime: Long,
    isSyncing: Boolean,
    onDismiss: () -> Unit,
    onSyncNow: () -> Unit,
    onRestoreBackup: (ParsedBackupData) -> Unit
) {
    val context = LocalContext.current
    var showRestoreInput by remember { mutableStateOf(false) }
    var restoreJsonText by remember { mutableStateOf("") }
    var restoreError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = AppleBlue.copy(alpha = 0.12f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = AppleBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Google Drive সিঙ্ক ও ব্যাকআপ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "ক্লাউড ডেটা ব্যাকআপ ও রিস্টোর",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // OAuth Cloud Connection Status
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Google Drive OAuth সংযুক্ত",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857),
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Drive Scope: drive.file সক্রিয়",
                                color = Color(0xFF047857).copy(alpha = 0.8f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Last Sync Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "সর্বশেষ সিঙ্ক সময়:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = GoogleDriveSyncManager.formatSyncTime(lastSyncTime, isBangla = true),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (lastSyncTime > 0) Color(0xFF0F766E) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = if (lastSyncTime > 0) Color(0xFF0F766E) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(
                    text = "দোকানের সকল স্টক পণ্য, বিক্রয় মেমো, খরচের খাতা এবং কর্মচারীর তথ্য ব্যাকআপ হিসেবে গুগল ড্রাইভে নিরাপদ থাকবে।",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!showRestoreInput) {
                    // Sync Now Button
                    Button(
                        onClick = onSyncNow,
                        enabled = !isSyncing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("btn_sync_now_drive"),
                        colors = ButtonDefaults.buttonColors(containerColor = AppleBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ড্রাইভে সিঙ্ক হচ্ছে...", fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("এখনই Google ড্রাইভে সিঙ্ক ও সেভ করুন", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Restore Option Toggle
                    OutlinedButton(
                        onClick = { showRestoreInput = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("btn_toggle_restore_drive"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ড্রাইভ ব্যাকআপ থেকে রিস্টোর করুন", fontSize = 12.sp)
                    }
                } else {
                    // Restore JSON Input Form
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Google Drive থেকে ডাউনলোডকৃত ব্যাকআপ ফাইলটির কন্টেন্ট এখানে পেস্ট করুন:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = restoreJsonText,
                            onValueChange = {
                                restoreJsonText = it
                                restoreError = null
                            },
                            placeholder = { Text("{\n  \"app\": \"APPLE AREA BD\",\n  \"products\": [...]\n}", fontSize = 11.sp) },
                            minLines = 4,
                            maxLines = 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("drive_restore_json_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        if (restoreError != null) {
                            Text(
                                text = restoreError!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showRestoreInput = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("ফিরে যান", fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    val parsed = GoogleDriveSyncManager.parseBackupJson(restoreJsonText)
                                    if (parsed != null) {
                                        onRestoreBackup(parsed)
                                        showRestoreInput = false
                                        onDismiss()
                                    } else {
                                        restoreError = "ভুল ফাইল ফরম্যাট! সঠিক JSON ব্যাকআপ পেস্ট করুন।"
                                    }
                                },
                                enabled = restoreJsonText.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_confirm_drive_restore")
                            ) {
                                Text("রিস্টোর নিশ্চিত", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!showRestoreInput) {
                TextButton(onClick = onDismiss) {
                    Text("বন্ধ করুন")
                }
            }
        }
    )
}
