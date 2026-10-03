package com.aboveware.aboveabc80.keyboard

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.content.res.XmlResourceParser
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.TypedValue
import android.util.Xml
import com.aboveware.aboveabc80.R
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import kotlin.math.roundToInt

/**
 * Loads an XML description of a Keyboard and stores the attributes of the keys. A Keyboard
 * consists of rows of keys.
 *
 * The layout file for a Keyboard contains XML that looks like the following snippet:
 * <pre>
 * &lt;Keyboard
 * android:keyWidth="%10p"
 * android:keyHeight="50px"
 * android:horizontalGap="2px"
 * android:verticalGap="2px" &gt;
 * &lt;Row android:keyWidth="32px" &gt;
 * &lt;Key android:keyLabel="A" /&gt;
 * ...
 * &lt;/Row&gt;
 * ...
 * &lt;/Keyboard&gt;
</pre> *
 *
 * @attr ref android.R.styleable#Keyboard_keyWidth
 * @attr ref android.R.styleable#Keyboard_keyHeight
 * @attr ref android.R.styleable#Keyboard_horizontalGap
 * @attr ref android.R.styleable#Keyboard_verticalGap
 */

@ExperimentalUnsignedTypes
class KeyboardViewController(context: Context, xmlLayoutResId: Int, modeId: Int = 0) {
    /**
     * Default key background
     */
    var keyBackground: Drawable? = null
        private set

    var defaultLabelColor: Int = Color.WHITE
    var defaultSecondLabelColor: Int = Color.WHITE
    var defaultThirdLabelColor: Int = Color.WHITE
    var defaultFourthLabelColor: Int = Color.WHITE
    var defaultFifthLabelColor: Int = Color.WHITE
    var defaultSixthLabelColor: Int = Color.WHITE

    var defaultLabelUnderline: Boolean = false
    var defaultSecondLabelUnderline: Boolean = false
    var defaultThirdLabelUnderline: Boolean = false
    var defaultFourthLabelUnderline: Boolean = false
    var defaultFifthLabelUnderline: Boolean = false
    var defaultSixthLabelUnderline: Boolean = false

    /**
     * Horizontal gap default for all rows
     */
    private var horizontalGap = 0

    /**
     * Default key width
     */
    private var keyWidth = 0

    /**
     * Default key height
     */
    private var keyHeight = 0

    /**
     * Default gap between rows
     */
    private var verticalGap = 0

    /**
     * Returns the total height of the Keyboard
     *
     * @return the total height of the Keyboard
     */
    /**
     * Total height of the Keyboard, including the padding and keys
     */
    var height = 0
        private set

    /**
     * Total width of the Keyboard, including left side gaps and keys, but not any gaps on the
     * right side.
     */
    var minWidth = 0
        internal set

    /**
     * List of keys in this Keyboard
     */
    private var mKeys = mutableListOf<Key>()

    /**
     * List of modifier keys such as Shift & Alt, if any
     */
    private var mModifierKeys = mutableListOf<Key>()

    /**
     * Width of the screen available to fit the Keyboard
     */
    private var mDisplayWidth = 0

    /**
     * Height of the screen
     */
    private var mDisplayHeight = 0

    /**
     * Keyboard mode, or zero, if none.
     */
    private var mKeyboardMode = 0
    private var mCellWidth = 0
    private var mCellHeight = 0
    private var mGridNeighbors: Array<IntArray?>? = null
    private var mProximityThreshold = 0
    private val rows = ArrayList<Row?>()

    val characterMappings = mutableMapOf<Char, Char>()

    init {
        val dm = context.resources.displayMetrics
        mDisplayWidth = if (dm.widthPixels > 0) dm.widthPixels else 1080
        mDisplayHeight = if (dm.heightPixels > 0) dm.heightPixels else 1920
        horizontalGap = 0
        keyWidth = mDisplayWidth / 10
        verticalGap = 0
        keyHeight = keyWidth
        mKeys = ArrayList()
        mModifierKeys = ArrayList()
        mKeyboardMode = modeId
        loadKeyboard(context, context.resources.getXml(xmlLayoutResId))

        mKeys.forEach { key ->
            val label = key.label?.toString()
            val secondLabel = key.secondLabel?.toString()
            if (label?.length == 1 && secondLabel?.length == 1) {
                characterMappings[label[0]] = secondLabel[0]
            }
        }
    }

    class Row {
        /**
         * Default width of a key in this row.
         */
        var defaultWidth = 0

