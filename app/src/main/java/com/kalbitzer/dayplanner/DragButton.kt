package com.kalbitzer.dayplanner

import android.R.attr.path
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import androidx.fragment.app.FragmentManager
import java.lang.Integer.MAX_VALUE


class DragButton(
    context: Context?,
    private var selectorSquare: SelectorSquare,
    private val isTopPosition: Boolean
) :
    androidx.appcompat.widget.AppCompatButton(
        context!!
    ) {
    private var buttonClicked = false
    private var resumeFocus = false
    private var paint = Paint()

    init {
        this.background.alpha = 0
        paint.color = Color.BLACK
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {


        if (event != null) {

            selectorSquare.isDragMode = true

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val fragment: DayViewFragment = FragmentManager.findFragment(this)
                    if (selectorSquare.titleField.hasFocus()) {
                        resumeFocus = true
                    }
                    selectorSquare.titleField.visibility = View.INVISIBLE
                    selectorSquare.isDragMode = true
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true)
                    }
                    buttonClicked = true
                    selectorSquare.transformToMatchParentLayout()
                    SystemUtil.createShortVibration(context)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (buttonClicked) {

                        if (isTopPosition) {
                            if(!moveTopButton(event.y))
                                return false
                        } else {
                            if (!moveBottomButton(event.y))
                                return false
                        }

                        selectorSquare.invalidate()
                    }
                }
                MotionEvent.ACTION_UP -> {
                    selectorSquare.titleField.visibility = View.VISIBLE
                    buttonClicked = false
                    selectorSquare.transformToMatchContent()
                    if (resumeFocus) {
                        selectorSquare.titleField.requestFocus()
                        resumeFocus = false
                        val fragment: DayViewFragment = FragmentManager.findFragment(this)
                        fragment.isInEditText = true
                    }
                    selectorSquare.isDragMode = false
                }
                else -> {
                    resumeFocus = false
                }
            }

        }
        return super.onTouchEvent(event)
    }

    /**
     * calculates the boundary, which the drag button shouldn't be able to exceed
     */
    private fun getDrawBoundary(y_prev: Float): Int {
        val ssList = selectorSquare.getSelectorSquares()
        var boundary = MAX_VALUE
        if (isTopPosition)
            boundary = 0
        for (ss in ssList) {
            if (selectorSquare.appointmentId == ss.appointmentId) {
                continue
            }
            val otherSsPositions = selectorSquare.getRelativeTopBot(ss)
            if (isTopPosition && (otherSsPositions["bot"]!! <= y_prev) && (otherSsPositions["bot"]!! > boundary)) {
                //nearest bot pos that is located above the ss
                boundary = otherSsPositions["bot"]!!
            } else if (!isTopPosition && (otherSsPositions["top"]!! >= y_prev) && (otherSsPositions["top"]!! < boundary)) {
                //nearest top pos that is located below the ss
                boundary = otherSsPositions["top"]!!
            }
        }
        return boundary
    }

    /**
     * check if redrawing fits to the boundary (getDrawBoundary)
     * return: true -> overlap
     */
    private fun checkCollisionReDrawBorder(yRedraw: Float, y_prev: Float): Boolean {
        var boundary = getDrawBoundary(y_prev)
        return ((isTopPosition && yRedraw.toInt() < boundary) || (!isTopPosition && yRedraw.toInt() > boundary))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paint = Paint()
        paint.color = Color.BLACK
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL

        val rectWidth = width
        val rectHeight = if (isTopPosition) (height * 0.5).toFloat() else height

        val topLeftRadius = if (isTopPosition) 0f else 30f
        val topRightRadius = if (isTopPosition) 0f else 30f
        val bottomLeftRadius = if (isTopPosition) 30f else 0f
        val bottomRightRadius = if (isTopPosition) 30f else 0f

        val topPosition = if (isTopPosition) 0f else (height * 0.5).toFloat()

        val rect = RectF()
        rect.set(0F, topPosition, rectWidth.toFloat(), rectHeight.toFloat())
        val path = Path()
        path.reset()
        path.addRoundRect(
            rect,
            floatArrayOf(
                topLeftRadius,
                topLeftRadius,
                topRightRadius,
                topRightRadius,
                bottomLeftRadius,
                bottomLeftRadius,
                bottomRightRadius,
                bottomRightRadius
            ),
            Path.Direction.CW
        )

        canvas.drawPath(path, paint)
    }

    private fun moveBottomButton(movePosition: Float): Boolean {
        val offset = selectorSquare.dragButtonBottom.y
        val clippedPosition = selectorSquare.clipToTile(movePosition + offset)
        if (checkCollisionReDrawBorder(clippedPosition, selectorSquare.dragButtonBottom.y)) {
            return false
        }
        val squareSize = clippedPosition - selectorSquare.topBorder

        if (squareSize >= RECTANGLE_MIN_SIZE && clippedPosition >= 0) {
            selectorSquare.bottomBorder = clippedPosition
            selectorSquare.buttonPositionBottom =
                clippedPosition - (BUTTON_HITBOX_SIZE) // move it up so its not below the square
            selectorSquare.dragButtonBottom.y =
                clippedPosition - (BUTTON_HITBOX_SIZE)
        }
        return true
    }

    private fun moveTopButton(movePosition: Float): Boolean {
        val offset = selectorSquare.dragButtonTop.y
        val clippedPosition = selectorSquare.clipToTile(movePosition + offset)
        /**
         * be wary, that the initial position is used from the button -> if changed change here also
         */
        if (checkCollisionReDrawBorder(clippedPosition, selectorSquare.dragButtonTop.y)) {
            return false
        }
        val squareSize = selectorSquare.bottomBorder - (clippedPosition)
        if (squareSize >= RECTANGLE_MIN_SIZE && clippedPosition >= 0) {
            selectorSquare.topBorder = clippedPosition
            selectorSquare.buttonPositionTop = clippedPosition
            selectorSquare.dragButtonTop.y = clippedPosition
        }
        return true
    }

    /**
     * Increase size of ss downwards by stepSize until targetYPosition is reached
     */
    fun increaseSizeDownwards(targetYPosition: Int, stepSize: Int = 10) {
        for (pos in 0 until targetYPosition + 1 step (targetYPosition / stepSize)) {
            if (!moveBottomButton(pos.toFloat()))
                break
        }
    }
}