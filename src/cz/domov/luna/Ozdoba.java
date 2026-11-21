// SPDX-License-Identifier: Apache-2.0
package cz.domov.luna;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

/**
 * Luna's violet glass bauble with a ring of fairy lights, drawn on a 170 x 220 grid.
 * Every shader is built once in grid units and the canvas is scaled, so a frame allocates nothing.
 */
final class Ozdoba {
    static final float W = 170, H = 220;
    static final float CX = 85, CY = 130, R = 62, R_SVETLA = 78;

    /** What the lights do: twinkle and swell with the voice, chase while thinking, pulse while answering, or rest. */
    static final int POSLOUCHA = 0, PREMYSLI = 1, ODPOVIDA = 2, KLID = 3;

    private static final int[] BARVY = {0xFFFFD36B, 0xFFFF8FC7, 0xFFFFA24A, 0xFF7FE3C0};

    private final Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint provazek = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ocko = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cepicka = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint koule = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stin = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pasek = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tecka = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint odlesk = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hvezda = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hvezdaZare = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint[] zare = new Paint[12];
    private final Paint[] zarovka = new Paint[12];
    private final float[] x = new float[12], y = new float[12], faze = new float[12], delka = new float[12];
    private final Path kruh = new Path();
    private final Path tvarHvezdy = new Path();
    private final RectF rect = new RectF();
    private float hlas;

