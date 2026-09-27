package com.example.bodycamai.map

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OfflineMapManager {
    private val _regions = MutableStateFlow(listOf(
        OfflineRegion("local", "Текущий регион", 420),
        OfflineRegion("training", "Учебная зона", 180),
        OfflineRegion("custom", "Пользовательская область", 0)
    ))
    val regions: StateFlow<List<OfflineRegion>> = _regions.asStateFlow()
    fun markDownloaded(id: String) { _regions.value = _regions.value.map { if (it.id == id) it.copy(downloaded=true, progress=100, paused=false) else it } }
    fun pause(id: String) { _regions.value = _regions.value.map { if (it.id == id && it.progress in 1..99) it.copy(paused=true) else it } }
    fun cancel(id: String) { _regions.value = _regions.value.map { if (it.id == id) it.copy(progress=0, downloaded=false, paused=false) else it } }
    fun delete(id: String) { _regions.value = _regions.value.map { if (it.id == id) it.copy(downloaded=false, progress=0, paused=false) else it } }
}
