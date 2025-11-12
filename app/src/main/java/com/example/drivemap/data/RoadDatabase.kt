package com.example.drivemap.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [RoadSegmentEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RoadDatabase : RoomDatabase() {
    abstract fun roadSegmentDao(): RoadSegmentDao
}
