package com.menu.brawl;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class OverlayService extends Service {

    private WindowManager wm;
    private View menuView;
    private boolean menuVisible = false;

    // Hile state
    public static boolean aimEnabled = false;
    public static boolean espEnabled = false;
    public static boolean autoDodgeEnabled = false;
    public static boolean speedEnabled = false;
    public static float aimFov = 150f;
    public static float aimSmooth = 3.0f;

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        buildMenu();
        startForeground(1, buildNotification());

        // Ana döngü — her 33ms (30fps)
        new Thread(this::gameLoop).start();
    }

    private void buildMenu() {
        menuView = new LinearLayout(this) {{
            setOrientation(LinearLayout.VERTICAL);
            setBackgroundColor(Color.argb(220, 10, 10, 10));
            setPadding(24, 24, 24, 24);
        }};

        LinearLayout ll = (LinearLayout) menuView;

        // Başlık
        TextView title = new TextView(this);
        title.setText("⚡ BRAWL MENU");
        title.setTextColor(Color.parseColor("#FF6B00"));
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);
        ll.addView(title);

        addDivider(ll);

        // Toggles
        ll.addView(makeToggle("🎯 Auto Aim",    v -> aimEnabled = ((Switch)v).isChecked()));
        ll.addView(makeToggle("👁 ESP",          v -> espEnabled = ((Switch)v).isChecked()));
        ll.addView(makeToggle("💨 Auto Dodge",   v -> autoDodgeEnabled = ((Switch)v).isChecked()));
        ll.addView(makeToggle("⚡ Speed",        v -> speedEnabled = ((Switch)v).isChecked()));

        addDivider(ll);

        // Aim FOV slider
        ll.addView(makeLabel("Aim FOV: 150"));
        SeekBar fovBar = new SeekBar(this);
        fovBar.setMax(300);
        fovBar.setProgress(150);
        fovBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) { aimFov = p; }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        ll.addView(fovBar);

        // Smooth slider
        ll.addView(makeLabel("Smooth: 3.0"));
        SeekBar smoothBar = new SeekBar(this);
        smoothBar.setMax(100);
        smoothBar.setProgress(30);
        smoothBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) { aimSmooth = p / 10f; }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        ll.addView(smoothBar);

        // Layout params
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            380, WindowManager.LayoutParams.WRAP_CONTENT,
            Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 20;
        params.y = 200;

        makeDraggable(menuView, params);
        wm.addView(menuView, params);
        menuView.setVisibility(View.GONE);
    }

    private LinearLayout makeToggle(String label, View.OnClickListener cb) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(13);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Switch sw = new Switch(this);
        sw.setOnCheckedChangeListener((btn, checked) -> cb.onClick(btn));

        row.addView(tv);
        row.addView(sw);
        return row;
    }

    private TextView makeLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#AAAAAA"));
        tv.setTextSize(11);
        tv.setPadding(0, 6, 0, 2);
        return tv;
    }

    private void addDivider(LinearLayout ll) {
        View d = new View(this);
        d.setBackgroundColor(Color.parseColor("#333333"));
        d.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 1));
        ll.addView(d);
    }

    private void makeDraggable(View v, WindowManager.LayoutParams p) {
        v.setOnTouchListener(new View.OnTouchListener() {
            int ix, iy, ox, oy;
            public boolean onTouch(View view, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        ix = (int)e.getRawX(); iy = (int)e.getRawY();
                        ox = p.x; oy = p.y;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        p.x = ox + (int)e.getRawX() - ix;
                        p.y = oy + (int)e.getRawY() - iy;
                        wm.updateViewLayout(v, p);
                        return true;
                }
                return false;
            }
        });
    }

    private void gameLoop() {
        while (true) {
            try {
                if (autoDodgeEnabled) AutoDodge.tick();
                if (aimEnabled)       AutoAim.tick();
                Thread.sleep(33);
            } catch (InterruptedException e) { break; }
        }
    }

    public void toggleMenu() {
        menuView.setVisibility(menuVisible ? View.GONE : View.VISIBLE);
        menuVisible = !menuVisible;
    }

    private Notification buildNotification() {
        NotificationChannel ch = null;
        if (Build.VERSION.SDK_INT >= 26) {
            ch = new NotificationChannel("menu","Menu", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
        return new Notification.Builder(this, "menu")
            .setContentTitle("Brawl Menu Aktif")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build();
    }

    @Override public IBinder onBind(Intent i) { return null; }
}
