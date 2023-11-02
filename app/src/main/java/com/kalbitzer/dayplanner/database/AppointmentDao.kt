package com.kalbitzer.dayplanner.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.kalbitzer.dayplanner.database.Appointment
import java.util.*

/*
* Copyright (C) 2017 Google Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/ /**
 * The Room Magic is in this file, where you map a Java method call to an SQL query.
 *
 * When you are using complex data types, such as Date, you have to also supply type converters.
 * To keep this example basic, no types that require type converters are used.
 * See the documentation at
 * https://developer.android.com/topic/libraries/architecture/room.html#type-converters
 */
@Dao
interface AppointmentDao {
    // LiveData is a data holder class that can be observed within a given lifecycle.
    @get:Query("SELECT * FROM appointment_table ORDER BY id DESC")
    val allAppointments: LiveData<List<Appointment>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(appointment: Appointment?): Long

    @Query("DELETE FROM appointment_table")
    fun deleteAll()

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(appointment: Appointment?): Int

    @Delete
    fun delete(appointment: Appointment?)

    @Query("DELETE FROM appointment_table WHERE id=:id")
    fun delete(id: Long)


    @Query("SELECT * FROM appointment_table WHERE date = :selectedDate")
    fun getAppointmentsForDate(selectedDate: Date): List<Appointment>

}