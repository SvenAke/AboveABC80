package com.aboveware.aboveabc80.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import androidx.core.graphics.withTranslation
import com.aboveware.aboveabc80.R
import com.aboveware.aboveabc80.Abc80Log
import com.aboveware.aboveabc80.keyboard.KeyboardView.Companion.LONG_PRESS_TIMEOUT
import java.util.Arrays
import kotlin.math.max
import kotlin.math.min

/**
 * A view that renders a virtual [KeyboardViewController]. It handles rendering of keys and
 * detecting key presses and touch movements.
 */
@ExperimentalUnsignedTypes
@OptIn(ExperimentalUnsignedTypes::class, ExperimentalStdlibApi::class)
class KeyboardView(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
) : View(context, attrs, defStyleAttr, defStyleRes) {

    private val extraPadding = 0

    /**
     * Settings
     */
    private var mLabelTextSize = 0
    private var mKeyTextSize = 0
    private var mKeyTextColor = 0
    private val mBackgroundDimAmount: Float
    private var mVerticalCorrection = 0
    private var mProximityThreshold = 0
    private var mProximityCorrectOn = false
    private val mPaint: Paint
    private var mPadding: Rect
    private var mInvalidatedKey: KeyboardViewController.Key? = null

    private var _keyboardViewController: KeyboardViewController? = null
    val keyboardViewController: KeyboardViewController
        get() = _keyboardViewController!!

    var keyboardXmlResId: Int = R.xml.adm3a
        set(value) {
            if (field != value || _keyboardViewController == null) {
                field = value
                val controller = KeyboardViewController(context, value)
                _keyboardViewController = controller
                onMappingsLoaded?.invoke(controller.characterMappings)
                controller.keyBackground?.let { keyBackground = it }
                computeProximityThreshold(_keyboardViewController)
                requestLayout()
                invalidateAllKeys()
            }
        }

    var onMappingsLoaded: ((Map<Char, Char>) -> Unit)? = null

    var keyBackground: Drawable? = null
        set(value) {
            field = value
            mPadding.set(0, 0, 0, 0)
            field?.getPadding(mPadding)
            invalidateAllKeys()
            invalidate()
        }

    var keyboard: Keyboard? = null

    private val mDistances = IntArray(MAX_NEARBY_KEYS)

    /**
     * Whether the keyboard bitmap needs to be redrawn before it's blitted.
     */
    private var mDrawPending = false

    /**
     * The dirty region in the keyboard bitmap
     */
    private val mDirtyRect = Rect()

    /**
     * The keyboard bitmap for faster updates
     */
    private var mBuffer: Bitmap? = null

    /**
     * The canvas for the above mutable keyboard bitmap
     */
    private var canvas: Canvas? = null

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int = R.attr.keyboardViewStyle
    ) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    var longPressHandler: Handler? = null

    fun removeLongPress(key: KeyboardViewController.Key) {
        longPressHandler?.apply {
            removeMessages(MSG_LONG_PRESS, key)
        }
    }

    /**
     * Posts a long press message for a given key.
     *
     * This function first removes any existing long press messages from the handler queue
     * to prevent multiple long press events from firing. It then checks if the provided
     * key is 'sticky'. If it is, a new long press message is created and sent to the
     * [longPressHandler] with a delay equal to [LONG_PRESS_TIMEOUT].
     *
     * @param key The [KeyboardViewController.Key] for which to post a long press message.
     * @see [removeLongPress]
     * @see [longPressHandler]
     */
    fun postLongPress(key: KeyboardViewController.Key) {
        removeLongPress(key)
        if (key.sticky)
            longPressHandler?.apply {
                sendMessageDelayed(obtainMessage(MSG_LONG_PRESS, key), LONG_PRESS_TIMEOUT)
            }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        longPressHandler ?: run {
            longPressHandler = object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    if (msg.what == MSG_LONG_PRESS) {
                        val key = msg.obj as KeyboardViewController.Key
                        key.on = true
                        Abc80Log.keyboard("MSG_LONG_PRESS $key")
                        removeLongPress(key)
                    }
                }
            }
        }
    }


    public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Determine available width
        var width = MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)

        if (widthMode == MeasureSpec.UNSPECIFIED) {
            width = keyboardViewController.minWidth + paddingLeft + paddingRight
        }

        if (isInEditMode && width <= 0) width = 1080

        // Resize keyboard to fit the determined width and calculate new height
        keyboardViewController.resize(width - paddingLeft - paddingRight)

        var height = keyboardViewController.height + paddingTop + paddingBottom
        if (isInEditMode && height <= 0) {
            // Fallback height for preview if keyboard calculation failed
            height = 800
        }

        setMeasuredDimension(width, height)
    }

    /**
     * Compute the average distance between adjacent keys (horizontally and vertically)
     * and square it to get the proximity threshold. We use a square here and in computing
     * the touch distance from a key's center to avoid taking a square root.
     *
     * @param keyboard
     */
    private fun computeProximityThreshold(keyboard: KeyboardViewController?) {
        if (keyboard == null) return
        val keys = keyboardViewController.keys
        val length = keys.size
        var dimensionSum = 0
        for (i in 0 until length) {
            val key = keys[i]
            dimensionSum += min(key.width, key.height) + key.gap
        }
        if (dimensionSum < 0 || length == 0) return
        mProximityThreshold = (dimensionSum * 1.4f / length).toInt()
        mProximityThreshold *= mProximityThreshold // Square it
    }

    public override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        keyboardViewController.resize(w - paddingLeft - paddingRight)
        computeProximityThreshold(keyboardViewController)
        // Release the buffer, if any and it will be reallocated on the next draw
        mBuffer = null
    }

    public override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mDrawPending || mBuffer == null) {
            onBufferDraw()
        }
        canvas.drawBitmap(mBuffer!!, 0f, 0f, null)
    }

    private fun onBufferDraw() {
        if (mBuffer == null) {
            // Make sure our bitmap is at least 1x1
            val width = max(1, width)
            val height = max(1, height)
            mBuffer = createBitmap(width, height)
            canvas = Canvas(mBuffer!!)
            invalidateAllKeys()
        }
        canvas?.let { canvas ->
            // Clear the buffer
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            drawKeyboard(canvas)
        }
        mDrawPending = false
        mDirtyRect.setEmpty()
    }

    /**
     * Draws the keyboard to the canvas.
     *
     * This function clips the drawing to the dirty rectangle to optimize rendering.
     * It iterates through all the keys in the keyboard and draws each one.
     * If an invalidated key is present and the clip region is completely contained within that key,
     * only that key will be redrawn.
     * The background of the keyboard is drawn if no specific key is invalidated.
     *
     * @param canvas The canvas on which to draw the keyboard.
     */
    private fun drawKeyboard(canvas: Canvas) {
        canvas.withSave { // mDirtyRect) {
            val paint = Paint(mPaint)
            val kbdPaddingLeft = paddingLeft

            // Anchor to bottom if view is taller than keyboard
            val keyboardHeight = keyboardViewController.height
            val availableHeight = height - paddingTop - paddingBottom
            val bottomOffset =
                if (availableHeight > keyboardHeight) availableHeight - keyboardHeight else 0

            val kbdPaddingTop = paddingTop + bottomOffset
            paint.color = mKeyTextColor

//            if (mInvalidatedKey == null) {
//                resources.decodeBitmap(R.drawable.keyboard_background)
//                    ?.let { drawBitmap(it, null, clipBounds, null) }
//            }

            drawAllKeys(
                paint,
                kbdPaddingLeft,
                kbdPaddingTop
            )
            mInvalidatedKey = null
        }
    }

    private fun Canvas.drawAllKeys(
        paint: Paint,
        kbdPaddingLeft: Int,
        kbdPaddingTop: Int
    ) {
        val keys = keyboardViewController.keys
        if (keys.isEmpty() && isInEditMode) {
            paint.color = Color.RED
            paint.textSize = 40f
            drawText("NO KEYS LOADED - XML: $keyboardXmlResId", 50f, 100f, paint)
            return
        }
        for (key in keys) {
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.isUnderlineText = false
            val kbdBackground = key.keyBackground ?: keyBackground
            kbdBackground?.let {
                val xOffset = (key.x + kbdPaddingLeft).toFloat()
                val yOffset = (key.y + kbdPaddingTop).toFloat()
                withSave {
                    translate(xOffset, yOffset)
                    drawKey(it, key, this, paint)
                }
            }
        }
    }

    private fun drawKey(
        keyBackground: Drawable,
        key: KeyboardViewController.Key,
        canvas: Canvas,
        paint: Paint
    ) {
        if (key.isLed) {
            drawLed(key, canvas, paint)
            return
        }
        if (key.customBitmap != null) {
            drawCustomKeyBackground(key.customBitmap!!, key, canvas)
        } else {
            keyBackground.state = key.currentDrawableState
            val bounds = keyBackground.bounds
            if (key.width != bounds.right || key.height != bounds.bottom)
                keyBackground.setBounds(0, 0, key.width, key.height)
            keyBackground.draw(canvas)
        }

        val effectiveFontSize = if (height > 0) height / 30f * 1.4f else 28f

        // Special case: Standalone Icon (e.g. Backspace, Arrows)
        // If there are no meaningful text labels, center the icon perfectly on the key.
        val hasText = listOf(
            key.label,
            key.secondLabel,
            key.thirdLabel,
            key.fourthLabel,
            key.fifthLabel,
            key.sixthLabel
        )
            .any { it != null && it.trim().isNotEmpty() }

        if (key.icon != null && !hasText) {
            val iconSize = (effectiveFontSize * 1.4f).toInt()
            val intrinsicWidth = key.icon!!.intrinsicWidth
            val intrinsicHeight = key.icon!!.intrinsicHeight

            val drawWidth: Int
            val drawHeight: Int
            if (intrinsicWidth > 0 && intrinsicHeight > 0) {
                val ratio = intrinsicWidth.toFloat() / intrinsicHeight.toFloat()
                if (ratio > 1f) {
                    drawWidth = iconSize
                    drawHeight = (iconSize / ratio).toInt()
                } else {
                    drawHeight = iconSize
                    drawWidth = (iconSize * ratio).toInt()
                }
            } else {
                drawWidth = iconSize
                drawHeight = iconSize
            }

            val x = (key.width - drawWidth) / 2f
            // Adjust for 3D effect: shift up by 8% of key height to center on the actual key surface
            val y = (key.height - drawHeight) / 2f - (key.height * 0.08f)

            key.icon!!.setBounds(0, 0, drawWidth, drawHeight)
            key.icon!!.setTint(Color.BLACK)
            canvas.withTranslation(x, y) {
                key.icon!!.draw(this)
            }
            return
        }

        // Vertical alignment should occur within the upper 2/3 of the button
        val labelAreaHeight = key.height * 2f / 3f

        // If we have many labels, shrink font to fit in the 2/3 area
        val totalUnits = computeRequiredVerticalUnits(key)
        var fontSize = effectiveFontSize
        if (totalUnits * fontSize > labelAreaHeight) {
            fontSize = labelAreaHeight / totalUnits
        }
        if (key.ledId == "upper_case") {
            fontSize *= 0.75f
        }

        val occupiedHeight = totalUnits * fontSize

        val targetTop = when (key.verticalAlignment) {
            Paint.Align.LEFT -> 0f // Top of area
            Paint.Align.CENTER -> (labelAreaHeight - occupiedHeight) / 2f
            Paint.Align.RIGHT -> labelAreaHeight - occupiedHeight
        }

        // Base drawing always starts at y=0 (relative to canvas translation)
        // Labels are drawn with their baselines at multiples of fontSize
        // We need to shift everything so the first label's top (y=0) starts at targetTop
        var yOffset = targetTop

        val labelsList = listOfNotNull(
            key.label,
            key.secondLabel,
            key.thirdLabel,
            key.fourthLabel,
            key.fifthLabel,
            key.sixthLabel
        )
        val hasLongText = labelsList.any { it.length > 1 }
        val hasMultipleRows = totalUnits >= 2.0f

        if (hasLongText && hasMultipleRows) {
            // Adjust down slightly for multi-row buttons with long text (e.g. FEED LINE, RETURN)
            yOffset += fontSize * 0.2f
        }
        if (key.ledId == "upper_case") {
            yOffset += key.height * 0.08f
        }

        if (yOffset != 0f) {
            canvas.translate(0f, yOffset)
        }

        val (commonFontSize, commonTextScale) = computeCommonFontSizeAndScale(key, fontSize, paint)

        key.thirdLabel?.apply {
            drawThirdLabel(paint, commonFontSize, commonTextScale, key, canvas)
        }
        key.label?.apply {
            drawLabel(paint, commonFontSize, commonTextScale, key, canvas)
        }
        key.icon?.apply {
            drawKeyIcon(fontSize, key, canvas)
        }
        key.secondLabel?.apply {
            drawSecondLabel(paint, commonFontSize, commonTextScale, key, canvas)
        }
        key.secondKeyIcon?.apply {
            drawSecondKeyIcon(fontSize, canvas, key.label != null || key.icon != null)
        }
        key.fourthLabel?.apply {
            drawFourthLabel(paint, commonFontSize, commonTextScale, key, canvas)
        }
        key.fourthKeyIcon?.apply {
            drawFourthKeyIcon(fontSize, canvas)
        }
        key.fifthLabel?.apply {
            drawFifthLabel(paint, commonFontSize, commonTextScale, key, canvas)
        }
        key.sixthLabel?.apply {
            drawSixthLabel(paint, commonFontSize, commonTextScale, key, canvas)
        }

        if (yOffset != 0f) {
            canvas.translate(0f, -yOffset)
        }
        if (key.ledId == "upper_case") {
            drawInlineLed(key, canvas, paint)
        }
    }

    private fun drawInlineLed(key: KeyboardViewController.Key, canvas: Canvas, paint: Paint) {
        val ledName = key.ledId ?: return
        val ledOn = keyboard?.leds?.get(ledName)?.isOn ?: (key.on || isInEditMode)
        val radius = (key.height * 0.055f).coerceAtLeast(2f)
        val centerX = key.width * 0.66f
        val centerY = key.height * 0.16f
        val oldColor = paint.color
        val oldStyle = paint.style
        paint.style = Paint.Style.FILL
        paint.color = Color.DKGRAY
        canvas.drawCircle(centerX, centerY, radius * 1.45f, paint)
        paint.color = if (ledOn) key.ledColor else Color.BLACK
        canvas.drawCircle(centerX, centerY, radius, paint)
        paint.color = oldColor
        paint.style = oldStyle
    }

    private fun drawLed(key: KeyboardViewController.Key, canvas: Canvas, paint: Paint) {
        // Use 50% for label and 50% for LED rectangle to give more room for text
        val labelHeight = key.height * 0.5f
        val ledHeight = key.height - labelHeight

        // Ensure alpha is fully opaque for LED drawing
        paint.alpha = 255

        // Draw a background for the label area to ensure contrast
        paint.color = -0x2e2e37 // 0xFFD1D1C9 as a signed Int
        canvas.drawRect(0f, 0f, key.width.toFloat(), labelHeight, paint)

        // Draw the LED background (the dark part) with rounded corners
        paint.color = Color.DKGRAY
        val cornerRadius = ledHeight * 0.2f
        canvas.drawRoundRect(
            0f,
            labelHeight,
            key.width.toFloat(),
            key.height.toFloat(),
            cornerRadius,
            cornerRadius,
            paint
        )

        // Check state from the common Keyboard model
        // Note: Due to project-wide swap, app:keyLabel is in secondLabel
        val ledName = key.ledId ?: key.secondLabel?.toString()
        val ledState = keyboard?.leds?.get(ledName)?.isOn ?: (key.on || key.pressed || isInEditMode)

        // Draw the "active" part of the LED with rounded corners
        paint.color = if (ledState) key.ledColor else Color.BLACK

        val margin = ledHeight * 0.2f
        val innerCornerRadius = cornerRadius * 0.8f
        canvas.drawRoundRect(
            margin,
            labelHeight + margin,
            key.width - margin,
            key.height - margin,
            innerCornerRadius,
            innerCornerRadius,
            paint
        )

        // Draw label or icon above LED
        val displayLabel = key.secondLabel ?: key.label
        val displayIcon = key.icon ?: key.fourthKeyIcon ?: key.secondKeyIcon

        if (displayIcon != null) {
            val iconSize = (labelHeight * 1.5f).toInt()
            val x = (key.width - iconSize) / 2f
            val y = (labelHeight - iconSize) / 2f
            displayIcon.setBounds(0, 0, iconSize, iconSize)
            displayIcon.setTint(Color.BLACK)
            canvas.withTranslation(x, y) {
                displayIcon.draw(this)
            }
        } else if (displayLabel != null) {
            paint.color = Color.BLACK
            paint.isUnderlineText = key.secondLabelUnderline || key.labelUnderline
            // Use a slightly larger relative font size for these small labels
            fitText(displayLabel, labelHeight * 1.0f, key.width.toFloat() * 1.6f, paint)
            paint.textAlign = Paint.Align.CENTER
            // Draw text centered in the labelHeight area (y is baseline)
            canvas.drawText(displayLabel.toString(), key.width / 2f, labelHeight * 0.85f, paint)
        }
    }

    private fun drawCustomKeyBackground(
        drawable: Drawable,
        key: KeyboardViewController.Key,
        canvas: Canvas
    ) {
        drawable.state = key.currentDrawableState
        drawable.jumpToCurrentState()
        val bitmap = drawable.drawableToBitmap() ?: return
        val bw = bitmap.width
        val bh = bitmap.height

        val sideSrcWidth = (bw * 0.25f).toInt()

        // Scale sides to match key height while maintaining aspect ratio
        val scaleY = key.height.toFloat() / bh.toFloat()
        val sideDestWidth = (sideSrcWidth * scaleY).toInt()

        // Left 25%
        val leftSrc = Rect(0, 0, sideSrcWidth, bh)
        val leftDest = Rect(0, 0, sideDestWidth, key.height)
        canvas.drawBitmap(bitmap, leftSrc, leftDest, null)

        // Right 25%
        val rightSrc = Rect(bw - sideSrcWidth, 0, bw, bh)
        val rightDest = Rect(key.width - sideDestWidth, 0, key.width, key.height)
        canvas.drawBitmap(bitmap, rightSrc, rightDest, null)

        // Middle 50% stretched
        val middleSrc = Rect(sideSrcWidth, 0, bw - sideSrcWidth, bh)
        val middleDest = Rect(sideDestWidth, 0, key.width - sideDestWidth, key.height)
        canvas.drawBitmap(bitmap, middleSrc, middleDest, null)
    }

    private fun computeRequiredVerticalUnits(key: KeyboardViewController.Key): Float {
        var currentY = 0f
        // Line 1: Second and Third labels
        if (key.secondLabel != null || key.thirdLabel != null) currentY += 1.1f
        // Line 2: Main label and Fourth label/icon
        if (key.label != null || key.fourthLabel != null || key.fourthKeyIcon != null || key.icon != null) currentY += 1.1f
        // Line 3: Fifth and Sixth labels
        if (key.fifthLabel != null || key.sixthLabel != null) currentY += 1.1f

        return if (currentY > 0) currentY - 0.1f else 0f
    }

    private fun computeCommonFontSizeAndScale(
        key: KeyboardViewController.Key,
        baseSize: Float,
        paint: Paint
    ): Pair<Float, Float> {
        val labels = listOfNotNull(
            key.label,
            key.secondLabel,
            key.thirdLabel,
            key.fourthLabel,
            key.fifthLabel,
            key.sixthLabel
        )
        if (labels.isEmpty()) return baseSize to 1.0f

        // Use the smaller size if any label is longer than 2 chars
        var minSize = baseSize
        labels.forEach { if (it.length > 2) minSize = min(minSize, baseSize * 0.85f) }

        paint.textSize = minSize
        paint.textScaleX = 1.0f
        var minScale = 1.0f
        val maxWidth = key.width * 0.45f

        labels.forEach {
            val textWidth = paint.measureText(it.toString())
            if (textWidth > maxWidth) {
                minScale = min(minScale, maxWidth / textWidth)
            }
        }

        return minSize to minScale
    }

    private fun fitText(text: CharSequence, baseSize: Float, maxWidth: Float, paint: Paint): Float {
        val size = if (text.length > 2) baseSize * 0.85f else baseSize
        paint.textSize = size
        paint.textScaleX = 1.0f
        val textWidth = paint.measureText(text.toString())
        if (textWidth > maxWidth) {
            paint.textScaleX = maxWidth / textWidth
        }
        return size
    }

    private fun Drawable.drawSecondKeyIcon(
        fontSizeUnit: Float,
        canvas: Canvas,
        hasPrimaryLabel: Boolean
    ) {
        val x = 0.1f * fontSizeUnit
        val y = (if (hasPrimaryLabel) 1.3f else 0.1f) * fontSizeUnit
        canvas.translate(x, y)
        setBounds(0, 0, 2 * fontSizeUnit.toInt(), fontSizeUnit.toInt())
        setTint(Color.BLACK)
        draw(canvas)
        canvas.translate(-x, -y)
    }

    private fun CharSequence.drawSecondLabel(
        paint: Paint,
        fontSizeUnit: Float,
        textScaleX: Float,
        key: KeyboardViewController.Key,
        canvas: Canvas
    ) {
        paint.textSize = fontSizeUnit
        paint.textScaleX = textScaleX
        paint.color = Color.BLACK
        paint.isUnderlineText = key.secondLabelUnderline
        // Split both lines if either line 1 or line 2 has a right-side label
        val isSplit = key.thirdLabel != null || key.fourthLabel != null
        val alignment = if (isSplit) Paint.Align.LEFT else key.horizontalAlignment
        paint.textAlign = alignment

        val margin = fontSizeUnit * 0.8f
        val x = when (alignment) {
            Paint.Align.LEFT -> margin
            Paint.Align.CENTER -> key.width / 2f
            Paint.Align.RIGHT -> key.width - margin
        }
        val y = 1.0f // Line 1

        canvas.drawText(toString(), x, y * fontSizeUnit, paint)
    }

    private fun Drawable.drawKeyIcon(
        fontSizeUnit: Float,
        key: KeyboardViewController.Key,
        canvas: Canvas
    ) {
        val isSplit = key.thirdLabel != null || key.fourthLabel != null
        val alignment = if (isSplit) Paint.Align.LEFT else key.horizontalAlignment

        val margin = fontSizeUnit * 0.8f
        val x = when (alignment) {
            Paint.Align.LEFT -> margin
            Paint.Align.CENTER -> key.width / 2f - fontSizeUnit / 2f
            Paint.Align.RIGHT -> key.width - margin - fontSizeUnit
        }
        val yUnits =
            if (key.secondLabel != null || key.thirdLabel != null) 2.1f else 1.0f // Line 2 (or 1)

        val drawY = (yUnits - 0.9f) * fontSizeUnit
        canvas.translate(x, drawY)
        setBounds(0, 0, fontSizeUnit.toInt(), fontSizeUnit.toInt())
        setTint(Color.BLACK)
        draw(canvas)
        canvas.translate(-x, -drawY)
    }

    private fun CharSequence.drawSixthLabel(
        paint: Paint,
        fontSizeUnit: Float,
        textScaleX: Float,
        key: KeyboardViewController.Key,
        canvas: Canvas
    ) {
        paint.textSize = fontSizeUnit
        paint.textScaleX = textScaleX
        paint.color = Color.BLACK
        paint.isUnderlineText = key.sixthLabelUnderline
        val alignment = if (key.fifthLabel != null) Paint.Align.RIGHT else key.horizontalAlignment
        paint.textAlign = alignment

        val margin = fontSizeUnit * 0.8f
        val x = when (alignment) {
            Paint.Align.LEFT -> margin
            Paint.Align.CENTER -> key.width / 2f
            Paint.Align.RIGHT -> key.width - margin
        }

        var y = 1.0f
        if (key.secondLabel != null || key.thirdLabel != null) y += 1.1f
        if (key.label != null || key.fourthLabel != null || key.icon != null) y += 1.1f

        canvas.drawText(toString(), x, y * fontSizeUnit, paint)
    }

    private fun CharSequence.drawFifthLabel(
        paint: Paint,
        fontSizeUnit: Float,
        textScaleX: Float,
        key: KeyboardViewController.Key,
        canvas: Canvas
    ) {
        paint.textSize = fontSizeUnit
        paint.textScaleX = textScaleX
        paint.color = Color.BLACK
        paint.isUnderlineText = key.fifthLabelUnderline
        val alignment = if (key.sixthLabel != null) Paint.Align.LEFT else key.horizontalAlignment
        paint.textAlign = alignment

        val margin = fontSizeUnit * 0.8f
        val x = when (alignment) {
            Paint.Align.LEFT -> margin
            Paint.Align.CENTER -> key.width / 2f
            Paint.Align.RIGHT -> key.width - margin
        }

        var y = 1.0f
        if (key.secondLabel != null || key.thirdLabel != null) y += 1.1f
        if (key.label != null || key.fourthLabel != null || key.icon != null) y += 1.1f

        canvas.drawText(toString(), x, y * fontSizeUnit, paint)
    }

    private fun Drawable.drawFourthKeyIcon(
        fontSizeUnit: Float,
        canvas: Canvas
    ) {
        // Approximate position for legacy icon
        val x = 3.3f * fontSizeUnit
        val y = 3.1f * fontSizeUnit
        canvas.translate(x, y)
        setBounds(0, 0, fontSizeUnit.toInt(), fontSizeUnit.toInt())
        setTint(Color.BLACK)
        draw(canvas)
        canvas.translate(-x, -y)
    }

    private fun CharSequence.drawFourthLabel(
        paint: Paint,
        fontSizeUnit: Float,
        textScaleX: Float,
        key: KeyboardViewController.Key,
        canvas: Canvas
    ) {
        paint.textSize = fontSizeUnit
        paint.textScaleX = textScaleX
        paint.color = Color.BLACK
        paint.isUnderlineText = key.fourthLabelUnderline
        // Split both lines if either line 1 or line 2 has a right-side label
        val isSplit = key.thirdLabel != null || key.fourthLabel != null
        val alignment = if (isSplit) Paint.Align.RIGHT else key.horizontalAlignment
        paint.textAlign = alignment

        val margin = fontSizeUnit * 0.8f
        val x = when (alignment) {
            Paint.Align.LEFT -> margin
            Paint.Align.CENTER -> key.width / 2f
            Paint.Align.RIGHT -> key.width - margin
        }
        val y =
            if (key.secondLabel != null || key.thirdLabel != null) 2.1f else 1.0f // Line 2 (or 1)

        canvas.drawText(toString(), x, y * fontSizeUnit, paint)
    }

    private fun CharSequence.drawThirdLabel(
        paint: Paint,
        fontSizeUnit: Float,
        textScaleX: Float,
        key: KeyboardViewController.Key,
        canvas: Canvas
    ) {
        paint.textSize = fontSizeUnit
        paint.textScaleX = textScaleX
        paint.color = Color.BLACK
        paint.isUnderlineText = key.thirdLabelUnderline
        // Split both lines if either line 1 or line 2 has a right-side label
        val isSplit = key.thirdLabel != null || key.fourthLabel != null
        val alignment = if (isSplit) Paint.Align.RIGHT else key.horizontalAlignment
        paint.textAlign = alignment

        val margin = fontSizeUnit * 0.8f
        val x = when (alignment) {
            Paint.Align.LEFT -> margin
            Paint.Align.CENTER -> key.width / 2f
            Paint.Align.RIGHT -> key.width - margin
        }
        val y = 1.0f // Line 1
        canvas.drawText(toString(), x, y * fontSizeUnit, paint)
    }

    private fun CharSequence.drawLabel(
        paint: Paint,
        fontSizeUnit: Float,
        textScaleX: Float,
        key: KeyboardViewController.Key,
        canvas: Canvas
    ) {
        paint.textSize = fontSizeUnit
        paint.textScaleX = textScaleX
        paint.color = Color.BLACK
        paint.isUnderlineText = key.labelUnderline
        // Split both lines if either line 1 or line 2 has a right-side label
        val isSplit = key.thirdLabel != null || key.fourthLabel != null
        val alignment = if (isSplit) Paint.Align.LEFT else key.horizontalAlignment
        paint.textAlign = alignment

        val margin = fontSizeUnit * 0.8f
        val x = when (alignment) {
            Paint.Align.LEFT -> margin
            Paint.Align.CENTER -> key.width / 2f
            Paint.Align.RIGHT -> key.width - margin
        }
        val y =
            if (key.secondLabel != null || key.thirdLabel != null) 2.1f else 1.0f // Line 2 (or 1)

        canvas.drawText(toString(), x, y * fontSizeUnit, paint)
    }

    private fun getKeyIndices(x: Int, y: Int): Int {
        var primaryIndex = NOT_A_KEY
        var closestKey = NOT_A_KEY
        var closestKeyDist = mProximityThreshold + 1
        Arrays.fill(mDistances, Int.MAX_VALUE)
        val nearestKeyIndices = keyboardViewController.getNearestKeys(x, y)
        val keyCount = nearestKeyIndices!!.size
        for (i in 0 until keyCount) {
            val key = keyboardViewController.keys[nearestKeyIndices[i]]
            var dist = 0
            val isInside = key.isInside(x, y)
            if (isInside) {
                primaryIndex = nearestKeyIndices[i]
            }
            if (((mProximityCorrectOn
                        && key.squaredDistanceFrom(x, y).also { dist = it } < mProximityThreshold)
                        || isInside)
            ) {
                // Find insertion point
                if (dist < closestKeyDist) {
                    closestKeyDist = dist
                    closestKey = nearestKeyIndices[i]
                }
            }
        }
        if (primaryIndex == NOT_A_KEY) {
            primaryIndex = closestKey
        }
        return primaryIndex
    }

    /**
     * Requests a redraw of the entire keyboard. Calling [.invalidate] is not sufficient
     * because the keyboard renders the keys to an off-screen buffer and an invalidate() only
     * draws the cached buffer.
     *
     * @see .invalidateKey
     */
    fun invalidateAllKeys() {
        mDirtyRect.union(0, 0, width, height)
        mDrawPending = true
    }

    private fun invalidateKey(key: KeyboardViewController.Key) {
        mInvalidatedKey = key
        mDirtyRect.union(
            key.x + paddingLeft, key.y + paddingTop,
            key.x + key.width + paddingLeft, key.y + key.height + paddingTop
        )
        onBufferDraw()
    }

    inner class KeysPressed {
        var pressed: Boolean
            get() = keysPressed.isNotEmpty()
            set(value) {
                keysPressed.forEach {
                    it.pressed = value
                }
            }

        private val keysPressed = mutableListOf<KeyboardViewController.Key>()
        fun contains(key: KeyboardViewController.Key) = keysPressed.contains(key)
        fun add(key: KeyboardViewController.Key) = keysPressed.add(key).also {
            pressed = true
            handleKeyPressed(key)
        }

        fun remove(key: KeyboardViewController.Key) =
            keysPressed.remove(key).also {
                key.pressed = false
                keyboardViewController.handleKeyRelease(keyboard!!, key)
            }

        fun clear() = keysPressed.clear()
        fun containsAll(other: MutableList<KeyboardViewController.Key>) =
            keysPressed.size == other.size &&
                    keysPressed.containsAll(other)

        override fun toString() = keysPressed.toString()

        fun addAll(other: MutableList<KeyboardViewController.Key>) {
            pressed = false
            clear()
            keysPressed.addAll(other)
            pressed = true
        }

        fun invalidate() = keysPressed.forEach {
            invalidateKey(it)
        }

        fun releaseAll() = keysPressed.forEach {
            keyboardViewController.handleKeyRelease(keyboard!!, it)
        }

        fun forEach(function: (key: KeyboardViewController.Key) -> Unit) =
            keysPressed.forEach { it -> function(it) }
    }

    val keysPressed = KeysPressed()

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val keyIndex =
                    getKeyIndices(event.x.toInt() - paddingLeft, event.y.toInt() - paddingRight)
                if (keyIndex == NOT_A_KEY) return true
                val key = keyboardViewController.keys[keyIndex]
                if (key.isLed) return true
                postLongPress(key)
                if (!keysPressed.contains(key)) keysPressed.add(key)
                keysPressed.invalidate()
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                val movedKeysPressed = mutableListOf<KeyboardViewController.Key>()
                for (i in 0 until event.pointerCount) {
                    val keyIndex = getKeyIndices(
                        event.getX(i).toInt() - paddingLeft,
                        event.getY(i).toInt() - paddingRight
                    )
                    if (keyIndex != NOT_A_KEY) {
                        val key = keyboardViewController.keys[keyIndex]
                        if (!key.isLed) movedKeysPressed.add(key)
                    }
                }
                if (!keysPressed.containsAll(movedKeysPressed)) {
                    keysPressed.forEach {
                        if (!movedKeysPressed.contains(it)) keyboardViewController.handleKeyRelease(
                            keyboard!!, it
                        )
                    }
                    movedKeysPressed.forEach {
                        if (!keysPressed.contains(it)) keyboardViewController.handleKeyPress(
                            keyboard!!,
                            it
                        )
                    }
                    keysPressed.pressed = false
                    keysPressed.addAll(movedKeysPressed)
                }
                keysPressed.invalidate()
                invalidate()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val keyIndex =
                    getKeyIndices(event.x.toInt() - paddingLeft, event.y.toInt() - paddingRight)
                if (keyIndex == NOT_A_KEY) return true
                val key = keyboardViewController.keys[keyIndex]
                if (key.isLed) return true
                removeLongPress(key)
                key.pressed = false
                keysPressed.invalidate()
                keysPressed.remove(key)
                keyboardViewController.keys.filter { it.sticky }.forEach {
                    invalidateKey(it)
                }
                invalidate()
            }

            MotionEvent.ACTION_CANCEL -> {
                keysPressed.invalidate()
                keysPressed.releaseAll()
                keysPressed.clear()
                invalidate()
            }
        }
        return true
    }

    private fun handleKeyPressed(key: KeyboardViewController.Key) {
        // click()
        keyboardViewController.handleKeyPress(keyboard!!, key)
    }

    private fun closing() {
        mBuffer = null
        canvas = null
    }


    public override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        closing()
    }

    companion object {
        private const val NOT_A_KEY = -1
        private const val MAX_NEARBY_KEYS = 12
        private const val MSG_LONG_PRESS = 4
        private val LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout().toLong()
    }

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        mPadding = Rect(0, 0, 0, 0)
        keyboardXmlResId = R.xml.adm3a
        var attributes = context.obtainStyledAttributes(
            attrs, R.styleable.KeyboardView, defStyleAttr, defStyleRes
        )
        val keyTextSize = 0
        for (i in 0 until attributes.indexCount) {
            when (val attr = attributes.getIndex(i)) {
                R.styleable.KeyboardView_defaultKeyBackground -> keyBackground =
                    attributes.getDrawable(attr)

                R.styleable.KeyboardView_verticalCorrection -> mVerticalCorrection =
                    attributes.getDimensionPixelOffset(attr, 0)

                R.styleable.KeyboardView_keyTextSize -> mKeyTextSize =
                    attributes.getDimensionPixelSize(attr, 28)

                R.styleable.KeyboardView_keyTextColor -> mKeyTextColor =
                    attributes.getColor(attr, -0x1000000)

                R.styleable.KeyboardView_labelTextSize -> mLabelTextSize =
                    attributes.getDimensionPixelSize(attr, 28)
            }
        }
        if (isInEditMode) {
            keyBackground = ResourcesCompat.getDrawable(resources, R.drawable.key, null)
            mKeyTextColor = resources.getColor(R.color.black, null)
            mKeyTextSize = resources.getDimensionPixelSize(R.dimen.keyboardTextSize)
            mLabelTextSize = resources.getDimensionPixelSize(R.dimen.keyboardLabelTextSize)
        }
        mBackgroundDimAmount =
            attributes.getFloat(R.styleable.KeyboardView_backgroundDimAmount, 0.5f)
        attributes.recycle()
        mPaint = Paint()
        mPaint.isAntiAlias = true
        mPaint.textSize = keyTextSize.toFloat()
        mPaint.textAlign = Paint.Align.CENTER
        mPaint.alpha = 255
        mPadding = Rect(0, 0, 0, 0)
        keyBackground?.getPadding(mPadding)
    }


    fun find(zxKey: Keys): KeyboardViewController.Key? {
        val searchFor = zxKey.name.removePrefix("Key").uppercase()
        return keyboardViewController.keys.firstOrNull {
            it.label.toString().uppercase() == searchFor ||
                    it.secondLabel.toString().uppercase() == searchFor ||
                    it.thirdLabel.toString().uppercase() == searchFor ||
                    it.fifthLabel.toString().uppercase() == searchFor
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val zxKey = keyboard?.onKeyUp(secondKeyAltLeft(keyCode)) ?: Keys.KeyNone
        if (zxKey != Keys.KeyNone) {
            find(zxKey)?.let {
                it.pressed = false
                invalidateKey(it)
            }
            return true
        } else {
            event?.unicodeChar?.toChar()?.let { char ->
                if (char.code != 0) {
                    keyboard?.onKeyReleaseEvent(char)
                    return true
                }
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    private var lastKeyCode = KeyEvent.KEYCODE_UNKNOWN
    private fun secondKeyAltLeft(keyCode: Int): Int {
        if (lastKeyCode == KeyEvent.KEYCODE_CTRL_LEFT && keyCode == KeyEvent.KEYCODE_ALT_LEFT) {
            lastKeyCode = KeyEvent.KEYCODE_UNKNOWN
            return KeyEvent.KEYCODE_ALT_RIGHT
        }
        lastKeyCode = keyCode
        return keyCode
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if ((event?.repeatCount ?: 0) > 0) return true

        val zxKey = keyboard?.onKeyDown(secondKeyAltLeft(keyCode)) ?: Keys.KeyNone
        if (zxKey != Keys.KeyNone) {
            find(zxKey)?.let {
                it.pressed = true
                invalidateKey(it)
            }
            return true
        } else {
            event?.unicodeChar?.toChar()?.let { char ->
                if (char.code != 0) {
                    keyboard?.onKeyEvent(char)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    fun Drawable.drawableToBitmap(): Bitmap? {
        if (this is BitmapDrawable)
            return bitmap

        val bitmap = createBitmap(intrinsicWidth, intrinsicHeight)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        return bitmap
    }
}