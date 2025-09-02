package com.yl.ylappmonitor;

import android.content.Context;
import android.view.Choreographer;

// FPSMonitor.java - 帧率监控
public class FPSMonitor implements Choreographer.FrameCallback {
    private static FPSMonitor instance;
    private long lastFrameTime = 0;
    private int frameCount = 0;
    private int fps = 0;

    public static void start(Context context) {
        if (instance == null) {
            instance = new FPSMonitor();
        }
        Choreographer.getInstance().postFrameCallback(instance);
    }

    public static int getAvgFPS() {
        return instance != null ? instance.fps : 0;
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (lastFrameTime == 0) {
            lastFrameTime = frameTimeNanos;
        }

        long diff = (frameTimeNanos - lastFrameTime) / 1000000;
        if (diff >= 1000) {
            fps = frameCount;
            frameCount = 0;
            lastFrameTime = frameTimeNanos;
        } else {
            frameCount++;
        }
        Choreographer.getInstance().postFrameCallback(this);
    }
}