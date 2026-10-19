package com.example.albumcanvas.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.albumcanvas.data.SongDetails
import com.example.albumcanvas.data.pref.SettingsData
import com.example.albumcanvas.utils.ImageProcessor

@Composable
fun HomeScreen(
    song: SongDetails?,
    status: String,
    colors: ImageProcessor.PaletteColors?,
    settings: SettingsData,
    lastUpdateStr: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Card
        StatusCard(status = status, lastUpdateStr = lastUpdateStr, colors = colors)

        // Live Preview Mockup
        WallpaperPreviewCard(song = song, settings = settings, colors = colors)

        // Metadata Card
        if (song != null) {
            SongDetailsCard(song = song)
        } else {
            NoMediaCard()
        }
    }
}

@Composable
fun StatusCard(status: String, lastUpdateStr: String, colors: ImageProcessor.PaletteColors?) {
    val isActive = status == "Playing" || status == "Active"
    val statusColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val statusIcon = if (isActive) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = "Status",
                tint = statusColor,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    text = "Service Status: $status",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Last updated: $lastUpdateStr",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun WallpaperPreviewCard(
    song: SongDetails?,
    settings: SettingsData,
    colors: ImageProcessor.PaletteColors?
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Live Wallpaper Preview",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(
            modifier = Modifier
                .width(200.dp)
                .height(340.dp)
                .clip(RoundedCornerShape(24.dp))
                .shadow(8.dp)
                .background(Color.Black)
        ) {
            // 1. Render Background (AMOLED black or linear gradient)
            if (settings.amoledBlack) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
            } else if (settings.gradientBackground && colors != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(colors.darkMuted),
                                    Color(colors.primary),
                                    Color(colors.muted)
                                )
                            )
                        )
                )
            } else if (colors != null) {
                Box(modifier = Modifier.fillMaxSize().background(Color(colors.primary)))
            } else {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
            }

            // 2. Draw Artwork on top
            if (song?.artwork != null) {
                val contentScale = when (settings.cropMode) {
                    "CROP" -> ContentScale.Crop
                    "FIT" -> ContentScale.Fit
                    "STRETCH" -> ContentScale.FillBounds
                    else -> ContentScale.Crop // FILL
                }
                
                Image(
                    bitmap = song.artwork.asImageBitmap(),
                    contentDescription = "Preview Artwork",
                    contentScale = contentScale,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (settings.blurAmount > 0) {
                                Modifier.blur(settings.blurAmount.dp / 3) // Scale blur down for preview sizing
                            } else {
                                Modifier
                            }
                        )
                )
            }

            // 3. Draw Overlay
            if (settings.overlayOpacity > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = settings.overlayOpacity))
                )
            }

            // 4. Draw Vignette
            if (settings.circularVignette) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                                radius = 400f
                            )
                        )
                )
            }

            // Simulated UI overlays
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = song?.title ?: "No Song",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song?.artist ?: "Unknown Artist",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun SongDetailsCard(song: SongDetails) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (song.artwork != null) {
                Image(
                    bitmap = song.artwork.asImageBitmap(),
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Music",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.album,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Player: ${song.appName}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun NoMediaCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "No Media",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "No Media Playing",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Start playing music in any media player application to update the wallpaper.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
