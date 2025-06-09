package com.yl.yldesktop.utils;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

public class AnimationUtil {

    public static void startScaleUpAnimation(View view){
        ScaleAnimation scaleUpAnimation;
        // 创建放大动画
        scaleUpAnimation = new ScaleAnimation(
                1.0f, 1.1f, // x轴方向初始尺寸到最终尺寸的比例
                1.0f, 1.1f, // y轴方向初始尺寸到最终尺寸的比例
                Animation.RELATIVE_TO_SELF, 0.5f, // 动画中心点相对于自身的x轴位置
                Animation.RELATIVE_TO_SELF, 0.5f); // 动画中心点相对于自身的y轴位置
        scaleUpAnimation.setDuration(200); // 设置动画持续时间为200毫秒
        scaleUpAnimation.setFillAfter(true); // 设置动画结束后保持最终状态
        view.startAnimation(scaleUpAnimation);
    }

    public static void startScaleDownAnimation(View view){
        ScaleAnimation scaleDownAnimation;
        // 创建放大动画
        // 创建缩小动画
        scaleDownAnimation = new ScaleAnimation(
                1.1f, 1.0f, // x轴方向初始尺寸到最终尺寸的比例
                1.1f, 1.0f, // y轴方向初始尺寸到最终尺寸的比例
                Animation.RELATIVE_TO_SELF, 0.5f, // 动画中心点相对于自身的x轴位置
                Animation.RELATIVE_TO_SELF, 0.5f); // 动画中心点相对于自身的y轴位置
        scaleDownAnimation.setDuration(200); // 设置动画持续时间为200毫秒
        scaleDownAnimation.setFillAfter(true); // 设置动画结束后保持最终状态
        view.startAnimation(scaleDownAnimation);
    }

}
