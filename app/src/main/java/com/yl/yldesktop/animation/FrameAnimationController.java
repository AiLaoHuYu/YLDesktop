package com.yl.yldesktop.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

public class FrameAnimationController {
    private ImageView imageView;
    private int[] frameResources;
    private long[] frameDurations;
    private ValueAnimator animator;
    private AnimationStateListener listener;

    public interface AnimationStateListener {
        void onFrameChanged(int frame);
        void onAnimationStart();
        void onAnimationEnd();
    }

    public FrameAnimationController(ImageView imageView, int[] frameResources, long[] frameDurations) {
        this.imageView = imageView;
        this.frameResources = frameResources;
        this.frameDurations = frameDurations;
    }

    public void setAnimationStateListener(AnimationStateListener listener) {
        this.listener = listener;
    }

    public void start(boolean needRelay) {
        stop();

        long totalDuration = 0;
        for (long duration : frameDurations) {
            totalDuration += duration;
        }

        animator = ValueAnimator.ofInt(0, frameResources.length - 1);
        animator.setDuration(totalDuration);
        if (needRelay) {
            animator.setRepeatCount(ValueAnimator.INFINITE);
        }
        animator.setInterpolator(new LinearInterpolator());

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int frame = (int) animation.getAnimatedValue();
                imageView.setImageResource(frameResources[frame]);

                if (listener != null) {
                    listener.onFrameChanged(frame);
                }
            }
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (listener != null) {
                    listener.onAnimationStart();
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (listener != null) {
                    listener.onAnimationEnd();
                }
            }
        });

        animator.start();
    }

    public void stop() {
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
    }

    public boolean isRunning() {
        return animator != null && animator.isRunning();
    }
}
