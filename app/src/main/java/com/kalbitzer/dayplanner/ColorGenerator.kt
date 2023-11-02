package com.kalbitzer.dayplanner

import android.graphics.Color
import java.util.*

class ColorGenerator {
    private val pastelColors = arrayOf(
        "#1976D2", "#388E3C", "#F57C00", "#E53935", "#7B1FA2",
        "#FBC02D", "#0288D1", "#388E3C", "#D81B60", "#8E24AA",
        "#C2185B", "#FFA000", "#00796B", "#1976D2", "#0288D1",
        "#F57C00", "#9E9D24", "#D81B60", "#8E24AA", "#C2185B",
        "#FFA000", "#E64A19", "#8BC34A", "#FF6F00", "#009688",
        "#FDD835", "#D84315", "#43A047", "#1E88E5", "#3949AB"
    )

    fun getColor(): Int {
        val random = Random()
        val randomIndex = random.nextInt(pastelColors.size)
        val colorString = pastelColors[randomIndex]
        return Color.parseColor(colorString)
    }
}