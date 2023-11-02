package com.kalbitzer.dayplanner.database

import android.content.Context
import androidx.room.Database
import com.kalbitzer.dayplanner.database.Appointment
import androidx.room.TypeConverters
import androidx.room.RoomDatabase
import com.kalbitzer.dayplanner.database.AppointmentDao
import kotlin.jvm.Volatile
import com.kalbitzer.dayplanner.database.AppointmentDatabase
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.Executors

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
 * This is the backend. The database. This used to be done by the OpenHelper.
 * The fact that this has very few comments emphasizes its coolness.  In a real
 * app, consider exporting the schema to help you with migrations.
 */
@Database(entities = [Appointment::class], version = 1, exportSchema = false)
@TypeConverters(
    Converters::class
)
internal abstract class AppointmentDatabase : RoomDatabase() {
    abstract fun appointmentDao(): AppointmentDao

    companion object {
        // marking the instance as volatile to ensure atomic access to the variable
        @Volatile
        private var INSTANCE: AppointmentDatabase? = null
        private const val NUMBER_OF_THREADS = 4
        val databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS)
        fun getDatabase(context: Context): AppointmentDatabase? {
            if (INSTANCE == null) {
                synchronized(AppointmentDatabase::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(
                            context.applicationContext,
                            AppointmentDatabase::class.java, "appointment_database"
                        )
                            .addCallback(sRoomDatabaseCallback)
                            .build()
                    }
                }
            }
            return INSTANCE
        }

        /**
         * Override the onCreate method to populate the database.
         * For this sample, we clear the database every time it is created.
         */
        private val sRoomDatabaseCallback: Callback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                databaseWriteExecutor.execute {

                    val dao = INSTANCE!!.appointmentDao()
                    dao.deleteAll() //TODO maybe change
                }
            }
        }
    }
}