// Advent calendar screensaver for Luna's kitchen kiosk.
// The plugin passes the weather in the URL fragment; everything else comes from the kiosk's clock.
(function () {
  'use strict';

  var volby = {};
  try { volby = JSON.parse(decodeURIComponent(location.hash.slice(1) || '%7B%7D')) || {}; } catch (e) { volby = {}; }

  var $ = function (id) { return document.getElementById(id); };
  var scena = $('scena');

  // ---- fit the 780 x 360 scene to whatever the kiosk gives the screensaver ----
  function prizpusob() {
    var w = window.innerWidth || 780, h = window.innerHeight || 360;
    var k = Math.min(w / 780, h / 360);
    scena.style.transform = 'translate(' + ((w - 780 * k) / 2) + 'px,' + ((h - 360 * k) / 2) + 'px) scale(' + k + ')';
  }
  window.addEventListener('resize', prizpusob);
  prizpusob();

  function nahoda(seed) {
    var a = seed >>> 0;
    return function () {
      a = (a + 0x6D2B79F5) >>> 0;
      var t = a;
      t = Math.imul(t ^ (t >>> 15), t | 1);
      t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  }

  // ---- the season: which doors are open, how many candles burn, what the countdown says ----
  function den(y, m, d) { return new Date(y, m - 1, d); }
  function rozdil(a, b) {
    return Math.round((den(b.getFullYear(), b.getMonth() + 1, b.getDate()) - den(a.getFullYear(), a.getMonth() + 1, a.getDate())) / 86400000);
  }
  function stav(now) {
    var y = now.getFullYear(), m = now.getMonth() + 1, d = now.getDate();
    var dnes = den(y, m, d);
    var rokVanoc = m === 1 ? y - 1 : y;
    var stedry = den(rokVanoc, 12, 24);
    var ctvrta = den(rokVanoc, 12, 24 - stedry.getDay());
    var nedele = [3, 2, 1, 0].map(function (k) { return den(rokVanoc, 12, ctvrta.getDate() - 7 * k); });
    var faze;
    if ((m === 1 && d >= 7) || (m >= 2 && m <= 10)) faze = 'zima';
    else if (dnes < den(rokVanoc, 12, 1)) faze = 'pred';
    else if (m === 12 && d <= 23) faze = 'advent';
    else if (m === 12 && d === 24) faze = 'stedry';
    else if (m === 12 && d <= 30) faze = 'vanoce';
    else if (m === 12 && d === 31) faze = 'silvestr';
    else if (m === 1 && d === 1) faze = 'novyrok';
    else faze = 'trikrale';
    var dvere = [];
    for (var n = 1; n <= 24; n++) {
      var s = 'zavrene';
      if (faze === 'advent') s = n < d ? 'otevrene' : (n === d ? 'dnes' : 'zavrene');
      else if (faze === 'stedry') s = n === 24 ? 'dnes' : 'otevrene';
      else if (faze === 'vanoce' || faze === 'silvestr' || faze === 'novyrok' || faze === 'trikrale') s = 'otevrene';
      dvere[n] = s;
    }
    var svicky = 0;
    if (faze !== 'zima') {
      svicky = (faze === 'pred' || faze === 'advent' || faze === 'stedry') ? nedele.filter(function (x) { return x <= dnes; }).length : 4;
    }
    var prvni = nedele[0];
    var adventni = 0;
    nedele.forEach(function (x, i) { if (rozdil(x, dnes) === 0) adventni = i + 1; });
    return {
      faze: faze, dvere: dvere, svicky: svicky, d: d,
      doStedreho: rozdil(dnes, stedry),
      doNovehoRoku: rozdil(dnes, den(rokVanoc + 1, 1, 1)),
      prvniAdvent: prvni.getDate() + '. ' + (prvni.getMonth() + 1) + '.',
      adventniNedele: adventni,
      rokNovy: rokVanoc + 1
    };
  }
  function dny(n) {
    if (n === 1) return { sloveso: 'zbývá', jednotka: 'den' };
    if (n >= 2 && n <= 4) return { sloveso: 'zbývají', jednotka: 'dny' };
    return { sloveso: 'zbývá', jednotka: 'dní' };
  }

  // ---- weather: the chip and what falls over the scene ----
  var NAZVY = { 'sunny': 'Jasno', 'clear-night': 'Jasná noc', 'partlycloudy': 'Polojasno', 'cloudy': 'Zataženo', 'rainy': 'Déšť', 'pouring': 'Liják', 'lightning-rainy': 'Bouřka', 'lightning': 'Bouřka', 'snowy': 'Sněží', 'snowy-rainy': 'Déšť se sněhem', 'fog': 'Mlha', 'hail': 'Kroupy', 'windy': 'Větrno', 'windy-variant': 'Větrno', 'exceptional': 'Nevlídno' };
  var MRAK = '<path d="M7 14h10a4 4 0 0 0 .6-7.95A6 6 0 0 0 6.2 7.5 3.3 3.3 0 0 0 7 14z"/>';
  var IKONY = {
    slunce: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#FFD27A" stroke-width="1.8" stroke-linecap="round"><circle cx="12" cy="12" r="4"/><path d="M12 2.5v2 M12 19.5v2 M4.9 4.9l1.4 1.4 M17.7 17.7l1.4 1.4 M2.5 12h2 M19.5 12h2 M4.9 19.1l1.4-1.4 M17.7 6.3l1.4-1.4"/></svg>',
    mesic: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#FFE3B8" stroke-width="1.8" stroke-linejoin="round"><path d="M19.5 14.5A8 8 0 1 1 9.5 4.5a6.5 6.5 0 0 0 10 10z"/></svg>',
    polojasno: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#FFE3B8" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M8 3.5v1.5 M3.5 8h1.5 M4.8 4.8l1 1 M11.2 4.8l-1 1"/><path d="M6 10.5a3 3 0 0 1 5-2.2"/><path d="M9 20h8a3.5 3.5 0 0 0 .5-7 5 5 0 0 0-9.4 1.6A2.8 2.8 0 0 0 9 20z"/></svg>',
    mrak: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#FFE3B8" stroke-width="1.8" stroke-linejoin="round"><path d="M7 18h10a4 4 0 0 0 .6-7.95A6 6 0 0 0 6.2 11.5 3.3 3.3 0 0 0 7 18z"/></svg>',
    dest: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#BFE0FF" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">' + MRAK + '<path d="M8.5 17.5l-1 2.5 M12.5 17.5l-1 2.5 M16.5 17.5l-1 2.5"/></svg>',
    snih: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#FFFFFF" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">' + MRAK + '<path d="M8.5 18v.01 M12 20v.01 M15.5 18v.01 M10 22v.01 M14 22v.01" stroke-width="2.6"/></svg>',
    mlha: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#E9DED2" stroke-width="1.8" stroke-linecap="round"><path d="M4 8h16 M3 12h18 M5 16h14 M8 20h8"/></svg>',
    bourka: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#FFE3B8" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">' + MRAK + '<path d="M12.5 15l-2.2 3.8h3.2l-2 3.7" stroke="#FFD27A"/></svg>'
  };
  var IKONA_PRO = { 'sunny': 'slunce', 'clear-night': 'mesic', 'partlycloudy': 'polojasno', 'cloudy': 'mrak', 'windy': 'mrak', 'windy-variant': 'mrak', 'exceptional': 'mrak', 'rainy': 'dest', 'pouring': 'dest', 'hail': 'dest', 'snowy': 'snih', 'snowy-rainy': 'snih', 'fog': 'mlha', 'lightning': 'bourka', 'lightning-rainy': 'bourka' };

  function pocasi() {
    var chip = $('pocasi');
    var p = volby.maPocasi ? volby.pocasi : null;
    if (!p) { chip.classList.add('skryt'); return; }
    $('ikona').innerHTML = IKONY[IKONA_PRO[p] || 'mrak'];
    var t = volby.teplota;
    if (typeof t === 'number' && isFinite(t)) {
      $('teplota').textContent = (t < 0 ? '−' + Math.abs(Math.round(t)) : String(Math.round(t))) + ' °C';
    } else {
      $('teplota').classList.add('skryt');
    }
    $('nazevPocasi').textContent = NAZVY[p] || p;

    var vrstva = $('castice');
    var r = nahoda(p.split('').reduce(function (a, ch) { return (a * 31 + ch.charCodeAt(0)) >>> 0; }, 7));
    var html = '';
    var dest = { 'rainy': 30, 'pouring': 54, 'lightning-rainy': 38, 'snowy-rainy': 16, 'hail': 26 }[p] || 0;
    var i, dur;
    for (i = 0; i < dest; i++) {
      dur = (p === 'pouring' ? 0.5 : 0.65) + r() * 0.25;
      html += '<span class="kapka" style="left:' + (-5 + r() * 125).toFixed(1) + '%;height:' + Math.round(10 + r() * 9) + 'px;animation-duration:' + dur.toFixed(2) + 's;animation-delay:' + (-r() * dur).toFixed(2) + 's"></span>';
    }
    var sklo = { 'rainy': 14, 'pouring': 22, 'lightning-rainy': 16, 'snowy-rainy': 8, 'hail': 10 }[p] || 0;
    for (i = 0; i < sklo; i++) {
      dur = 6 + r() * 6;
      var s = 3 + r() * 4.5;
      html += '<span class="sklo" style="left:' + (2 + r() * 95).toFixed(1) + '%;top:' + (8 + r() * 80).toFixed(1) + '%;width:' + s.toFixed(1) + 'px;height:' + (s * 1.15).toFixed(1) + 'px;animation-duration:' + dur.toFixed(1) + 's;animation-delay:' + (-r() * dur).toFixed(1) + 's"></span>';
    }
    var snih = { 'snowy': 48, 'snowy-rainy': 20 }[p] || 0;
    for (i = 0; i < snih; i++) {
      dur = 7 + r() * 7;
      var v = (2 + r() * 3.5).toFixed(1);
      html += '<span class="vlocka" style="left:' + (r() * 100).toFixed(1) + '%;animation-duration:' + dur.toFixed(1) + 's;animation-delay:' + (-r() * dur).toFixed(1) + 's"><span style="width:' + v + 'px;height:' + v + 'px;background:rgba(255,255,255,' + (0.55 + r() * 0.4).toFixed(2) + ');animation-duration:' + (2.5 + r() * 2.5).toFixed(1) + 's"></span></span>';
    }
    if (p === 'fog') {
      html += '<div class="mlha"></div><div class="pruh" style="top:6%;animation-duration:46s;animation-delay:-10s"></div><div class="pruh" style="top:36%;animation-duration:58s;animation-delay:-30s"></div><div class="pruh" style="top:64%;animation-duration:40s;animation-delay:-5s"></div>';
    }
    if (p === 'lightning-rainy' || p === 'lightning') html += '<div class="blesk"></div>';
    vrstva.innerHTML = html;
  }

  // ---- fairy lights along the top ----
  (function () {
    var barvy = [['#FFD36B', 'rgba(255,211,107,0.65)'], ['#FFA24A', 'rgba(255,162,74,0.6)'], ['#FF5C5C', 'rgba(255,92,92,0.55)'], ['#FFD36B', 'rgba(255,211,107,0.65)'], ['#FF8FC7', 'rgba(255,143,199,0.55)'], ['#FFB65C', 'rgba(255,182,92,0.6)'], ['#7FE3C0', 'rgba(127,227,192,0.5)'], ['#7FB8FF', 'rgba(127,184,255,0.5)']];
    var r = nahoda(99), html = '';
    for (var i = 0; i < 16; i++) {
      var seg = Math.floor(i / 2), t = i % 2 ? 0.7 : 0.3, b = barvy[i % barvy.length], dur = 2 + r() * 1.6;
      html += '<div class="zarovka" style="left:' + ((seg + t) / 8 * 100).toFixed(2) + '%;top:' + Math.round(4 + 32 * t * (1 - t)) + 'px;background:' + b[0] + ';box-shadow:0 0 9px 2px ' + b[1] + ';animation-duration:' + dur.toFixed(2) + 's;animation-delay:' + (-r() * dur).toFixed(2) + 's"></div>';
    }
    $('svetla').insertAdjacentHTML('beforeend', html);
  })();

  // ---- the parts that change with the date ----
  var PORADI = [13, 4, 19, 8, 22, 1, 10, 16, 2, 24, 11, 6, 21, 7, 14, 3, 18, 9, 5, 23, 12, 20, 15, 17];
  var LIC = ['#F6E7CC', '#F3C6B8', '#D3E5C6', '#F7D597'];
  var CIS = ['#9B2A22', '#8E2430', '#24543D', '#8A3B12'];
  var HVEZDA = '<path d="M12 2.5l2.6 6.1 6.6.5-5 4.3 1.6 6.4L12 16.4l-5.8 3.4 1.6-6.4-5-4.3 6.6-.5z"/>';
  var POZICE = [{ x: 15, base: 69, h: 26 }, { x: 38, base: 58, h: 23 }, { x: 63, base: 58, h: 23 }, { x: 86, base: 69, h: 26 }];
  var DNY = ['neděle', 'pondělí', 'úterý', 'středa', 'čtvrtek', 'pátek', 'sobota'];
  var MESICE = ['ledna', 'února', 'března', 'dubna', 'května', 'června', 'července', 'srpna', 'září', 'října', 'listopadu', 'prosince'];
  var posledniDen = '';

  function ted() {
    var now = new Date();
    if (typeof volby.datum === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(volby.datum)) {
      var p = volby.datum.split('-').map(Number);
      var c = typeof volby.cas === 'string' ? volby.cas.split(':').map(Number) : [now.getHours(), now.getMinutes()];
      now = new Date(p[0], p[1] - 1, p[2], c[0] || 0, c[1] || 0);
    }
    return now;
  }

  function nastav(id, text) {
    var el = $(id);
    el.textContent = text;
    el.classList.toggle('skryt', !text);
  }

  function kalendar(now) {
    var s = stav(now);
    $('mrizka').classList.toggle('zima', s.faze === 'zima');
    if (s.faze === 'zima') return;

    var html = '';
    PORADI.forEach(function (n) {
      var st = s.dvere[n], ctyri = n === 24;
      var barva = ctyri ? '#B0352C' : LIC[n % 4], cislo = ctyri ? '#FFE09A' : CIS[n % 4];
      html += '<div class="dvere' + (st === 'dnes' ? ' dnes' : '') + '">';
      if (st === 'otevrene') {
        html += '<div class="vnitrek"><svg width="18" height="18" viewBox="0 0 24 24" fill="#E8B45A" opacity="0.7">' + HVEZDA + '</svg><span class="maleN">' + n + '</span><div class="kridlo" style="background:' + barva + '"></div></div>';
      } else {
        if (st === 'dnes') html += '<div class="svit"></div><div class="paprsek"></div>';
        html += '<div class="lic" style="background:' + barva + '">' + (ctyri ? '<svg width="15" height="15" viewBox="0 0 24 24" fill="#FFD98A">' + HVEZDA + '</svg>' : '') + '<span class="n" style="color:' + cislo + '">' + n + '</span><span class="klika"></span></div>';
      }
      html += '</div>';
    });
    $('dvirka').innerHTML = html;

    var svicky = '';
    POZICE.forEach(function (c, i) {
      var top = c.base - c.h, hori = i < s.svicky;
      if (hori) svicky += '<div class="zar" style="left:' + (c.x - 15) + 'px;top:' + (top - 27) + 'px"></div>';
      svicky += '<div class="svicka" style="left:' + c.x + 'px;top:' + top + 'px;height:' + c.h + 'px"></div>';
      svicky += '<div class="knot" style="left:' + (c.x + 4.25) + 'px;top:' + (top - 3) + 'px"></div>';
      if (hori) svicky += '<div class="plamen" style="left:' + (c.x + 1) + 'px;top:' + (top - 15) + 'px;animation-delay:' + (-i * 0.37).toFixed(2) + 's"></div>';
    });
    $('svicky').innerHTML = svicky;
  }

  function odpocet(now) {
    var s = stav(now);
    var horni = '', cislo = '', jednotka = '', nadpis = '', spodni = '';
    var hori = s.svicky === 1 ? 'Na věnci hoří 1 svíčka' : 'Na věnci hoří ' + s.svicky + ' svíčky';
    var tv;
    if (s.faze === 'pred' || s.faze === 'advent') {
      tv = dny(s.doStedreho);
      horni = 'Do Štědrého dne ' + tv.sloveso; cislo = String(s.doStedreho); jednotka = tv.jednotka;
      if (s.svicky === 0) spodni = 'Advent začíná v neděli ' + s.prvniAdvent;
      else if (s.adventniNedele > 0) spodni = 'Dnes je ' + s.adventniNedele + '. adventní neděle';
      else spodni = hori;
    } else if (s.faze === 'stedry') {
      horni = 'Dnes je'; nadpis = 'Štědrý den'; spodni = 'Veselé Vánoce';
    } else if (s.faze === 'vanoce') {
      tv = dny(s.doNovehoRoku);
      horni = 'Do Nového roku ' + tv.sloveso; cislo = String(s.doNovehoRoku); jednotka = tv.jednotka;
      spodni = s.d === 25 ? 'Veselé Vánoce, dnes je Boží hod' : (s.d === 26 ? 'Veselé Vánoce, dnes je svatý Štěpán' : 'Krásné svátky');
    } else if (s.faze === 'silvestr') {
      var zbyva = 24 * 60 - (now.getHours() * 60 + now.getMinutes());
      horni = 'Dnes je'; nadpis = 'Silvestr';
      spodni = 'Do půlnoci zbývá ' + Math.floor(zbyva / 60) + ' h ' + (zbyva % 60) + ' min';
    } else if (s.faze === 'novyrok') {
      horni = 'Šťastný nový rok'; nadpis = String(s.rokNovy); spodni = 'PF ' + s.rokNovy;
    } else if (s.faze === 'trikrale') {
      horni = 'Šťastný nový rok'; nadpis = String(s.rokNovy);
      spodni = s.d === 6 ? 'Dnes jsou Tři králové, Vánoce končí' : 'Vánoce končí na Tři krále, 6. 1.';
    }
    nastav('horni', horni);
    nastav('cislo', cislo);
    $('jednotka').textContent = jednotka;
    $('cisloRadek').classList.toggle('skryt', !cislo);
    nastav('nadpis', nadpis);
    nastav('spodni', spodni);
  }

  function tik() {
    var now = ted();
    $('datum').textContent = DNY[now.getDay()] + ' ' + now.getDate() + '. ' + MESICE[now.getMonth()];
    $('hodiny').textContent = now.getHours() + ':' + String(now.getMinutes()).padStart(2, '0');
    var klic = now.getFullYear() + '-' + now.getMonth() + '-' + now.getDate();
    if (klic !== posledniDen) {
      posledniDen = klic;
      kalendar(now);
    }
    odpocet(now);
  }

  pocasi();
  tik();
  // Cheap enough to check every few seconds, so the clock never lags.
  setInterval(tik, 5000);
})();
