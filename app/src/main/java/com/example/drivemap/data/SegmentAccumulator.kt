package com.example.drivemap.data

import com.example.drivemap.location.LocationSample
import com.example.drivemap.model.LatLon
import com.example.drivemap.util.Distance
import com.example.drivemap.util.haversineDistance
import kotlin.math.roundToInt

internal class SegmentAccumulator {
    private data class TrackedSample(
        val sample: LocationSample,
        val distanceFromPrevious: Double
    )

    private val samples = mutableListOf<TrackedSample>()
    private var accumulatedDistance = 0.0

    fun append(sample: LocationSample): List<CompletedSegment> {
        val distance = samples.lastOrNull()?.let { last ->
            haversineDistance(
                last.sample.latitude,
                last.sample.longitude,
                sample.latitude,
                sample.longitude
            )
        } ?: 0.0
        samples.add(TrackedSample(sample, distance))
        accumulatedDistance += distance

        val results = mutableListOf<CompletedSegment>()
        while (accumulatedDistance >= SEGMENT_LENGTH_METERS && samples.size >= 2) {
            finalizeSegment()?.let { results.add(it) } ?: break
        }
        return results
    }

    private fun finalizeSegment(): CompletedSegment? {
        if (samples.size < 2) return null

        var distanceCovered = 0.0
        var index = 0
        val firstSample = samples.first().sample

        while (index + 1 < samples.size) {
            val current = samples[index + 1]
            val previousSample = samples[index].sample
            val segmentDistance = current.distanceFromPrevious
            if (distanceCovered + segmentDistance >= SEGMENT_LENGTH_METERS) {
                val remaining = SEGMENT_LENGTH_METERS - distanceCovered
                val ratio = if (segmentDistance == 0.0) 0.0 else remaining / segmentDistance
                val interpolated = interpolate(previousSample, current.sample, ratio)

                val durationSeconds = (interpolated.timestampMillis - firstSample.timestampMillis)
                    .coerceAtLeast(1L) / 1000.0
                val averageSpeedKmh = Distance
                    .metersPerSecondToKmh(SEGMENT_LENGTH_METERS / durationSeconds)
                    .roundToInt()

                val completed = CompletedSegment(
                    start = LatLon(firstSample.latitude, firstSample.longitude),
                    end = LatLon(interpolated.latitude, interpolated.longitude),
                    distanceMeters = SEGMENT_LENGTH_METERS,
                    averageSpeedKmh = averageSpeedKmh,
                    speedLimitKmh = averageSpeedKmh
                )

                val leftoverDistance = (segmentDistance - remaining).coerceAtLeast(0.0)
                samples[index + 1] = current.copy(distanceFromPrevious = leftoverDistance)
                samples.subList(0, index + 1).clear()
                samples.add(0, TrackedSample(interpolated, 0.0))
                accumulatedDistance -= SEGMENT_LENGTH_METERS
                return completed
            } else {
                distanceCovered += segmentDistance
                index += 1
            }
        }

        return null
    }

    private fun interpolate(
        start: LocationSample,
        end: LocationSample,
        ratio: Double
    ): LocationSample {
        val clamped = ratio.coerceIn(0.0, 1.0)
        val lat = start.latitude + (end.latitude - start.latitude) * clamped
        val lon = start.longitude + (end.longitude - start.longitude) * clamped
        val timeDiff = end.timestampMillis - start.timestampMillis
        val timestamp = start.timestampMillis + (timeDiff * clamped).toLong()
        val speed = when {
            start.speedMps != null && end.speedMps != null ->
                start.speedMps + (end.speedMps - start.speedMps) * clamped
            end.speedMps != null -> end.speedMps
            else -> start.speedMps
        }
        val bearing = end.bearing ?: start.bearing
        return LocationSample(
            latitude = lat,
            longitude = lon,
            speedMps = speed,
            bearing = bearing,
            timestampMillis = timestamp
        )
    }

    fun reset() {
        samples.clear()
        accumulatedDistance = 0.0
    }

    data class CompletedSegment(
        val start: LatLon,
        val end: LatLon,
        val distanceMeters: Double,
        val averageSpeedKmh: Int,
        val speedLimitKmh: Int
    )

    companion object {
        private const val SEGMENT_LENGTH_METERS = 500.0
    }
}
