// SPDX-License-Identifier: Apache-2.0
package cz.domov.luna;

import java.util.HashMap;
import java.util.Map;

/** Weather mapping and when the screensaver page is sent again. Runs on a plain JVM with -ea. */
public final class PocasiTest {
    private static Map<String, Object> snimek(String status, String state, Object teplota, String jednotka) {
        Map<String, Object> attributes = new HashMap<>();
        if (teplota != null) attributes.put("temperature", teplota);
        if (jednotka != null) attributes.put("temperature_unit", jednotka);
        Map<String, Object> s = new HashMap<>();
        s.put("status", status);
        s.put("state", state);
        s.put("attributes", attributes);
        return s;
    }

    public static void main(String[] args) {
        Pocasi dest = Pocasi.zeSnimku(snimek("available", "rainy", 3.6, "°C"));
        assert dest != null && "rainy".equals(dest.stav) && dest.teplota == 4 : "rain at 4 °C";
        Pocasi mraz = Pocasi.zeSnimku(snimek("available", "snowy", -2.4, "°C"));
        assert mraz.teplota == -2 : "rounds below zero";
        Pocasi f = Pocasi.zeSnimku(snimek("available", "sunny", 50, "°F"));
        assert f.teplota == 10 : "Fahrenheit to Celsius";
        assert Pocasi.zeSnimku(snimek("available", "unavailable", 3, "°C")) == null : "not a weather state";
        assert Pocasi.zeSnimku(snimek("disconnected", "rainy", 3, "°C")) == null : "not available";
        Pocasi bez = Pocasi.zeSnimku(snimek("available", "fog", null, null));
        assert bez != null && bez.teplota == null : "fog without temperature";

        long t = 1_000_000L;
        assert Pocasi.znovu(null, 0, dest, t) : "first weather is sent";
        assert !Pocasi.znovu(dest, t, null, t + 1) : "unavailable keeps the last one";
        assert Pocasi.znovu(dest, t, new Pocasi("snowy", 4), t + 1000) : "a new condition is sent at once";
        assert !Pocasi.znovu(dest, t, new Pocasi("rainy", 5), t + 60_000) : "a degree waits";
        assert Pocasi.znovu(dest, t, new Pocasi("rainy", 5), t + Pocasi.PAUZA_MS) : "a degree goes after the pause";
        assert !Pocasi.znovu(dest, t, new Pocasi("rainy", 4), t + Pocasi.PAUZA_MS * 3) : "nothing changed";

        Map<String, Object> data = Pocasi.data(dest);
        assert Boolean.TRUE.equals(data.get("maPocasi")) && "rainy".equals(data.get("pocasi")) && Integer.valueOf(4).equals(data.get("teplota"));
        Map<String, Object> prazdna = Pocasi.data(null);
        assert Boolean.FALSE.equals(prazdna.get("maPocasi")) && prazdna.get("pocasi") == null;
        System.out.println("PocasiTest: ok");
    }
}
