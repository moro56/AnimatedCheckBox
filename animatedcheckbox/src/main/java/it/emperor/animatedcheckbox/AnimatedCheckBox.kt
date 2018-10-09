package it.emperor.animatedcheckbox

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import it.emperor.animatedcheckbox.extension.animateColor
import it.emperor.animatedcheckbox.extension.clamp
import it.emperor.animatedcheckbox.extension.toPx
import it.emperor.animatedcheckbox.extension.toRange

// Default values
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
    private var padding: Float = 2f.toPx()

    private var checked: Boolean = false
    private var onChange: (checked: Boolean) -> Unit = {}

    // Internal fields
    private var animationProgress: Float
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
            hookStrokeWidth = array.getDimension(R.styleable.AnimatedCheckBox_acb_hook_stroke_width, DEFAULT_HOOK_STROKE_WIDTH)
            borderCheckedStrokeWidth = array.getDimension(R.styleable.AnimatedCheckBox_acb_border_checked_stroke_width, DEFAULT_BORDER_CHECKED_STROKE_WIDTH)
            duration = array.getInteger(R.styleable.AnimatedCheckBox_acb_animation_duration, DEFAULT_ANIMATION_DURATION.toInt()).toLong()
            checked = array.getBoolean(R.styleable.AnimatedCheckBox_acb_checked, checked)
            colorAnimation = if (checked) hookColor else borderNotCheckedColor
            padding = array.getDimension(R.styleable.AnimatedCheckBox_acb_padding, padding)
            array.recycle()
        }
        animationProgress = if (checked) 1f else 0f

        if (useAnimatedColor()) {
            Color.colorToHSV(hookColor, colorFrom)
            Color.colorToHSV(borderNotCheckedColor, colorTo)
        }

        // Initialize paint object
        paint = Paint()
        paint.isAntiAlias = true
        paint.isDither = true
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
        paint.pathEffect = CornerPathEffect(10f)

        // Initialize path object
        path = Path()

        // Initialize animator object
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

        // Initialize click listener
        setOnClickListener {
            updateState(!checked, true)
        }
    }

    /**
     * Register a callback to be invoked when the state changes.
     *
     * @param onChange callback to be invoked
     */
    fun setOnChangeListener(onChange: (checked: Boolean) -> Unit = {}) {
        this.onChange = onChange
    }

    /**
     * Update the state of this view.
     *
     * @param checked the new state
     * @param animate animating the update of the state
     */
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

    /**
     * Update the duration of the animation
     *
     * @param duration the duration of the animation
     */
    fun updateDuration(duration: Long) {
        this.duration = duration
        invalidate()
    }

    /**
     * The animation is currently running
     *
     * @return animation is running
     */
    fun isAnimating() = animator.isRunning

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        // Joint between the hook and the circle border
        leftHookCircleX = radius() * Math.cos(Math.toRadians(160.0))
        leftHookCircleY = radius() * Math.sin(Math.toRadians(160.0)) * -1

        // Joint between the two sides of the hook
        hookCenterX = leftHookCircleX + radius() * 0.9f * Math.cos(Math.toRadians(-20.0))
        hookCenterY = leftHookCircleY + radius() * Math.sin(Math.toRadians(-20.0)) * -1 + hookOffsetY()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw circle
        drawCircle(canvas)
        // Draw borders and hook
        drawCircleBorderAndHook(canvas)
    }

    /**
     * Draw circle
     *
     * @param canvas Canvas
     */
    private fun drawCircle(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = circleColor
        paint.alpha = (animationProgress * 255).toInt()
        canvas.drawCircle(centerX(), centerY(), radius(), paint)
    }

    /**
     * Draw borders and hook
     *
     * @param canvas Canvas
     */
    private fun drawCircleBorderAndHook(canvas: Canvas) {
        paint.style = Paint.Style.STROKE
        paint.alpha = 255

        path.reset()

        // Draw borders
        drawCircleBorder(canvas)
        // Draw hook
        drawHook()

        canvas.drawPath(path, paint)
    }

    /**
     * Draw borders
     *
     * @param canvas Canvas
     */
    private fun drawCircleBorder(canvas: Canvas) {
        // Draw circle border for the unchecked state
        if (animationProgress < 0.7f) {
            val arcProgress = (1f - animationProgress).toRange(0.3f, 1f, 0f, 1f).clamp(0f, 1f)
            path.arcTo(centerX() - radius(), centerY() - radius(), centerX() + radius(), centerY() + radius(),
                    200 + 360f * arcProgress, -360f * arcProgress, false)
        }

        // Draw circle border for the checked state, if specified
        if (borderCheckedStrokeWidth > 0 && animationProgress > 0.7f) {
            paint.strokeWidth = borderCheckedStrokeWidth
            paint.color = borderCheckedColor

            val arcProgressBorder = (1f - animationProgress).toRange(0f, 0.3f, 1f, 0f).clamp(0f, 1f)
            canvas.drawArc(centerX() - radiusBorderChecked(), centerY() - radiusBorderChecked(), centerX() + radiusBorderChecked(), centerY() + radiusBorderChecked(),
                    200f, -360f * arcProgressBorder, false, paint)
        }
    }

    /**
     * Draw hook
     *
     * @param canvas Canvas
     */
    private fun drawHook() {
        paint.strokeWidth = hookStrokeWidth
        if (useAnimatedColor()) {
            paint.color = colorAnimation
        } else {
            paint.color = hookColor
        }
        paint.alpha = 255

        // Draw left side hook
        drawLeftHook()
        // Draw right side hook
        drawRightHook()
    }

    /**
     * Draw left side hook
     */
    private fun drawLeftHook() {
        if (animationProgress < 0.7f) {
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

    /**
     * Draw right side hook
     */
    private fun drawRightHook() {
        if (animationProgress >= 0.7f) {
            val progress = animationProgress.toRange(0.7f, 1f, 0f, 0.75f)
            val lineRadius = radius() * progress

            val endX: Float = (lineRadius * Math.cos(Math.toRadians(45.0))).toFloat()
            val endY: Float = (lineRadius * Math.sin(Math.toRadians(45.0)) * -1 + hookOffsetY() * animationProgress).toFloat()

            // RightHook
            path.moveTo((centerX() + hookCenterX).toFloat(), (centerY() + hookCenterY).toFloat())
            path.lineTo(centerX() + endX, centerY() + endY)
        }
    }

    /**
     * Start the animation
     */
    private fun startAnimation() {
        val endValue = if (checked) 1f else 0f
        val fraction = Math.abs(endValue - animationProgress)
        animator.setFloatValues(animationProgress, endValue)
        animator.duration = (duration * fraction).toLong()
        animator.start()
    }

    /**
     * Update the state of this view during the animation
     *
     * @param animation the current animation
     */
    private fun update(animation: ValueAnimator) {
        val fraction = animation.animatedValue as Float
        animationProgress = fraction
        if (useAnimatedColor()) {
            if (!checked) colorAnimation = colorAnimationHsv.animateColor(colorFrom, colorTo, animation.animatedFraction)
            else colorAnimation = colorAnimationHsv.animateColor(colorTo, colorFrom, animation.animatedFraction)
        }
        invalidate()
    }

    private fun padding() = padding * 2
    private fun width() = width - padding()
    private fun height() = height - padding()
    private fun centerX() = (width() + padding()) / 2f
    private fun centerY() = (height() + padding()) / 2f
    private fun hookOffsetY() = height() / 8f
    private fun radius() = (if (width() > height()) height() else width()) / 2f
    private fun radiusBorderChecked() = (if (width() > height()) height() else width()) / 2f
    private fun useAnimatedColor() = hookColor != borderNotCheckedColor
}