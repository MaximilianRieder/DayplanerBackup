package com.kalbitzer.dayplanner

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.text.*
import android.util.TypedValue
import android.view.Gravity
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.FragmentManager
import kotlin.math.roundToInt
import kotlin.math.truncate


@SuppressLint("ClickableViewAccessibility")
class CompatibleEditText(context: Context, private val parentSS: SelectorSquare) :
    androidx.appcompat.widget.AppCompatEditText(context) {

    var previousText: String

    init {
        background = null
        setPadding(0, 0, 0, 0)
        this.background = null
        //this.isCursorVisible = false
        this.previousText = this.text.toString()

        this.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // Move the cursor to the end of the text
                this.text?.let { this.setSelection(it.length) }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return false
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        val fragment: DayViewFragment = FragmentManager.findFragment(parentSS)
        if (focused) {
            // TODO evtl drag and drop and buttons chang
            if (fragment.isInEditText && !parentSS.isDragMode) {
                fragment.removeFocusTextFields(this)

            } else {
                fragment.isInEditText = true
                fragment.previousSelectedSS = parentSS
                val inputMethodManager =
                    context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_FORCED)

                this.previousText = this.text.toString()
            }
        } else if (!fragment.openedDeleteDialog && !parentSS.isDragMode) {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(this.windowToken, 0)

            if (!parentSS.isSaved) {
                parentSS.mAppointmentViewModel.delete(parentSS.appointmentId)
                val parentview = parentSS.parent as ViewGroup
                parentview.removeView(parentSS)
            }
            val parsedText = parseDurationFromString(this.previousText)
            Log.e("asd", parsedText.toString())
            if (parsedText == null)
                this.setText(this.previousText)
            else {
                val targetSize = SharedVariables.tileSize * (parsedText.second / 15) // TODO wrong size so far
                parentSS.increaseSizeDownwards(targetSize.toInt())
                this.setText(parsedText.first)
            }
        }
    }

    override fun onEditorAction(actionCode: Int) {
        super.onEditorAction(actionCode)
        if (actionCode == EditorInfo.IME_ACTION_DONE) {
            this.clearFocus()

        }
    }

    private fun parseDurationFromString(input: String): Pair<String, Int>? {

        val regex = "(\\d+)\\s*(h|min|std)\\s+(.*)".toRegex()
        val matchResult = regex.find(input)

        return matchResult?.let {
            val (number, unit, title) = it.destructured
            if (unit == "h" ||unit ==  "std")
                Pair(title, number.toInt() * 60)
            else Pair(title, number.toInt())
        }

    }
}