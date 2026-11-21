// SPDX-License-Identifier: Apache-2.0
package cz.domov.luna;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.jxl.kiosk.plugins.OverlayFactory;
import me.jxl.kiosk.plugins.OverlaySpec;
import me.jxl.kiosk.plugins.PluginHost;

/** Start, skin selection, overlay looks and weather publishing against a fake host. No views are built. */
public final class LifecycleTest {
    static final class FakeHost implements PluginHost {
        final List<String> log = new ArrayList<>();
        final List<Map<String, Object>> published = new ArrayList<>();
        Map<String, Object> voiceState = new HashMap<>();
        String lastOverlay;
        OverlaySpec lastSpec;

        @Override public void executeCommand(String command, Map<String, Object> arguments, CommandCallback callback) {
            log.add("cmd " + command);
            if ("getVoiceState".equals(command)) callback.onResult(true, voiceState, null);
            else callback.onResult(true, new HashMap<String, Object>(), null);
        }
        @Override public void subscribe(String event) { log.add("sub " + event); }
        @Override public void unsubscribe(String event) { log.add("unsub " + event); }
        @Override public void showOverlay(String key, OverlaySpec spec, OverlayFactory factory) { log.add("show " + key); lastOverlay = key; lastSpec = spec; }
        @Override public void hideOverlay(String key) { log.add("hide " + key); lastOverlay = null; }
        @Override public void publishScreensaverAsset(String key, String title, String entry, Map<String, Object> data) {
            assert "advent".equals(key) && "advent/index.html".equals(entry) && title.length() <= 80;
            published.add(data);
        }
        @Override public String assetPath(String name) { throw new UnsupportedOperationException("fake"); }
        @Override public void status(String message, boolean error) { log.add((error ? "error " : "status ") + message); }
        @Override public void showWindow(String title, String message, String buttonLabel) {}
        @Override public void hideWindow() {}
        @Override public void log(String message) {}
    }

    public static void main(String[] args) throws Exception {
        FakeHost host = new FakeHost();
        host.voiceState.put("skinSelected", true);
        host.voiceState.put("overlay", "hidden");
        host.voiceState.put("state", "idle");
        LunaVanoce plugin = new LunaVanoce();
        Map<String, Object> settings = new HashMap<>();
        settings.put("weatherEntity", "weather.domov");
        plugin.start(host, settings);

        assert host.log.contains("sub voice.skin") : "follows the skin choice";
        assert host.log.contains("sub voice.state") && host.log.contains("sub voice.text") : "follows the turn once picked";
        assert host.log.contains("sub ha.entity.weather.domov") : "follows the weather";
        assert host.published.size() == 1 && Boolean.FALSE.equals(host.published.get(0).get("maPocasi")) : "registers the screensaver at once";
        assert host.lastOverlay == null : "nothing drawn while the overlay is hidden";

        plugin.onEvent("ks.voice.overlay", Collections.<String, Object>singletonMap("overlay", "full"));
        assert "luna_vanoce".equals(host.lastOverlay) && host.lastSpec.onTop() && !host.lastSpec.touchable() : "full look over everything, touches pass";
        assert host.lastSpec.width() == OverlaySpec.FILL : "full screen";
        plugin.onEvent("ks.voice.overlay", Collections.<String, Object>singletonMap("overlay", "docked"));
        assert OverlaySpec.BOTTOM.equals(host.lastSpec.anchor()) && host.lastSpec.width() == OverlaySpec.WRAP : "docked card at the bottom";
        plugin.onEvent("ks.voice.overlay", Collections.<String, Object>singletonMap("overlay", "hidden"));
        assert host.lastOverlay == null : "hides with the turn";

        plugin.onEvent("ks.voice.skin", Collections.<String, Object>singletonMap("selected", false));
        assert host.log.contains("unsub voice.state") : "lets the turn go when another skin is picked";
        plugin.onEvent("ks.voice.overlay", Collections.<String, Object>singletonMap("overlay", "full"));
        assert host.lastOverlay == null : "never draws while another skin is picked";

        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("status", "available");
        snapshot.put("state", "rainy");
        snapshot.put("attributes", Collections.<String, Object>singletonMap("temperature", 3.6));
        plugin.onEvent("ks.ha.entity.weather.domov", snapshot);
        assert host.published.size() == 2 && "rainy".equals(host.published.get(1).get("pocasi")) && Integer.valueOf(4).equals(host.published.get(1).get("teplota")) : "sends the weather";
        plugin.onEvent("ks.ha.entity.weather.domov", snapshot);
        assert host.published.size() == 2 : "the same weather is not sent twice";
        plugin.onEvent("ks.ha.entity.weather.jinde", snapshot);
        assert host.published.size() == 2 : "ignores other entities";

        settings.put("weatherEntity", "");
        plugin.configure(settings);
        assert host.log.contains("unsub ha.entity.weather.domov") && host.published.size() == 3 && Boolean.FALSE.equals(host.published.get(2).get("maPocasi")) : "clearing the entity drops the weather";

        plugin.stop();
        plugin.onEvent("ks.ha.entity.weather.domov", snapshot);
        assert host.published.size() == 3 : "silent after stop";
        System.out.println("LifecycleTest: ok");
    }
}
