package com.example.drivemap.data

import android.content.Context
import androidx.room.Room
import com.example.drivemap.location.GeocodeResolver
import com.example.drivemap.util.Clock

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context,
        RoadDatabase::class.java,
        "road-database"
    ).fallbackToDestructiveMigration().build()

    private val geocodeResolver = GeocodeResolver(context.applicationContext)
    private val clock = Clock.SystemClock

    val roadRepository: RoadRepository = RoadRepository(
        segmentDao = database.roadSegmentDao(),
        geocodeResolver = geocodeResolver,
        clock = clock
    )
}
