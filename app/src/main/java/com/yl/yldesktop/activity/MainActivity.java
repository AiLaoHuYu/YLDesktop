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
import android.text.TextUtils;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amap.api.location.AMapLocationClient;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.weather.LocalWeatherLive;
import com.bumptech.glide.Glide;
import com.yl.basemvp.BaseActivity;
import com.yl.yldesktop.R;
import com.yl.yldesktop.adapter.DeepSeekRecyAdapter;
import com.yl.yldesktop.model.MediaModel;
import com.yl.yldesktop.overlay.DrivingRouteOverlay;
import com.yl.yldesktop.presenter.MainPresenter;

import java.util.Calendar;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends BaseActivity<MainPresenter> {

    private final String TAG = MainActivity.class.getSimpleName();
    private MapView mapView;
    private AMap aMap;
    private RecyclerView mDeepSeekRecy;
    private DeepSeekRecyAdapter mDeepSeekRecyAdapter;
    private LinearLayout mDeepseekLl, mDeepseekLlContent;
    private Button mDeepseekBtn;
    private Button mEmptyView;
    private ImageView musicPrev, musicPlayStop, musicNext;
    private TextView musicTitle, musicAuthor, noMusicTips, noWeatherTips, weatherTemperature, todayDate, weatherText, weatherArea, weatherWind;
    private ImageView musicImg, weatherImg;
    private LinearLayout musicLlContent;
    private RelativeLayout weatherRl;
    private MediaModel currentMediaModel;
    private final String START_NAVIGATION = "com.yl.deepseek.start.navigation";

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
        mDeepSeekRecy = findViewById(R.id.deepseek_recy);
        mDeepseekLl = findViewById(R.id.deepseek_ll);
        mDeepseekLlContent = findViewById(R.id.deepseek_ll_content);
        mDeepseekBtn = findViewById(R.id.deepseek_btn);
        musicPrev = findViewById(R.id.music_prev);
        musicPlayStop = findViewById(R.id.music_play_stop);
        musicNext = findViewById(R.id.music_next);
        musicTitle = findViewById(R.id.music_title);
        musicAuthor = findViewById(R.id.music_author);
        musicImg = findViewById(R.id.music_img);
        noMusicTips = findViewById(R.id.no_music_tips);
        musicLlContent = findViewById(R.id.music_ll_contenet);
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
        musicLlContent.setOnClickListener(mPresenter);
        musicPrev.setOnClickListener(mPresenter);
        musicPlayStop.setOnClickListener(mPresenter);
        musicNext.setOnClickListener(mPresenter);
        mDeepseekLl.setOnClickListener(mPresenter);
        mDeepseekBtn.setOnClickListener(mPresenter);
        mDeepSeekRecyAdapter = new DeepSeekRecyAdapter(this, mPresenter.getDeepseekSettingModels());
        mDeepSeekRecy.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mDeepSeekRecy.setAdapter(mDeepSeekRecyAdapter);
        mPresenter.initMedia();
        mPresenter.checkingAndroidVersion();
        initMap(savedInstanceState);
        registerBroadcast();
    }

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
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    public void refreshDeepseekData() {
        mPresenter.initData();
        mDeepSeekRecyAdapter.setDataList(mPresenter.getDeepseekSettingModels());
        mDeepSeekRecyAdapter.notifyDataSetChanged();
    }

    public void changeMusicPlayPauseBtn(boolean isPlaying) {
        if (isPlaying) {
            musicPlayStop.setImageResource(R.drawable.stop);
        } else {
            musicPlayStop.setImageResource(R.drawable.music_play);
        }
    }

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
        if (!isShow) {
            musicLlContent.setVisibility(GONE);
            noMusicTips.setVisibility(VISIBLE);
        } else {
            if (currentMediaModel != null) {
                if (currentMediaModel.equals(model)) {
                    return false;
                }
            }
            currentMediaModel = model;
            musicLlContent.setVisibility(VISIBLE);
            noMusicTips.setVisibility(GONE);
            if (!TextUtils.isEmpty(currentMediaModel.getTitle())) {
                musicTitle.setText(currentMediaModel.getTitle());
            }
            if (!TextUtils.isEmpty(currentMediaModel.getArtist())) {
                musicAuthor.setText(currentMediaModel.getArtist());
            }
            if (isPlaying) {
                musicPlayStop.setImageResource(R.drawable.stop);
            } else {
                musicPlayStop.setImageResource(R.drawable.music_play);
            }
            if (currentMediaModel.getAlbumArt() != null) {
//                Glide.with(this).load(currentMediaModel.getAlbumArt()).override(100,100).into(musicImg);
            }
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG, "onResume: ");
        mapView.onResume();
        mPresenter.initMedia();
        refreshDeepseekData();
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