    Ozdoba() {
        halo.setShader(new RadialGradient(CX, CY, 91, new int[] {0x8CB07CFF, 0x29B07CFF, 0x00B07CFF}, new float[] {0f, 0.45f, 0.7f}, Shader.TileMode.CLAMP));
        provazek.setColor(0x8CFFDCAA);
        provazek.setStrokeWidth(1.5f);
        ocko.setStyle(Paint.Style.STROKE);
        ocko.setStrokeWidth(2f);
        ocko.setColor(0xFFE9B955);
        cepicka.setShader(new LinearGradient(0, 54, 0, 70, new int[] {0xFFFBE08F, 0xFFD6A040, 0xFFA9762A}, new float[] {0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
        koule.setShader(new RadialGradient(65, 105, 112, new int[] {0xFFF5EBFF, 0xFFC9A6FF, 0xFF8E57EC, 0xFF5B2DB4, 0xFF2D1466}, new float[] {0f, 0.12f, 0.42f, 0.7f, 1f}, Shader.TileMode.CLAMP));
        stin.setShader(new RadialGradient(72, 112, 76, new int[] {0x00140032, 0x00140032, 0x80140032}, new float[] {0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
        pasek.setShader(new LinearGradient(0, 124, 0, 142, 0xFFEFC45A, 0xFFC98F2E, Shader.TileMode.CLAMP));
        pasek.setAlpha(242);
        tecka.setColor(0xFFFFF3C9);
        odlesk.setShader(new RadialGradient(63, 93, 21, 0xD9FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        hvezda.setColor(0xFFFFD98A);
        kruh.addCircle(CX, CY, R, Path.Direction.CW);
        for (int i = 0; i < 12; i++) {
            double uhel = i / 12.0 * Math.PI * 2 - Math.PI / 2;
            x[i] = (float) (CX + Math.cos(uhel) * R_SVETLA);
            y[i] = (float) (CY + Math.sin(uhel) * R_SVETLA);
            int barva = BARVY[i % BARVY.length];
            zarovka[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
            zarovka[i].setColor(barva);
            zare[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
            zare[i].setShader(new RadialGradient(x[i], y[i], 10, (barva & 0x00FFFFFF) | 0xB3000000, barva & 0x00FFFFFF, Shader.TileMode.CLAMP));
            // A fixed scatter, so the twinkle looks random but never jumps between frames.
            faze[i] = (float) ((i * 2.399963) % (Math.PI * 2));
            delka[i] = 1.0f + ((i * 7) % 9) / 10f;
        }
        for (int i = 0; i < 10; i++) {
            double uhel = Math.PI / 5 * i - Math.PI / 2;
            float r = i % 2 == 0 ? 9f : 4f;
            float px = (float) (Math.cos(uhel) * r), py = (float) (Math.sin(uhel) * r);
            if (i == 0) tvarHvezdy.moveTo(px, py); else tvarHvezdy.lineTo(px, py);
        }
        tvarHvezdy.close();
        hvezdaZare.setShader(new RadialGradient(0, 0, 16, 0xCCFFD278, 0x00FFD278, Shader.TileMode.CLAMP));
    }

    /**
     * Draws into a box of the given size. t is seconds, level the voice level 0 to 1 already eased,
     * withLights false for the small docked bauble.
     */
    void draw(Canvas c, float sirka, float vyska, float t, int rezim, float level, boolean withLights) {
        hlas = level;
        float k = Math.min(sirka / W, vyska / H);
        c.save();
        c.translate((sirka - W * k) / 2f, (vyska - H * k) / 2f);
        c.scale(k, k);
        // A slow swing on its string.
        c.rotate((float) (2.5 * Math.sin(t * Math.PI * 2 / 6.0)), CX, 0);

        float puls;
        if (rezim == PREMYSLI) puls = vlna(t, 2.8f, 0);
        else if (rezim == ODPOVIDA) puls = Math.min(1f, 0.5f * vlna(t, 0.9f, 0) + hlas);
        else if (rezim == POSLOUCHA) puls = Math.min(1f, 0.6f * vlna(t, 1.4f, 0) + 0.7f * hlas);
        else puls = 0.5f * vlna(t, 3.2f, 0);
        c.save();
        float s = 0.94f + 0.12f * puls;
        c.scale(s, s, CX, CY);
        halo.setAlpha(Math.round(255 * (0.55f + 0.45f * puls)));
        c.drawCircle(CX, CY, 91, halo);
        c.restore();

        if (withLights) c.drawLine(CX, 0, CX, 48, provazek);
        c.drawCircle(CX - 1, 48, 6, ocko);
        rect.set(69, 54, 101, 70);
        c.drawRoundRect(rect, 4, 4, cepicka);

        c.drawCircle(CX, CY, R, koule);
        c.save();
        c.clipPath(kruh);
        rect.set(13, 124, 157, 142);
        c.drawRoundRect(rect, 72, 9, pasek);
        for (float px = 19; px < 152; px += 12) c.drawCircle(px, 133, 2f, tecka);
        c.drawCircle(CX, CY, R, stin);
        c.restore();
        c.save();
        c.rotate(-28, 63, 93);
        rect.set(42, 82, 84, 104);
        c.drawOval(rect, odlesk);
        c.restore();

        if (withLights) {
            for (int i = 0; i < 12; i++) {
                float b = jas(i, t, rezim);
                zare[i].setAlpha(Math.round(255 * b));
                c.drawCircle(x[i], y[i], 10, zare[i]);
                zarovka[i].setAlpha(Math.round(255 * (0.35f + 0.65f * b)));
                c.drawCircle(x[i], y[i], 3.5f, zarovka[i]);
            }
            if (rezim == PREMYSLI) {
                double uhel = (t / 2.2) * Math.PI * 2;
                c.save();
                c.translate((float) (CX + Math.sin(uhel) * R_SVETLA), (float) (CY - Math.cos(uhel) * R_SVETLA));
                c.drawCircle(0, 0, 16, hvezdaZare);
                c.rotate((float) Math.toDegrees(uhel));
                c.drawPath(tvarHvezdy, hvezda);
                c.restore();
            }
        }
        c.restore();
    }

    private float jas(int i, float t, int rezim) {
        float b;
        if (rezim == PREMYSLI) {
            float f = (t - i * 0.12f) / 1.44f;
            f = f - (float) Math.floor(f);
            float v = f < 0.1f ? f / 0.1f : Math.max(0f, 1f - (f - 0.1f) / 0.6f);
            b = 0.25f + 0.75f * v;
        } else if (rezim == ODPOVIDA) {
            b = 0.3f + 0.5f * vlna(t, 0.9f, (i % 2) * (float) Math.PI) + 0.5f * hlas;
        } else if (rezim == POSLOUCHA) {
            b = 0.8f * vlna(t, delka[i], faze[i]) + 0.6f * hlas;
        } else {
            b = 0.25f + 0.5f * vlna(t, delka[i] * 2.2f, faze[i]);
        }
        return Math.max(0f, Math.min(1f, b));
    }

    /** 0 to 1 and back over the period, starting at the given phase. */
    static float vlna(float t, float perioda, float faze) {
        return (float) (0.5 - 0.5 * Math.cos(t * Math.PI * 2 / perioda + faze));
    }
}
