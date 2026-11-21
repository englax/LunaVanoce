// SPDX-License-Identifier: Apache-2.0
package cz.domov.luna;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

/**
 * Luna's violet glass bauble with a ring of fairy lights, designed on a 170 x 220 grid.
 * Everything that does not move (the bauble, its halo, the glow of each light) is drawn once into bitmaps
 * when the view gets its size. A frame then only blits those bitmaps with new alpha values, which is
 * cheap enough for a low frame rate on any phone.
 */
final class Ozdoba {
    static final float W = 170, H = 220;
    static final float CX = 85, CY = 130, R = 62, R_SVETLA = 78;

    /** What the lights do: twinkle and swell with the voice, chase while thinking, pulse while answering, or rest. */
    static final int POSLOUCHA = 0, PREMYSLI = 1, ODPOVIDA = 2, KLID = 3;

    private static final int[] BARVY = {0xFFFFD36B, 0xFFFF8FC7, 0xFFFFA24A, 0xFF7FE3C0};

    private final boolean lights;
    private final float[] x = new float[12], y = new float[12], faze = new float[12], delka = new float[12];
    private final Paint blit = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint haloPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint spritePaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint[] jadro = new Paint[12];
    private final Paint hvezda = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path tvarHvezdy = new Path();

    private Bitmap staticka, halo, hvezdaZare;
    private final Bitmap[] zare = new Bitmap[4];
    private int sirka, vyska;
    private float k, tx, ty, zarePul, hvezdaPul;
    private float hlas;

