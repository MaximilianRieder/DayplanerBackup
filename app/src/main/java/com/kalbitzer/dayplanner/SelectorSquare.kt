package com.kalbitzer.dayplanner

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.view.children
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import com.kalbitzer.dayplanner.SystemUtil.pxToDp
import com.kalbitzer.dayplanner.TimeUtil.getTimeFromPixelPosition
import com.kalbitzer.dayplanner.database.Appointment
import com.kalbitzer.dayplanner.database.AppointmentViewModel
import com.kalbitzer.dayplanner.databinding.FragmentDayViewBinding
import java.util.*


const val BUTTON_DRAWN_RADIUS_SIZE = 20f
const val RECTANGLE_MIN_SIZE = 60
const val BUTTON_HITBOX_SIZE = 60

private const val STROKE_WIDTH = 10f
private const val BORDER_FRACTION = 0.1
private const val BORDER_TEXTFIELD = 0.1
private const val BUTTON_TOP_HORIZONTAL_FRACTION = 0.2
private const val BUTTON_BOTTOM_HORIZONTAL_FRACTION = 0.7
// how much smaller the text is compared to the tilesize
private const val TEXT_TILE_OFFSET = 0.15

private const val LONG_CLICK_DURATION = 300


class SelectorSquare @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    val binding: FragmentDayViewBinding,
    private val tileSize: Float,
    private val date: Date,
    private var titleText: String = "",
    private val initialSquarePosition: Float = 0f,
    private var sizeInPixels: Float = 200f,
    var appointmentId: Long = -1L

) : RelativeLayout(
    context,
    attrs,
    defStyleAttr
) {
    lateinit var mAppointmentViewModel: AppointmentViewModel

    var topBorder: Float = 0f
    var bottomBorder = topBorder + sizeInPixels
    val paint = Paint()
    val titleField = CompatibleEditText(context, this)
    val dragButtonTop = DragButton(context, this, true)
    val dragButtonBottom = DragButton(context, this, false)

    var buttonPositionTop = 0F
    var buttonPositionBottom = 0F
    private var isInitialized = false

    val deleteDialog = deleteDialog()

    //var openedDeleteDialog = false
    var isDragMode = false
    var isSaved = false
    var dialogIsYes = false

    init {
        paint.color = ColorGenerator().getColor()
        isClickable = true
        setWillNotDraw(false)
        initTextField()
        setDragAndDrop()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mAppointmentViewModel =
            ViewModelProvider(ViewTreeViewModelStoreOwner.get(this)!!)[AppointmentViewModel::class.java]
        setAtPosition()
    }

    /**
     * First time this object gets a size, buttons and params get initialized, on all other size
     * changes do nothing
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (isInitialized) {
            return
        }
        buttonPositionTop = topBorder
        buttonPositionBottom = bottomBorder - BUTTON_HITBOX_SIZE

        val params = LayoutParams(BUTTON_HITBOX_SIZE, BUTTON_HITBOX_SIZE)
        params.leftMargin = (width * BUTTON_TOP_HORIZONTAL_FRACTION).toInt()
        params.topMargin = buttonPositionTop.toInt()
        addView(dragButtonTop, params)

        val paramsBot = LayoutParams(BUTTON_HITBOX_SIZE, BUTTON_HITBOX_SIZE)
        paramsBot.leftMargin = (width * BUTTON_BOTTOM_HORIZONTAL_FRACTION).toInt()
        paramsBot.topMargin = buttonPositionBottom.toInt()
        addView(dragButtonBottom, paramsBot)

        // causes the view to correctly update when the children are changed
        val handler = Handler(Looper.getMainLooper())
        handler.post { requestLayout() }

        isInitialized = true
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        /*if (isDragMode){
            var paint2 = Paint()
            paint2.color = Color.GRAY
            canvas.drawRoundRect(
                (width * LEFT_BORDER_FRACTION).toFloat(),
                bottomBorder,
                (width * RIGHT_BORDER_FRACTION).toFloat(),
                topBorder,
                20F,
                20F,
                paint2
            )
        }*/
        canvas.drawRoundRect(
            (width * BORDER_FRACTION).toFloat(),
            bottomBorder,
            (width * (1 - BORDER_FRACTION)).toFloat(),
            topBorder,
            20F,
            20F,
            paint
        )
        val justOneTile = height < (1.5 * tileSize)
        setTextFieldPadding(justOneTile)
    }

    fun initTextField() {
        this.isFocusableInTouchMode = true
        this.isFocusable = true
        titleField.inputType = InputType.TYPE_CLASS_TEXT
        titleField.imeOptions = EditorInfo.IME_ACTION_DONE
        titleField.gravity = Gravity.LEFT
        titleField.layoutParams = LinearLayout.LayoutParams(
            LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        titleField.inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE
        titleField.setLines(3) // Set the number of lines
        titleField.maxLines = 3 // Set the maximum number of lines
        titleField.isSingleLine = false
        titleField.maxEms = 10 // Set the maximum width in terms of "M" characters
        titleField.setHorizontallyScrolling(false)
//
//        val pixelSize = tileSize - (TEXT_TILE_OFFSET * tileSize)
//        titleField.setTextSize(TypedValue.COMPLEX_UNIT_PX, pixelSize.toFloat())

//        // Set the auto-sizing text type uniformly with configuration
//        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
//            titleField,
//            8, // Min text size in scaled pixels
//            24, // Max text size in scaled pixels
//            2, // Step granularity in scaled pixels
//            TypedValue.COMPLEX_UNIT_SP // Units for the text sizes
//        )

        addView(titleField)
        titleField.previousText = titleText
        titleField.setOnEditorActionListener { textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                titleText = textView.text.toString()
                titleField.previousText = titleText
                saveToDatabase()
                isSaved = true
                false
            }
            false
        }
    }

    /**
     * sets the padding of the text in SS depending if the SS is only one or more tiles big
     */
    private fun setTextFieldPadding(justOneTile: Boolean) {
        val paddingWidth = ((this.width * BORDER_FRACTION) + BUTTON_HITBOX_SIZE * 0.75).toInt()
        val paddingHeight = if(justOneTile) (BUTTON_HITBOX_SIZE * 0.3).toInt() else (BUTTON_HITBOX_SIZE * 0.8).toInt()
        titleField.setPadding(paddingWidth, paddingHeight, paddingWidth, paddingHeight)
    }

    /**
     * Save this SelectorSquare as an Appointment in the Database
     */
    fun saveToDatabase() {
        val layoutParams = this.layoutParams as FrameLayout.LayoutParams
        val startTime =
            getTimeFromPixelPosition(layoutParams.topMargin.toFloat(), tileSize).toString()
        val endTime = getTimeFromPixelPosition(
            layoutParams.topMargin + layoutParams.height.toFloat(), tileSize
        ).toString()
        if (appointmentId < 0) {
            val appointment = Appointment(titleText, startTime, endTime, date)
            val id = mAppointmentViewModel.insert(appointment)
            appointmentId = id
        } else {
            val appointment = Appointment(appointmentId, titleText, startTime, endTime, date)
            mAppointmentViewModel.update(appointment)
        }
    }


    /** This changes back the size of this object to perfectly fit the drawn rectangle
     * This sets the top Margin to where it was drawn in the matchParent view so its starts at y=0
     * again.
     */
    fun transformToMatchContent() {
        val targetHeightNew = (bottomBorder - topBorder)
        val params: FrameLayout.LayoutParams = FrameLayout.LayoutParams(
            LayoutParams.MATCH_PARENT,
            targetHeightNew.toInt()
        )

        params.topMargin = topBorder.toInt()
        layoutParams = params

        dragButtonTop.y = 0f
        val layoutBottomButton = dragButtonBottom.layoutParams as LayoutParams
        layoutBottomButton.topMargin -= topBorder.toInt()

//        Log.e("bug-button", "topboarder: $topBorder")
//        Log.e("bug-button", "topposButton: ${dragButtonTop.y}")
//        Log.e("bug-button", "topbutton: ${layoutTopButton.topMargin - topBorder}")


        bottomBorder -= topBorder
        topBorder = 0F
        invalidate()
        saveToDatabase()
    }


    /**
     * In order to enable increasing the size, the canvas must be increased to fit the parents size.
     * This moves the view to y=0 on parent view and adjusts where it is drawn so it looks the same
     */
    fun transformToMatchParentLayout() {
        val sizeSquare = bottomBorder - topBorder
        topBorder = this.y
        bottomBorder = this.y + sizeSquare
        val layoutTopButton = dragButtonTop.layoutParams as LayoutParams
        layoutTopButton.topMargin += this.y.toInt()
        val layoutBottomButton = dragButtonBottom.layoutParams as LayoutParams
        layoutBottomButton.topMargin += this.y.toInt()
        val params = layoutParams as FrameLayout.LayoutParams
        if (params != null) {
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
            params.topMargin = 0
            layoutParams = params
        }
        invalidate()
    }

    /**
     * sets listeners and logic for drag and drop of the selector square
     */
    @SuppressLint("ClickableViewAccessibility")
    fun setDragAndDrop() {
        this.setOnTouchListener(object : OnTouchListener {
            var firstEventPos = 0f
            var firstTopMargin = 0f
            var firstTimeTriggered = true
            var firstPressed: Long = 0
            var isDragAndDrop = false
            var boundaries: Rect? = null
            override fun onTouch(view: View, event: MotionEvent): Boolean {
                // when in drag and drop mode
                if (isDragAndDrop) {
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true)
                    }
                    val par = view.layoutParams as FrameLayout.LayoutParams
                    when (event.action) {
                        MotionEvent.ACTION_MOVE -> {
                            if (firstTimeTriggered) {
                                firstEventPos = event.rawY
                                firstTopMargin = par.topMargin.toFloat()
                                firstTimeTriggered = false
                                return true
                            }
                            moveSelectorSquare(view, event, par, firstEventPos, firstTopMargin)
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            moveSelectorSquare(view, event, par, firstEventPos, firstTopMargin)
                            saveToDatabase()
                            isDragAndDrop = false
                            firstTimeTriggered = true
                            return true

                        }
                        MotionEvent.ACTION_DOWN -> {
                            return true
                        }
                    }
                } else {
                    // when not in drag and drop mode
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            // save position and time of the initial click
                            if (parent != null) {
                                parent.requestDisallowInterceptTouchEvent(true)
                            }
                            boundaries = Rect(
                                event.x.toInt() - 20,
                                event.y.toInt() - 20,
                                event.x.toInt() + 20,
                                event.y.toInt() + 20
                            )
                            firstPressed = System.currentTimeMillis()
                            Thread {
                                while (true) {
                                    if (System.currentTimeMillis() - firstPressed > LONG_CLICK_DURATION) {
                                        SystemUtil.createShortVibration(context)
                                        break
                                    }
                                    Thread.sleep(10)
                                }
                            }.start()
                            return false
                        }
                        MotionEvent.ACTION_MOVE -> {
                            // propagate event to parent view if moved out of bounds -> for scrolling
                            return if (boundaries!!.contains(
                                    event.x.toInt(),
                                    event.y.toInt()
                                )
                            ) {
                                true
                            } else {
                                // longclick -> set drag and drop -> otherwise abort
                                if (System.currentTimeMillis() - firstPressed > 300) {
                                    isDragAndDrop = true
                                    false
                                } else {
                                    firstPressed = System.currentTimeMillis()
                                    if (parent != null) {
                                        parent.requestDisallowInterceptTouchEvent(false)
                                    }
                                    false
                                }
                            }
                        }
                        MotionEvent.ACTION_UP -> {
                            // if long clicked and not drag and drop
                            if ((System.currentTimeMillis() - firstPressed > 300) && (boundaries!!.contains(
                                    event.x.toInt(),
                                    event.y.toInt()
                                ))
                            ) {
                                val fragment: DayViewFragment = FragmentManager.findFragment(view)
                                fragment.openedDeleteDialog = true
                                deleteDialog.show()
                                isDragAndDrop = false
                                return false
                            } else if (!isSaved) {
                                // normal click on current ss -> do nothing
                                return false
                            } else {
                                // otherwise go to title field
                                if (parent != null) {
                                    parent.requestDisallowInterceptTouchEvent(false)
                                }
                                isDragAndDrop = false
                                handler.postDelayed(
                                    {
                                        titleField.requestFocus()
                                    },
                                    100
                                ) //This is necessary because otherwise the keyboard will not enter into editText
                                return false
                            }
                        }
                    }
                }
                return false
            }
        })
    }


    /**
     * moves the selector square to the new position (clipped)
     * return: the new position
     */
    fun moveSelectorSquare(
        view: View,
        event: MotionEvent,
        par: FrameLayout.LayoutParams,
        firstEventPos: Float,
        firstTopMargin: Float
    ) {
        var newTopMargin = firstTopMargin + event.rawY - firstEventPos
        newTopMargin = clipToTile(newTopMargin)
        var newBottomPos = newTopMargin + view.height
        var bottomBorder = (view.parent as View).height

        if (checkCollisionMoveSS(newTopMargin, newBottomPos)) {
            return
        }
        if ((newTopMargin < 0) || (newBottomPos > bottomBorder))
            return

        par.topMargin = newTopMargin.toInt()
        view.layoutParams = par
    }

    /**
     * check if ss overlaps with another ss
     * return true if collision happens
     */
    private fun checkCollisionMoveSS(yTop: Float, yBottom: Float): Boolean {
        //todo evlt refactor for better performance -> just get once
        val ssList = getSelectorSquares()
        for (ss in ssList) {
            if (appointmentId == ss.appointmentId) {
                continue
            }
            val otherSsPositions = getRelativeTopBot(ss)
            if ((yTop.toInt() < otherSsPositions["bot"]!!) && (yTop.toInt() >= otherSsPositions["top"]!!)) {
                return true
            }
            if ((yTop.toInt() < otherSsPositions["top"]!!) && (yBottom.toInt() > otherSsPositions["bot"]!!)) {
                return true
            }
            if ((yBottom.toInt() <= otherSsPositions["bot"]!!) && (yBottom.toInt() > otherSsPositions["top"]!!)) {
                return true
            }
        }
        return false
    }

    /**
     * returns all selector squares in hourDropArea
     */
    fun getSelectorSquares(): List<SelectorSquare> {
        var ssList = mutableListOf<SelectorSquare>()
        for (childView in binding.hourDropArea.children) {
            if (childView is SelectorSquare) {
                ssList.add(childView)
            }
        }
        return ssList
    }

    /**
     * returns dictionary with keys top and bot for the relative positions of the ss to hour drop area
     */
    fun getRelativeTopBot(childView: View): Map<String, Int> {
        var parentViewGroup = binding.hourDropArea
        val offsetViewBounds = Rect()
        childView.getDrawingRect(offsetViewBounds)
        parentViewGroup.offsetDescendantRectToMyCoords(
            childView,
            offsetViewBounds
        )
        val relativeTop: Int = offsetViewBounds.top
        val relativeBot: Int = offsetViewBounds.bottom
        return mapOf("top" to relativeTop, "bot" to relativeBot)
    }

    /**
     * function that returns the respective top position of the hour the selector square would be
     * drawn on
     */
    fun clipToTile(yPos: Float): Float {
        if (yPos.toInt() == 0)
            return 0f
        val halfTileSize = tileSize / 2
        val remaining = yPos % tileSize
        val tileFactorFloor = yPos.toInt() / tileSize.toInt()
        return if (remaining < halfTileSize) {
            tileFactorFloor * tileSize
        } else {
            (tileFactorFloor + 1) * tileSize
        }
    }

    /**
     * Sets the position as specified in the class parameter. Clips to the next available spot
     */
    private fun setAtPosition() {
        val params: FrameLayout.LayoutParams = FrameLayout.LayoutParams(
            LayoutParams.MATCH_PARENT,
            sizeInPixels.toInt()
        )
        params.topMargin = clipToTile(initialSquarePosition).toInt()
        layoutParams = params
        titleField.setText(titleText)
    }

    private fun deleteDialog(): AlertDialog {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Delete item")
        builder.setMessage("Do you want to delete this Appointment?")
        builder.setPositiveButton("Yes") { dialog, which ->
            val fragment: DayViewFragment = FragmentManager.findFragment(this)
            fragment.openedDeleteDialog = false
            dialogIsYes = true
            mAppointmentViewModel.delete(appointmentId)
            val parentview = parent as ViewGroup
            parentview.removeView(this)
        }
        builder.setNegativeButton("No") { dialog, which ->
            dialog.dismiss()
        }
        val deleteDialog = builder.create()
        deleteDialog.setOnDismissListener {
            // on Dismiss except for yes calls
            if (!dialogIsYes) {
                val fragment: DayViewFragment = FragmentManager.findFragment(this)
                if (fragment.isInEditText) {
                    if (fragment.previousSelectedSS != null) {
                        fragment.isInEditText = false
                        fragment.previousSelectedSS!!.titleField.requestFocus()
                        fragment.isInEditText = true
                    } else {
                        fragment.isInEditText = false
                        titleField.requestFocus()
                        fragment.isInEditText = true
                    }
                }
                fragment.openedDeleteDialog = false
            }
            dialogIsYes = false
        }
        return deleteDialog
    }

    fun increaseSizeDownwards(targetSize:Int){
        val targetPosition = targetSize + topBorder
        dragButtonBottom.increaseSizeDownwards(targetPosition.toInt())
    }
}