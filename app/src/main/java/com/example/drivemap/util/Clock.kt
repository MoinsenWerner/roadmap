package com.example.drivemap.util

interface Clock {
    fun now(): Long

    object SystemClock : Clock {
        override fun now(): Long = System.currentTimeMillis()
    }
}
