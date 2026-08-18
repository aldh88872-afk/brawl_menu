package com.menu.brawl;

import android.graphics.*;
import android.os.*;

public class AutoAim {

    private static final int SCREEN_W = 1080;
    private static final int SCREEN_H = 2400;
    private static final int CENTER_X = 540;
    private static final int CENTER_Y = 1080;

    // Düşman renk aralıkları — Brawl Stars enemy health bar kırmızısı
    private static final int[] ENEMY_LOW  = {180, 20, 20};
    private static final int[] ENEMY_HIGH = {255, 60, 60};

    private static long lastAim = 0;

    public static void tick() {
        long now = System.currentTimeMillis();
        if (now - lastAim < 100) return;
        lastAim = now;

        int[] target = findEnemy();
        if (target == null) return;

        float fov = OverlayService.aimFov;
        float smooth = OverlayService.aimSmooth;

        float dist = (float) Math.sqrt(
            Math.pow(target[0] - CENTER_X, 2) +
            Math.pow(target[1] - CENTER_Y, 2)
        );

        if (dist > fov) return; // FOV dışı

        // Joystick sağ tarafta — ateş joystick merkezi
        int fireJoyX = 900, fireJoyY = 2100;

        int dx = target[0] - CENTER_X;
        int dy = target[1] - CENTER_Y;
        float mag = (float) Math.sqrt(dx * dx + dy * dy);

        int aimX = (int)(fireJoyX + (dx / mag) * 120);
        int aimY = (int)(fireJoyY + (dy / mag) * 120);

        aimX = Math.max(750, Math.min(1050, aimX));
        aimY = Math.max(1900, Math.min(2300, aimY));

        swipeTo(fireJoyX, fireJoyY, aimX, aimY, (int)(smooth * 10));
    }

    private static int[] findEnemy() {
        try {
            Runtime.getRuntime().exec("screencap -p /data/local/tmp/aim.png").waitFor();
            Bitmap bmp = BitmapFactory.decodeFile("/data/local/tmp/aim.png");
            if (bmp == null) return null;

            int fov = (int) OverlayService.aimFov;
            int x1 = Math.max(0, CENTER_X - fov);
            int y1 = Math.max(0, CENTER_Y - fov);
            int x2 = Math.min(SCREEN_W, CENTER_X + fov);
            int y2 = Math.min(SCREEN_H, CENTER_Y + fov);

            int bestX = -1, bestY = -1;
            float bestDist = Float.MAX_VALUE;

            for (int y = y1; y < y2; y += 3) {
                for (int x = x1; x < x2; x += 3) {
                    int px = bmp.getPixel(x, y);
                    int r = Color.red(px), g = Color.green(px), b = Color.blue(px);
                    if (r >= ENEMY_LOW[0] && r <= ENEMY_HIGH[0] &&
                        g >= ENEMY_LOW[1] && g <= ENEMY_HIGH[1] &&
                        b >= ENEMY_LOW[2] && b <= ENEMY_HIGH[2]) {
                        float d = (float) Math.sqrt(
                            Math.pow(x - CENTER_X, 2) + Math.pow(y - CENTER_Y, 2));
                        if (d < bestDist) { bestDist = d; bestX = x; bestY = y; }
                    }
                }
            }

            bmp.recycle();
            return bestX == -1 ? null : new int[]{bestX, bestY};
        } catch (Exception e) { return null; }
    }

    private static void swipeTo(int x1, int y1, int x2, int y2, int ms) {
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
