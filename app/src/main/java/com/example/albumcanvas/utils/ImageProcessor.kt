package com.example.albumcanvas.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import androidx.palette.graphics.Palette
import com.example.albumcanvas.data.pref.SettingsData
import java.util.Random

object ImageProcessor {

    /**
     * Extracts palette colors from the album artwork.
     */
    fun extractPalette(bitmap: Bitmap): PaletteColors {
        val palette = Palette.from(bitmap).generate()
        val defaultBg = 0xFF121212.toInt()
        val defaultAccent = 0xFF4CAF50.toInt()
        
        return PaletteColors(
            primary = palette.getDominantColor(defaultBg),
            accent = palette.getVibrantColor(defaultAccent),
            vibrant = palette.getVibrantColor(defaultAccent),
            muted = palette.getMutedColor(defaultBg),
            lightVibrant = palette.getLightVibrantColor(0xFFE8F5E9.toInt()),
            darkMuted = palette.getDarkMutedColor(0xFF1B5E20.toInt())
        )
    }

    data class PaletteColors(
        val primary: Int,
        val accent: Int,
        val vibrant: Int,
        val muted: Int,
        val lightVibrant: Int,
        val darkMuted: Int
    )

    /**
     * Applies the complete image processing pipeline to generate the wallpaper bitmap.
     * Note: This is used for static wallpapers or previews. In the Live Wallpaper service,
     * hardware-accelerated blur can be applied directly on the canvas using RenderEffect.
     */
    fun processWallpaper(
        artwork: Bitmap,
        screenWidth: Int,
        screenHeight: Int,
        settings: SettingsData,
        colors: PaletteColors
    ): Bitmap {
        // Step 1: Create background canvas matching screen size
        val finalBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(finalBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // Step 2: Draw Background (AMOLED black or Gradient or Muted Color)
        if (settings.amoledBlack) {
            canvas.drawColor(Color.BLACK)
        } else if (settings.gradientBackground) {
            val gradient = LinearGradient(
                0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(),
                intArrayOf(colors.darkMuted, colors.primary, colors.muted),
                null, Shader.TileMode.CLAMP
            )
            paint.shader = gradient
            canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), paint)
            paint.shader = null
        } else {
            canvas.drawColor(colors.primary)
        }

        // Step 3: Draw Album Art on top with crop/fit mode
        val destRect = calculateDestinationRect(artwork.width, artwork.height, screenWidth, screenHeight, settings.cropMode)
        
        // Apply soft drop shadow or round corners on art if in FIT/STRETCH modes
        if ((settings.cropMode == "FIT" || settings.cropMode == "STRETCH") && settings.softShadow) {
            drawShadow(canvas, destRect)
        }
        
        canvas.drawBitmap(artwork, null, destRect, paint)

        // Step 4: Apply Blur (CPU fallback / StackBlur)
        if (settings.blurAmount > 0) {
            // Apply blur on the whole wallpaper
            val blurred = stackBlur(finalBitmap, settings.blurAmount)
            canvas.drawBitmap(blurred, 0f, 0f, paint)
        }

        // Step 5: Brightness, Contrast, Saturation
        applyColorAdjustments(canvas, screenWidth, screenHeight, settings)

