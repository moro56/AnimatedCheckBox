package it.emperor.animatedcheckbox

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import it.emperor.animatedcheckbox.extension.animateColor
import it.emperor.animatedcheckbox.extension.clamp
import it.emperor.animatedcheckbox.extension.toRange

private const val DEFAULT_CIRCLE_COLOR: Int = Color.GREEN
private const val DEFAULT_HOOK_COLOR: Int = Color.BLACK
private const val DEFAULT_BORDER_CHECKED_COLOR: Int = Color.BLACK
private const val DEFAULT_HOOK_STROKE_WIDTH: Float = 1f
private const val DEFAULT_BORDER_CHECKED_STROKE_WIDTH: Float = 0f
private const val DEFAULT_ANIMATION_DURATION: Long = 250

class AnimatedCheckBox @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    private val animator: ValueAnimator
    private val paint: Paint
    private val path: Path

    private var circleColor: Int = DEFAULT_CIRCLE_COLOR
    private var hookColor: Int = DEFAULT_HOOK_COLOR
    private var borderCheckedColor: Int = DEFAULT_BORDER_CHECKED_COLOR
    private var borderNotCheckedColor: Int = hookColor
    private var hookStrokeWidth: Float = DEFAULT_HOOK_STROKE_WIDTH
    private var borderCheckedStrokeWidth: Float = DEFAULT_BORDER_CHECKED_STROKE_WIDTH
    private var duration: Long = DEFAULT_ANIMATION_DURATION

    private var checked: Boolean = false
    private var animationProgress: Float
    private var onChange: (checked: Boolean) -> Unit = {}

    private val colorFrom = FloatArray(3)
    private val colorTo = FloatArray(3)
    private val colorAnimationHsv = FloatArray(3)
    private var colorAnimation: Int = borderNotCheckedColor
    private var leftHookCircleX = 0.0
    private var leftHookCircleY = 0.0
    private var hookCenterX = 0.0
    private var hookCenterY = 0.0

    init {
        attrs?.let {
            val array = context.obtainStyledAttributes(attrs, R.styleable.AnimatedCheckBox)
            circleColor = array.getColor(R.styleable.AnimatedCheckBox_acb_circle_color, DEFAULT_CIRCLE_COLOR)
            hookColor = array.getColor(R.styleable.AnimatedCheckBox_acb_hook_color, DEFAULT_HOOK_COLOR)
            borderCheckedColor = array.getColor(R.styleable.AnimatedCheckBox_acb_border_checked_color, DEFAULT_BORDER_CHECKED_COLOR)
            borderNotCheckedColor = array.getColor(R.styleable.AnimatedCheckBox_acb_border_not_checked_color, hookColor)
            colorAnimation = borderNotCheckedColor
            hookStrokeWidth = array.getDimension(R.styleable.AnimatedCheckBox_acb_hook_stroke_width, DEFAULT_HOOK_STROKE_WIDTH)
            borderCheckedStrokeWidth = array.getDimension(R.styleable.AnimatedCheckBox_acb_border_checked_stroke_width, DEFAULT_BORDER_CHECKED_STROKE_WIDTH)
            duration = array.getInteger(R.styleable.AnimatedCheckBox_acb_animation_duration, DEFAULT_ANIMATION_DURATION.toInt()).toLong()
            checked = array.getBoolean(R.styleable.AnimatedCheckBox_acb_checked, checked)
            array.recycle()
        }
        animationProgress = if (checked) 1f else 0f

        if (useAnimatedColor()) {
            Color.colorToHSV(hookColor, colorFrom)
            Color.colorToHSV(borderNotCheckedColor, colorTo)
        }

        paint = Paint()
        paint.isAntiAlias = true

        path = Path()

        animator = ValueAnimator()
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener {
            update(it)
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationCancel(animation: Animator?) {
                animation?.removeAllListeners()
            }

            override fun onAnimationEnd(animation: Animator?) {
                onChange(checked)
            }
        })

        setOnClickListener {
            updateState(!checked, true)
        }
    }

    fun setOnChangeListener(onChange: (checked: Boolean) -> Unit = {}) {
        this.onChange = onChange
    }

    @JvmOverloads
    fun updateState(checked: Boolean, animate: Boolean = false) {
        this.checked = checked
        animator.cancel()

        if (animate) {
            startAnimation()
        } else {
            animationProgress = if (checked) 1f else 0f
        }
    }

    fun updateDuration(duration: Long) {
        this.duration = duration
        invalidate()
    }

    fun isAnimating() = animator.isRunning

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        leftHookCircleX = radius() * Math.cos(Math.toRadians(160.0))
        leftHookCircleY = radius() * Math.sin(Math.toRadians(160.0)) * -1

        hookCenterX = leftHookCircleX + radius() * 0.9f * Math.cos(Math.toRadians(-20.0))
        hookCenterY = leftHookCircleY + radius() * Math.sin(Math.toRadians(-20.0)) * -1 + hookOffsetY()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawCircle(canvas)
        drawCircleBorderAndHook(canvas)
    }

    private fun drawCircle(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = circleColor
        paint.alpha = (animationProgress * 255).toInt()
        canvas.drawCircle(centerX(), centerY(), radius(), paint)
    }

    private fun drawCircleBorderAndHook(canvas: Canvas) {
        paint.style = Paint.Style.STROKE
        paint.alpha = 255

        path.reset()

        drawCircleBorder(canvas)
        drawHook()

        canvas.drawPath(path, paint)
    }

    private fun drawCircleBorder(canvas: Canvas) {
        if (animationProgress < 0.7) {
            val arcProgress = (1f - animationProgress).toRange(0.3f, 1f, 0f, 1f).clamp(0f, 1f)
            path.arcTo(centerX() - radius(), centerY() - radius(), centerX() + radius(), centerY() + radius(),
                    200 + 360f * arcProgress, -360f * arcProgress, false)
        }

        if (borderCheckedStrokeWidth > 0) {
            paint.strokeWidth = borderCheckedStrokeWidth
            paint.color = borderCheckedColor

            val arcProgressBorder = (1f - animationProgress).toRange(0f, 0.3f, 1f, 0f).clamp(0f, 1f)
            canvas.drawArc(centerX() - radiusBorderChecked(), centerY() - radiusBorderChecked(), centerX() + radiusBorderChecked(), centerY() + radiusBorderChecked(),
                    200f, -360f * arcProgressBorder, false, paint)
        }
    }

    private fun drawHook() {
        paint.strokeWidth = hookStrokeWidth
        if (useAnimatedColor()) {
            paint.color = colorAnimation
        } else {
            paint.color = hookColor
        }
        paint.alpha = 255

        drawLeftHook()
        drawRightHook()
    }

    private fun drawLeftHook() {
        if (animationProgress < 0.7) {
            val progress = animationProgress.toRange(0f, 0.7f, 0f, 1f)
            val lineRadius = radius() * progress

            val endX: Float = (leftHookCircleX + lineRadius * 0.9f * Math.cos(Math.toRadians(-20.0))).toFloat()
            val endY: Float = (leftHookCircleY + lineRadius * Math.sin(Math.toRadians(-20.0)) * -1 + (hookOffsetY() * progress)).toFloat()

            // LeftHook: left half-segment
            path.lineTo(centerX() + endX, centerY() + endY)
        } else {
            val progress = animationProgress.toRange(0.7f, 1f, 0f, 0.5f)
            val lineRadius = radius() * (1 - progress)

            val endX: Float = (lineRadius * Math.cos(Math.toRadians(160.0))).toFloat()
            val endY: Float = (lineRadius * Math.sin(Math.toRadians(160.0)) * -1 + (hookOffsetY() * progress)).toFloat()

            // LeftHook: right half-segment
            path.moveTo((centerX() + hookCenterX).toFloat(), (centerY() + hookCenterY).toFloat())
            path.lineTo(centerX() + endX, centerY() + endY)
        }
    }

    private fun drawRightHook() {
        if (animationProgress >= 0.7f) {
            val progress = animationProgress.toRange(0.7f, 1f, 0f, 0.75f)
            val lineRadius = radius() * progress

            val endX: Float = (lineRadius * Math.cos(Math.toRadians(45.0))).toFloat()
            val endY: Float = (lineRadius * Math.sin(Math.toRadians(45.0)) * -1 + hookOffsetY() * animationProgress).toFloat()

            // RightHook
            path.lineTo((centerX() + hookCenterX).toFloat(), (centerY() + hookCenterY).toFloat())
            path.lineTo(centerX() + endX, centerY() + endY)
        }
    }

    private fun startAnimation() {
        val endValue = if (checked) 1f else 0f
        val fraction = Math.abs(endValue - animationProgress)
        animator.setFloatValues(animationProgress, endValue)
        animator.duration = (duration * fraction).toLong()
        animator.start()
    }

    private fun update(animation: ValueAnimator) {
        val fraction = animation.animatedValue as Float
        animationProgress = fraction
        if (useAnimatedColor()) {
            if (!checked) colorAnimation = colorAnimationHsv.animateColor(colorFrom, colorTo, animation.animatedFraction)
            else colorAnimation = colorAnimationHsv.animateColor(colorTo, colorFrom, animation.animatedFraction)
        }
        invalidate()
    }

    private fun centerX() = width / 2f
    private fun centerY() = height / 2f
    private fun strokeOffset() = hookStrokeWidth / 2f
    private fun strokeOffsetBorderChecked() = borderCheckedStrokeWidth / 2f
    private fun hookOffsetY() = height / 8f - strokeOffset() / 2f
    private fun radius() = ((if (width > height) height else width) / 2f - strokeOffset())
    private fun radiusBorderChecked() = ((if (width > height) height else width) / 2f - strokeOffsetBorderChecked())
    private fun useAnimatedColor() = hookColor != borderNotCheckedColor
}