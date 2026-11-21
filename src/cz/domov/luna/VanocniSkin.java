// SPDX-License-Identifier: Apache-2.0
package cz.domov.luna;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Collections;
import java.util.Map;
import me.jxl.kiosk.plugins.KsTheme;
import me.jxl.kiosk.plugins.OverlayFactory;
import me.jxl.kiosk.plugins.OverlaySpec;
import me.jxl.kiosk.plugins.PluginHost;

/**
 * The Christmas voice skin. While it is picked under Voice Satellite > Appearance > Skin, KS draws nothing of its
 * own for a turn and this draws it: full screen a dimmed warm room with falling snow, the bauble and the words;
 * docked a card at the bottom with a small bauble and fairy lights on its edge. Touches always pass through.
 */
final class VanocniSkin {
    static final String KEY = "luna_vanoce";
    static final int DOCK_INSET = 24;
    static final float DEFAULT_BACKGROUND = 0.82f;
    static final String[] EVENTS = {"voice.overlay", "voice.state", "voice.level", "voice.text", "voice.appearance"};

    private PluginHost host;
    private int generation;
    private boolean selected;
    private boolean skinHeard;
    private boolean stateHeard;
    private boolean appearanceHeard;
    private boolean following;
    private String overlay = "hidden";
    private String drawn;

    // Read by the views on the main thread.
    private volatile float level;
    private volatile String state = "idle";
    private volatile String command = "";
    private volatile String answer = "";
    private volatile boolean announcement;
    private volatile Float background;
    private volatile float textScale = 1;
    private volatile boolean reactive = true;
    /** The showing view, touched on the main thread only. */
    private Obsah view;

    synchronized void start(PluginHost host) {
        this.host = host;
        final int current = ++generation;
        try {
            host.subscribe("voice.skin");
        } catch (IllegalArgumentException | UnsupportedOperationException olderHost) {
            return;
        }
        try {
            host.executeCommand("getVoiceState", Collections.<String, Object>emptyMap(), (ok, data, error) -> {
                synchronized (VanocniSkin.this) {
                    if (current != generation || !ok || !(data instanceof Map)) return;
                    Map<?, ?> s = (Map<?, ?>) data;
                    if (!skinHeard) selected = Boolean.TRUE.equals(s.get("skinSelected"));
                    if ("hidden".equals(overlay) && s.get("overlay") instanceof String) overlay = (String) s.get("overlay");
                    if (!stateHeard && s.get("state") instanceof String) state = (String) s.get("state");
                    follow();
                    update();
                }
            });
        } catch (UnsupportedOperationException olderHost) {
            // The voice.skin event alone still brings the skin up.
        }
    }

    /** Follow the turn only while the skin is picked, so the plugin costs nothing while another skin draws. */
    private void follow() {
        if (host == null || selected == following) return;
        following = selected;
        final int current = generation;
        for (String event : EVENTS) {
            try {
                if (following) host.subscribe(event);
                else host.unsubscribe(event);
            } catch (IllegalArgumentException olderHost) {
                // An event this Kiosk Satellite does not have.
            }
        }
        if (!following) return;
        try {
            host.executeCommand("getVoiceAppearance", Collections.<String, Object>emptyMap(), (ok, data, error) -> {
                synchronized (VanocniSkin.this) {
                    if (current == generation && ok && data instanceof Map && !appearanceHeard) appearance((Map<?, ?>) data);
                }
            });
            host.executeCommand("getVoiceText", Collections.<String, Object>emptyMap(), (ok, data, error) -> {
                synchronized (VanocniSkin.this) {
                    if (current == generation && ok && data instanceof Map && command.isEmpty() && answer.isEmpty()) words((Map<?, ?>) data);
                }
            });
        } catch (UnsupportedOperationException olderHost) {
            // Events alone still bring the skin up.
        }
    }

    synchronized void onEvent(String event, Map<String, Object> payload) {
        if (host == null) return;
        if ("ks.voice.skin".equals(event)) {
            selected = Boolean.TRUE.equals(payload.get("selected"));
            skinHeard = true;
            follow();
        } else if ("ks.voice.overlay".equals(event)) {
            overlay = String.valueOf(payload.get("overlay"));
        } else if ("ks.voice.state".equals(event)) {
            stateHeard = true;
            state = payload.get("state") instanceof String ? (String) payload.get("state") : "idle";
            refreshView();
            return;
        } else if ("ks.voice.level".equals(event)) {
            Object value = payload.get("level");
            level = value instanceof Number ? ((Number) value).floatValue() : 0;
            return;
        } else if ("ks.voice.text".equals(event)) {
            words(payload);
            return;
        } else if ("ks.voice.appearance".equals(event)) {
            appearanceHeard = true;
            appearance(payload);
            return;
        } else {
            return;
        }
        update();
    }

