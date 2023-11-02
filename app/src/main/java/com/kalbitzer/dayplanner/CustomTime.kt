package com.kalbitzer.dayplanner

data class CustomTime(val hour:Int, val minute:Int) {
    constructor(timeStr: String) : this(
        timeStr.split(":").getOrNull(0)?.toIntOrNull() ?: 0,
        timeStr.split(":").getOrNull(1)?.toIntOrNull() ?: 0
    )
    override fun toString(): String { //ensure always in format hh:mm
        return String.format("%02d:%02d", hour, minute)
    }
    fun equals(time: CustomTime): Boolean {
        return time.hour == hour && time.minute == minute
    }
    fun toMinutes():Int{
        return hour * 60 + minute
    }
    fun fromMinutes(minutes: Int): CustomTime {
        val newHour = minutes / 60
        val newMinute = minutes % 60
        return CustomTime(newHour, newMinute)
    }
    fun subtractMinutes(minutes: Int): CustomTime {
        val newMinutes = toMinutes() - minutes
        return fromMinutes(newMinutes)
    }
    companion object {
        fun fromString(timeStr: String): CustomTime {
            val parts = timeStr.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toIntOrNull()
                val minute = parts[1].toIntOrNull()

                if (hour != null && minute != null) {
                    return CustomTime(hour, minute)
                }
            }
            throw IllegalArgumentException("Invalid time format: $timeStr")
        }
        fun getCurrentTime(): CustomTime {
            val cal = java.util.Calendar.getInstance()
            return CustomTime(cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
        }
    }
}

