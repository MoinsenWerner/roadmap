package com.example.drivemap.data

sealed interface GeocodeResult {
    data class Details(val street: String?, val locality: String?) : GeocodeResult
    data object None : GeocodeResult
}
