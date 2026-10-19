package com.example.albumcanvas.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.albumcanvas.data.pref.SettingsData
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settings: SettingsData,
    onSettingsChanged: (SettingsData) -> Unit,
    onClearCache: () -> Unit,
    onResetDefaults: () -> Unit,
    onExportSettings: ((String) -> Unit) -> Unit,
    onImportSettings: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // General Section
        SectionTitle("General Preferences")
        ToggleItem(
            label = "Enabled Wallpaper Service",
            checked = settings.isServiceEnabled,
            onCheckedChange = { onSettingsChanged(settings.copy(isServiceEnabled = it)) }
        )
        ToggleItem(
            label = "Ignore Advertisements",
            checked = settings.ignoreAds,
            onCheckedChange = { onSettingsChanged(settings.copy(ignoreAds = it)) }
        )
        ToggleItem(
            label = "Force Dark Mode",
            checked = settings.darkMode,
            onCheckedChange = { onSettingsChanged(settings.copy(darkMode = it)) }
        )
        ToggleItem(
            label = "Dynamic UI Material Theme",
            checked = settings.dynamicMaterialYou,
            onCheckedChange = { onSettingsChanged(settings.copy(dynamicMaterialYou = it)) }
        )

        // System Setups
        SectionTitle("System Configuration")
        
        Button(
            onClick = { openNotificationListenerSettings(context) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Setup Notification Listener Access")
        }

        Button(
            onClick = { openBatteryOptimizationSettings(context) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text("Disable Battery Optimization")
        }

        // Cache Management
        SectionTitle("Cache Management")
        OutlinedButton(
            onClick = {
                onClearCache()
                Toast.makeText(context, "Artwork Cache Cleared", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Clear Cached Artworks")
        }

        // Backup/Restore
        SectionTitle("Backup & Restore Settings")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    onExportSettings { json ->
                        // Simple copy to clipboard or toast for export demo
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("AlbumCanvasSettings", json)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Settings copied to Clipboard", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Export JSON")
            }
            
            Button(
                onClick = {
                    // Try to read clipboard to import
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val item = clipboard.primaryClip?.getItemAt(0)
                    val text = item?.text?.toString() ?: ""
                    if (text.isNotBlank()) {
                        onImportSettings(text)
                        Toast.makeText(context, "Settings Imported Successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "No valid settings on Clipboard", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("Import Clipboard")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Reset
        OutlinedButton(
            onClick = {
                onResetDefaults()
                Toast.makeText(context, "Defaults Restored", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Restore Factory Defaults")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Developer Credit
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "AlbumCanvas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "v1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Developed by Darsheel Shokeen",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun openNotificationListenerSettings(context: Context) {
    val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
    context.startActivity(intent)
}

private fun openBatteryOptimizationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback to general battery list if direct request fails
        val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        context.startActivity(fallback)
    }
}
