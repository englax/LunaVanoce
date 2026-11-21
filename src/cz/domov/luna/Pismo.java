// SPDX-License-Identifier: Apache-2.0
package cz.domov.luna;

import android.graphics.Typeface;
import java.io.File;
import me.jxl.kiosk.plugins.KsTheme;
import me.jxl.kiosk.plugins.PluginHost;

/** Nunito from the package (the screensaver uses the same file), with the KS typeface as a fallback. */
final class Pismo {
    private static volatile Typeface w700, w800, w900;

    private Pismo() {}

    static void load(PluginHost host) {
        try {
            String path = host.assetPath("fonts/Nunito.ttf");
            if (path == null || !new File(path).isFile()) return;
            w700 = build(path, 700);
            w800 = build(path, 800);
            w900 = build(path, 900);
        } catch (RuntimeException | LinkageError unavailable) {
            // Older hosts without assets, or a font Android refuses: KsTheme's Rubik takes over.
            w700 = w800 = w900 = null;
        }
    }

    private static Typeface build(String path, int weight) {
        return new Typeface.Builder(path).setFontVariationSettings("'wght' " + weight).setWeight(weight).build();
    }

    static Typeface get(KsTheme theme, int weight) {
        Typeface chosen = weight >= 900 ? w900 : (weight >= 800 ? w800 : w700);
        return chosen != null ? chosen : theme.typeface(weight);
    }
}