        /**
         * Default height of a key in this row.
         */
        var defaultHeight = 0

        /**
         * Default horizontal gap between keys in this row.
         */
        var defaultHorizontalGap = 0

        /**
         * Vertical gap following this row.
         */
        var verticalGap = 0

        /**
         * Initial gap of this row.
         */
        var horizontalGap = 0

        /**
         * Initial indent of this row.
         */
        var initialIndent = 0
        var mKeys = mutableListOf<Key>()

        /**
         * Edge flags for this row of keys. Possible values that can be assigned are
         * [EDGE_TOP][KeyboardViewController.EDGE_TOP] and [EDGE_BOTTOM][KeyboardViewController.EDGE_BOTTOM]
         */
        var rowEdgeFlags = 0

        /**
         * The Keyboard mode for this row
         */
        var mode = 0
        var parent: KeyboardViewController

        constructor(res: Resources, parent: KeyboardViewController, parser: XmlResourceParser?) {
            this.parent = parent
            var a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.Keyboard)
            defaultWidth = getDimensionOrFraction(
                a, R.styleable.Keyboard_keyWidth,
                parent.mDisplayWidth, parent.mDisplayWidth, parent.keyWidth
            )
            defaultHeight = getDimensionOrFraction(
                a, R.styleable.Keyboard_keyHeight,
                parent.mDisplayHeight, parent.mDisplayHeight, parent.keyHeight
            )
            defaultHorizontalGap = getDimensionOrFraction(
                a, R.styleable.Keyboard_horizontalGap,
                parent.mDisplayWidth, parent.mDisplayWidth, parent.horizontalGap
            )
            horizontalGap = getDimensionOrFraction(
                a, R.styleable.Keyboard_horizontalGap,
                parent.mDisplayWidth, parent.mDisplayWidth, parent.horizontalGap
            )
            verticalGap = getDimensionOrFraction(
                a, R.styleable.Keyboard_verticalGap,
                parent.mDisplayHeight, parent.mDisplayHeight, parent.verticalGap
            )
            a.recycle()
            a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.Keyboard_Row)
            rowEdgeFlags = a.getInt(R.styleable.Keyboard_Row_rowEdgeFlags, 0)
            mode = a.getResourceId(R.styleable.Keyboard_Row_keyboardMode, 0)
            initialIndent = getDimensionOrFraction(
                a, R.styleable.Keyboard_Row_initialIndent,
                defaultWidth, parent.mDisplayWidth, 0
            )
            verticalGap = getDimensionOrFraction(
                a, R.styleable.Keyboard_Row_verticalRowGap,
                parent.mDisplayHeight, parent.mDisplayHeight, verticalGap
            )
            a.recycle()
        }
    }

    /**
     * Class for describing the position and characteristics of a single key in the Keyboard.
     *
     * @attr ref android.R.styleable#Keyboard_keyWidth
     * @attr ref android.R.styleable#Keyboard_keyHeight
     * @attr ref android.R.styleable#Keyboard_horizontalGap
     * @attr ref android.R.styleable#Keyboard_Key_codes
     * @attr ref android.R.styleable#Keyboard_Key_keyIcon
     * @attr ref android.R.styleable#Keyboard_Key_keyLabel
     * @attr ref android.R.styleable#Keyboard_Key_iconPreview
     * @attr ref android.R.styleable#Keyboard_Key_isSticky
     * @attr ref android.R.styleable#Keyboard_Key_isRepeatable
     * @attr ref android.R.styleable#Keyboard_Key_isModifier
     * @attr ref android.R.styleable#Keyboard_Key_popupKeyboard
     * @attr ref android.R.styleable#Keyboard_Key_popupCharacters
     * @attr ref android.R.styleable#Keyboard_Key_keyOutputText
     * @attr ref android.R.styleable#Keyboard_Key_keyEdgeFlags
     */
    class Key(parent: Row?) {
        @JvmField
        var keyBackground: Drawable? = null

        @JvmField
        var padding: Rect? = null

        var codes = ""

        var label: CharSequence? = null
        var secondLabel: CharSequence? = null
        var thirdLabel: CharSequence? = null
        var fourthLabel: CharSequence? = null
        var fifthLabel: CharSequence? = null
        var sixthLabel: CharSequence? = null

        var labelColor: Int = Color.WHITE
        var secondLabelColor: Int = Color.WHITE
        var thirdLabelColor: Int = Color.WHITE
        var fourthLabelColor: Int = Color.WHITE
        var fifthLabelColor: Int = Color.WHITE
        var sixthLabelColor: Int = Color.WHITE

        var labelUnderline: Boolean = false
        var secondLabelUnderline: Boolean = false
        var thirdLabelUnderline: Boolean = false
        var fourthLabelUnderline: Boolean = false
        var fifthLabelUnderline: Boolean = false
        var sixthLabelUnderline: Boolean = false

        var secondKeyIcon: Drawable? = null
        var fourthKeyIcon: Drawable? = null

        var ledId: String? = null

        var horizontalAlignment = Paint.Align.CENTER
        var verticalAlignment = Paint.Align.CENTER
        var attachTo: CharSequence? = null
        var horizontalMargin = 0
        var customBitmap: Drawable? = null
        var isLed = false
        var ledColor: Int = Color.GREEN

        /**
         * Icon to display instead of a label. Icon takes precedence over a label
         */
        @JvmField
        var icon: Drawable? = null

        /**
         * Preview version of the icon, for the preview popup
         */
        @JvmField
        var iconPreview: Drawable? = null

        /**
         * Width of the key, not including the gap
         */
        var width: Int = 0

        /**
         * Height of the key, not including the gap
         */
        var height: Int = 0

        /**
         * The horizontal gap before this key
         */
        var gap: Int = 0

        /**
         * Whether this key is sticky, i.e., a toggle key with auto-release
         */
        @JvmField
        var sticky = false

        /**
         * Whether this key is sticky, i.e., a toggle key
         */
        @JvmField
        var verySticky = false

        /**
         * X coordinate of the key in the Keyboard layout
         */
        @JvmField
        var x = 0

        /**
         * Y coordinate of the key in the Keyboard layout
         */
        @JvmField
        var y = 0

        /**
         * The current pressed state of this key
         */
        @JvmField
        var pressed = false

        /**
         * If this is a sticky key, is it on?
         */
        @JvmField
        var on = false

        /**
         * Flags that specify the anchoring to edges of the Keyboard for detecting touch events
         * that are just out of the boundary of the key. This is a bit mask of
         * [KeyboardViewController.EDGE_LEFT], [KeyboardViewController.EDGE_RIGHT], [KeyboardViewController.EDGE_TOP] and
         * [KeyboardViewController.EDGE_BOTTOM].
         */
        var edgeFlags: Int = 0

        /**
         * Whether this is a modifier key, such as Shift or Alt
         */
        private var modifier = false

        /**
         * The Keyboard that this key belongs to
         */
        private val keyboard: KeyboardViewController = parent!!.parent


        /**
         * Whether this key repeats itself when held down
         */
        @JvmField
        var repeatable = false

        /**
         * Create a key with the given top-left coordinate and extract its attributes from
         * the XML parser.
         *
         * @param res    resources associated with the caller's context
         * @param parent the row that this key belongs to. The row must already be attached to
         * a [KeyboardViewController].
         * @param x      the x coordinate of the top-left
         * @param y      the y coordinate of the top-left
         * @param parser the XML parser containing the attributes for this key
         */
        constructor(
            res: Resources,
            parent: Row?,
            x: Int,
            y: Int,
            parser: XmlResourceParser?
        ) : this(parent) {
            this.x = x
            this.y = y
            var attributes = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.Keyboard)
            width = getDimensionOrFraction(
                attributes,
                R.styleable.Keyboard_keyWidth,
                keyboard.mDisplayWidth,
                keyboard.mDisplayWidth,
                parent!!.defaultWidth
            )
            height =
                getDimensionOrFraction(
                    attributes,
                    R.styleable.Keyboard_keyHeight,
                    keyboard.mDisplayHeight,
                    keyboard.mDisplayHeight,
                    parent.defaultHeight
                )
            gap = getDimensionOrFraction(
                attributes,
                R.styleable.Keyboard_horizontalGap,
                keyboard.mDisplayWidth,
                keyboard.mDisplayWidth,
                parent.defaultHorizontalGap
            )
            attributes.recycle()

            attributes = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.Keyboard_Key)
            this.x += gap
            codes = attributes.getString(R.styleable.Keyboard_Key_codes) ?: ""
            keyBackground = attributes.getDrawable(R.styleable.Keyboard_Key_keyBackground)
            padding = Rect(0, 0, 0, 0)
            if (keyBackground != null) {
                keyBackground!!.getPadding(padding!!)
            }
            iconPreview = attributes.getDrawable(R.styleable.Keyboard_Key_iconPreview)
            if (iconPreview != null) {
                iconPreview!!.setBounds(
                    0, 0, iconPreview!!.intrinsicWidth,
                    iconPreview!!.intrinsicHeight
                )
            }
            repeatable = attributes.getBoolean(R.styleable.Keyboard_Key_isRepeatable, true)
            modifier = attributes.getBoolean(R.styleable.Keyboard_Key_isModifier, false)
            sticky = attributes.getBoolean(R.styleable.Keyboard_Key_isSticky, false)
            verySticky = attributes.getBoolean(R.styleable.Keyboard_Key_isVerySticky, false)
            if (verySticky) sticky = true
            edgeFlags = attributes.getInt(R.styleable.Keyboard_Key_keyEdgeFlags, 0)
            edgeFlags = edgeFlags or parent.rowEdgeFlags
            icon = attributes.getDrawable(R.styleable.Keyboard_Key_keyIcon)
            icon?.apply {
                setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            }
            label = attributes.getText(R.styleable.Keyboard_Key_secondKeyLabel)
            labelColor = attributes.getColor(
                R.styleable.Keyboard_Key_secondKeyLabelColor,
                keyboard.defaultSecondLabelColor
            )
            secondLabel = attributes.getText(R.styleable.Keyboard_Key_keyLabel)
            secondLabelColor =
                attributes.getColor(
                    R.styleable.Keyboard_Key_keyLabelColor,
                    keyboard.defaultLabelColor
                )
            secondKeyIcon = attributes.getDrawable(R.styleable.Keyboard_Key_secondKeyIcon)
            secondKeyIcon?.apply {
                setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            }
            thirdLabel = attributes.getText(R.styleable.Keyboard_Key_forthKeyLabel)
            thirdLabelColor =
                attributes.getColor(
                    R.styleable.Keyboard_Key_forthKeyLabelColor,
                    keyboard.defaultFourthLabelColor
                )
            fourthLabel = attributes.getText(R.styleable.Keyboard_Key_thirdKeyLabel)
            fourthLabelColor =
                attributes.getColor(
                    R.styleable.Keyboard_Key_thirdKeyLabelColor,
                    keyboard.defaultThirdLabelColor
                )
            fourthKeyIcon = attributes.getDrawable(R.styleable.Keyboard_Key_forthKeyIcon)
            fourthKeyIcon?.apply {
                setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            }
            fifthLabel = attributes.getText(R.styleable.Keyboard_Key_sixthKeyLabel)
            fifthLabelColor =
                attributes.getColor(
                    R.styleable.Keyboard_Key_sixthKeyLabelColor,
                    keyboard.defaultSixthLabelColor
                )
            sixthLabel = attributes.getText(R.styleable.Keyboard_Key_fifthKeyLabel)
            sixthLabelColor =
                attributes.getColor(
                    R.styleable.Keyboard_Key_fifthKeyLabelColor,
                    keyboard.defaultFifthLabelColor
                )

            labelUnderline = attributes.getBoolean(
                R.styleable.Keyboard_Key_secondKeyLabelUnderline,
                keyboard.defaultSecondLabelUnderline
            )
            secondLabelUnderline = attributes.getBoolean(
                R.styleable.Keyboard_Key_keyLabelUnderline,
                keyboard.defaultLabelUnderline
            )
            thirdLabelUnderline = attributes.getBoolean(
                R.styleable.Keyboard_Key_forthKeyLabelUnderline,
                keyboard.defaultFourthLabelUnderline
            )
            fourthLabelUnderline = attributes.getBoolean(
                R.styleable.Keyboard_Key_thirdKeyLabelUnderline,
                keyboard.defaultThirdLabelUnderline
            )
            fifthLabelUnderline = attributes.getBoolean(
                R.styleable.Keyboard_Key_sixthKeyLabelUnderline,
                keyboard.defaultSixthLabelUnderline
            )
            sixthLabelUnderline = attributes.getBoolean(
                R.styleable.Keyboard_Key_fifthKeyLabelUnderline,
                keyboard.defaultFifthLabelUnderline
            )

            attachTo = attributes.getText(R.styleable.Keyboard_Key_keyAttachTo)
            customBitmap = attributes.getDrawable(R.styleable.Keyboard_Key_keyCustomBitmap)
            isLed = attributes.getBoolean(R.styleable.Keyboard_Key_isLed, false)
            ledColor = attributes.getColor(R.styleable.Keyboard_Key_ledColor, Color.GREEN)
            ledId = attributes.getString(R.styleable.Keyboard_Key_keyLedId)
            horizontalMargin =
                getDimensionOrFraction(
                    attributes,
                    R.styleable.Keyboard_Key_horizontalMargin,
                    keyboard.mDisplayWidth,
                    keyboard.mDisplayWidth,
                    0
                )
            horizontalAlignment = Paint.Align.entries.toTypedArray()[attributes.getInt(
                R.styleable.Keyboard_Key_keyLabelHorizontalAlignment,
                Paint.Align.CENTER.ordinal
            )]
            verticalAlignment = Paint.Align.entries.toTypedArray()[attributes.getInt(
                R.styleable.Keyboard_Key_keyLabelVerticalAlignment,
                Paint.Align.CENTER.ordinal
            )]
            attributes.recycle()
        }

        override fun toString(): String {
            return "Key{$label,$secondLabel,$thirdLabel,$fourthLabel,$fifthLabel,$sixthLabel}"
        }

        /**
         * Detects if a point falls inside this key.
         *
         * @param x the x-coordinate of the point
         * @param y the y-coordinate of the point
         * @return whether or not the point falls inside the key. If the key is attached to an edge,
         * it will assume that all points between the key and the edge are considered to be inside
         * the key.
         */
        fun isInside(x: Int, y: Int): Boolean {
            val leftEdge = false // edgeFlags and EDGE_LEFT > 0
            val rightEdge = false // edgeFlags and EDGE_RIGHT > 0
            val topEdge = false // edgeFlags and EDGE_TOP > 0
            val bottomEdge = false // edgeFlags and EDGE_BOTTOM > 0
            return ((x >= this.x || leftEdge && x <= this.x + width)
                    && (x < this.x + width || rightEdge && x >= this.x)
                    && (y >= this.y || topEdge && y <= this.y + height)
                    && (y < this.y + height || bottomEdge && y >= this.y))
        }

        /**
         * Returns the square of the distance between the center of the key and the given point.
         *
         * @param x the x-coordinate of the point
         * @param y the y-coordinate of the point
         * @return the square of the distance of the point from the center of the key
         */
        fun squaredDistanceFrom(x: Int, y: Int): Int {
            val xDist = this.x + width / 2 - x
            val yDist = this.y + height / 2 - y
            return xDist * xDist + yDist * yDist
        }

        /**
         * Returns the drawable state for the key, based on the current state and type of the key.
         *
         * @return the drawable state of the key.
         * @see android.graphics.drawable.StateListDrawable.setState
         */
        val currentDrawableState: IntArray
            get() {
                var states = KEY_STATE_NORMAL
                if (on) {
                    states = if (sticky) {
                        KEY_STATE_PRESSED_ON
                    } else {
                        if (pressed) {
                            KEY_STATE_PRESSED_ON
                        } else {
                            KEY_STATE_NORMAL_ON
                        }
                    }
                } else {
                    if (sticky) {
                        states = if (pressed) {
                            KEY_STATE_PRESSED_OFF
                        } else {
                            KEY_STATE_NORMAL_OFF
                        }
                    } else {
                        if (pressed) {
                            states = KEY_STATE_PRESSED
                        }
                    }
                }
                return states
            }

        companion object {
            private val KEY_STATE_NORMAL_ON = intArrayOf(
                android.R.attr.state_checkable,
                android.R.attr.state_checked
            )
            private val KEY_STATE_PRESSED_ON = intArrayOf(
                android.R.attr.state_pressed,
                android.R.attr.state_checkable,
                android.R.attr.state_checked
            )
            private val KEY_STATE_NORMAL_OFF = intArrayOf(
                android.R.attr.state_checkable
            )
            private val KEY_STATE_PRESSED_OFF = intArrayOf(
                android.R.attr.state_pressed,
                android.R.attr.state_checkable
            )
            private val KEY_STATE_NORMAL = intArrayOf()
            val KEY_STATE_PRESSED = intArrayOf(
                android.R.attr.state_pressed
            )
        }


        /**
         * Create an empty key with no attributes.
         */
        init {
            parent?.let {
                height = parent.defaultHeight
                width = parent.defaultWidth
                gap = parent.defaultHorizontalGap
                edgeFlags = parent.rowEdgeFlags
            }
        }
    }

    private fun applyAttachToAlignment() {
        mKeys.forEachIndexed { index, k ->
            val target = k.attachTo?.toString()?.trim() ?: return@forEachIndexed
            if (target.isEmpty()) return@forEachIndexed

            val keyToAttach = mKeys.find {
                it.codes.split(",").any { c -> c.trim() == target }
            }

            if (keyToAttach != null) {
                val gap = keyToAttach.x - k.x
                for (i in index until mKeys.size) {
                    val currentKey = mKeys[i]
                    if (currentKey.y != k.y) break
                    currentKey.x += gap
                    if (i == index) {
                        currentKey.gap += gap
                    }
                }
            }
        }
    }

    fun findMostRightKeyXPosition(): Int {
        // Find the x position of the most right key
        return mKeys.maxOfOrNull { it.x + it.width } ?: 0
    }

    fun resize(newWidth: Int) {
        if (newWidth <= 0 || rows.isEmpty() || mKeys.isEmpty()) return

        val currentRight = findMostRightKeyXPosition()
        val currentLeft = mKeys.minOf { it.x }
        val currentWidth = (currentRight - currentLeft).toFloat()
        if (currentWidth <= 0f) return
        val scaleFactor = newWidth.toFloat() / currentWidth

        var currentY = 0
        rows.forEach {
            it?.let { row ->
                var x = (row.initialIndent * scaleFactor).toInt()
                row.verticalGap = (row.verticalGap * scaleFactor).toInt()
                row.defaultHeight = (row.defaultHeight * scaleFactor).toInt()

                row.mKeys.forEach { key ->
                    key.width = (key.width * scaleFactor).toInt()
                    key.height = (key.height * scaleFactor).toInt()
                    key.gap = (key.gap * scaleFactor).toInt()
                    key.horizontalMargin = (key.horizontalMargin * scaleFactor).toInt()

                    x += key.gap
                    key.x = x
                    key.y = currentY
                    x += key.width + key.horizontalMargin
                }
                currentY += row.defaultHeight + row.verticalGap
            }
        }

        applyAttachToAlignment()

        minWidth = newWidth
        height = currentY

        val rightmost = findMostRightKeyXPosition()
        val leftmost = mKeys.minOf { it.x }
        val keyboardWidth = rightmost - leftmost
        val targetLeft = (newWidth - keyboardWidth) / 2
        val shift = targetLeft - leftmost
        if (shift != 0) {
            mKeys.forEach { key ->
                key.x += shift
            }
        }
    }

    val keys: MutableList<Key>
        get() = mKeys

    private fun computeNearestNeighbors() {
        // Round-up so we don't have any pixels outside the grid
        mCellWidth = (minWidth + GRID_WIDTH - 1) / GRID_WIDTH
        mCellHeight = (height + GRID_HEIGHT - 1) / GRID_HEIGHT
        mGridNeighbors = arrayOfNulls(GRID_SIZE)
        val indices = IntArray(mKeys.size)
        val gridWidth = GRID_WIDTH * mCellWidth
        val gridHeight = GRID_HEIGHT * mCellHeight
        var x = 0
        while (x < gridWidth) {
            var y = 0
            while (y < gridHeight) {
                var count = 0
                for (i in mKeys.indices) {
                    val key = mKeys[i]
                    if (key.squaredDistanceFrom(
                            x,
                            y
                        ) < mProximityThreshold || key.squaredDistanceFrom(
                            x + mCellWidth - 1,
                            y
                        ) < mProximityThreshold || (key.squaredDistanceFrom(
                            x + mCellWidth - 1,
                            y + mCellHeight - 1
                        )
                                < mProximityThreshold) || key.squaredDistanceFrom(
                            x,
                            y + mCellHeight - 1
                        ) < mProximityThreshold
                    ) {
                        indices[count++] = i
                    }
                }
                val cell = IntArray(count)
                System.arraycopy(indices, 0, cell, 0, count)
                mGridNeighbors!![y / mCellHeight * GRID_WIDTH + x / mCellWidth] = cell
                y += mCellHeight
            }
            x += mCellWidth
        }
    }

    /**
     * Returns the indices of the keys that are closest to the given point.
     *
     * @param x the x-coordinate of the point
     * @param y the y-coordinate of the point
     * @return the array of integer indices for the nearest keys to the given point. If the given
     * point is out of range, then an array of size zero is returned.
     */
    fun getNearestKeys(x: Int, y: Int): IntArray? {
        if (mGridNeighbors == null) computeNearestNeighbors()
        if (x in 0 until minWidth && y >= 0 && y < height) {
            val index = y / mCellHeight * GRID_WIDTH + x / mCellWidth
            if (index < GRID_SIZE) {
                return mGridNeighbors!![index]
            }
        }
        return IntArray(0)
    }

    private fun createRowFromXml(res: Resources, parser: XmlResourceParser?): Row {
        return Row(res, this, parser)
    }

    private fun createKeyFromXml(
        res: Resources, parent: Row?, x: Int, y: Int,
        parser: XmlResourceParser?
    ): Key {
        return Key(res, parent, x, y, parser)
    }

    private fun loadKeyboard(context: Context, parser: XmlResourceParser) {
        var inKey = false
        var inRow = false
        var row = 0
        var x = 0
        var y = 0
        var key: Key? = null
        var currentRow: Row? = null
        val res = context.resources
        var skipRow: Boolean
        try {
            var event: Int
            while (parser.next().also { event = it } != XmlResourceParser.END_DOCUMENT) {
                if (event == XmlResourceParser.START_TAG) {
                    val tag = parser.name
                    if (XML_ROW_TAG == tag) {
                        inRow = true
                        currentRow = createRowFromXml(res, parser)
                        x = currentRow.initialIndent
                        rows.add(currentRow)
                        skipRow = currentRow.mode != 0 && currentRow.mode != mKeyboardMode
                        if (skipRow) {
                            skipToEndOfRow(parser)
                            inRow = false
                        }
                    } else if (XML_KEY_TAG == tag) {
                        inKey = true
                        key = createKeyFromXml(res, currentRow, x, y, parser)
                        mKeys.add(key)
                        currentRow!!.mKeys.add(key)
                    } else if (XML_KEYBOARD_TAG == tag) {
                        parseKeyboardAttributes(res, parser)
                    }
                } else if (event == XmlResourceParser.END_TAG) {
                    if (inKey) {
                        inKey = false
                        key?.let { k ->
                            x += k.gap + k.width + k.horizontalMargin
                        }
                        if (x > minWidth) {
                            minWidth = x
                        }
                    } else if (inRow) {
                        inRow = false
                        y += currentRow!!.verticalGap
                        y += currentRow.defaultHeight
                        row++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse error:$e")
            e.printStackTrace()
        }
        height = if (y > 0) y - (currentRow?.verticalGap ?: verticalGap) else 0
        if (height <= 0 && rows.isNotEmpty()) {
            // Fallback if height calculation resulted in 0 but we have rows
            rows.forEach { r ->
                height += (r?.defaultHeight ?: 50) + (r?.verticalGap ?: 0)
            }
            if (height > 0) height -= (rows.lastOrNull()?.verticalGap ?: verticalGap)
        }

        applyAttachToAlignment()

        mKeys.forEach { k ->
            if (k.x + k.width > minWidth) {
                minWidth = k.x + k.width
            }
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun skipToEndOfRow(parser: XmlResourceParser) {
        var event: Int
        while (parser.next().also { event = it } != XmlResourceParser.END_DOCUMENT) {
            if (event == XmlResourceParser.END_TAG
                && parser.name == XML_ROW_TAG
            ) {
                break
            }
        }
    }

    private fun parseKeyboardAttributes(res: Resources, parser: XmlResourceParser) {
        val attribute = res.obtainAttributes(
            Xml.asAttributeSet(parser),
            R.styleable.Keyboard
        )
        keyBackground = attribute.getDrawable(R.styleable.Keyboard_keyBackground)
        defaultLabelColor = attribute.getColor(R.styleable.Keyboard_keyLabelColor, Color.WHITE)
        defaultSecondLabelColor =
            attribute.getColor(R.styleable.Keyboard_secondKeyLabelColor, Color.WHITE)
        defaultThirdLabelColor =
            attribute.getColor(R.styleable.Keyboard_thirdKeyLabelColor, Color.WHITE)
        defaultFourthLabelColor =
            attribute.getColor(R.styleable.Keyboard_forthKeyLabelColor, Color.WHITE)
        defaultFifthLabelColor =
            attribute.getColor(R.styleable.Keyboard_fifthKeyLabelColor, Color.WHITE)
        defaultSixthLabelColor =
            attribute.getColor(R.styleable.Keyboard_sixthKeyLabelColor, Color.WHITE)
        defaultLabelUnderline =
            attribute.getBoolean(R.styleable.Keyboard_secondKeyLabelUnderline, false)
        defaultSecondLabelUnderline =
            attribute.getBoolean(R.styleable.Keyboard_keyLabelUnderline, false)
        defaultThirdLabelUnderline =
            attribute.getBoolean(R.styleable.Keyboard_forthKeyLabelUnderline, false)
        defaultFourthLabelUnderline =
            attribute.getBoolean(R.styleable.Keyboard_thirdKeyLabelUnderline, false)
        defaultFifthLabelUnderline =
            attribute.getBoolean(R.styleable.Keyboard_sixthKeyLabelUnderline, false)
        defaultSixthLabelUnderline =
            attribute.getBoolean(R.styleable.Keyboard_fifthKeyLabelUnderline, false)

        keyWidth = getDimensionOrFraction(
            attribute,
            R.styleable.Keyboard_keyWidth,
            mDisplayWidth, mDisplayWidth, mDisplayWidth / 10
        )
        keyHeight = getDimensionOrFraction(
            attribute,
            R.styleable.Keyboard_keyHeight,
            mDisplayHeight, mDisplayHeight, 50
        )
        horizontalGap = getDimensionOrFraction(
            attribute,
            R.styleable.Keyboard_horizontalGap,
            mDisplayWidth, mDisplayWidth, 0
        )
        verticalGap = getDimensionOrFraction(
            attribute,
            R.styleable.Keyboard_verticalGap,
            mDisplayHeight, mDisplayHeight, 0
        )
        mProximityThreshold = (keyWidth * SEARCH_DISTANCE).toInt()
        mProximityThreshold *= mProximityThreshold // Square it for comparison
        attribute.recycle()
    }

    fun forEach(action: (Key) -> Unit) = keys.forEach(action)

    companion object {
        const val TAG = "Keyboard"

        // Keyboard XML Tags
        private const val XML_KEYBOARD_TAG = "Keyboard"
        private const val XML_ROW_TAG = "Row"
        private const val XML_KEY_TAG = "Key"
        const val EDGE_LEFT = 0x01
        const val EDGE_RIGHT = 0x02
        const val EDGE_TOP = 0x04
        const val EDGE_BOTTOM = 0x08

        // Variables for pre-computing nearest keys.
        private const val GRID_WIDTH = 10
        private const val GRID_HEIGHT = 5
        private const val GRID_SIZE = GRID_WIDTH * GRID_HEIGHT

        /**
         * Number of key widths from current touch point to search for nearest keys.
         */
        private const val SEARCH_DISTANCE = 1.8f
        fun getDimensionOrFraction(
            a: TypedArray,
            index: Int,
            base: Int,
            pbase: Int,
            defValue: Int
        ): Int {
            val value = a.peekValue(index) ?: return defValue
            if (value.type == TypedValue.TYPE_DIMENSION) {
                return a.getDimensionPixelOffset(index, defValue)
            } else if (value.type == TypedValue.TYPE_FRACTION) {
                // Round it to avoid values like 47.9999 from getting truncated
                return a.getFraction(index, base, pbase, defValue.toFloat()).roundToInt()
            }
            return defValue
        }
    }

    fun handleKeyRelease(keyboard: Keyboard, key: Key) {
        if (key.sticky) {
            key.on = !key.on
        }

        if (!key.on) keyboard.onKeyUp(key.codes)

        if (!key.sticky) {
            val stickiesOn = keys.filter { it.sticky && it.on && !it.verySticky }
            stickiesOn.forEach {
                keyboard.onKeyUp(it.codes)
                it.on = false
            }
        }
    }

    /**
     * Send a key press to the listener.
     * If we're running the emulator we can not hold multiple keys down.
     * If a key is sticky and on is true we've long pressed this one and should act as we're
     * pressing it.
     */
    fun handleKeyPress(keyboard: Keyboard, key: Key) {
        keys.filter { it != key && it.sticky && it.on }.forEach {
            keyboard.onKeyDown(
                it.codes,
                it.label?.toString(),
                it.secondLabel?.toString(),
                it.thirdLabel?.toString(),
                it.fourthLabel?.toString(),
                it.fifthLabel?.toString(),
                it.sixthLabel?.toString()
            )
        }
        keyboard.onKeyDown(
            key.codes,
            key.label?.toString(),
            key.secondLabel?.toString(),
            key.thirdLabel?.toString(),
            key.fourthLabel?.toString(),
            key.fifthLabel?.toString(),
            key.sixthLabel?.toString()
        )
        Thread.sleep(10)
    }
}