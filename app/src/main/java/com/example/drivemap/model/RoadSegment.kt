package com.example.drivemap.model

data class RoadSegment(
    val id: Long,
    val start: LatLon,
    val end: LatLon,
    val distanceMeters: Double,
    val averageSpeedKmh: Int,
    val speedLimitKmh: Int,
    val createdAt: Long
) {
    val midpoint: LatLon = LatLon(
        latitude = (start.latitude + end.latitude) / 2.0,
        longitude = (start.longitude + end.longitude) / 2.0
    )
}
