package com.kalbitzer.dayplanner.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.kalbitzer.dayplanner.database.AppointmentRepository
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
 * View Model to keep a reference to the word repository and
 * an up-to-date list of all words.
 */
class AppointmentViewModel(application: Application?) : AndroidViewModel(application!!) {
    private val mRepository: AppointmentRepository

    // Using LiveData and caching what getAlphabetizedWords returns has several benefits:
    // - We can put an observer on the data (instead of polling for changes) and only update the
    //   the UI when the data actually changes.
    // - Repository is completely separated from the UI through the ViewModel.
    val allAppointments: LiveData<List<Appointment>>
    var appointmentsForDate: LiveData<List<Appointment>>

    init {
        mRepository = AppointmentRepository(application)
        allAppointments = mRepository.allAppointments
        appointmentsForDate = MutableLiveData<List<Appointment>>()
    }
    fun insert(appointment: Appointment):Long {
        return mRepository.insert(appointment)
    }
    fun update(appointment: Appointment){
        mRepository.update(appointment)
    }
    fun deleteAll(){
        mRepository.deleteAll()
    }
    fun delete(id:Long){
        mRepository.delete(id)
    }
    fun loadAppointmentsForDate(date: Date) {
        appointmentsForDate = mRepository.getAppointmentsForDate(date)
    }


}