    private void appearance(Map<?, ?> settings) {
        background = settings.get("background") instanceof Number ? ((Number) settings.get("background")).floatValue() : null;
        textScale = settings.get("textScale") instanceof Number ? ((Number) settings.get("textScale")).floatValue() : 1;
        reactive = !Boolean.FALSE.equals(settings.get("reactive"));
        final Obsah current = view;
        if (current != null) current.post(current::restyle);
    }

    private void words(Map<?, ?> text) {
        command = text.get("command") instanceof String ? ((String) text.get("command")).trim() : "";
        answer = text.get("answer") instanceof String ? ((String) text.get("answer")).trim() : "";
        announcement = Boolean.TRUE.equals(text.get("announcement"));
        refreshView();
    }

    private void refreshView() {
        final Obsah current = view;
        if (current != null) current.post(current::refresh);
    }

    private void update() {
        String want = selected && ("full".equals(overlay) || "docked".equals(overlay)) ? overlay : null;
        if (want == null ? drawn == null : want.equals(drawn)) return;
        drawn = want;
        if (want == null) {
            host.hideOverlay(KEY);
            level = 0;
        } else if ("full".equals(want)) {
            host.showOverlay(KEY, animated(OverlaySpec.fullScreen().onTop(true).touchable(false).closeOnBack(false), OverlaySpec.FADE, OverlaySpec.FADE, 300), new Factory(false));
        } else {
            host.showOverlay(KEY, animated(OverlaySpec.at(OverlaySpec.BOTTOM).inset(DOCK_INSET).onTop(true).touchable(false).closeOnBack(false), OverlaySpec.SLIDE, OverlaySpec.SLIDE, 250), new Factory(true));
        }
    }

    private static OverlaySpec animated(OverlaySpec spec, String enter, String exit, int ms) {
        try {
            return spec.enter(enter).exit(exit).transitionDuration(ms);
        } catch (LinkageError olderHost) {
            return spec;
        }
    }

    synchronized void stop() {
        host = null;
        generation++;
        selected = skinHeard = stateHeard = appearanceHeard = following = false;
        overlay = "hidden";
        drawn = null;
        level = 0;
        state = "idle";
        command = answer = "";
        announcement = false;
        background = null;
        textScale = 1;
        reactive = true;
    }

    // ---- what the views show, worked out from the turn ----

    int rezim() {
        String s = state;
        if ("processing".equals(s)) return Ozdoba.PREMYSLI;
        if ("responding".equals(s)) return Ozdoba.ODPOVIDA;
        if ("listening".equals(s)) return Ozdoba.POSLOUCHA;
        return answer.isEmpty() ? Ozdoba.POSLOUCHA : Ozdoba.KLID;
    }

    String stitek() {
        if (announcement) return "LUNA · OZNÁMENÍ";
        switch (rezim()) {
            case Ozdoba.PREMYSLI: return "LUNA · PŘEMÝŠLÍ";
            case Ozdoba.ODPOVIDA: return "LUNA · ODPOVÍDÁ";
            case Ozdoba.KLID: return "LUNA";
            default: return "LUNA · POSLOUCHÁ";
        }
    }

    final class Factory implements OverlayFactory {
        private final boolean docked;

        Factory(boolean docked) { this.docked = docked; }

        @Override public View create(Context context, KsTheme theme) {
            Obsah obsah = docked ? new Dok(context, theme) : new Plny(context, theme);
            view = obsah;
            return obsah;
        }

        @Override public void onThemeChanged(View v, KsTheme theme) {
            ((Obsah) v).theme = theme;
            ((Obsah) v).restyle();
        }

        @Override public void onDestroy(View destroyed) {
            if (view == destroyed) view = null;
        }
    }

    /** Shared by both looks: the eased voice level, the clock and the thinking dots. */
    abstract class Obsah extends FrameLayout {
        KsTheme theme;
        float shown;
        int dots = -1;

        Obsah(Context context, KsTheme theme) {
            super(context);
            this.theme = theme;
        }

        float seconds() { return SystemClock.uptimeMillis() / 1000f; }

        /** Called once per frame by the bauble. Eases the level so 30 readings a second still move smoothly. */
        void frame(float t) {
            shown += ((reactive ? level : 0) - shown) * 0.25f;
            if (rezim() == Ozdoba.PREMYSLI && answer.isEmpty()) {
                int d = (int) (t / 0.4f) % 4;
                if (d != dots) { dots = d; post(this::refresh); }
            } else if (dots != -1) {
                dots = -1;
                post(this::refresh);
            }
        }

