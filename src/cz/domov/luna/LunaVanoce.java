// SPDX-License-Identifier: Apache-2.0
package cz.domov.luna;

import java.util.Map;
import me.jxl.kiosk.plugins.KioskPlugin;
import me.jxl.kiosk.plugins.PluginHost;

/**
 * Luna Vánoce: a Christmas voice skin for Luna and an advent calendar screensaver.
 * The skin draws only while it is picked under Voice Satellite > Appearance > Skin.
 * The screensaver shows up as "Adventní kalendář (Luna Vánoce)" under Screensaver mode.
 */
public final class LunaVanoce implements KioskPlugin {
    private final VanocniSkin skin = new VanocniSkin();
    private final AdventniSporic sporic = new AdventniSporic();
    private PluginHost host;

    @Override public void start(PluginHost host, Map<String, Object> settings) {
        this.host = host;
        Pismo.load(host);
        skin.start(host);
        sporic.start(host, settings);
    }

    @Override public void configure(Map<String, Object> settings) {
        sporic.configure(settings);
    }

    @Override public void execute(String command, Map<String, Object> arguments) {
        // No commands.
    }

    @Override public void onEvent(String event, Map<String, Object> payload) {
        if (host == null || event == null) return;
        if (event.startsWith("ks.voice.")) skin.onEvent(event, payload);
        else if (event.startsWith("ks.ha.entity.")) sporic.onEvent(event, payload);
    }

    @Override public void stop() {
        skin.stop();
        sporic.stop();
        host = null;
    }
}