    Ozdoba(boolean lights) {
        this.lights = lights;
        for (int i = 0; i < 12; i++) {
            double uhel = i / 12.0 * Math.PI * 2 - Math.PI / 2;
            x[i] = (float) (CX + Math.cos(uhel) * R_SVETLA);
            y[i] = (float) (CY + Math.sin(uhel) * R_SVETLA);
            jadro[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
            jadro[i].setColor(BARVY[i % BARVY.length]);
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
        hvezda.setColor(0xFFFFD98A);
    }

    /** Builds the bitmaps for a view of this size. Cheap to call again with the same size. */
    void prepare(int w, int h) {
        if (w <= 0 || h <= 0 || (w == sirka && h == vyska && staticka != null)) return;
        release();
        sirka = w;
        vyska = h;
        k = Math.min(w / W, h / H);
        tx = (w - W * k) / 2f;
        ty = (h - H * k) / 2f;

        staticka = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(staticka);
        c.translate(tx, ty);
        c.scale(k, k);
        kresliOzdobu(c);

        halo = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        c = new Canvas(halo);
        c.translate(tx, ty);
        c.scale(k, k);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new RadialGradient(CX, CY, 91, new int[] {0x8CB07CFF, 0x29B07CFF, 0x00B07CFF}, new float[] {0f, 0.45f, 0.7f}, Shader.TileMode.CLAMP));
        c.drawCircle(CX, CY, 91, p);

        if (lights) {
            int g = Math.max(4, Math.round(20 * k));
            zarePul = g / 2f;
            for (int i = 0; i < 4; i++) {
                zare[i] = Bitmap.createBitmap(g, g, Bitmap.Config.ARGB_8888);
                Paint z = new Paint(Paint.ANTI_ALIAS_FLAG);
                int barva = BARVY[i];
                z.setShader(new RadialGradient(g / 2f, g / 2f, g / 2f, (barva & 0x00FFFFFF) | 0xB3000000, barva & 0x00FFFFFF, Shader.TileMode.CLAMP));
                new Canvas(zare[i]).drawCircle(g / 2f, g / 2f, g / 2f, z);
            }
            int s = Math.max(4, Math.round(32 * k));
            hvezdaPul = s / 2f;
            hvezdaZare = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
            Paint z = new Paint(Paint.ANTI_ALIAS_FLAG);
            z.setShader(new RadialGradient(s / 2f, s / 2f, s / 2f, 0xCCFFD278, 0x00FFD278, Shader.TileMode.CLAMP));
            new Canvas(hvezdaZare).drawCircle(s / 2f, s / 2f, s / 2f, z);
        }
    }

    /** The parts that never move, on the 170 x 220 grid. Runs once per size. */
    private void kresliOzdobu(Canvas c) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        RectF rect = new RectF();
        if (lights) {
            p.setColor(0x8CFFDCAA);
            p.setStrokeWidth(1.5f);
            c.drawLine(CX, 0, CX, 48, p);
        }
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2f);
        p.setColor(0xFFE9B955);
        c.drawCircle(CX - 1, 48, 6, p);
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0, 54, 0, 70, new int[] {0xFFFBE08F, 0xFFD6A040, 0xFFA9762A}, new float[] {0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
        rect.set(69, 54, 101, 70);
        c.drawRoundRect(rect, 4, 4, p);
        p.setShader(new RadialGradient(65, 105, 112, new int[] {0xFFF5EBFF, 0xFFC9A6FF, 0xFF8E57EC, 0xFF5B2DB4, 0xFF2D1466}, new float[] {0f, 0.12f, 0.42f, 0.7f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(CX, CY, R, p);

        c.save();
        Path kruh = new Path();
        kruh.addCircle(CX, CY, R, Path.Direction.CW);
        c.clipPath(kruh);
        p.setShader(new LinearGradient(0, 124, 0, 142, 0xFFEFC45A, 0xFFC98F2E, Shader.TileMode.CLAMP));
        p.setAlpha(242);
        rect.set(13, 124, 157, 142);
        c.drawRoundRect(rect, 72, 9, p);
        p.setShader(null);
        p.setAlpha(255);
        p.setColor(0xFFFFF3C9);
        for (float px = 19; px < 152; px += 12) c.drawCircle(px, 133, 2f, p);
        p.setShader(new RadialGradient(72, 112, 76, new int[] {0x00140032, 0x00140032, 0x80140032}, new float[] {0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(CX, CY, R, p);
        c.restore();

        c.save();
        c.rotate(-28, 63, 93);
        p.setShader(new RadialGradient(63, 93, 21, 0xD9FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        rect.set(42, 82, 84, 104);
        c.drawOval(rect, p);
        c.restore();
    }

    void release() {
        if (staticka != null) staticka.recycle();
        if (halo != null) halo.recycle();
        if (hvezdaZare != null) hvezdaZare.recycle();
        for (int i = 0; i < 4; i++) {
            if (zare[i] != null) zare[i].recycle();
            zare[i] = null;
        }
        staticka = halo = hvezdaZare = null;
        sirka = vyska = 0;
    }

    /** One frame. t is seconds, level the eased voice level from 0 to 1. */
    void draw(Canvas c, float t, int rezim, float level) {
        if (staticka == null) return;
        hlas = level;
        float pivotX = tx + CX * k, stredY = ty + CY * k;
        c.save();
        // A slow swing on its string.
        c.rotate((float) (2.5 * Math.sin(t * Math.PI * 2 / 6.0)), pivotX, ty);

        float puls;
        if (rezim == PREMYSLI) puls = vlna(t, 2.8f, 0);
        else if (rezim == ODPOVIDA) puls = Math.min(1f, 0.5f * vlna(t, 0.9f, 0) + hlas);
        else if (rezim == POSLOUCHA) puls = Math.min(1f, 0.6f * vlna(t, 1.4f, 0) + 0.7f * hlas);
        else puls = 0.5f * vlna(t, 3.2f, 0);
        float s = 0.94f + 0.12f * puls;
        c.save();
        c.scale(s, s, pivotX, stredY);
        haloPaint.setAlpha(Math.round(255 * (0.55f + 0.45f * puls)));
        c.drawBitmap(halo, 0, 0, haloPaint);
        c.restore();

        c.drawBitmap(staticka, 0, 0, blit);

        if (lights) {
            float jadroR = 3.5f * k;
            for (int i = 0; i < 12; i++) {
                float b = jas(i, t, rezim);
                float px = tx + x[i] * k, py = ty + y[i] * k;
                spritePaint.setAlpha(Math.round(255 * b));
                c.drawBitmap(zare[i % 4], px - zarePul, py - zarePul, spritePaint);
                jadro[i].setAlpha(Math.round(255 * (0.35f + 0.65f * b)));
                c.drawCircle(px, py, jadroR, jadro[i]);
            }
            if (rezim == PREMYSLI) {
                double uhel = (t / 2.2) * Math.PI * 2;
                float px = (float) (pivotX + Math.sin(uhel) * R_SVETLA * k);
                float py = (float) (stredY - Math.cos(uhel) * R_SVETLA * k);
                c.drawBitmap(hvezdaZare, px - hvezdaPul, py - hvezdaPul, blit);
                c.save();
                c.translate(px, py);
                c.scale(k, k);
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
