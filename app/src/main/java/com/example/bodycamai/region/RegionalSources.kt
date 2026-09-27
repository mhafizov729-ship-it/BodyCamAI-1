package com.example.bodycamai.region

/**
 * Adapter boundary for permitted official/public sources.
 * The app never treats a visual face match as proof of identity or guilt.
 */
data class RegionConfig(
    val id: String,
    val name: String,
    val countryCode: String,
    val enabled: Boolean = true
)

data class PublicRecordMatch(
    val possibleMatch: Boolean,
    val displayName: String?,
    val status: String,
    val confidence: Float?,
    val sourceName: String?,
    val sourceUrl: String?,
    val checkedAt: Long
)

interface RegionalPublicSource {
    val region: RegionConfig
    suspend fun lookupPermitted(query: String): PublicRecordMatch
}

class RegionalSourceRegistry {
    private val regions = mutableListOf(
        RegionConfig("RU", "Россия", "RU"),
        RegionConfig("US", "США", "US"),
        RegionConfig("EU", "Европа", "EU")
    )

    fun availableRegions(): List<RegionConfig> = regions.toList()

    fun addRegion(region: RegionConfig) {
        if (regions.none { it.id == region.id }) regions += region
    }
}
