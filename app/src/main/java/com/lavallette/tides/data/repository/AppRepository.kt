package com.lavallette.tides.data.repository

import android.content.Context
import com.lavallette.tides.data.local.CacheStore
import com.lavallette.tides.data.model.TideDirection
import com.lavallette.tides.data.model.TideEvent
import com.lavallette.tides.data.model.TideSample
import com.lavallette.tides.data.model.TideSnapshot
import com.lavallette.tides.data.model.TideType
import com.lavallette.tides.data.model.WeatherHour
import com.lavallette.tides.data.model.WeatherNow
import com.lavallette.tides.data.model.WeatherSnapshot
import com.lavallette.tides.data.remote.ApiClients
import com.lavallette.tides.data.remote.NoaaPrediction
import com.lavallette.tides.data.remote.StationInfo
import com.lavallette.tides.data.remote.WeatherCodes
import kotlinx.serialization.encodeToString
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

data class DashboardData(
    val tides: TideSnapshot,
    val weather: WeatherSnapshot
)

class AppRepository(context: Context) {
    private val cache = CacheStore(context.applicationContext)
    private val zone = ZoneId.of("America/New_York")
    private val noaaFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val dateFmt = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val openMeteoFmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    suspend fun load(@Suppress("UNUSED_PARAMETER") forceRefresh: Boolean = false): DashboardData {
        return try {
            val now = System.currentTimeMillis()
            val tides = fetchTides(now)
            val weather = fetchWeather(now)
            cache.save(
                tideJson = ApiClients.json().encodeToString(serializeTide(tides)),
                weatherJson = ApiClients.json().encodeToString(serializeWeather(weather)),
                savedAt = now
            )
            DashboardData(tides, weather)
        } catch (networkError: Exception) {
            val cached = cache.load()
                ?: throw networkError
            val (tideJson, weatherJson, savedAt) = cached
            val tides = deserializeTide(tideJson, savedAt)
            val weather = deserializeWeather(weatherJson, savedAt)
            DashboardData(tides, weather)
        }
    }

    private suspend fun fetchTides(nowMillis: Long): TideSnapshot {
        val today = LocalDate.now(zone)
        val begin = today.minusDays(1)
        val end = today.plusDays(7)
        val beginStr = begin.format(dateFmt)
        val endStr = end.format(dateFmt)

        // Seaside Heights (8533071) is a subordinate station: NOAA only returns
        // high/low predictions here (interval=hilo). Continuous/hourly products
        // fail with a misleading "Datum input is valid" error — so we synthesize
        // today's curve by cosine-interpolating between consecutive extremes.
        val extremesResp = ApiClients.noaa.predictions(
            station = StationInfo.ID,
            beginDate = beginStr,
            endDate = endStr,
            interval = "hilo"
        )
        extremesResp.error?.message?.let { throw IllegalStateException(it) }

        val extremes = (extremesResp.predictions ?: emptyList()).mapNotNull { it.toEvent() }
            .sortedBy { it.timeMillis }
        if (extremes.isEmpty()) {
            throw IllegalStateException("No tide predictions returned for ${StationInfo.NAME}")
        }
        val curve = synthesizeCurveFromExtremes(extremes, today)

        val nextHigh = extremes.firstOrNull { it.type == TideType.HIGH && it.timeMillis >= nowMillis }
        val nextLow = extremes.firstOrNull { it.type == TideType.LOW && it.timeMillis >= nowMillis }

        val currentHeight = interpolateHeight(curve, nowMillis)
            ?: extremes.minByOrNull { abs(it.timeMillis - nowMillis) }?.heightFt

        val direction = when {
            nextHigh == null && nextLow == null -> TideDirection.SLACK
            nextHigh != null && (nextLow == null || nextHigh.timeMillis < nextLow.timeMillis) ->
                TideDirection.RISING
            else -> TideDirection.FALLING
        }

        val weekStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val weekEnd = today.plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()
        val weekly = extremes.filter { it.timeMillis in weekStart until weekEnd }

        return TideSnapshot(
            direction = direction,
            currentHeightFt = currentHeight,
            nextHigh = nextHigh,
            nextLow = nextLow,
            todayCurve = curve,
            weeklyExtremes = weekly,
            stationId = StationInfo.ID,
            stationName = StationInfo.NAME,
            fetchedAtMillis = nowMillis,
            fromCache = false
        )
    }

