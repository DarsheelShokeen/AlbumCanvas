package com.example.albumcanvas.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.albumcanvas.data.pref.SettingsData

@Composable
fun CustomizationScreen(
    settings: SettingsData,
    onSettingsChanged: (SettingsData) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Artwork Customization",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Wallpaper Target Section
        SectionTitle("Wallpaper Target")
        WallpaperTargetSelector(
            selectedTarget = settings.wallpaperTarget,
            onTargetSelected = { onSettingsChanged(settings.copy(wallpaperTarget = it)) }
        )

        // Sizing & Fit Modes
        SectionTitle("Sizing Mode")
        FitModeSelector(
            selectedMode = settings.cropMode,
            onModeSelected = { onSettingsChanged(settings.copy(cropMode = it)) }
        )

        // Color & Adjustments Sliders
        SectionTitle("Image Adjustments")
        
        // Blur
        SliderItem(
            label = "Blur Amount",
            value = settings.blurAmount.toFloat(),
            valueRange = 0f..100f,
            steps = 100,
            displayValue = "${settings.blurAmount}",
            onValueChange = { onSettingsChanged(settings.copy(blurAmount = it.toInt())) }
        )

        // Brightness
        SliderItem(
            label = "Brightness",
            value = settings.brightness,
            valueRange = 0.5f..1.5f,
            steps = 20,
            displayValue = String.format("%.2f", settings.brightness),
            onValueChange = { onSettingsChanged(settings.copy(brightness = it)) }
        )

        // Contrast
        SliderItem(
            label = "Contrast",
            value = settings.contrast,
            valueRange = 0.5f..1.5f,
            steps = 20,
            displayValue = String.format("%.2f", settings.contrast),
            onValueChange = { onSettingsChanged(settings.copy(contrast = it)) }
        )

        // Saturation
        SliderItem(
            label = "Saturation",
            value = settings.saturation,
            valueRange = 0.0f..2.0f,
            steps = 20,
            displayValue = String.format("%.2f", settings.saturation),
            onValueChange = { onSettingsChanged(settings.copy(saturation = it)) }
        )

        // Dim Overlay
        SliderItem(
            label = "Dim Overlay Opacity",
            value = settings.overlayOpacity,
            valueRange = 0.0f..0.9f,
            steps = 18,
            displayValue = "${(settings.overlayOpacity * 100).toInt()}%",
            onValueChange = { onSettingsChanged(settings.copy(overlayOpacity = it)) }
        )

        // Background Style
        SectionTitle("Background Aesthetics")
        ToggleItem(
            label = "AMOLED Black Background",
            checked = settings.amoledBlack,
            onCheckedChange = { onSettingsChanged(settings.copy(amoledBlack = it, gradientBackground = if (it) false else settings.gradientBackground)) }
        )
        ToggleItem(
            label = "Dynamic Color Gradient Background",
            checked = settings.gradientBackground,
            onCheckedChange = { onSettingsChanged(settings.copy(gradientBackground = it, amoledBlack = if (it) false else settings.amoledBlack)) }
        )

        // Transition Effects
        SectionTitle("Transitions (Live Wallpaper Only)")
        TransitionEffectSelector(
            selectedEffect = settings.transitionEffect,
            onEffectSelected = { onSettingsChanged(settings.copy(transitionEffect = it)) }
        )

        SliderItem(
            label = "Transition Duration",
            value = settings.animationSpeed.toFloat(),
            valueRange = 100f..3000f,
            steps = 29,
            displayValue = "${settings.animationSpeed} ms",
            onValueChange = { onSettingsChanged(settings.copy(animationSpeed = it.toInt())) }
        )

        // Premium Filters / Styling Options
        SectionTitle("Premium Image Effects")
        ToggleItem(
            label = "Noise / Film Grain",
            checked = settings.noiseGrain,
            onCheckedChange = { onSettingsChanged(settings.copy(noiseGrain = it)) }
        )
        ToggleItem(
            label = "Duotone Styling",
            checked = settings.duotone,
            onCheckedChange = { onSettingsChanged(settings.copy(duotone = it)) }
        )
        ToggleItem(
            label = "Circular Vignette",
            checked = settings.circularVignette,
            onCheckedChange = { onSettingsChanged(settings.copy(circularVignette = it)) }
        )
        ToggleItem(
            label = "Soft Drop Shadow on Art",
            checked = settings.softShadow,
            onCheckedChange = { onSettingsChanged(settings.copy(softShadow = it)) }
        )
        ToggleItem(
            label = "Top & Bottom Edge Fade",
            checked = settings.edgeFade,
            onCheckedChange = { onSettingsChanged(settings.copy(edgeFade = it)) }
        )
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun SliderItem(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    displayValue: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 14.sp)
            Text(text = displayValue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            valueRange = valueRange,
            steps = steps,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ToggleItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun WallpaperTargetSelector(
    selectedTarget: String,
    onTargetSelected: (String) -> Unit
) {
    val targets = listOf("HOME" to "Home Screen", "LOCK" to "Lock Screen", "BOTH" to "Both Screens")
    Column(Modifier.selectableGroup()) {
        targets.forEach { (key, label) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .selectable(
                        selected = (selectedTarget == key),
                        onClick = { onTargetSelected(key) },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (selectedTarget == key),
                    onClick = null // handled by row click
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

@Composable
fun FitModeSelector(
    selectedMode: String,
    onModeSelected: (String) -> Unit
) {
    val modes = listOf("CROP" to "Center Crop", "FIT" to "Fit Screen", "STRETCH" to "Stretch Fill")
    Column(Modifier.selectableGroup()) {
        modes.forEach { (key, label) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .selectable(
                        selected = (selectedMode == key),
                        onClick = { onModeSelected(key) },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (selectedMode == key),
                    onClick = null
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransitionEffectSelector(
    selectedEffect: String,
    onEffectSelected: (String) -> Unit
) {
    val effects = listOf(
        "CROSSFADE" to "Crossfade",
        "ZOOM_IN" to "Zoom In",
        "ZOOM_OUT" to "Zoom Out",
        "BLUR_FADE" to "Blur Fade",
        "KEN_BURNS" to "Ken Burns",
        "SLIDE" to "Slide",
        "SCALE" to "Scale",
        "NONE" to "None (Instant)"
    )
    
    var expanded = remember { androidx.compose.runtime.mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded.value,
        onExpandedChange = { expanded.value = !expanded.value }
    ) {
        val selectedLabel = effects.firstOrNull { it.first == selectedEffect }?.second ?: "Crossfade"
        
        TextField(
            readOnly = true,
            value = selectedLabel,
            onValueChange = {},
            label = { Text("Transition Effect") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded.value) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false }
        ) {
            effects.forEach { (key, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onEffectSelected(key)
                        expanded.value = false
                    }
                )
            }
        }
    }
}
