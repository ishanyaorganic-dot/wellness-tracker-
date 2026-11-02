package com.dailywellness.tracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DailyDao {
    @Query("SELECT * FROM daily_entries WHERE date = :date LIMIT 1")
    suspend fun get(date: String): DailyEntry?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entry: DailyEntry)
    @Query("SELECT * FROM daily_entries WHERE date LIKE :monthPrefix || '%' ORDER BY date DESC")
    suspend fun month(monthPrefix: String): List<DailyEntry>
}