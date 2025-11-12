package com.example.drivemap.data

import com.example.drivemap.location.LocationSample
import com.example.drivemap.location.RoadHeading
import com.example.drivemap.model.LatLon
import com.example.drivemap.model.RoadSegment
import com.example.drivemap.util.Distance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoadRepository(
    private val segmentDao: RoadSegmentDao,
    private val geocodeResolver: GeocodeResolver,
    private val clock: com.example.drivemap.util.Clock
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val segmentAccumulator = SegmentAccumulator()

    private val _driveState = MutableStateFlow(DriveState())
    val driveState: Flow<DriveState> = _driveState

    val segments: Flow<List<RoadSegment>> = segmentDao
        .observeSegments()
        .map { list ->
            list.map { entity ->
                RoadSegment(
                    id = entity.id,
                    start = LatLon(entity.startLat, entity.startLon),
                    end = LatLon(entity.endLat, entity.endLon),
                    distanceMeters = entity.distanceMeters,
                    averageSpeedKmh = entity.averageSpeedKmh,
                    speedLimitKmh = entity.speedLimitKmh,
                    createdAt = entity.createdAt
                )
            }
        }

    val combinedState: Flow<RoadSnapshot> = combine(segments, driveState) { segments, drive ->
        RoadSnapshot(segments = segments, driveState = drive)
    }.stateIn(scope, SharingStarted.Eagerly, RoadSnapshot())

    fun recordLocation(sample: LocationSample) {
        scope.launch {
            mutex.withLock {
                val completedSegments = segmentAccumulator.append(sample)
                completedSegments.forEach { completed ->
                    val entity = RoadSegmentEntity(
                        startLat = completed.start.latitude,
                        startLon = completed.start.longitude,
                        endLat = completed.end.latitude,
                        endLon = completed.end.longitude,
                        distanceMeters = completed.distanceMeters,
                        averageSpeedKmh = completed.averageSpeedKmh,
                        speedLimitKmh = completed.speedLimitKmh,
                        createdAt = clock.now()
                    )
                    segmentDao.insert(entity)
                }
                _driveState.value = _driveState.value.copy(
                    lastKnownLocation = LatLon(sample.latitude, sample.longitude),
                    lastSpeedKmh = sample.speedMps?.let { Distance.metersPerSecondToKmh(it) },
                    heading = RoadHeading(sample.bearing ?: 0f)
                )
            }
        }
    }

    fun setStationary(isStationary: Boolean) {
        _driveState.value = _driveState.value.copy(isStationary = isStationary)
    }

    suspend fun updateSpeedLimit(segmentId: Long, speedLimit: Int) {
        segmentDao.updateSpeedLimit(segmentId, speedLimit)
    }

    suspend fun resolveLocationDetails(location: LatLon): GeocodeResult = geocodeResolver.resolve(location)

    data class DriveState(
        val lastKnownLocation: LatLon? = null,
        val lastSpeedKmh: Double? = null,
        val heading: RoadHeading = RoadHeading(0f),
        val isStationary: Boolean = true
    )

    data class RoadSnapshot(
        val segments: List<RoadSegment> = emptyList(),
        val driveState: DriveState = DriveState()
    )

}
