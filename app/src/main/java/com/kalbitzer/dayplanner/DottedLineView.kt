package com.kalbitzer.dayplanner

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View


class DottedLineView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null) :
    View(context, attrs) {
    private val paint: Paint = Paint()
    private val pathEffect: PathEffect
    private var yPosition:Float = 0F

    init {

        // Create a Paint object for drawing the dotted line
        paint.color = Color.CYAN // Set the color of the line
        paint.strokeWidth = 10f // Set the line width
        paint.style = Paint.Style.STROKE

        // Set the StrokeCap to ROUND to make the dots appear round
        paint.strokeCap = Paint.Cap.ROUND;

        // Create a PathEffect to make the line dotted with round dots
        val radius = 10f // Adjust the radius of the round dots as needed
        pathEffect = DashPathEffect(floatArrayOf(2 * radius, 2 * radius), 0F)
        paint.pathEffect = pathEffect

        // to elevate above all other views
        elevation = 10f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Get the width and height of the view
        val width = width
        //val height = height

        // Create a path for the dotted line
        val path = Path()
        path.moveTo(0f, yPosition) // Move to the starting position

        // Draw the dotted line from left to right, spanning the entire width
        path.lineTo(width.toFloat(), yPosition)

        // Set the path effect and draw the path
        paint.pathEffect = pathEffect
        canvas.drawPath(path, paint)
    }

    public fun setPosition(yPos:Float){
        yPosition = yPos
        invalidate()
    }
}
