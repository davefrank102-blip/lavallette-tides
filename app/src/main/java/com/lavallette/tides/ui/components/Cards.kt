package com.lavallette.tides.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lavallette.tides.data.model.TideDirection
import com.lavallette.tides.data.model.TideEvent
import com.lavallette.tides.data.model.TideSnapshot
import com.lavallette.tides.data.model.TideType
import com.lavallette.tides.data.model.WeatherHour
import com.lavallette.tides.data.model.WeatherSnapshot
import com.lavallette.tides.data.remote.WeatherCodes
import com.lavallette.tides.ui.theme.CardGlass
import com.lavallette.tides.ui.theme.Coral
import com.lavallette.tides.ui.theme.Foam
import com.lavallette.tides.ui.theme.OceanBright
import com.lavallette.tides.ui.theme.Sand
import com.lavallette.tides.ui.theme.SeaGlass
import com.lavallette.tides.ui.theme.Sun
import com.lavallette.tides.ui.theme.TextMuted
import com.lavallette.tides.ui.theme.TextPrimary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val zone = ZoneId.of("America/New_York")
private val timeFmt = DateTimeFormatter.ofPattern("h:mm a").withZone(zone)
private val dayFmt = DateTimeFormatter.ofPattern("EEE M/d").withZone(zone)
private val hourFmt = DateTimeFormatter.ofPattern("ha").withZone(zone)

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(CardGlass, Color(0xB0083550))
                )
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        content()
    }
}

@Composable
fun TideStatusCard(tides: TideSnapshot) {
    val rising = tides.direction == TideDirection.RISING
    val dirLabel = when (tides.direction) {
        TideDirection.RISING -> "Rising"
        TideDirection.FALLING -> "Falling"
        TideDirection.SLACK -> "Slack"
    }
    val accent = if (rising) SeaGlass else Coral

    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (rising) Icons.AutoMirrored.Filled.TrendingUp
                else Icons.AutoMirrored.Filled.TrendingDown,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Tide is $dirLabel", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                val height = tides.currentHeightFt?.let { String.format("%.1f ft MLLW", it) } ?: "—"
                Text("Approx. now · $height", color = TextMuted, fontSize = 14.sp)
            }
            Icon(Icons.Default.WaterDrop, null, tint = Foam, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NextTideChip(
                label = "Next High",
                event = tides.nextHigh,
                color = SeaGlass,
                modifier = Modifier.weight(1f)
            )
            NextTideChip(
                label = "Next Low",
                event = tides.nextLow,
                color = Sand,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NextTideChip(
    label: String,
    event: TideEvent?,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(12.dp)
    ) {
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        if (event == null) {
            Text("—", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        } else {
            Text(timeFmt.format(Instant.ofEpochMilli(event.timeMillis)), color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(String.format("%.1f ft", event.heightFt), color = TextMuted, fontSize = 13.sp)
        }
    }
}

@Composable
fun WeatherCard(weather: WeatherSnapshot) {
    val c = weather.current
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(WeatherCodes.emoji(c.weatherCode), fontSize = 36.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Beach weather", color = TextMuted, fontSize = 13.sp)
                Text(
                    "${c.tempF.roundToInt()}°F · ${c.conditionLabel}",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(Icons.Default.WbSunny, null, tint = Sun)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Air, null, tint = Foam, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "Wind ${c.windMph.roundToInt()} mph ${degreesToCompass(c.windDirectionDeg)}",
                color = TextMuted,
                fontSize = 14.sp
            )
        }
        if (weather.forecastHours.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text("Next hours", color = Sand, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                weather.forecastHours.take(5).forEach { hour ->
                    HourChip(hour, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HourChip(hour: WeatherHour, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(OceanBright.copy(alpha = 0.25f))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            hourFmt.format(Instant.ofEpochMilli(hour.timeMillis)).lowercase(),
            color = TextMuted,
            fontSize = 11.sp
        )
        Text(WeatherCodes.emoji(hour.weatherCode), fontSize = 16.sp)
        Text("${hour.tempF.roundToInt()}°", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text("${hour.windMph.roundToInt()} mph", color = TextMuted, fontSize = 10.sp)
    }
}

@Composable
fun WeeklyTideList(events: List<TideEvent>) {
    GlassCard {
        Text("This week", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Highs & lows · ocean station", color = TextMuted, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        val byDay = events.groupBy { dayFmt.format(Instant.ofEpochMilli(it.timeMillis)) }
        byDay.forEach { (day, dayEvents) ->
            Text(day, color = Sand, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            dayEvents.forEach { event ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isHigh = event.type == TideType.HIGH
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isHigh) SeaGlass else Coral)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (isHigh) "High" else "Low",
                        color = TextPrimary,
                        modifier = Modifier.width(48.dp),
                        fontSize = 14.sp
                    )
                    Text(
                        timeFmt.format(Instant.ofEpochMilli(event.timeMillis)),
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp
                    )
                    Text(
                        String.format("%.1f ft", event.heightFt),
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun degreesToCompass(deg: Int): String {
    val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val idx = ((deg % 360) / 45.0).roundToInt() % 8
    return dirs[idx]
}
