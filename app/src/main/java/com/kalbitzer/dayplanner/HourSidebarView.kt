package com.kalbitzer.dayplanner
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View


public class HourSidebarView(context: Context, attributeSet: AttributeSet? = null): View(context, attributeSet){

    var hourPixels = 0

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // TODO hardcode in config file
        canvas.drawColor(121212)

        for (i in 0..23) {
            val paint = Paint()
            // TODO hardcode in config file
            paint.color = Color.WHITE
            // TODO hardcode
            paint.textSize = convertDpToPixels(20f)

            var hour = i
            var hourStr = ""
            hourStr = if (hour < 10) "0$i:00" else "$i:00"

            canvas.drawText(hourStr, 0f, ((i * hourPixels) + 15).toFloat(), paint)
        }
    }

    // TODO in util klasse
    private fun convertDpToPixels(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
    }

    public fun setHourText(hourPixelSize: Float){
        hourPixels = hourPixelSize.toInt()
        invalidate()
    }
}