// SPDX-License-Identifier: Apache-2.0
package cz.domov.luna;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Turns a Home Assistant weather snapshot into the screensaver's data and decides when to send it again. No Android code, so it is tested on a plain JVM. */
final class Pocasi {
    /** Temperature-only changes wait this long, because each publish reloads the screensaver page. */
    static final long PAUZA_MS = 10 * 60 * 1000L;
    static final Set<String> STAVY = new HashSet<>(Arrays.asList(
        "clear-night", "cloudy", "exceptional", "fog", "hail", "lightning", "lightning-rainy", "partlycloudy",
        "pouring", "rainy", "snowy", "snowy-rainy", "sunny", "windy", "windy-variant"));

    final String stav;
    final Integer teplota;

    Pocasi(String stav, Integer teplota) {
        this.stav = stav;
        this.teplota = teplota;
    }

    /** Null while the entity is unavailable, unknown or not a weather state, so the last good weather stays on screen. */
    static Pocasi zeSnimku(Map<?, ?> snimek) {
        if (snimek == null || !"available".equals(snimek.get("status"))) return null;
        Object state = snimek.get("state");
        if (!(state instanceof String) || !STAVY.contains(state)) return null;
        Integer teplota = null;
        Object attributes = snimek.get("attributes");
        if (attributes instanceof Map) {
            Map<?, ?> a = (Map<?, ?>) attributes;
            Object t = a.get("temperature");
            if (t instanceof Number && !Double.isNaN(((Number) t).doubleValue())) {
                double c = ((Number) t).doubleValue();
                Object unit = a.get("temperature_unit");
                if (unit instanceof String && ((String) unit).contains("F")) c = (c - 32) * 5 / 9;
                teplota = (int) Math.round(c);
            }
        }
        return new Pocasi((String) state, teplota);
    }

    /** Send again when the conditions change, or the whole degree changes and the last send is old enough. */
    static boolean znovu(Pocasi posledni, long kdy, Pocasi nove, long ted) {
        if (nove == null) return false;
        if (posledni == null) return true;
        if (!posledni.stav.equals(nove.stav)) return true;
        boolean jinaTeplota = posledni.teplota == null ? nove.teplota != null : !posledni.teplota.equals(nove.teplota);
        return jinaTeplota && ted - kdy >= PAUZA_MS;
    }

    /** The scalar data the screensaver page reads from its URL fragment. */
    static Map<String, Object> data(Pocasi p) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("maPocasi", p != null);
        data.put("pocasi", p != null ? p.stav : null);
        data.put("teplota", p != null && p.teplota != null ? p.teplota : null);
        return data;
    }
}
