package com.kalbitzer.dayplanner.database

import android.app.Application
import android.content.Context
import com.kalbitzer.dayplanner.database.AppointmentDao
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kalbitzer.dayplanner.database.Appointment
import com.kalbitzer.dayplanner.database.AppointmentDatabase
import java.util.*
import java.util.concurrent.Future

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
 * Abstracted Repository as promoted by the Architecture Guide.
 * https://developer.android.com/topic/libraries/architecture/guide.html
 */
internal class AppointmentRepository(application: Application?) {
    private val mAppointmentDao: AppointmentDao
    val allAppointments: LiveData<List<Appointment>> // this does not cause performance issues because it is lazy
    private val appointmentsForDate: MutableLiveData<List<Appointment>>

    init {
        val db = AppointmentDatabase.getDatabase(application!!.applicationContext)
        mAppointmentDao = db!!.appointmentDao()
        allAppointments = mAppointmentDao.allAppointments
        appointmentsForDate = MutableLiveData<List<Appointment>>()
    }

    // You must call this on a non-UI thread or your app will throw an exception. Room ensures
    // that you're not doing any long running operations on the main thread, blocking the UI.
    fun insert(appointment: Appointment): Long {
        var id = 0L
        val task = AppointmentDatabase.databaseWriteExecutor.submit {
            id = mAppointmentDao.insert(appointment)
        }
        task.get()
        return id // todo ev. einfach future benutzen um db lag zu verhindern
    }

    fun delete(appointment: Appointment) {
        AppointmentDatabase.databaseWriteExecutor.execute { mAppointmentDao.delete(appointment) }
    }
    fun delete(id: Long) {
        AppointmentDatabase.databaseWriteExecutor.execute { mAppointmentDao.delete(id) }
    }

    fun update(appointment: Appointment) {
        AppointmentDatabase.databaseWriteExecutor.submit {
            mAppointmentDao.update(appointment)
        }
    }
    fun deleteAll() {
        AppointmentDatabase.databaseWriteExecutor.execute { mAppointmentDao.deleteAll() }
    }
    fun getAppointmentsForDate(date: Date): LiveData<List<Appointment>> {
        // Run the database query on a background thread
        AppointmentDatabase.databaseWriteExecutor.execute {
            val appointments = mAppointmentDao.getAppointmentsForDate(date)
            // Update the LiveData with the new data
            appointmentsForDate.postValue(appointments)
        }
        return appointmentsForDate
    }

}