    private suspend fun fetchWeather(nowMillis: Long): WeatherSnapshot {
        val resp = ApiClients.openMeteo.forecast()
        val current = resp.current ?: throw IllegalStateException("No weather current data")
        val nowWx = WeatherNow(
            tempF = current.temperature2m,
            weatherCode = current.weatherCode,
            windMph = current.windSpeed10m,
            windDirectionDeg = current.windDirection10m,
            conditionLabel = WeatherCodes.label(current.weatherCode)
        )
        val hourly = resp.hourly
        val hours = mutableListOf<WeatherHour>()
        if (hourly != null) {
            val n = minOf(
                hourly.time.size,
                hourly.temperature2m.size,
                hourly.weatherCode.size,
                hourly.windSpeed10m.size
            )
            for (i in 0 until n) {
                val t = parseOpenMeteo(hourly.time[i]) ?: continue
                if (t < nowMillis - 60 * 60 * 1000L) continue
                hours += WeatherHour(
                    timeMillis = t,
                    tempF = hourly.temperature2m[i],
                    weatherCode = hourly.weatherCode[i],
                    windMph = hourly.windSpeed10m[i],
                    conditionLabel = WeatherCodes.label(hourly.weatherCode[i])
                )
                if (hours.size >= 12) break
            }
        }
        return WeatherSnapshot(
            current = nowWx,
            forecastHours = hours,
            fetchedAtMillis = nowMillis,
            fromCache = false
        )
    }

    private fun NoaaPrediction.toEvent(): TideEvent? {
        val millis = parseNoaa(t) ?: return null
        val height = v.toDoubleOrNull() ?: return null
        val tideType = when (type?.uppercase()) {
            "H" -> TideType.HIGH
            "L" -> TideType.LOW
            else -> return null
        }
        return TideEvent(millis, height, tideType)
    }

    private fun NoaaPrediction.toSample(): TideSample? {
        val millis = parseNoaa(t) ?: return null
        val height = v.toDoubleOrNull() ?: return null
        return TideSample(millis, height)
    }

    private fun parseNoaa(value: String): Long? = try {
        LocalDateTime.parse(value, noaaFmt).atZone(zone).toInstant().toEpochMilli()
    } catch (_: Exception) {
        null
    }

    private fun parseOpenMeteo(value: String): Long? = try {
        LocalDateTime.parse(value, openMeteoFmt).atZone(zone).toInstant().toEpochMilli()
    } catch (_: Exception) {
        null
    }


    /**
     * Build a smooth day curve from high/low extremes (cosine between neighbors).
     * Used when the NOAA station does not publish continuous predictions.
     */
    private fun synthesizeCurveFromExtremes(
        extremes: List<TideEvent>,
        day: LocalDate
    ): List<TideSample> {
        if (extremes.size < 2) {
            return extremes.map { TideSample(it.timeMillis, it.heightFt) }
        }
        val dayStart = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val stepMs = 15L * 60L * 1000L
        val samples = ArrayList<TideSample>()
        var t = dayStart
        while (t < dayEnd) {
            val height = cosineHeightAt(extremes, t) ?: break
            samples += TideSample(t, height)
            t += stepMs
        }
        return samples
    }

    private fun cosineHeightAt(extremes: List<TideEvent>, t: Long): Double? {
        val before = extremes.lastOrNull { it.timeMillis <= t }
        val after = extremes.firstOrNull { it.timeMillis >= t }
        return when {
            before == null -> after?.heightFt
            after == null -> before.heightFt
            before.timeMillis == after.timeMillis -> before.heightFt
            else -> {
                val ratio = (t - before.timeMillis).toDouble() /
                    (after.timeMillis - before.timeMillis).toDouble()
                val smooth = (1.0 - kotlin.math.cos(Math.PI * ratio)) / 2.0
                before.heightFt + (after.heightFt - before.heightFt) * smooth
            }
        }
    }

