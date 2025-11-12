package com.example.drivemap.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "road_segments")
data class RoadSegmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "start_lat")
    val startLat: Double,
    @ColumnInfo(name = "start_lon")
    val startLon: Double,
    @ColumnInfo(name = "end_lat")
    val endLat: Double,
    @ColumnInfo(name = "end_lon")
    val endLon: Double,
    @ColumnInfo(name = "distance_m")
    val distanceMeters: Double,
    @ColumnInfo(name = "average_speed_kmh")
    val averageSpeedKmh: Int,
    @ColumnInfo(name = "speed_limit_kmh")
    val speedLimitKmh: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
