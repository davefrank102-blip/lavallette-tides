package com.lavallette.tides.data.model

import kotlinx.serialization.Serializable

enum class TideType { HIGH, LOW }

data class TideEvent(
    val timeMillis: Long,
    val heightFt: Double,
    val type: TideType
)

data class TideSample(
    val timeMillis: Long,
    val heightFt: Double
)

enum class TideDirection { RISING, FALLING, SLACK }

data class TideSnapshot(
    val direction: TideDirection,
    val currentHeightFt: Double?,
    val nextHigh: TideEvent?,
    val nextLow: TideEvent?,
    val todayCurve: List<TideSample>,
    val weeklyExtremes: List<TideEvent>,
    val stationId: String,
    val stationName: String,
    val fetchedAtMillis: Long,
    val fromCache: Boolean
)

data class WeatherNow(
    val tempF: Double,
    val weatherCode: Int,
    val windMph: Double,
    val windDirectionDeg: Int,
    val conditionLabel: String
)

data class WeatherHour(
    val timeMillis: Long,
    val tempF: Double,
    val weatherCode: Int,
    val windMph: Double,
    val conditionLabel: String
)

data class WeatherSnapshot(
    val current: WeatherNow,
    val forecastHours: List<WeatherHour>,
    val fetchedAtMillis: Long,
    val fromCache: Boolean
)

@Serializable
data class CachedBundle(
    val tideJson: String,
    val weatherJson: String,
    val savedAtMillis: Long
)