    private fun interpolateHeight(curve: List<TideSample>, now: Long): Double? {
        if (curve.isEmpty()) return null
        val before = curve.lastOrNull { it.timeMillis <= now }
        val after = curve.firstOrNull { it.timeMillis >= now }
        return when {
            before == null -> after?.heightFt
            after == null -> before.heightFt
            before.timeMillis == after.timeMillis -> before.heightFt
            else -> {
                val ratio = (now - before.timeMillis).toDouble() /
                    (after.timeMillis - before.timeMillis).toDouble()
                before.heightFt + (after.heightFt - before.heightFt) * ratio
            }
        }
    }

    // --- Simple JSON cache DTOs ---

    @kotlinx.serialization.Serializable
    private data class TideCacheDto(
        val direction: String,
        val currentHeightFt: Double? = null,
        val nextHigh: EventDto? = null,
        val nextLow: EventDto? = null,
        val todayCurve: List<SampleDto> = emptyList(),
        val weeklyExtremes: List<EventDto> = emptyList(),
        val stationId: String,
        val stationName: String
    )

    @kotlinx.serialization.Serializable
    private data class EventDto(val timeMillis: Long, val heightFt: Double, val type: String)

    @kotlinx.serialization.Serializable
    private data class SampleDto(val timeMillis: Long, val heightFt: Double)

    @kotlinx.serialization.Serializable
    private data class WeatherCacheDto(
        val tempF: Double,
        val weatherCode: Int,
        val windMph: Double,
        val windDirectionDeg: Int,
        val conditionLabel: String,
        val hours: List<HourDto> = emptyList()
    )

    @kotlinx.serialization.Serializable
    private data class HourDto(
        val timeMillis: Long,
        val tempF: Double,
        val weatherCode: Int,
        val windMph: Double,
        val conditionLabel: String
    )

    private fun serializeTide(t: TideSnapshot): TideCacheDto = TideCacheDto(
        direction = t.direction.name,
        currentHeightFt = t.currentHeightFt,
        nextHigh = t.nextHigh?.let { EventDto(it.timeMillis, it.heightFt, it.type.name) },
        nextLow = t.nextLow?.let { EventDto(it.timeMillis, it.heightFt, it.type.name) },
        todayCurve = t.todayCurve.map { SampleDto(it.timeMillis, it.heightFt) },
        weeklyExtremes = t.weeklyExtremes.map { EventDto(it.timeMillis, it.heightFt, it.type.name) },
        stationId = t.stationId,
        stationName = t.stationName
    )

    private fun deserializeTide(json: String, savedAt: Long): TideSnapshot {
        val dto = ApiClients.json().decodeFromString<TideCacheDto>(json)
        fun ev(e: EventDto) = TideEvent(e.timeMillis, e.heightFt, TideType.valueOf(e.type))
        return TideSnapshot(
            direction = TideDirection.valueOf(dto.direction),
            currentHeightFt = dto.currentHeightFt,
            nextHigh = dto.nextHigh?.let(::ev),
            nextLow = dto.nextLow?.let(::ev),
            todayCurve = dto.todayCurve.map { TideSample(it.timeMillis, it.heightFt) },
            weeklyExtremes = dto.weeklyExtremes.map(::ev),
            stationId = dto.stationId,
            stationName = dto.stationName,
            fetchedAtMillis = savedAt,
            fromCache = true
        )
    }

    private fun serializeWeather(w: WeatherSnapshot): WeatherCacheDto = WeatherCacheDto(
        tempF = w.current.tempF,
        weatherCode = w.current.weatherCode,
        windMph = w.current.windMph,
        windDirectionDeg = w.current.windDirectionDeg,
        conditionLabel = w.current.conditionLabel,
        hours = w.forecastHours.map {
            HourDto(it.timeMillis, it.tempF, it.weatherCode, it.windMph, it.conditionLabel)
        }
    )

    private fun deserializeWeather(json: String, savedAt: Long): WeatherSnapshot {
        val dto = ApiClients.json().decodeFromString<WeatherCacheDto>(json)
        return WeatherSnapshot(
            current = WeatherNow(
                tempF = dto.tempF,
                weatherCode = dto.weatherCode,
                windMph = dto.windMph,
                windDirectionDeg = dto.windDirectionDeg,
                conditionLabel = dto.conditionLabel
            ),
            forecastHours = dto.hours.map {
                WeatherHour(it.timeMillis, it.tempF, it.weatherCode, it.windMph, it.conditionLabel)
            },
            fetchedAtMillis = savedAt,
            fromCache = true
        )
    }
}