        // Step 6: Dim/Color Overlays
        if (settings.overlayOpacity > 0f) {
            paint.color = Color.BLACK
            paint.alpha = (settings.overlayOpacity * 255).toInt()
            canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), paint)
            paint.alpha = 255
        }

        // Step 7: Premium Effects (Noise, Duotone, Vignette)
        if (settings.noiseGrain) {
            addNoiseGrain(canvas, screenWidth, screenHeight)
        }
        if (settings.duotone) {
            applyDuotone(canvas, screenWidth, screenHeight, colors.darkMuted, colors.lightVibrant)
        }
        if (settings.circularVignette) {
            applyVignette(canvas, screenWidth, screenHeight)
        }
        if (settings.edgeFade) {
            applyEdgeFade(canvas, screenWidth, screenHeight)
        }

        return finalBitmap
    }

    private fun calculateDestinationRect(
        artWidth: Int,
        artHeight: Int,
        scrWidth: Int,
        scrHeight: Int,
        mode: String
    ): Rect {
        val artRatio = artWidth.toFloat() / artHeight
        val scrRatio = scrWidth.toFloat() / scrHeight

        return when (mode) {
            "CROP" -> {
                if (artRatio > scrRatio) {
                    val newWidth = (scrHeight * artRatio).toInt()
                    val offset = (newWidth - scrWidth) / 2
                    Rect(-offset, 0, scrWidth + offset, scrHeight)
                } else {
                    val newHeight = (scrWidth / artRatio).toInt()
                    val offset = (newHeight - scrHeight) / 2
                    Rect(0, -offset, scrWidth, scrHeight + offset)
                }
            }
            "FIT" -> {
                // Scale so that it fits completely with letterboxing
                if (artRatio > scrRatio) {
                    val newHeight = (scrWidth / artRatio).toInt()
                    val offset = (scrHeight - newHeight) / 2
                    Rect(0, offset, scrWidth, scrHeight - offset)
                } else {
                    val newWidth = (scrHeight * artRatio).toInt()
                    val offset = (scrWidth - newWidth) / 2
                    Rect(offset, 0, scrWidth - offset, scrHeight)
                }
            }
            "STRETCH" -> {
                Rect(0, 0, scrWidth, scrHeight)
            }
            else -> { // FILL - center crop (default)
                if (artRatio > scrRatio) {
                    val newWidth = (scrHeight * artRatio).toInt()
                    val offset = (newWidth - scrWidth) / 2
                    Rect(-offset, 0, scrWidth + offset, scrHeight)
                } else {
                    val newHeight = (scrWidth / artRatio).toInt()
                    val offset = (newHeight - scrHeight) / 2
                    Rect(0, -offset, scrWidth, scrHeight + offset)
                }
            }
        }
    }

    private fun drawShadow(canvas: Canvas, targetRect: Rect) {
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            alpha = 130
            maskFilter = null // Render shadow blurring
        }
        val shadowRect = RectF(
            targetRect.left + 15f,
            targetRect.top + 15f,
            targetRect.right + 15f,
            targetRect.bottom + 15f
        )
        canvas.drawRoundRect(shadowRect, 16f, 16f, shadowPaint)
    }

    private fun applyColorAdjustments(canvas: Canvas, width: Int, height: Int, settings: SettingsData) {
        // Brightness, Contrast, Saturation can be adjusted using a ColorMatrixColorFilter
        val cm = ColorMatrix()
        
        // Saturation
        cm.setSaturation(settings.saturation)
        
        // Brightness & Contrast
        // Contrast value around 1.0. Scale factors.
        val c = settings.contrast
        val b = (settings.brightness - 1.0f) * 255f
        val translateMat = floatArrayOf(
            c, 0f, 0f, 0f, b,
            0f, c, 0f, 0f, b,
            0f, 0f, c, 0f, b,
            0f, 0f, 0f, 1f, 0f
        )
        
        val contrastMatrix = ColorMatrix(translateMat)
        contrastMatrix.postConcat(cm)
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(contrastMatrix)
        }
        
        // Redraw canvas with filters applied
        val tempBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val tempCanvas = Canvas(tempBitmap)
        tempCanvas.drawBitmap(tempBitmap, 0f, 0f, paint)
    }

    private fun addNoiseGrain(canvas: Canvas, width: Int, height: Int) {
        val noisePaint = Paint().apply {
            color = Color.WHITE
            alpha = 15 // Very subtle grain
        }
        val random = Random()
        val step = 2 // pixel step to speed up
        for (x in 0 until width step step) {
            for (y in 0 until height step step) {
                if (random.nextFloat() < 0.15f) {
                    noisePaint.color = if (random.nextBoolean()) Color.WHITE else Color.BLACK
                    noisePaint.alpha = random.nextInt(12) + 3
                    canvas.drawRect(x.toFloat(), y.toFloat(), (x + step).toFloat(), (y + step).toFloat(), noisePaint)
                }
            }
        }
    }

    private fun applyDuotone(canvas: Canvas, width: Int, height: Int, colorDark: Int, colorLight: Int) {
        val r1 = Color.red(colorDark) / 255f
        val g1 = Color.green(colorDark) / 255f
        val b1 = Color.blue(colorDark) / 255f
        
        val r2 = Color.red(colorLight) / 255f
        val g2 = Color.green(colorLight) / 255f
        val b2 = Color.blue(colorLight) / 255f

        val duotoneMat = floatArrayOf(
            r2 - r1, 0f, 0f, 0f, r1 * 255f,
            g2 - g1, 0f, 0f, 0f, g1 * 255f,
            b2 - b1, 0f, 0f, 0f, b1 * 255f,
            0f, 0f, 0f, 1f, 0f
        )
        
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(duotoneMat)
        }
        
        // Draw back onto canvas with duotone filter
        val rect = Rect(0, 0, width, height)
        canvas.drawBitmap(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888), rect, rect, paint)
    }

    private fun applyVignette(canvas: Canvas, width: Int, height: Int) {
        val colors = intArrayOf(Color.TRANSPARENT, Color.argb(160, 0, 0, 0))
        val stops = floatArrayOf(0.4f, 1.0f)
        val cx = width / 2f
        val cy = height / 2f
        val radius = Math.hypot(cx.toDouble(), cy.toDouble()).toFloat()
        
        val shader = RadialGradient(cx, cy, radius, colors, stops, Shader.TileMode.CLAMP)
        val paint = Paint().apply {
            this.shader = shader
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    private fun applyEdgeFade(canvas: Canvas, width: Int, height: Int) {
        val edgeWidth = width * 0.15f
        val edgeHeight = height * 0.15f
        val paint = Paint()

        // Top edge fade
        paint.shader = LinearGradient(0f, 0f, 0f, edgeHeight, Color.BLACK, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        canvas.drawRect(0f, 0f, width.toFloat(), edgeHeight, paint)

        // Bottom edge fade
        paint.shader = LinearGradient(0f, height.toFloat(), 0f, height - edgeHeight, Color.BLACK, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, height - edgeHeight, width.toFloat(), height.toFloat(), paint)

        paint.xfermode = null
    }

    /**
     * Stack Blur Algorithm by Mario Klingemann.
     * High performance, CPU-based image blurring.
     */
    fun stackBlur(sentBitmap: Bitmap, radius: Int): Bitmap {
        var r = radius
        if (r < 1) return sentBitmap
        
        // Scale down the bitmap for blurring to optimize performance and prevent memory spikes
        val scaleFactor = when {
            r > 80 -> 16
            r > 40 -> 8
            r > 20 -> 4
            else -> 2
        }
        r /= scaleFactor
        if (r < 1) r = 1

        val width = sentBitmap.width / scaleFactor
        val height = sentBitmap.height / scaleFactor
        val bitmap = Bitmap.createScaledBitmap(sentBitmap, width, height, false)

        val w = bitmap.width
        val h = bitmap.height

        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = r + r + 1

        val r8 = IntArray(wh)
        val g8 = IntArray(wh)
        val b8 = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int

        val vmin = IntArray(Math.max(w, h))
        val dv = IntArray(256 * div)
        for (i in 0 until 256 * div) {
            dv[i] = i / div
        }

        yw = 0
        yi = 0

        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = r + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int

        for (y in 0 until h) {
            bsum = 0
            gsum = bsum
            rsum = gsum
            boutsum = rsum
            goutsum = boutsum
            routsum = goutsum
            binsum = routsum
            ginsum = binsum
            rinsum = ginsum
            for (i in -r..r) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))]
                sir = stack[i + r]
                sir[0] = p shr 16 and 0xff
                sir[1] = p shr 8 and 0xff
                sir[2] = p and 0xff
                val rbs = r1 - Math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
            }
            stackpointer = r

            for (x in 0 until w) {
                r8[yi] = dv[rsum]
                g8[yi] = dv[gsum]
                b8[yi] = dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - r + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (y == 0) {
                    vmin[x] = Math.min(x + r1, wm)
                }
                p = pix[yw + vmin[x]]

                sir[0] = p shr 16 and 0xff
                sir[1] = p shr 8 and 0xff
                sir[2] = p and 0xff

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi++
            }
            yw += w
        }

        for (x in 0 until w) {
            bsum = 0
            gsum = bsum
            rsum = gsum
            boutsum = rsum
            goutsum = boutsum
            routsum = goutsum
            binsum = routsum
            ginsum = binsum
            rinsum = ginsum
            yp = -r * w
            for (i in -r..r) {
                yi = Math.max(0, yp) + x
                sir = stack[i + r]
                sir[0] = r8[yi]
                sir[1] = g8[yi]
                sir[2] = b8[yi]
                val rbs = r1 - Math.abs(i)
                rsum += r8[yi] * rbs
                gsum += g8[yi] * rbs
                bsum += b8[yi] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                yp += w
            }
            yi = x
            stackpointer = r
            for (y in 0 until h) {
                pix[yi] = -0x1000000 and pix[yi] or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - r + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w
                }
                p = x + vmin[y]

                sir[0] = r8[p]
                sir[1] = g8[p]
                sir[2] = b8[p]

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi += w
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        
        // Scale back up
        val finalBlurred = Bitmap.createScaledBitmap(bitmap, sentBitmap.width, sentBitmap.height, true)
        bitmap.recycle()
        return finalBlurred
    }
}
