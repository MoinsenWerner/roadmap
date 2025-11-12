package com.example.drivemap.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RoadSegmentDao {
    @Query("SELECT * FROM road_segments ORDER BY created_at ASC")
    fun observeSegments(): Flow<List<RoadSegmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(segment: RoadSegmentEntity): Long

    @Query("UPDATE road_segments SET speed_limit_kmh = :speedLimit WHERE id = :segmentId")
    suspend fun updateSpeedLimit(segmentId: Long, speedLimit: Int)
}
