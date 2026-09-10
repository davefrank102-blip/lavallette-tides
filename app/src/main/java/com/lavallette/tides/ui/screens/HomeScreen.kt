package com.lavallette.tides.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lavallette.tides.ui.components.GlassCard
import com.lavallette.tides.ui.components.TideChart
import com.lavallette.tides.ui.components.TideStatusCard
import com.lavallette.tides.ui.components.WeatherCard
import com.lavallette.tides.ui.components.WeeklyTideList
import com.lavallette.tides.ui.theme.Foam
import com.lavallette.tides.ui.theme.OceanGradient
import com.lavallette.tides.ui.theme.Sand
import com.lavallette.tides.ui.theme.SeaGlass
import com.lavallette.tides.ui.theme.TextMuted
import com.lavallette.tides.ui.theme.TextPrimary
import com.lavallette.tides.viewmodel.MainViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val zone = ZoneId.of("America/New_York")
    val updatedFmt = DateTimeFormatter.ofPattern("h:mm a").withZone(zone)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OceanGradient)
    ) {
        when {
            state.loading && state.tides == null -> {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = SeaGlass)
                    Spacer(Modifier.height(12.dp))
                    Text("Fetching Lavallette tides...", color = TextMuted)
                }
            }
            state.error != null && state.tides == null -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Couldn't load data", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(state.error ?: "", color = TextMuted)
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { viewModel.refresh(true) }) {
                        Text("Try again", color = Sand)
                    }
                }
            }
            else -> {
                val tides = state.tides
                val weather = state.weather
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Lavallette", color = Foam, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("Tides & Beach Day", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { viewModel.refresh(true) }) {
                                if (state.refreshing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = SeaGlass,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.Refresh, "Refresh", tint = Foam)
                                }
                            }
                        }
                    }

                    if (state.error != null) {
                        item {
                            Text(
                                "Showing cached data - ${state.error}",
                                color = Sand,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (tides != null) {
                        item { TideStatusCard(tides) }
                        item {
                            GlassCard {
                                Text("Today's tide curve", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "${tides.stationName} - #${tides.stationId}",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                TideChart(samples = tides.todayCurve, nowMillis = System.currentTimeMillis())
                            }
                        }
                    }

                    if (weather != null) {
                        item { WeatherCard(weather) }
                    }

                    if (tides != null) {
                        item { WeeklyTideList(tides.weeklyExtremes) }
                        item {
                            val stamp = tides.fetchedAtMillis
                            val cacheNote = if (tides.fromCache) " - offline cache" else ""
                            Text(
                                "Updated ${updatedFmt.format(Instant.ofEpochMilli(stamp))}$cacheNote",
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
