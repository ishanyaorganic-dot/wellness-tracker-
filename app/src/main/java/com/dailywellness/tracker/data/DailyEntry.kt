package com.dailywellness.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_entries")
data class DailyEntry(
    @PrimaryKey val date: String,
    val waterMl: Int = 0,
    val juiceTaken: Boolean = false,
    val juiceName: String = "",
    val juiceMl: Int = 0,
    val omega3: Boolean = false,
    val creatine: Boolean = false,
    val multivitamin: Boolean = false,
    val magnesium: Boolean = false,
    val gymAttended: Boolean = false,
    val workoutType: String = "Strength training",
    val workoutMinutes: Int = 0,
    val intensity: String = "Moderate",
    val workoutNotes: String = "",
    val dailyNotes: String = ""
)