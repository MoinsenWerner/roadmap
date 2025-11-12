package com.example.drivemap.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.drivemap.data.GeocodeResult
import com.example.drivemap.data.RoadRepository
import com.example.drivemap.model.LatLon
import com.example.drivemap.model.RoadSegment
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainViewModel(private val repository: RoadRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState = _uiState.asStateFlow()

    private var geocodeJob: Job? = null

    init {
        observeRoadState()
    }

    private fun observeRoadState() {
        viewModelScope.launch {
            repository.combinedState.collectLatest { snapshot ->
                val drive = snapshot.driveState
                val location = drive.lastKnownLocation
                if (location != null) {
                    resolveGeocode(location)
                }
                _uiState.value = _uiState.value.copy(
                    segments = snapshot.segments,
                    currentLocation = location,
                    currentHeading = drive.heading.degrees,
                    isStationary = drive.isStationary,
                    selectedSegmentId = _uiState.value.selectedSegmentId?.takeIf { id ->
                        snapshot.segments.any { it.id == id }
                    }
                )
            }
        }
    }

    private fun resolveGeocode(location: LatLon) {
        geocodeJob?.cancel()
        geocodeJob = viewModelScope.launch {
            when (val result = repository.resolveLocationDetails(location)) {
                is GeocodeResult.Details -> {
                    _uiState.value = _uiState.value.copy(
                        streetName = result.street,
                        localityName = result.locality
                    )
                }
                GeocodeResult.None -> {
                    _uiState.value = _uiState.value.copy(
                        streetName = null,
                        localityName = null
                    )
                }
            }
        }
    }

    suspend fun updateSpeedLimit(segmentId: Long, speed: Int) {
        repository.updateSpeedLimit(segmentId, speed)
        _uiState.value = _uiState.value.copy(selectedSegmentId = null)
    }

    fun setEditingSegment(segmentId: Long?) {
        _uiState.value = _uiState.value.copy(selectedSegmentId = segmentId)
    }
}

data class MapUiState(
    val segments: List<RoadSegment> = emptyList(),
    val currentLocation: LatLon? = null,
    val currentHeading: Float = 0f,
    val isStationary: Boolean = true,
    val streetName: String? = null,
    val localityName: String? = null,
    val selectedSegmentId: Long? = null
)
