package com.yl.yldesktop.activity;


import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.weather.LocalWeatherLive;
import com.yl.basemvp.BaseActivity;
import com.yl.yldesktop.R;
import com.yl.yldesktop.animation.FrameAnimationController;
import com.yl.yldesktop.model.MediaModel;
import com.yl.yldesktop.overlay.DrivingRouteOverlay;
import com.yl.yldesktop.presenter.MainPresenter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends BaseActivity<MainPresenter> {

    private final String TAG = MainActivity.class.getSimpleName();
    private MapView mapView;
    private AMap aMap;
    //    private RecyclerView mDeepSeekRecy;
    private LinearLayout mDeepseekLl, mDeepseekLlContent;
    //    private Button mDeepseekBtn;
    private Button mEmptyView;
    //    private ImageView musicPrev, musicPlayStop, musicNext;
    private TextView noWeatherTips, weatherTemperature, todayDate, weatherText, weatherArea, weatherWind;
    private ImageView weatherImg;
    //    private LinearLayout musicLlContent;
    private RelativeLayout weatherRl;
    private MediaModel currentMediaModel;
    private final String START_NAVIGATION = "com.yl.deepseek.start.navigation";
    private ImageView characterImageView;
    private FrameAnimationController idleAnimation, oneClickAnimation, talkingAnimation, thinkingAnimation;
    private FrameAnimationController currentAnimation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate: ");
        enableImmersiveMode();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initPresenter() {
        mPresenter = new MainPresenter(this);
    }

    @Override
    protected void initData() {
        mPresenter.scanAllApp();
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
//        mDeepSeekRecy = findViewById(R.id.deepseek_recy);
        mDeepseekLl = findViewById(R.id.deepseek_ll);
        mDeepseekLlContent = findViewById(R.id.deepseek_ll_content);
//        mDeepseekBtn = findViewById(R.id.deepseek_btn);
//        musicPrev = findViewById(R.id.music_prev);
//        musicPlayStop = findViewById(R.id.music_play_stop);
//        musicNext = findViewById(R.id.music_next);
//        musicTitle = findViewById(R.id.music_title);
//        musicAuthor = findViewById(R.id.music_author);
//        musicImg = findViewById(R.id.music_img);
//        noMusicTips = findViewById(R.id.no_music_tips);
//        musicLlContent = findViewById(R.id.music_ll_contenet);
        noWeatherTips = findViewById(R.id.no_weather_tips);
        weatherTemperature = findViewById(R.id.weather_temperature);
        todayDate = findViewById(R.id.today_date);
        weatherText = findViewById(R.id.weather_text);
        weatherArea = findViewById(R.id.weather_area);
        weatherWind = findViewById(R.id.weather_wind);
        weatherImg = findViewById(R.id.weather_img);
        weatherRl = findViewById(R.id.weather_rl);
        mEmptyView = findViewById(R.id.empty_view);
        mEmptyView.bringToFront();
        mEmptyView.setOnClickListener(mPresenter);
//        musicLlContent.setOnClickListener(mPresenter);
//        musicPrev.setOnClickListener(mPresenter);
//        musicPlayStop.setOnClickListener(mPresenter);
//        musicNext.setOnClickListener(mPresenter);
        mDeepseekLl.setOnClickListener(mPresenter);
//        mDeepseekBtn.setOnClickListener(mPresenter);
//        mDeepSeekRecyAdapter = new DeepSeekRecyAdapter(this, mPresenter.getDeepseekSettingModels());
//        mDeepSeekRecy.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
//        mDeepSeekRecy.setAdapter(mDeepSeekRecyAdapter);
//        mPresenter.initMedia();
        mPresenter.checkingAndroidVersion();
        initMap(savedInstanceState);
        // 初始化视图
        characterImageView = findViewById(R.id.characterImageView);
        characterImageView.setOnClickListener(mPresenter);
        // 初始化动画
        setupAnimations();
        setAnimationState(AnimationState.IDLE);
        registerBroadcast();
    }

    private void setupAnimations() {
        int[] idleFrames = {R.drawable.idle_1, R.drawable.idle_2, R.drawable.idle_3, R.drawable.idle_4, R.drawable.idle_5, R.drawable.idle_6,
                R.drawable.idle_7, R.drawable.idle_8, R.drawable.idle_9, R.drawable.idle_10, R.drawable.idle_11, R.drawable.idle_12,
                R.drawable.idle_13, R.drawable.idle_14, R.drawable.idle_15, R.drawable.idle_16, R.drawable.idle_17, R.drawable.idle_18,
                R.drawable.idle_19, R.drawable.idle_20, R.drawable.idle_21, R.drawable.idle_22, R.drawable.idle_23, R.drawable.idle_24,
                R.drawable.idle_25, R.drawable.idle_26, R.drawable.idle_27, R.drawable.idle_28, R.drawable.idle_29, R.drawable.idle_30,
                R.drawable.idle_31, R.drawable.idle_32, R.drawable.idle_33, R.drawable.idle_34, R.drawable.idle_35, R.drawable.idle_36,
                R.drawable.idle_37, R.drawable.idle_38, R.drawable.idle_39, R.drawable.idle_40, R.drawable.idle_41, R.drawable.idle_42,
                R.drawable.idle_43, R.drawable.idle_44, R.drawable.idle_45, R.drawable.idle_46, R.drawable.idle_47, R.drawable.idle_48,
                R.drawable.idle_49, R.drawable.idle_50, R.drawable.idle_51, R.drawable.idle_52, R.drawable.idle_53, R.drawable.idle_54,
                R.drawable.idle_55, R.drawable.idle_56
        };
        long[] idleDurations = {100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100,
                100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100,
                100, 100, 100, 100, 100, 100, 100, 100, 100, 100};
        // 创建待机动画
        idleAnimation = new FrameAnimationController(characterImageView, idleFrames, idleDurations);

        int[] oneClickFrames = {R.drawable.oneclick_1, R.drawable.oneclick_2, R.drawable.oneclick_3, R.drawable.oneclick_4,
                R.drawable.oneclick_5, R.drawable.oneclick_6, R.drawable.oneclick_7, R.drawable.oneclick_8, R.drawable.oneclick_9,
                R.drawable.oneclick_10, R.drawable.oneclick_11, R.drawable.oneclick_12, R.drawable.oneclick_13, R.drawable.oneclick_14,
                R.drawable.oneclick_15, R.drawable.oneclick_16, R.drawable.oneclick_17, R.drawable.oneclick_18, R.drawable.oneclick_19,
                R.drawable.oneclick_20, R.drawable.oneclick_21, R.drawable.oneclick_22, R.drawable.oneclick_23, R.drawable.oneclick_24,
                R.drawable.oneclick_25, R.drawable.oneclick_26, R.drawable.oneclick_27, R.drawable.oneclick_28, R.drawable.oneclick_29,
                R.drawable.oneclick_30, R.drawable.oneclick_31, R.drawable.oneclick_32, R.drawable.oneclick_33, R.drawable.oneclick_34,
                R.drawable.oneclick_35, R.drawable.oneclick_36, R.drawable.oneclick_37, R.drawable.oneclick_38, R.drawable.oneclick_39,
                R.drawable.oneclick_40, R.drawable.oneclick_41, R.drawable.oneclick_42, R.drawable.oneclick_43, R.drawable.oneclick_44,
                R.drawable.oneclick_45, R.drawable.oneclick_46, R.drawable.oneclick_47, R.drawable.oneclick_48, R.drawable.oneclick_49,
                R.drawable.oneclick_50, R.drawable.oneclick_51, R.drawable.oneclick_52, R.drawable.oneclick_53, R.drawable.oneclick_54,
                R.drawable.oneclick_55, R.drawable.oneclick_56, R.drawable.oneclick_57, R.drawable.oneclick_58, R.drawable.oneclick_59,
                R.drawable.oneclick_60, R.drawable.oneclick_61, R.drawable.oneclick_62, R.drawable.oneclick_63, R.drawable.oneclick_64,
                R.drawable.oneclick_65, R.drawable.oneclick_66, R.drawable.oneclick_67, R.drawable.oneclick_68, R.drawable.oneclick_69,
                R.drawable.oneclick_70
        };
        long[] oneClickDurations = {100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100,
                100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100,
                100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100
        };

        oneClickAnimation = new FrameAnimationController(characterImageView, oneClickFrames, oneClickDurations);
        oneClickAnimation.setAnimationStateListener(new FrameAnimationController.AnimationStateListener() {
            @Override
            public void onFrameChanged(int frame) {
            }

            @Override
            public void onAnimationStart() {
                UsbDeviceConnection
            }

            @Override
            public void onAnimationEnd() {
                setAnimationState(AnimationState.IDLE);
            }
        });

    }

    public void setAnimationState(AnimationState state) {
        // 停止当前动画
        if (currentAnimation != null && currentAnimation.isRunning()) {
            currentAnimation.stop();
        }

        // 根据状态设置新动画
        switch (state) {
            case IDLE:
                idleAnimation.start(true);
                currentAnimation = idleAnimation;
                break;
            case TALKING:
                talkingAnimation.start(false);
                currentAnimation = talkingAnimation;
                break;
            case THINKING:
                thinkingAnimation.start(false);
                currentAnimation = thinkingAnimation;
                break;
            case ONECLICK:
                oneClickAnimation.start(false);
                currentAnimation = oneClickAnimation;
                break;
        }
    }

    // 动画状态枚举
    public enum AnimationState {
        IDLE, TALKING, THINKING, ONECLICK
    }

    private String toPoiLongitude = "0";
    private String toPoiLatitude = "0";

    private void registerBroadcast() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(START_NAVIGATION);
        ContextCompat.registerReceiver(this, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(START_NAVIGATION)) {
                    double latitude = intent.getDoubleExtra("latitude", 0);
                    double longitude = intent.getDoubleExtra("longitude", 0);
//                    isStartNavigation = true;
                    mPresenter.startRouteSearch(new LatLonPoint(latitude, longitude));
                }
            }
        }, intentFilter, ContextCompat.RECEIVER_EXPORTED);
        BroadcastReceiver gaodeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.e(TAG, "onReceive: " + intent.getAction());
                if (intent.getAction().equals("AUTONAVI_STANDARD_BROADCAST_SEND")) {
                    int keyType = intent.getIntExtra("KEY_TYPE", -1);
                    Log.e(TAG, "gaode: " + keyType);
                    if (keyType == 10019) {
                        int extraState = intent.getIntExtra("EXTRA_STATE", -1);
                        Log.e(TAG, "gaode extraState: " + extraState);
                        if (extraState == 9) {
                            aMap.clear();
                        } else if (extraState == 8) {
                            if (!toPoiLatitude.equals("0") || !toPoiLongitude.equals("0")) {
                                mPresenter.startRouteSearch(new LatLonPoint(Double.parseDouble(toPoiLatitude), Double.parseDouble(toPoiLongitude)));
                                toPoiLongitude = "0";
                                toPoiLatitude = "0";
                            }
                        }
                    } else if (keyType == 10056) {
                        try {
                            String extraRoadInfo = intent.getStringExtra("EXTRA_ROAD_INFO");
                            JSONObject jsonObject = new JSONObject(extraRoadInfo);
                            toPoiLongitude = jsonObject.getString("ToPoiLongitude");
                            toPoiLatitude = jsonObject.getString("ToPoiLatitude");
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }

                    }
                }
            }
        };
        IntentFilter gaoDeIntentFilter = new IntentFilter();
        gaoDeIntentFilter.addAction("AUTONAVI_STANDARD_BROADCAST_SEND");
        registerReceiver(gaodeReceiver, gaoDeIntentFilter);
    }

    private void initMap(Bundle savedInstanceState) {
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        if (aMap == null) {
            aMap = mapView.getMap();
            aMap.getUiSettings().setAllGesturesEnabled(false);
            aMap.getUiSettings().setMyLocationButtonEnabled(false);
            aMap.getUiSettings().setScaleControlsEnabled(false);
            //定位样式
            MyLocationStyle myLocationStyle = new MyLocationStyle();
            // 自定义定位蓝点图标
            myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.gps_point));
            // 自定义精度范围的圆形边框颜色  都为0则透明
            myLocationStyle.strokeColor(Color.argb(0, 0, 0, 0));
            // 自定义精度范围的圆形边框宽度  0 无宽度
            myLocationStyle.strokeWidth(0);
            // 设置圆形的填充颜色  都为0则透明
            myLocationStyle.radiusFillColor(Color.argb(0, 0, 0, 0));
            //设置定位蓝点的Style
            aMap.setMyLocationStyle(myLocationStyle);
        }
        aMap.getUiSettings().setZoomControlsEnabled(false);
        aMap.getUiSettings().setScaleControlsEnabled(false);
