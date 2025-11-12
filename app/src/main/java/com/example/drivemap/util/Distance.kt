package com.example.drivemap.util

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object Distance {
    fun metersPerSecondToKmh(speed: Double): Double = speed * 3.6
}

private const val EARTH_RADIUS_METERS = 6_371_000.0

fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
    val c = 2 * asin(sqrt(a))
    return EARTH_RADIUS_METERS * c
}
