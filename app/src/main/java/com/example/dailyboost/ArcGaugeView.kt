package com.example.dailyboost

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class ArcGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var startAngle = 140f
    private var sweepAngle = 260f
    private var trackThickness = dp(18f)
    private var trackColor = color(R.color.accent)
    private var progressColor = color(R.color.primary)
    private var tickCount = 18
    private var tickLength = dp(10f)
    private var tickColor = Color.parseColor("#D0D3D9")

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = dp(3f)
    }

    private val arcRect = RectF()
    private var progress = 0f // 0..100

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.ArcGaugeView, 0, 0).apply {
            try {
                startAngle = getFloat(R.styleable.ArcGaugeView_agv_startAngle, startAngle)
                sweepAngle = getFloat(R.styleable.ArcGaugeView_agv_sweepAngle, sweepAngle)
                trackThickness = getDimension(R.styleable.ArcGaugeView_agv_trackThickness, trackThickness)
                trackColor = getColor(R.styleable.ArcGaugeView_agv_trackColor, trackColor)
                progressColor = getColor(R.styleable.ArcGaugeView_agv_progressColor, progressColor)
                tickCount = getInt(R.styleable.ArcGaugeView_agv_tickCount, tickCount)
                tickLength = getDimension(R.styleable.ArcGaugeView_agv_tickLength, tickLength)
                tickColor = getColor(R.styleable.ArcGaugeView_agv_tickColor, tickColor)
            } finally {
                recycle()
            }
        }
        trackPaint.strokeWidth = trackThickness
        trackPaint.color = trackColor
        progressPaint.strokeWidth = trackThickness
        progressPaint.color = progressColor
        tickPaint.color = tickColor
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = resolveSize(dp(280f).toInt(), widthMeasureSpec)
        val h = resolveSize(dp(240f).toInt(), heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val pad = trackThickness / 2f + dp(6f)
        arcRect.set(pad, pad, w - pad, h - pad)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Track
        canvas.drawArc(arcRect, startAngle, sweepAngle, false, trackPaint)

        // Progress
        val sweep = sweepAngle * (progress.coerceIn(0f, 100f) / 100f)
        canvas.drawArc(arcRect, startAngle, sweep, false, progressPaint)

        // Ticks
        if (tickCount > 0) {
            val cx = arcRect.centerX()
            val cy = arcRect.centerY()
            val radius = arcRect.width() / 2f - trackThickness * 0.6f
            val gap = sweepAngle / (tickCount + 1)
            for (i in 1..tickCount) {
                val ang = Math.toRadians((startAngle + gap * i).toDouble())
                val sx = (cx + (radius - tickLength) * cos(ang)).toFloat()
                val sy = (cy + (radius - tickLength) * sin(ang)).toFloat()
                val ex = (cx + radius * cos(ang)).toFloat()
                val ey = (cy + radius * sin(ang)).toFloat()
                canvas.drawLine(sx, sy, ex, ey, tickPaint)
            }
        }
    }

    fun setProgress(percent: Int, animate: Boolean = true) {
        val target = percent.coerceIn(0, 100).toFloat()
        if (!animate) {
            progress = target
            invalidate()
            return
        }
        val start = progress
        val delta = target - start
        val steps = 22
        val stepDelta = delta / steps
        val frame = 14L
        var i = 0
        post(object : Runnable {
            override fun run() {
                if (i < steps) {
                    progress += stepDelta
                    invalidate()
                    i++
                    postDelayed(this, frame)
                } else {
                    progress = target
                    invalidate()
                }
            }
        })
    }

    private fun dp(v: Float) = v * resources.displayMetrics.density
    private fun color(res: Int) = ContextCompat.getColor(context, res)
}
