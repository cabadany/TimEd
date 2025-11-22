package com.example.timed_mobile.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import com.example.timed_mobile.R
import kotlin.math.sin
import kotlin.random.Random

/**
 * A subtle animated "lava lamp" style background. Draws a few soft radial blobs
 * that drift and gently change size, layered with additive alpha.
 */
class LavaLampView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class Blob(
        var x: Float,
        var y: Float,
        var baseRadius: Float,
        var velocityX: Float,
        var velocityY: Float,
        var radiusPhase: Float,
        val colorStart: Int,
        val colorEnd: Int,
        val radiusVariance: Float,
        val frequency: Float
    )

    private val blobs = mutableListOf<Blob>()
    private val blobCount = 5
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var animator: ValueAnimator? = null
    private var lastFrameTimeNs: Long = 0L

    private val rnd = Random(System.currentTimeMillis())

    private val blendMode = if (android.os.Build.VERSION.SDK_INT >= 29) BlendMode.SRC_OVER else null

    private val primaryMedium = fetchColor("primary_medium_blue", Color.parseColor("#0288D1"))
    private val primaryLight = fetchColor("primary_light_sky", Color.parseColor("#B3E5FC"))
    private val primaryDeep = fetchColor("primary_deep_blue", Color.parseColor("#0D0084"))

    // Rounded corner + edge fade helpers
    private val clipPath = Path()
    private val rectF = RectF()
    private var cornerRadiusPx: Float = dp(26f)
    private var roundBottomCorners: Boolean = false
    // Removed glass sheen and edge fade paints to keep colors vivid
    // (no base gradient) the live blobs will draw directly; keep view lightweight

    // Solid base fill (no gradient) to avoid transparency holes and keep color present
    private val baseFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        // Use a slightly darkened variant of primaryDeep to let blobs pop on top
        color = boostVibrance(primaryDeep, 1.0f, 0.55f)
    }

    // Vibrance helpers
    private fun boostVibrance(color: Int, satMult: Float, valMult: Float): Int {
        val a = Color.alpha(color)
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[1] = (hsv[1] * satMult).coerceIn(0f, 1f) // saturation
        hsv[2] = (hsv[2] * valMult).coerceIn(0f, 1f) // value/brightness
        return Color.HSVToColor(a, hsv)
    }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.LavaLampView)
            cornerRadiusPx = a.getDimension(R.styleable.LavaLampView_cornerRadius, cornerRadiusPx)
            roundBottomCorners = a.getBoolean(R.styleable.LavaLampView_roundBottomCorners, roundBottomCorners)
            a.recycle()
        }
    }

    private fun fetchColor(name: String, fallback: Int): Int {
        val resId = resources.getIdentifier(name, "color", context.packageName)
        return if (resId != 0) resources.getColor(resId, context.theme) else fallback
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (blobs.isEmpty()) initBlobs(width.takeIf { it > 0 } ?: 1080,
            height.takeIf { it > 0 } ?: 600)
        startAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }

    private fun initBlobs(w: Int, h: Int) {
        blobs.clear()
        repeat(blobCount) { i ->
            val baseR = (minOf(w, h) * (0.30f + 0.15f * rnd.nextFloat()))
            val vx =
                (w * 0.0045f + w * 0.0015f * rnd.nextFloat()) * if (rnd.nextBoolean()) 1 else -1
            val vy =
                (h * 0.0045f + h * 0.0015f * rnd.nextFloat()) * if (rnd.nextBoolean()) 1 else -1
            val colorA = when (i % 3) {
                0 -> primaryMedium
                1 -> primaryLight
                else -> primaryDeep
            }
            val colorB = when (i % 3) {
                0 -> primaryLight
                1 -> primaryMedium
                else -> primaryMedium
            }
            blobs += Blob(
                x = rnd.nextFloat() * w,
                y = rnd.nextFloat() * h,
                baseRadius = baseR,
                velocityX = vx,
                velocityY = vy,
                radiusPhase = rnd.nextFloat() * (Math.PI.toFloat() * 2f),
                colorStart = colorA,
                colorEnd = colorB,
                radiusVariance = baseR * (0.08f + rnd.nextFloat() * 0.12f),
                frequency = 0.28f + rnd.nextFloat() * 0.22f
            )
        }
    }

    private fun startAnimation() {
        if (animator != null) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 20_000L
            repeatMode = ValueAnimator.RESTART
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { invalidate() }
            start()
        }
        lastFrameTimeNs = System.nanoTime()
    }

    private fun stopAnimation() {
        animator?.cancel()
        animator = null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0 && blobs.isEmpty()) initBlobs(w, h)
        rectF.set(0f, 0f, w.toFloat(), h.toFloat())
        clipPath.reset()
        val br = if (roundBottomCorners) cornerRadiusPx else 0f
        val radii = floatArrayOf(
            cornerRadiusPx, cornerRadiusPx, // top-left
            cornerRadiusPx, cornerRadiusPx, // top-right
            br, br, // bottom-right
            br, br  // bottom-left
        )
        clipPath.addRoundRect(rectF, radii, Path.Direction.CW)

        // No persistent base gradient — the lava lamp will show only the animated blobs.
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (blobs.isEmpty()) return

        val now = System.nanoTime()
        val deltaSec = ((now - lastFrameTimeNs).coerceAtMost(66_000_000L)) / 1_000_000_000f
        lastFrameTimeNs = now

        val save = canvas.save()
        // Clip to rounded rect so blobs don't show a hard edge crop.
        canvas.clipPath(clipPath)

        // Simple solid base (no gradients)
        canvas.drawRect(rectF, baseFillPaint)

    // Update & draw each blob (vibrant colors)
        blobs.forEachIndexed { index, b ->
            b.x += b.velocityX * deltaSec
            b.y += b.velocityY * deltaSec

            // wrap softly (wrap instead of bounce for smoother drift)
            if (b.x < -b.baseRadius) b.x = width + b.baseRadius
            if (b.x > width + b.baseRadius) b.x = -b.baseRadius
            if (b.y < -b.baseRadius) b.y = height + b.baseRadius
            if (b.y > height + b.baseRadius) b.y = -b.baseRadius

            b.radiusPhase += b.frequency * deltaSec
            val dynamicR = (b.baseRadius + sin(b.radiusPhase) * b.radiusVariance)

            // slow hue shift between start/end using an additional temporal blend
            val timeBlend = ((System.currentTimeMillis() / 1000f) * 0.015f + index * 0.17f) % 1f
            val blendedEdge = lerpColor(b.colorEnd, b.colorStart, timeBlend)
            val startV = boostVibrance(b.colorStart, 1.14f, 1.08f)
            val edgeV  = boostVibrance(blendedEdge, 1.08f, 1.06f)

            val gradient = RadialGradient(
                b.x, b.y, dynamicR,
                startV.modifyAlpha(0.44f),
                edgeV.modifyAlpha(0.06f),
                Shader.TileMode.CLAMP
            )
            paint.shader = gradient
            if (blendMode != null) {
                paint.blendMode = blendMode
            } else {
                @Suppress("DEPRECATION")
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
            }
            canvas.drawCircle(b.x, b.y, dynamicR, paint)
        }
        paint.shader = null

        // Simplified: no edge fades or glass sheen overlays
        canvas.restoreToCount(save)
    }

    private fun Int.modifyAlpha(multiplier: Float): Int {
        val a = ((Color.alpha(this) / 255f) * multiplier).coerceIn(0f, 1f)
        return (this and 0x00FFFFFF) or ((a * 255).toInt() shl 24)
    }

    private fun lerpColor(from: Int, to: Int, t: Float): Int {
        val clamped = t.coerceIn(0f, 1f)
        val a = (Color.alpha(from) + (Color.alpha(to) - Color.alpha(from)) * clamped).toInt()
        val r = (Color.red(from) + (Color.red(to) - Color.red(from)) * clamped).toInt()
        val g = (Color.green(from) + (Color.green(to) - Color.green(from)) * clamped).toInt()
        val b = (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * clamped).toInt()
        return Color.argb(a, r, g, b)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
    
}