//        aMap.setOnMapClickListener(mainFragmentPresenter);
        // 设置定位监听
        aMap.setLocationSource(mPresenter);
        // 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        aMap.setMyLocationEnabled(true);
        // 设置定位的类型为定位模式，有定位、跟随或地图根据面向方向旋转几种
        aMap.setMyLocationType(AMap.LOCATION_TYPE_LOCATE);
        aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false
        aMap.stopAnimation();
    }


    /**
     * 请求权限结果
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //设置权限请求结果
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    /**
     * 沉浸式模式
     */
    private void enableImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

//    public void refreshDeepseekData() {
//        mPresenter.initData();
//        mDeepSeekRecyAdapter.setDataList(mPresenter.getDeepseekSettingModels());
//        mDeepSeekRecyAdapter.notifyDataSetChanged();
//    }

//    public void changeMusicPlayPauseBtn(boolean isPlaying) {
//        if (isPlaying) {
//            musicPlayStop.setImageResource(R.drawable.stop);
//        } else {
//            musicPlayStop.setImageResource(R.drawable.music_play);
//        }
//    }

    public void changeWeatherUi(LocalWeatherLive localWeatherLive) {
        if (localWeatherLive == null) {
            noWeatherTips.setVisibility(VISIBLE);
            weatherImg.setVisibility(GONE);
            weatherRl.setVisibility(GONE);
        } else {
            noWeatherTips.setVisibility(GONE);
            weatherImg.setVisibility(VISIBLE);
            weatherRl.setVisibility(VISIBLE);
            weatherTemperature.setText(localWeatherLive.getTemperature() + "°");
            Calendar calendar = Calendar.getInstance();
            //获取系统的日期
            //年
            int year = calendar.get(Calendar.YEAR);
            //月
            int month = calendar.get(Calendar.MONTH) + 1;
            //
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            todayDate.setText(year + "/" + month + "/" + day);
            weatherText.setText(localWeatherLive.getWeather());
            changeWeatherImgByData(localWeatherLive.getWeather());
            weatherArea.setText(localWeatherLive.getCity());
            weatherWind.setText(localWeatherLive.getWindDirection() + "风     " + localWeatherLive.getWindPower() + "级");
        }
    }

    private void changeWeatherImgByData(String weather) {
        if (weather.contains("风")) {
            weatherImg.setImageResource(R.drawable.gale);
        } else if (weather.contains("晴")) {
            weatherImg.setImageResource(R.drawable.fine);
        } else if (weather.contains("云")) {
            weatherImg.setImageResource(R.drawable.cloudy);
        } else if (weather.contains("小雨")) {
            weatherImg.setImageResource(R.drawable.light_rain);
        } else if (weather.contains("中雨")) {
            weatherImg.setImageResource(R.drawable.moderate_rain);
        } else if (weather.contains("大雨")) {
            weatherImg.setImageResource(R.drawable.big_rain);
        } else if (weather.contains("暴雨")) {
            weatherImg.setImageResource(R.drawable.rainstorm);
        } else if (weather.contains("雷") && weather.contains("雨")) {
            weatherImg.setImageResource(R.drawable.thunder_storm);
        } else if (weather.contains("雨")) {
            weatherImg.setImageResource(R.drawable.rain);
        } else if (weather.contains("霾")) {
            weatherImg.setImageResource(R.drawable.haze);
        } else if (weather.contains("小雪")) {
            weatherImg.setImageResource(R.drawable.light_snow);
        } else if (weather.contains("中雪")) {
            weatherImg.setImageResource(R.drawable.moderate_snow);
        } else if (weather.contains("大雪")) {
            weatherImg.setImageResource(R.drawable.heavy_snow);
        } else if (weather.contains("暴雪")) {
            weatherImg.setImageResource(R.drawable.blizzard);
        } else if (weather.contains("雾")) {
            weatherImg.setImageResource(R.drawable.fog);
        }
    }

    public boolean changeMusicUi(boolean isShow, boolean isPlaying, MediaModel model) {
//        if (!isShow) {
//            musicLlContent.setVisibility(GONE);
//            noMusicTips.setVisibility(VISIBLE);
//        } else {
//            Log.e(TAG, "changeMusicUi: title " + isPlaying);
//            if (isPlaying) {
//                musicPlayStop.setImageResource(R.drawable.stop);
//            } else {
//                musicPlayStop.setImageResource(R.drawable.music_play);
//            }
//            if (currentMediaModel != null) {
//                if (currentMediaModel.equals(model)) {
//                    return false;
//                }
//            }
//            currentMediaModel = model;
//            musicLlContent.setVisibility(VISIBLE);
//            noMusicTips.setVisibility(GONE);
//            if (!TextUtils.isEmpty(currentMediaModel.getTitle())) {
//                musicTitle.setText(currentMediaModel.getTitle());
//            }
//            if (!TextUtils.isEmpty(currentMediaModel.getArtist())) {
//                musicAuthor.setText(currentMediaModel.getArtist());
//            }
//            if (currentMediaModel.getAlbumArt() != null) {
////                Glide.with(this).load(currentMediaModel.getAlbumArt()).override(100,100).into(musicImg);
//            }
//        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG, "onResume: ");
        mapView.onResume();
//        mPresenter.initMedia();
//        refreshDeepseekData();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.e(TAG, "onStop: ");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e(TAG, "onPause: ");
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy: ");
        mapView.onDestroy();
        mPresenter.detach();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    public void setDrivingRoute(DrivePath drivePath, LatLonPoint startPos, LatLonPoint targetPos) {
        DrivingRouteOverlay drivingRouteOverlay = new DrivingRouteOverlay(this, aMap, drivePath, startPos, targetPos);
        drivingRouteOverlay.setNodeIconVisibility(false);//设置节点（转弯）marker是否显示
        drivingRouteOverlay.setIsColorfulline(true);//是否用颜色展示交通拥堵情况，默认true
        drivingRouteOverlay.removeFromMap();//去掉DriveLineOverlay上的线段和标记。
        drivingRouteOverlay.addToMap(); //添加驾车路线添加到地图上显示。
        drivingRouteOverlay.zoomToSpan();//移动镜头到当前的视角。
        drivingRouteOverlay.setRouteWidth(1);//设置路线的宽度
    }

    public void moveToCenter(double latitude, double longitude) {
        if (aMap != null) {
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15));
        }
    }

}