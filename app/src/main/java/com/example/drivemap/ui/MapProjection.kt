package com.example.drivemap.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.example.drivemap.model.LatLon
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class MapProjection(
    private val canvasSize: Size,
    private val center: LatLon,
    private val headingDegrees: Float,
    private val scale: Double
) {
    private val originLatRad = Math.toRadians(center.latitude)
    private val headingRad = Math.toRadians(headingDegrees.toDouble())

    fun toOffset(point: LatLon): Offset {
        val (x, y) = toMeters(point)
        val rotatedX = (x * cos(-headingRad) - y * sin(-headingRad)).toFloat()
        val rotatedY = (x * sin(-headingRad) + y * cos(-headingRad)).toFloat()
        val scaledX = (canvasSize.width / 2f) + rotatedX * scale.toFloat()
        val scaledY = (canvasSize.height / 2f) - rotatedY * scale.toFloat()
        return Offset(scaledX, scaledY)
    }

    fun toLatLon(offset: Offset): LatLon {
        val centeredX = (offset.x - canvasSize.width / 2f) / scale
        val centeredY = (canvasSize.height / 2f - offset.y) / scale
        val rotatedX = centeredX * cos(headingRad) - centeredY * sin(headingRad)
        val rotatedY = centeredX * sin(headingRad) + centeredY * cos(headingRad)
        val lon = center.longitude + Math.toDegrees(rotatedX / (EARTH_RADIUS_METERS * cos(originLatRad)))
        val lat = center.latitude + Math.toDegrees(rotatedY / EARTH_RADIUS_METERS)
        return LatLon(lat, lon)
    }

    private fun toMeters(point: LatLon): Pair<Double, Double> {
        val deltaLon = Math.toRadians(point.longitude - center.longitude)
        val deltaLat = Math.toRadians(point.latitude - center.latitude)
        val x = EARTH_RADIUS_METERS * deltaLon * cos(originLatRad)
        val y = EARTH_RADIUS_METERS * deltaLat
        return x to y
    }

    companion object {
        private const val EARTH_RADIUS_METERS = 6_371_000.0

        fun fromSegments(
            size: Size,
            center: LatLon,
            heading: Float,
            points: List<LatLon>
        ): MapProjection {
            if (points.isEmpty()) {
                return MapProjection(size, center, heading, DEFAULT_SCALE)
            }
            var minX = Double.POSITIVE_INFINITY
            var maxX = Double.NEGATIVE_INFINITY
            var minY = Double.POSITIVE_INFINITY
            var maxY = Double.NEGATIVE_INFINITY
            val tempProjection = MapProjection(size, center, 0f, 1.0)
            points.forEach { point ->
                val (x, y) = tempProjection.toMeters(point)
                minX = min(minX, x)
                maxX = max(maxX, x)
                minY = min(minY, y)
                maxY = max(maxY, y)
            }
            val rangeX = maxX - minX
            val rangeY = maxY - minY
            val widthScale = if (rangeX == 0.0) DEFAULT_SCALE else (size.width * 0.45) / rangeX
            val heightScale = if (rangeY == 0.0) DEFAULT_SCALE else (size.height * 0.45) / rangeY
            val scale = max(DEFAULT_SCALE, min(widthScale, heightScale))
            return MapProjection(size, center, heading, scale)
        }

        private const val DEFAULT_SCALE = 0.002
    }
}

fun distanceToSegment(point: Offset, start: Offset, end: Offset): Float {
    if (start == end) {
        return point.minus(start).getDistance()
    }
    val line = end - start
    val t = ((point - start).dotProduct(line)) / line.dotProduct(line)
    val clamped = t.coerceIn(0f, 1f)
    val projection = start + line * clamped
    return (point - projection).getDistance()
}

private fun Offset.getDistance(): Float = sqrt(x.pow(2) + y.pow(2))