        String moment() {
            StringBuilder b = new StringBuilder("Moment");
            for (int i = 0; i < Math.max(0, dots); i++) b.append('.');
            return b.toString();
        }

        float sp(float size) { return size * textScale; }

        void text(TextView v, float size, int weight, int color) {
            v.setTypeface(Pismo.get(theme, weight));
            v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp(size));
            v.setTextColor(color);
        }

        abstract void restyle();
        abstract void refresh();
    }

    /** Draws the bauble every frame while it is on screen. */
    final class Kresba extends View {
        private final Obsah owner;
        private final Ozdoba ozdoba = new Ozdoba();
        private final boolean lights;

        Kresba(Context context, Obsah owner, boolean lights) {
            super(context);
            this.owner = owner;
            this.lights = lights;
        }

        @Override protected void onDraw(Canvas canvas) {
            float t = owner.seconds();
            owner.frame(t);
            ozdoba.draw(canvas, getWidth(), getHeight(), t, rezim(), owner.shown, lights);
            if (isShown()) postInvalidateOnAnimation();
        }
    }

    /** Full screen: the warm room dimmed, snow, the bauble and the words beside it. */
    final class Plny extends Obsah {
        private final TextView stav, rekl, odpoved;
        private final Paint zar1 = new Paint(), zar2 = new Paint(), snih = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float[] vx = new float[18], rychlost = new float[18], velikost = new float[18], posun = new float[18], vitr = new float[18];
        private final float density;

        Plny(Context context, KsTheme theme) {
            super(context, theme);
            setWillNotDraw(false);
            DisplayMetrics m = context.getResources().getDisplayMetrics();
            density = m.density;
            for (int i = 0; i < 18; i++) {
                vx[i] = ((i * 0.6180339f) % 1f);
                rychlost[i] = 22 + (i * 13) % 20;
                velikost[i] = 2f + (i * 5 % 7) / 2.4f;
                posun[i] = (i * 97) % 400;
                vitr[i] = 2.5f + (i * 3 % 5) / 2f;
            }
            snih.setColor(0xBFFFFFFF);

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            int vyska = Math.min(theme.px(220), Math.round(m.heightPixels * 0.62f));
            int sirka = Math.round(vyska * Ozdoba.W / Ozdoba.H);
            row.addView(new Kresba(context, this, true), new LinearLayout.LayoutParams(sirka, vyska));

            LinearLayout column = new LinearLayout(context);
            column.setOrientation(LinearLayout.VERTICAL);
            stav = new TextView(context);
            rekl = new TextView(context);
            odpoved = new TextView(context);
            stav.setLetterSpacing(0.15f);
            column.addView(stav);
            LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            p1.topMargin = theme.px(10);
            column.addView(rekl, p1);
            LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            p2.topMargin = theme.px(8);
            column.addView(odpoved, p2);
            rekl.setMaxLines(3);
            rekl.setEllipsize(TextUtils.TruncateAt.END);
            odpoved.setMaxLines(4);
            odpoved.setEllipsize(TextUtils.TruncateAt.END);
            int textSirka = Math.max(theme.px(200), Math.min(theme.px(440), m.widthPixels - sirka - theme.px(110)));
            LinearLayout.LayoutParams pc = new LinearLayout.LayoutParams(textSirka, LinearLayout.LayoutParams.WRAP_CONTENT);
            pc.leftMargin = theme.px(30);
            row.addView(column, pc);
            addView(row, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            restyle();
            refresh();
        }

        @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            zar1.setShader(new RadialGradient(0, h, Math.max(w, h) * 0.7f, 0x47FF8432, 0x00FF8432, Shader.TileMode.CLAMP));
            zar2.setShader(new RadialGradient(w, 0, Math.max(w, h) * 0.6f, 0x4DA0468C, 0x00A0468C, Shader.TileMode.CLAMP));
        }

        @Override void restyle() {
            text(stav, 11, 900, 0xFFD2BBFF);
            text(odpoved, 27, 900, 0xFFFFF4E4);
            refresh();
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            float opacity = background != null ? background : DEFAULT_BACKGROUND;
            canvas.drawColor(Color.argb(Math.round(255 * opacity), 0x1A, 0x0E, 0x10));
            canvas.drawRect(0, 0, getWidth(), getHeight(), zar1);
            canvas.drawRect(0, 0, getWidth(), getHeight(), zar2);
            float t = seconds();
            float h = getHeight() + 20 * density;
            for (int i = 0; i < 18; i++) {
                float y = ((t * rychlost[i] * density + posun[i] * density) % h) - 10 * density;
                float x = vx[i] * getWidth() + (float) Math.sin(t * Math.PI * 2 / vitr[i] + i) * 8 * density;
                canvas.drawCircle(x, y, velikost[i] * density / 2f, snih);
            }
            if (isShown()) postInvalidateOnAnimation();
        }

        @Override void refresh() {
            if (stav == null) return;
            stav.setText(stitek());
            boolean listening = rezim() == Ozdoba.POSLOUCHA && answer.isEmpty();
            if (command.isEmpty()) {
                rekl.setVisibility(listening ? VISIBLE : GONE);
                rekl.setText("…");
            } else {
                rekl.setVisibility(VISIBLE);
                rekl.setText("„" + command + "“");
            }
            if (listening) text(rekl, 26, 800, 0xFFFFF4E4);
            else text(rekl, 17, 800, 0xB8FFF3E2);
            if (!answer.isEmpty()) {
                odpoved.setVisibility(VISIBLE);
                odpoved.setText(answer);
            } else if (rezim() == Ozdoba.PREMYSLI) {
                odpoved.setVisibility(VISIBLE);
                odpoved.setText(moment());
            } else {
                odpoved.setVisibility(GONE);
            }
        }
    }

    /** Docked: a card at the bottom with a small bauble and a string of lights along its top edge. */
    final class Dok extends Obsah {
        private final LinearLayout card;
        private final TextView rekl, odpoved;
        private final Paint[] zarovky = new Paint[8];
        private final float density;

        Dok(Context context, KsTheme theme) {
            super(context, theme);
            DisplayMetrics m = context.getResources().getDisplayMetrics();
            density = m.density;
            int[] barvy = {0xFFFFD36B, 0xFFFF8FC7, 0xFFFFA24A, 0xFF7FE3C0};
            for (int i = 0; i < 8; i++) {
                zarovky[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
                zarovky[i].setColor(barvy[i % 4]);
            }
            setPadding(0, theme.px(6), 0, 0);
            setClipToPadding(false);
            card = new LinearLayout(context);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.addView(new Kresba(context, this, false), new LinearLayout.LayoutParams(theme.px(50), theme.px(62)));
            LinearLayout words = new LinearLayout(context);
            words.setOrientation(LinearLayout.VERTICAL);
            rekl = new TextView(context);
            odpoved = new TextView(context);
            rekl.setSingleLine(true);
            rekl.setEllipsize(TextUtils.TruncateAt.END);
            odpoved.setMaxLines(2);
            odpoved.setEllipsize(TextUtils.TruncateAt.END);
            words.addView(rekl);
            words.addView(odpoved);
            LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            wp.leftMargin = theme.px(14);
            card.addView(words, wp);
            int width = Math.min(theme.px(560), m.widthPixels - theme.px(48));
            addView(card, new FrameLayout.LayoutParams(width, LayoutParams.WRAP_CONTENT));
            restyle();
            refresh();
        }

        @Override void restyle() {
            GradientDrawable bubble = new GradientDrawable();
            bubble.setColor(0xF2221220);
            bubble.setCornerRadius(theme.px(24));
            bubble.setStroke(Math.max(1, theme.px(1)), 0x47CDB4FF);
            card.setBackground(bubble);
            card.setPadding(theme.px(14), theme.px(10), theme.px(20), theme.px(10));
            text(rekl, 12, 900, 0xFFE9DCFF);
            text(odpoved, 18, 900, 0xFFFFF4E4);
            refresh();
        }

        @Override void refresh() {
            if (rekl == null) return;
            rekl.setText(command.isEmpty() ? stitek() : "„" + command + "“");
            if (!answer.isEmpty()) odpoved.setText(answer);
            else if (rezim() == Ozdoba.PREMYSLI) odpoved.setText(moment());
            else odpoved.setText(command.isEmpty() ? "Poslouchám" : "");
            odpoved.setVisibility(odpoved.getText().length() == 0 ? GONE : VISIBLE);
        }

        @Override protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
            float t = seconds();
            float left = card.getLeft() + 26 * density, right = card.getRight() - 26 * density;
            float y = card.getTop();
            for (int i = 0; i < 8; i++) {
                float x = left + (right - left) * i / 7f;
                float b = 0.35f + 0.65f * Ozdoba.vlna(t, 1.6f + (i % 3) * 0.4f, i * 1.3f);
                zarovky[i].setAlpha(Math.round(255 * b));
                canvas.drawOval(x - 3 * density, y - 4 * density, x + 3 * density, y + 4 * density, zarovky[i]);
            }
            if (isShown()) postInvalidateOnAnimation();
        }
    }
}
