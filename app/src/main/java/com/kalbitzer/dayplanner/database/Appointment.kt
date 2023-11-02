package com.kalbitzer.dayplanner.database

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "appointment_table")
data class Appointment(
    @PrimaryKey(autoGenerate = true)
    var id:Long = 0,
    var title: String,
    var timeStart: String,
    var timeEnd: String,
    var date:Date){
    constructor(title: String, timeStart: String, timeEnd: String, date:Date) : this(0, title,timeStart,timeEnd, date)
}