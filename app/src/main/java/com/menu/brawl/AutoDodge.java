package com.menu.brawl;

import android.graphics.*;
import android.os.*;

public class AutoDodge {

    private static final int SCREEN_W = 1080;
    private static final int SCREEN_H = 2400;
    private static final int PLAYER_X = 540;
    private static final int PLAYER_Y = 1080;
    private static final int DODGE_DIST = 200;
    private static long lastDodge = 0;

    // Tehdit renkleri — Brawl Stars projectile renkleri
    private static final int[][] THREAT_COLORS = {
        {180, 30, 30, 255, 80, 80},   // kırmızı
        {200, 100, 20, 255, 180, 60}, // turuncu
        {220, 50, 50, 255, 120, 120}, // danger zone
    };

    public static void tick() {
        long now = System.currentTimeMillis();
        if (now - lastDodge < 300) return; // 300ms cooldown

        int[] threat = detectThreat();
        if (threat == null) return;

        lastDodge = now;

        int dx = threat[0] - PLAYER_X;
        int dy = threat[1] - PLAYER_Y;
        float mag = (float) Math.sqrt(dx * dx + dy * dy);
        if (mag < 1) return;

        // Ters yöne dodge
        int jx = 180, jy = 2100;
        int ex = (int)(jx + (-dx / mag) * DODGE_DIST);
        int ey = (int)(jy + (-dy / mag) * DODGE_DIST);

        ex = Math.max(50, Math.min(350, ex));
        ey = Math.max(1900, Math.min(2300, ey));

        swipe(jx, jy, ex, ey, 80);
    }

    private static int[] detectThreat() {
        try {
            java.lang.Process p = Runtime.getRuntime().exec("screencap -p /data/local/tmp/sc.png");
            p.waitFor();

            Bitmap bmp = BitmapFactory.decodeFile("/data/local/tmp/sc.png");
            if (bmp == null) return null;

            int x1 = Math.max(0, PLAYER_X - 300);
            int y1 = Math.max(0, PLAYER_Y - 300);
            int x2 = Math.min(SCREEN_W, PLAYER_X + 300);
            int y2 = Math.min(SCREEN_H, PLAYER_Y + 300);

            int totalX = 0, totalY = 0, count = 0;

            for (int y = y1; y < y2; y += 2) {
                for (int x = x1; x < x2; x += 2) {
                    int px = bmp.getPixel(x, y);
                    int r = Color.red(px);
                    int g = Color.green(px);
                    int b = Color.blue(px);

                    for (int[] c : THREAT_COLORS) {
                        if (r >= c[0] && r <= c[3] &&
                            g >= c[1] && g <= c[4] &&
                            b >= c[2] && b <= c[5]) {
                            totalX += x; totalY += y; count++;
                            break;
                        }
                    }
                }
            }

            bmp.recycle();
            if (count < 6) return null;
            return new int[]{totalX / count, totalY / count};

        } catch (Exception e) { return null; }
    }

    private static void swipe(int x1, int y1, int x2, int y2, int ms) {
        try {
            Runtime.getRuntime().exec(
                new String[]{"input", "swipe",
                    String.valueOf(x1), String.valueOf(y1),
                    String.valueOf(x2), String.valueOf(y2),
                    String.valueOf(ms)}
            );
        } catch (Exception ignored) {}
    }
}
