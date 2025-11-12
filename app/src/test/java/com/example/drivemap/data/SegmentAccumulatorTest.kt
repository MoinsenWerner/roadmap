package com.example.drivemap.data

import com.example.drivemap.location.LocationSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SegmentAccumulatorTest {
    private val accumulator = SegmentAccumulator()

    @Test
    fun createsSegmentAfterFiveHundredMeters() {
        val first = sample(lat = 0.0, lon = 0.0, timestamp = 0L, speed = 25.0)
        val second = sample(lat = 0.0045, lon = 0.0, timestamp = 20_000L, speed = 25.0)

        assertTrue(accumulator.append(first).isEmpty())
        val segments = accumulator.append(second)
        assertEquals(1, segments.size)
        val segment = segments.first()
        assertEquals(500.0, segment.distanceMeters, 1.0)
        assertEquals(90, segment.averageSpeedKmh)
        assertEquals(90, segment.speedLimitKmh)
    }

    @Test
    fun carriesOverRemainderForNextSegment() {
        val first = sample(0.0, 0.0, 0L, 25.0)
        val second = sample(0.0045, 0.0, 20_000L, 25.0)
        val third = sample(0.009, 0.0, 40_000L, 25.0)

        accumulator.append(first)
        val firstSegments = accumulator.append(second)
        assertEquals(1, firstSegments.size)

        val secondSegments = accumulator.append(third)
        assertEquals(1, secondSegments.size)
        val segment = secondSegments.first()
        // Expect the second segment to start roughly at the end of the first segment.
        assertEquals(0.0045, segment.start.latitude, 1e-3)
        assertEquals(0.009, segment.end.latitude, 1e-3)
        assertEquals(500.0, segment.distanceMeters, 1.0)
    }

    private fun sample(lat: Double, lon: Double, timestamp: Long, speed: Double): LocationSample {
        return LocationSample(
            latitude = lat,
            longitude = lon,
            speedMps = speed,
            bearing = 0f,
            timestampMillis = timestamp
        )
    }
}
