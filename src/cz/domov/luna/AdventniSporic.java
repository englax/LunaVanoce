// SPDX-License-Identifier: Apache-2.0
package cz.domov.luna;

import java.util.Map;
import me.jxl.kiosk.plugins.PluginHost;

/**
 * Registers the advent calendar page as a screensaver mode and keeps its weather current.
 * The page works out the date, doors, candles and countdowns itself from the kiosk's clock.
 */
final class AdventniSporic {
    static final String KEY = "advent";
    static final String TITLE = "Adventní kalendář";
    static final String ENTRY = "advent/index.html";

    private PluginHost host;
    private String entity = "";
    private Pocasi posledni;
    private long kdy;

    synchronized void start(PluginHost host, Map<String, Object> settings) {
        this.host = host;
        entity = "";
        posledni = null;
        publish(null);
        configure(settings);
    }

    synchronized void configure(Map<String, Object> settings) {
        if (host == null) return;
        Object value = settings == null ? null : settings.get("weatherEntity");
        String next = value instanceof String ? ((String) value).trim() : "";
        if (next.equals(entity)) return;
        if (!entity.isEmpty()) {
            try { host.unsubscribe("ha.entity." + entity); } catch (RuntimeException ignored) { /* already gone */ }
        }
        entity = next;
        posledni = null;
        if (entity.isEmpty()) {
            publish(null);
            return;
        }
        try {
            // The first event carries the current state, so the page gets the weather right away.
            host.subscribe("ha.entity." + entity);
        } catch (RuntimeException refused) {
            status("Počasí nejde sledovat: " + refused.getMessage(), true);
        }
    }

    synchronized void onEvent(String event, Map<String, Object> payload) {
        if (host == null || entity.isEmpty() || !event.equals("ks.ha.entity." + entity)) return;
        Pocasi nove = Pocasi.zeSnimku(payload);
        long ted = System.currentTimeMillis();
        if (!Pocasi.znovu(posledni, kdy, nove, ted)) return;
        posledni = nove;
        kdy = ted;
        publish(nove);
    }

    private void publish(Pocasi pocasi) {
        try {
            host.publishScreensaverAsset(KEY, TITLE, ENTRY, Pocasi.data(pocasi));
            status(pocasi == null ? (entity.isEmpty() ? "Spořič je připravený. Počasí nastavíš výš." : "Spořič je připravený, čeká na počasí.") : "Spořič ukazuje: " + pocasi.stav + (pocasi.teplota != null ? ", " + pocasi.teplota + " °C" : ""), false);
        } catch (UnsupportedOperationException | IllegalArgumentException | IllegalStateException failed) {
            status("Spořič se nepodařilo zaregistrovat: " + failed.getMessage(), true);
        }
    }

    private void status(String message, boolean error) {
        try { host.status(message, error); } catch (RuntimeException ignored) { /* older host */ }
    }

    synchronized void stop() {
        host = null;
        entity = "";
        posledni = null;
    }
}
