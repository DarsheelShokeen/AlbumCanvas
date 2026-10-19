package com.example.albumcanvas.data.pref

import kotlinx.serialization.Serializable

@Serializable
data class SettingsData(
    val blurAmount: Int = 20,
    val brightness: Float = 1.0f,
    val contrast: Float = 1.0f,
    val saturation: Float = 1.0f,
    val overlayOpacity: Float = 0.2f,
    val cropMode: String = "CROP", // CROP, FIT, FILL, STRETCH
    val wallpaperTarget: String = "BOTH", // HOME, LOCK, BOTH
    val animationSpeed: Int = 600, // Duration in ms (100 - 3000)
    val transitionEffect: String = "CROSSFADE", // CROSSFADE, ZOOM_IN, ZOOM_OUT, BLUR_FADE, KEN_BURNS, SLIDE, SCALE, NONE
    val amoledBlack: Boolean = false,
    val gradientBackground: Boolean = true,
    val dynamicMaterialYou: Boolean = true,
    val delayMs: Int = 500, // Update stabilization delay in ms
    val ignoreAds: Boolean = true,
    
    // Premium image customization
    val glassBlur: Boolean = false,
    val acrylicBlur: Boolean = false,
    val noiseGrain: Boolean = false,
    val frostedGlass: Boolean = false,
    val monochrome: Boolean = false,
    val duotone: Boolean = false,
    val circularVignette: Boolean = false,
    val edgeFade: Boolean = false,
    val softShadow: Boolean = false,
    
    // System settings
    val isServiceEnabled: Boolean = true,
    val darkMode: Boolean = true
)
