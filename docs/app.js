(() => {
  const STATION_ID = '8533071';
  const STATION_NAME = 'Seaside Heights, ocean';
  const LAT = 40.039;
  const LON = -74.050;
  const TZ = 'America/New_York';

  const $ = (id) => document.getElementById(id);
  const overlay = $('overlay');
  const errorEl = $('error');

  const fmt = {
    time: new Intl.DateTimeFormat('en-US', { timeZone: TZ, hour: 'numeric', minute: '2-digit' }),
    hour: new Intl.DateTimeFormat('en-US', { timeZone: TZ, hour: 'numeric' }),
    day: new Intl.DateTimeFormat('en-US', { timeZone: TZ, weekday: 'short', month: 'numeric', day: 'numeric' }),
    ymd: (d) => {
      const parts = new Intl.DateTimeFormat('en-CA', { timeZone: TZ, year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(d);
      const get = (t) => parts.find(p => p.type === t).value;
      return `${get('year')}${get('month')}${get('day')}`;
    }
  };

  function nyDate(offsetDays = 0) {
    // Approximate "today" in NY by formatting then parsing as local noon UTC-ish
    const now = new Date();
    const ymd = fmt.ymd(new Date(now.getTime() + offsetDays * 86400000));
    // Better: shift using formatter on now±days via midday UTC trick
    const mid = new Date(Date.UTC(
      Number(ymd.slice(0,4)),
      Number(ymd.slice(4,6)) - 1,
      Number(ymd.slice(6,8)),
      17, 0, 0
    ));
    return { ymd, date: mid };
  }

  function parseNoaaLocal(t) {
    // "2026-09-11 08:02" as America/New_York wall time
    const [date, time] = t.split(' ');
    const [y, m, d] = date.split('-').map(Number);
    const [hh, mm] = time.split(':').map(Number);
    // Construct as if NY by using Temporal-less offset estimate via locale
    const guess = new Date(Date.UTC(y, m - 1, d, hh + 4, mm)); // EST baseline
    // Refine: format guess in NY and adjust
    for (let i = 0; i < 3; i++) {
      const parts = new Intl.DateTimeFormat('en-US', {
        timeZone: TZ, year: 'numeric', month: '2-digit', day: '2-digit',
        hour: '2-digit', minute: '2-digit', hour12: false
      }).formatToParts(guess);
      const g = Object.fromEntries(parts.filter(p => p.type !== 'literal').map(p => [p.type, p.value]));
      const gotMin = Number(g.hour) * 60 + Number(g.minute);
      const wantMin = hh * 60 + mm;
      const dayDiff =
        (Date.UTC(y, m - 1, d) - Date.UTC(Number(g.year), Number(g.month) - 1, Number(g.day))) / 86400000;
      guess.setTime(guess.getTime() + ((wantMin - gotMin) + dayDiff * 1440) * 60000);
    }
    return guess.getTime();
  }

  async function fetchJson(url) {
    const res = await fetch(url);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
  }

  async function fetchTides() {
    const today = nyParts(0);
    const begin = nyParts(-1);
    const end = nyParts(7);
    const url = `https://api.tidesandcurrents.noaa.gov/api/prod/datagetter?station=${STATION_ID}&begin_date=${begin}&end_date=${end}&interval=hilo&product=predictions&datum=MLLW&units=english&time_zone=lst_ldt&format=json&application=LavalletteTidesWeb`;
    const data = await fetchJson(url);
    if (data.error?.message) throw new Error(data.error.message);
    const extremes = (data.predictions || []).map(p => ({
      t: parseNoaaLocal(p.t),
      h: Number(p.v),
      type: p.type === 'H' ? 'H' : 'L'
    })).filter(e => Number.isFinite(e.t) && Number.isFinite(e.h))
      .sort((a, b) => a.t - b.t);
    if (!extremes.length) throw new Error('No tide predictions returned');
    return { extremes, todayYmd: today };
  }

  function nyParts(offsetDays) {
    const now = Date.now() + offsetDays * 86400000;
    // Get calendar date in NY for "now + offset"
    const d = new Date(now);
    const parts = new Intl.DateTimeFormat('en-CA', { timeZone: TZ, year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(d);
    const get = (t) => parts.find(p => p.type === t).value;
    return `${get('year')}${get('month')}${get('day')}`;
  }

  function dayBounds(ymd) {
    // start/end of that NY calendar day in epoch ms
    const y = Number(ymd.slice(0,4)), m = Number(ymd.slice(4,6)), d = Number(ymd.slice(6,8));
    // search around UTC candidates
    let start = Date.UTC(y, m - 1, d, 4, 0, 0);
    const target = `${ymd}T00:00`;
    for (let i = 0; i < 8; i++) {
      const parts = new Intl.DateTimeFormat('en-CA', {
        timeZone: TZ, year: 'numeric', month: '2-digit', day: '2-digit',
        hour: '2-digit', minute: '2-digit', hour12: false
      }).formatToParts(new Date(start));
      const g = Object.fromEntries(parts.filter(p => p.type !== 'literal').map(p => [p.type, p.value]));
      const got = `${g.year}${g.month}${g.day}T${g.hour}:${g.minute}`;
      if (got === `${ymd}T00:00`) break;
      const gotMin = Number(g.hour) * 60 + Number(g.minute);
      const dayGot = Date.UTC(Number(g.year), Number(g.month) - 1, Number(g.day));
      const dayWant = Date.UTC(y, m - 1, d);
      start += ((dayWant - dayGot) / 60000 + (0 - gotMin)) * 60000;
    }
    return { start, end: start + 86400000 };
  }

  function cosineHeight(extremes, t) {
    let before = null, after = null;
    for (const e of extremes) {
      if (e.t <= t) before = e;
      if (e.t >= t) { after = e; break; }
    }
    if (!before) return after?.h;
    if (!after) return before.h;
    if (before.t === after.t) return before.h;
    const ratio = (t - before.t) / (after.t - before.t);
    const smooth = (1 - Math.cos(Math.PI * ratio)) / 2;
    return before.h + (after.h - before.h) * smooth;
  }

  function synthesizeCurve(extremes, ymd) {
    const { start, end } = dayBounds(ymd);
    const samples = [];
    const step = 15 * 60 * 1000;
    for (let t = start; t < end; t += step) {
      const h = cosineHeight(extremes, t);
      if (h == null) continue;
      samples.push({ t, h });
    }
    return samples;
  }

  async function fetchWeather() {
    const url = `https://api.open-meteo.com/v1/forecast?latitude=${LAT}&longitude=${LON}&current=temperature_2m,weather_code,wind_speed_10m,wind_direction_10m&hourly=temperature_2m,weather_code,wind_speed_10m&temperature_unit=fahrenheit&wind_speed_unit=mph&timezone=America%2FNew_York&forecast_days=3`;
    return fetchJson(url);
  }

  const WX = {
    label(code) {
      const map = {
        0: 'Clear sky', 1: 'Mainly clear', 2: 'Partly cloudy', 3: 'Overcast',
        45: 'Fog', 48: 'Fog', 51: 'Drizzle', 53: 'Drizzle', 55: 'Drizzle',
        61: 'Rain', 63: 'Rain', 65: 'Heavy rain', 71: 'Snow', 80: 'Showers', 95: 'Thunderstorm'
      };
      return map[code] || 'Weather';
    },
    emoji(code) {
      if (code === 0 || code === 1) return '☀️';
      if (code === 2) return '⛅';
      if (code === 3) return '☁️';
      if (code >= 45 && code < 50) return '🌫️';
      if (code >= 51 && code < 70) return '🌧️';
      if (code >= 71 && code < 80) return '❄️';
      if (code >= 80 && code < 90) return '🌦️';
      if (code >= 95) return '⛈️';
      return '🌤️';
    }
  };

  function compass(deg) {
    const dirs = ['N','NE','E','SE','S','SW','W','NW'];
    return dirs[Math.round(((deg % 360) / 45)) % 8];
  }

  function drawChart(samples, now) {
    const canvas = $('tideChart');
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    const cssW = canvas.clientWidth || 360;
    const cssH = 180;
    canvas.width = Math.floor(cssW * dpr);
    canvas.height = Math.floor(cssH * dpr);
    const ctx = canvas.getContext('2d');
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, cssW, cssH);
    if (samples.length < 2) return;

    const pad = { l: 8, r: 8, t: 12, b: 24 };
    const hs = samples.map(s => s.h);
    const minH = Math.min(...hs) - 0.3;
    const maxH = Math.max(...hs) + 0.3;
    const t0 = samples[0].t, t1 = samples[samples.length - 1].t;
    const x = (t) => pad.l + ((t - t0) / (t1 - t0)) * (cssW - pad.l - pad.r);
    const y = (h) => pad.t + (1 - (h - minH) / (maxH - minH)) * (cssH - pad.t - pad.b);

    // grid
    ctx.strokeStyle = 'rgba(255,255,255,0.08)';
    ctx.lineWidth = 1;
    for (let i = 0; i < 4; i++) {
      const yy = pad.t + i * (cssH - pad.t - pad.b) / 3;
      ctx.beginPath(); ctx.moveTo(pad.l, yy); ctx.lineTo(cssW - pad.r, yy); ctx.stroke();
    }

    // fill
    const grad = ctx.createLinearGradient(0, pad.t, 0, cssH - pad.b);
    grad.addColorStop(0, 'rgba(78,205,196,0.35)');
    grad.addColorStop(1, 'rgba(78,205,196,0.02)');
    ctx.beginPath();
    samples.forEach((s, i) => i ? ctx.lineTo(x(s.t), y(s.h)) : ctx.moveTo(x(s.t), y(s.h)));
    ctx.lineTo(x(samples[samples.length - 1].t), cssH - pad.b);
    ctx.lineTo(x(samples[0].t), cssH - pad.b);
    ctx.closePath();
    ctx.fillStyle = grad;
    ctx.fill();

    // line
    ctx.beginPath();
    samples.forEach((s, i) => i ? ctx.lineTo(x(s.t), y(s.h)) : ctx.moveTo(x(s.t), y(s.h)));
    ctx.strokeStyle = '#4ecdc4';
    ctx.lineWidth = 2.5;
    ctx.stroke();

    // now marker
    if (now >= t0 && now <= t1) {
      const nx = x(now);
      ctx.strokeStyle = '#e8c47c';
      ctx.lineWidth = 1.5;
      ctx.beginPath(); ctx.moveTo(nx, pad.t); ctx.lineTo(nx, cssH - pad.b); ctx.stroke();
      const nh = cosineHeight(
        samples.map(s => ({ t: s.t, h: s.h })),
        now
      );
      if (nh != null) {
        ctx.fillStyle = '#e8c47c';
        ctx.beginPath(); ctx.arc(nx, y(nh), 4.5, 0, Math.PI * 2); ctx.fill();
      }
    }

    // x labels
    ctx.fillStyle = '#9bb8c7';
    ctx.font = '11px -apple-system, BlinkMacSystemFont, sans-serif';
    ctx.textAlign = 'center';
    const labelCount = 5;
    for (let i = 0; i <= labelCount; i++) {
      const t = t0 + (i / labelCount) * (t1 - t0);
      const label = fmt.hour.format(new Date(t)).toLowerCase().replace(' ', '');
      ctx.fillText(label, x(t), cssH - 6);
    }
  }

  function render(tides, weather) {
    const now = Date.now();
    const { extremes, todayYmd } = tides;
    const nextHigh = extremes.find(e => e.type === 'H' && e.t >= now);
    const nextLow = extremes.find(e => e.type === 'L' && e.t >= now);
    const rising = nextHigh && (!nextLow || nextHigh.t < nextLow.t);
    const falling = nextLow && (!nextHigh || nextLow.t < nextHigh.t);
    const dir = rising ? 'Rising' : falling ? 'Falling' : 'Slack';
    const curve = synthesizeCurve(extremes, todayYmd);
    const nowH = cosineHeight(extremes, now);

    $('dirLabel').textContent = `Tide is ${dir}`;
    $('nowHeight').textContent = nowH == null ? 'Approx. now · —' : `Approx. now · ${nowH.toFixed(1)} ft MLLW`;
    $('nextHighTime').textContent = nextHigh ? fmt.time.format(new Date(nextHigh.t)) : '—';
    $('nextHighHt').textContent = nextHigh ? `${nextHigh.h.toFixed(1)} ft` : '—';
    $('nextLowTime').textContent = nextLow ? fmt.time.format(new Date(nextLow.t)) : '—';
    $('nextLowHt').textContent = nextLow ? `${nextLow.h.toFixed(1)} ft` : '—';
    $('stationLine').textContent = `${STATION_NAME} · #${STATION_ID}`;

    ['status','curveCard','weatherCard','weekCard'].forEach(id => { $(id).hidden = false; });
    lastCurve = curve;
    drawChart(curve, now);

    const c = weather.current;
    $('wxEmoji').textContent = WX.emoji(c.weather_code);
    $('wxMain').textContent = `${Math.round(c.temperature_2m)}°F · ${WX.label(c.weather_code)}`;
    $('wxWind').textContent = `Wind ${Math.round(c.wind_speed_10m)} mph ${compass(c.wind_direction_10m)}`;

    const hoursEl = $('hours');
    hoursEl.innerHTML = '';
    const ht = weather.hourly.time || [];
    let count = 0;
    for (let i = 0; i < ht.length && count < 5; i++) {
      const t = Date.parse(ht[i]); // ISO local from Open-Meteo with timezone
      // Open-Meteo returns "2026-09-10T20:00" without Z — interpret as NY by appending offset via Date parsing carefully
      const localMs = parseOpenMeteoLocal(ht[i]);
      if (localMs < now - 3600000) continue;
      const div = document.createElement('div');
      div.className = 'hour';
      div.innerHTML = `<div class="t">${fmt.hour.format(new Date(localMs)).toLowerCase()}</div>
        <div class="e">${WX.emoji(weather.hourly.weather_code[i])}</div>
        <div class="deg">${Math.round(weather.hourly.temperature_2m[i])}°</div>
        <div class="w">${Math.round(weather.hourly.wind_speed_10m[i])} mph</div>`;
      hoursEl.appendChild(div);
      count++;
    }

    const { start: weekStart } = dayBounds(todayYmd);
    const weekEnd = weekStart + 7 * 86400000;
    const weekly = extremes.filter(e => e.t >= weekStart && e.t < weekEnd);
    const byDay = new Map();
    for (const e of weekly) {
      const key = fmt.day.format(new Date(e.t));
      if (!byDay.has(key)) byDay.set(key, []);
      byDay.get(key).push(e);
    }
    const weekList = $('weekList');
    weekList.innerHTML = '';
    for (const [day, events] of byDay) {
      const block = document.createElement('div');
      block.className = 'day-block';
      block.innerHTML = `<p class="day-title">${day}</p>` + events.map(e => `
        <div class="tide-row">
          <span class="dot ${e.type === 'H' ? 'high' : 'low'}"></span>
          <span class="tide-kind">${e.type === 'H' ? 'High' : 'Low'}</span>
          <span class="tide-time">${fmt.time.format(new Date(e.t))}</span>
          <span class="tide-ht">${e.h.toFixed(1)} ft</span>
        </div>`).join('');
      weekList.appendChild(block);
    }

    $('updated').textContent = `Updated ${fmt.time.format(new Date())}`;
    try {
      localStorage.setItem('lavallette-cache', JSON.stringify({
        at: Date.now(), tides: { extremes, todayYmd }, weather
      }));
    } catch (_) {}
  }

  function parseOpenMeteoLocal(s) {
    // "2026-09-10T20:00"
    const [date, time] = s.split('T');
    const [y, m, d] = date.split('-').map(Number);
    const [hh, mm] = (time || '0:0').split(':').map(Number);
    // reuse NOAA parser style
    return parseNoaaLocal(`${date} ${String(hh).padStart(2,'0')}:${String(mm || 0).padStart(2,'0')}`);
  }

  async function load() {
    errorEl.hidden = true;
    overlay.hidden = false;
    $('refreshBtn').classList.add('spin');
    try {
      const [tides, weather] = await Promise.all([fetchTides(), fetchWeather()]);
      render(tides, weather);
      overlay.hidden = true;
    } catch (err) {
      overlay.hidden = true;
      try {
        const cached = JSON.parse(localStorage.getItem('lavallette-cache') || 'null');
        if (cached?.tides && cached?.weather) {
          render(cached.tides, cached.weather);
          $('updated').textContent = `Cached · ${fmt.time.format(new Date(cached.at))} · ${err.message || 'offline'}`;
          return;
        }
      } catch (_) {}
      $('errorMsg').textContent = err.message || String(err);
      errorEl.hidden = false;
    } finally {
      $('refreshBtn').classList.remove('spin');
    }
  }

  $('refreshBtn').addEventListener('click', load);
  $('retryBtn').addEventListener('click', load);
  let lastCurve = null;
  window.addEventListener('resize', () => {
    if (lastCurve && !$('curveCard').hidden) drawChart(lastCurve, Date.now());
  });

  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('./sw.js').catch(() => {});
  }

  load();
})();
