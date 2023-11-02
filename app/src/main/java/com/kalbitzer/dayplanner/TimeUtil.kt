package com.kalbitzer.dayplanner

import android.util.Log
import com.kalbitzer.dayplanner.database.Appointment
import java.text.SimpleDateFormat
import java.util.*

object TimeUtil {
    const val minutesPerTile = 15
    // todo überlegen ob tilesize als lokale variable, aber dann ist falls app resized pozenziell kaputt


    fun getPixelPositionFromDate(date: Date, tileSize: Float): Float {
        val cal = Calendar.getInstance()
        cal.time = date
        val hours = cal.get(Calendar.HOUR_OF_DAY)
        val minutes = cal.get(Calendar.MINUTE)
        val numberOfSections = hours * (60 / minutesPerTile) + (minutes / minutesPerTile)
        return (numberOfSections * tileSize)
    }


    fun getPixelPositionFromTime(time: CustomTime, tileSize: Float): Float {
        val hours = time.hour
        val minutes = time.minute
        val numberOfSections = hours * (60 / minutesPerTile) + (minutes / minutesPerTile)
        return (numberOfSections * tileSize)
    }

    fun getTimeFromPixelPosition(pixelPosition: Float, tileSize: Float): CustomTime {
        val numberSections = pixelPosition.toInt() / tileSize
        val sectionsPerHour = 60 / minutesPerTile
        val minuteSections = (numberSections % sectionsPerHour).toInt()
        var hours = ((numberSections - minuteSections) / sectionsPerHour).toInt().toString()
        if (hours.length < 2) hours = "0" + hours
        val minutes =
            if (minuteSections == 0) "00" else (minuteSections * minutesPerTile).toString()
        return CustomTime.fromString("$hours:$minutes")
    }

    fun getSizeFromTime(timeStart: CustomTime, timeEnd: CustomTime, tileSize: Float): Float {
        return getPixelPositionFromTime(timeEnd, tileSize) - getPixelPositionFromTime(
            timeStart,
            tileSize
        )
    }

    // create a list of dates ranging from x days ago to x days in the future, without time
    fun getDatesList(days: Int): List<Date> {
        val dates = ArrayList<Date>()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0) // Set hour to midnight
        cal.set(Calendar.MINUTE, 0)      // Set minute to zero
        cal.set(Calendar.SECOND, 0)      // Set second to zero
        cal.set(Calendar.MILLISECOND, 0) // Set millisecond to zero
        cal.add(Calendar.DAY_OF_YEAR, -days)
        for (i in 0 until days * 2 + 1) {
            dates.add(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return dates
    }

    // create a list of dates ranging from x days ago to x days in the future, without time
    fun getDatesList(days: Int, date: Date): List<Date> {
        val dates = ArrayList<Date>()
        val cal = Calendar.getInstance()
        cal.time = date
        cal.add(Calendar.DAY_OF_YEAR, -days)
        for (i in 0 until days * 2 + 1) {
            dates.add(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return dates
    }

    //parse a date to a string in format like "1st of January" or "2nd of February" u
    fun parseDateToString(date: Date): String {
        val cal = Calendar.getInstance()
        cal.time = date
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val dateFormat = SimpleDateFormat("MMMM")
        val currentMonthName = dateFormat.format(cal.time)
        return when (day) {
            1, 21, 31 -> "$day" + "st of " + currentMonthName
            2, 22 -> "$day" + "nd of " + currentMonthName
            3, 23 -> "$day" + "rd of " + currentMonthName
            else -> "$day" + "th of " + currentMonthName
        }
    }

    fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1: Calendar = Calendar.getInstance()
        val cal2: Calendar = Calendar.getInstance()
        cal1.time = date1
        cal2.time = date2
        return cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
    }


    /**
     * current day: scroll to half an hour before current time
     * current day after 21:00: 6 am (next day, switch done in MainActivity)
     * otherwise: Always scroll to first appointment of the day, if none, also to 6 AM
     */
    fun getScrollPosition(
        tileSize: Float,
        dayViewDate: Date,
        appointments: List<Appointment>?
    ): Float {
        val currentTime = CustomTime.getCurrentTime()
        if (isSameDay(dayViewDate, Date())) { // 15 min before current time
            return getPixelPositionFromTime(currentTime.subtractMinutes(15), tileSize)
        } else { // 15 min before first appointment of the day
            if (appointments?.isNotEmpty() == true) {
                val minTime = appointments.map { appointment -> CustomTime(appointment.timeStart) }
                    .minBy { it.toMinutes() }
                return getPixelPositionFromTime(minTime.subtractMinutes(15), tileSize)
            }
        } // else 5:45 am
        return getPixelPositionFromTime(CustomTime(5, 45), tileSize)
    }
}