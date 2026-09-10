package com.lavallette.tides.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

object StationInfo {
    /** Seaside Heights, ocean — Atlantic shoreline ~7 mi south of Lavallette on the same barrier island. */
    const val ID = "8533071"
    const val NAME = "Seaside Heights, ocean"
    const val WHY =
        "Closest NOAA ocean prediction station useful for Lavallette beach days " +
            "(~6.7 mi south). Nearer Barnegat Bay stations (e.g. Mantoloking 8532786) " +
            "are geographically closer but only ~0.4 ft range and do not reflect ocean beach tides."
}

interface NoaaApi {
    @GET("api/prod/datagetter")
    suspend fun predictions(
        @Query("station") station: String,
        @Query("begin_date") beginDate: String,
        @Query("end_date") endDate: String,
        @Query("interval") interval: String,
        @Query("product") product: String = "predictions",
        @Query("datum") datum: String = "MLLW",
        @Query("units") units: String = "english",
        @Query("time_zone") timeZone: String = "lst_ldt",
        @Query("format") format: String = "json",
        @Query("application") application: String = "LavalletteTides"
    ): NoaaPredictionsResponse
}

interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double = 40.039,
        @Query("longitude") longitude: Double = -74.050,
        @Query("current") current: String =
            "temperature_2m,weather_code,wind_speed_10m,wind_direction_10m",
        @Query("hourly") hourly: String =
            "temperature_2m,weather_code,wind_speed_10m",
        @Query("temperature_unit") temperatureUnit: String = "fahrenheit",
        @Query("wind_speed_unit") windSpeedUnit: String = "mph",
        @Query("timezone") timezone: String = "America/New_York",
        @Query("forecast_days") forecastDays: Int = 3
    ): OpenMeteoResponse
}

object ApiClients {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val contentType = "application/json".toMediaType()

    val noaa: NoaaApi = Retrofit.Builder()
        .baseUrl("https://api.tidesandcurrents.noaa.gov/")
        .client(okHttp)
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
        .create(NoaaApi::class.java)

    val openMeteo: OpenMeteoApi = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(okHttp)
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
        .create(OpenMeteoApi::class.java)

    fun json(): Json = json
}
