package com.example.drivemap.location

data class LocationSample(
    val latitude: Double,
    val longitude: Double,
    val speedMps: Double?,
    val bearing: Float?,
    val timestampMillis: Long
)

data class RoadHeading(val degrees: Float)
