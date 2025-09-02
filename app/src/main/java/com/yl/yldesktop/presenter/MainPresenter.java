package com.yl.yldesktop.presenter;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.NonNull;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.model.LatLng;
import com.amap.api.navi.enums.PathPlanningStrategy;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkRouteResult;
import com.amap.api.services.weather.LocalWeatherForecastResult;
import com.amap.api.services.weather.LocalWeatherLive;
import com.amap.api.services.weather.LocalWeatherLiveResult;
import com.amap.api.services.weather.WeatherSearch;
import com.amap.api.services.weather.WeatherSearchQuery;
import com.yl.basemvp.BasePresenter;
import com.yl.basemvp.SystemPropertiesReflection;
import com.yl.yldesktop.R;
import com.yl.yldesktop.activity.MainActivity;
import com.yl.yldesktop.model.AppInfoModel;
import com.yl.yldesktop.model.DeepseekSettingModel;
import com.yl.yldesktop.model.MediaModel;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class MainPresenter extends BasePresenter<MainActivity> implements AMap.OnMapClickListener, AMapLocationListener, View.OnClickListener, LocationSource, RouteSearch.OnRouteSearchListener, WeatherSearch.OnWeatherSearchListener {

    private List<PackageInfo> packageInfos;
    private List<AppInfoModel> appInfoModels = new ArrayList<>();
    private MyHandler myHandler;


    private List<DeepseekSettingModel> deepseekSettingModels;
    //请求权限码
    private static final int REQUEST_PERMISSIONS = 9527;
    private AMapLocationClient mlocationClient;
    private AMapLocationClientOption mLocationOption;
    private LocationSource.OnLocationChangedListener mListener;
    private boolean isPlaying = false;
    private RouteSearch routeSearch;
    private LatLonPoint mStartLatLonPoint;
    private String mCurrentCity = "null";
    private WeatherSearchQuery mWeatherQuery;
    private WeatherSearch mWeatherSearch;
    private LocalWeatherLive weatherlive;

    public MainPresenter(Activity activity) {
        attach((MainActivity) activity);
        myHandler = new MyHandler(mActivity.get());
        initData();
        setMaxVolume();
    }

    private void setMaxVolume() {
        AudioManager audioManager = (AudioManager) mActivity.get().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            // 恢复默认音量（50%）
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_RING, maxVolume, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, maxVolume, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, maxVolume, 0);
        }
    }

    private void initWeatherSearch() {
        try {
            mWeatherQuery = new WeatherSearchQuery(mCurrentCity, WeatherSearchQuery.WEATHER_TYPE_LIVE);
            mWeatherSearch = new WeatherSearch(mActivity.get());
            mWeatherSearch.setOnWeatherSearchListener(this);
            mWeatherSearch.setQuery(mWeatherQuery);
            mWeatherSearch.searchWeatherAsyn(); //异步搜索
        } catch (AMapException e) {
            throw new RuntimeException(e);
        }

    }

    public void initData() {
        if (deepseekSettingModels == null) {
            deepseekSettingModels = new ArrayList<>();
        } else {
            deepseekSettingModels.clear();
        }
        String deepseekVoiceSpeed = SystemPropertiesReflection.get("persist.sys.deepseek_voice_speed", "50");
        String deepseekVoicespeaker = SystemPropertiesReflection.get("persist.sys.deepseek_voice_speaker", "小美");
        String deepseekFontSize = SystemPropertiesReflection.get("persist.sys.deepseek_font_size", "中等");
        String deepseekFontColor = SystemPropertiesReflection.get("persist.sys.deepseek_font_color", "黑色");
        Log.e("TAG", "initData: " + deepseekVoiceSpeed + ":: " + deepseekVoicespeaker + ":: " + deepseekFontSize + ":: " + deepseekFontColor);
        deepseekSettingModels.add(new DeepseekSettingModel("语速", R.drawable.voice_speed, deepseekVoiceSpeed));
        deepseekSettingModels.add(new DeepseekSettingModel("发音人", R.drawable.speaker, deepseekVoicespeaker));
        deepseekSettingModels.add(new DeepseekSettingModel("字体大小", R.drawable.font_size, deepseekFontSize));
        deepseekSettingModels.add(new DeepseekSettingModel("字体颜色", R.drawable.font_color, deepseekFontColor));
        try {
            routeSearch = new RouteSearch(mActivity.get());
            routeSearch.setRouteSearchListener(this);
        } catch (AMapException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onItemClick(View view) {
    }

    public void scanAllApp() {
        if (myHandler == null) {
            myHandler = new MyHandler(mActivity.get());
        }
        myHandler.postDelayed(scanRunnable, 100);
    }

    private Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            PackageManager packageManager = mActivity.get().getPackageManager();
            packageInfos = packageManager.getInstalledPackages(0);
            for (PackageInfo packageInfo : packageInfos) {
                //得到手机上已经安装的应用的名字,即在AndriodMainfest.xml中的app_name。
                String appName = packageInfo.applicationInfo.loadLabel(mActivity.get().getPackageManager()).toString();
                //得到手机上已经安装的应用的图标,即在AndriodMainfest.xml中的icon。
                Drawable drawable = packageInfo.applicationInfo.loadIcon(mActivity.get().getPackageManager());
                //得到应用所在包的名字,即在AndriodMainfest.xml中的package的值。
                String packageName = packageInfo.packageName;
//                Log.e("packageInfo", "appName: " + appName + ":: packageName: " + packageName);
                if (packageName.equals("com.autonavi.amapauto") || packageName.equals("cn.kuwo.kwmusiccar")
                        || packageName.equals("com.android.settings") || packageName.equals("com.kugou.android.auto")) {
                    appInfoModels.add(new AppInfoModel(drawable, appName, packageName, 0));
                }
            }
        }
    };

    public List<PackageInfo> getPackageInfos() {
        return packageInfos;
    }

    class MyHandler extends Handler {

        private WeakReference<Activity> weakReference;

        public MyHandler(Activity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
        }
    }

    public void startRouteSearch(LatLonPoint mEndLatLonPoint) {
        RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(mStartLatLonPoint, mEndLatLonPoint);
        RouteSearch.DriveRouteQuery query = new RouteSearch.DriveRouteQuery(fromAndTo, PathPlanningStrategy.DRIVING_DEFAULT, null, null, "");
        routeSearch.calculateDriveRouteAsyn(query);
    }

    public List<DeepseekSettingModel> getDeepseekSettingModels() {
        return deepseekSettingModels;
    }

    public void openAmap() {
        // 检查高德地图是否安装
        PackageManager packageManager = mActivity.get().getPackageManager();
        try {
            packageManager.getPackageInfo("com.autonavi.amapauto", 0);

            // 构建高德地图的URI
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.autonavi.amapauto", "com.autonavi.amapauto.MainMapActivity"));
            // 启动高德地图应用
            mActivity.get().startActivity(intent);
        } catch (PackageManager.NameNotFoundException e) {
            // 未安装高德地图，提示用户安装
            e.printStackTrace();
        }
    }

    public void openKuwo() {
        // 检查酷我音乐是否安装
        PackageManager packageManager = mActivity.get().getPackageManager();
        try {
            packageManager.getPackageInfo("cn.kuwo.kwmusiccar", 0);

            // 构建酷我音乐的URI
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("cn.kuwo.kwmusiccar", "cn.kuwo.kwmusiccar.ui.MainActivity"));
            // 启动酷我音乐应用
            mActivity.get().startActivity(intent);
        } catch (PackageManager.NameNotFoundException e) {
            // 未安装酷我音乐，提示用户安装
            e.printStackTrace();
        }
    }

    public static void inputKeyEvent(int key) {
        try {
            String keyCommand = "input keyevent = " + key;
            Runtime runtime = Runtime.getRuntime();
            runtime.exec(keyCommand);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 检查Android版本
     */
    public void checkingAndroidVersion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Android6.0及以上先获取权限再定位
            requestPermission();
        } else {
            //Android6.0以下直接定位
//            weakReference.get().startLocation();
        }
    }

    /**
     * 动态请求权限
     */
    @AfterPermissionGranted(REQUEST_PERMISSIONS)
    private void requestPermission() {
        String[] permissions = {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.MEDIA_CONTENT_CONTROL,
                Manifest.permission.PACKAGE_USAGE_STATS
        };

        if (EasyPermissions.hasPermissions(mActivity.get(), permissions)) {
            //true 有权限 开始定位
//            weakReference.get().startLocation();
        } else {
            //false 无权限
            EasyPermissions.requestPermissions(mActivity.get(), "需要权限", REQUEST_PERMISSIONS, permissions);
        }
    }

    public void initMedia() {
        // 获取媒体会话管理器
        MediaSessionManager mediaSessionManager = (MediaSessionManager) mActivity.get().getSystemService(Context.MEDIA_SESSION_SERVICE);
        // 获取所有活跃的媒体会话
        List<MediaController> controllers = mediaSessionManager.getActiveSessions(null);
        String title = "";
        String artist = "";
        Bitmap albumArt = null;
        isPlaying = false;
        if (controllers.size() <= 0) {
            mActivity.get().changeMusicUi(false, isPlaying, null);
            return;
        }
        // 遍历找到音乐应用的控制器
        MediaMetadata metadata = controllers.get(0).getMetadata();
        if (metadata != null) {
            title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
            artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
            albumArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
            isPlaying = (controllers.get(0).getPlaybackState().getState()) == PlaybackState.STATE_PLAYING;
            // 显示音乐信息
//            Log.e("TAG", "title: " + title + ":: artist: " + artist + ":: isPlaying: " + isPlaying);
        }
        boolean isNeedGo = mActivity.get().changeMusicUi(true, isPlaying, new MediaModel(title, artist, albumArt));
        if (isNeedGo) {
            myHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    initMedia();
                }
            }, 100);
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        openAmap();
    }

    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (mListener != null && amapLocation != null) {
            if (amapLocation.getErrorCode() == 0) {
                Log.e("TAG", "onLocationChanged: " + amapLocation.getLatitude() + ":: " + amapLocation.getLongitude() + "::" + amapLocation.getCity());
                mStartLatLonPoint = new LatLonPoint(amapLocation.getLatitude(), amapLocation.getLongitude());
                if (!amapLocation.getCity().equals(mCurrentCity)) {
                    mCurrentCity = amapLocation.getCity();
                    initWeatherSearch();
                }
                mActivity.get().moveToCenter(amapLocation.getLatitude(), amapLocation.getLongitude());
                mListener.onLocationChanged(amapLocation);// 显示系统小蓝点
            } else {
                String errText = "定位失败," + amapLocation.getErrorCode() + ": " + amapLocation.getErrorInfo();
                Log.e("AmapErr", errText);
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.deepseek_ll || v.getId() == R.id.deepseek_btn) {
            try {
                Intent intent = new Intent("com.yl.deepseek.start");
                intent.setPackage("com.yl.deepseekxunfei");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                mActivity.get().startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (v.getId() == R.id.music_prev) {
            inputKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            myHandler.postDelayed(this::initMedia, 400);
        } else if (v.getId() == R.id.music_play_stop) {
            inputKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
            isPlaying = !isPlaying;
            mActivity.get().changeMusicPlayPauseBtn(isPlaying);
        } else if (v.getId() == R.id.music_next) {
            inputKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT);
            myHandler.postDelayed(this::initMedia, 400);
        } else if (v.getId() == R.id.empty_view) {
            openAmap();
        } else if (v.getId() == R.id.music_ll_contenet) {
            openKuwo();
        }
    }

    @Override
    public void activate(LocationSource.OnLocationChangedListener listener) {
        mListener = listener;
        if (mlocationClient == null) {
            //初始化定位
            try {
                mlocationClient = new AMapLocationClient(mActivity.get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            //初始化定位参数
            mLocationOption = new AMapLocationClientOption();
            //设置定位回调监听
            mlocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            mLocationOption.setNeedAddress(true);
            //设置定位参数
            mlocationClient.setLocationOption(mLocationOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            mlocationClient.startLocation();//启动定位
        }
    }

    @Override
    public void deactivate() {
        mListener = null;
        if (mlocationClient != null) {
            mlocationClient.stopLocation();
            mlocationClient.onDestroy();
        }
        mlocationClient = null;
    }

    @Override
    public void onBusRouteSearched(BusRouteResult busRouteResult, int i) {

    }

    @Override
    public void onDriveRouteSearched(DriveRouteResult driveRouteResult, int i) {
        if (i == 1000) {
            DrivePath drivePath = driveRouteResult.getPaths().get(0);
            mActivity.get().setDrivingRoute(drivePath, driveRouteResult.getStartPos(), driveRouteResult.getTargetPos());
        } else {
            Log.e("TAG", "onDriveRouteSearched: 路线规划失败");
        }
    }

    @Override
    public void onWalkRouteSearched(WalkRouteResult walkRouteResult, int i) {

    }

    @Override
    public void onRideRouteSearched(RideRouteResult rideRouteResult, int i) {

    }

    @Override
    public void onWeatherLiveSearched(LocalWeatherLiveResult weatherLiveResult, int rCode) {
        if (rCode == 1000) {
            if (weatherLiveResult != null && weatherLiveResult.getLiveResult() != null) {
                weatherlive = weatherLiveResult.getLiveResult();
                mActivity.get().changeWeatherUi(weatherlive);
            } else {
                mActivity.get().changeWeatherUi(null);
                Log.e("TAG", "onWeatherLiveSearched: no result");
            }
        } else {
            Log.e("TAG", "onWeatherLiveSearched error: " + rCode);
        }
    }

    @Override
    public void onWeatherForecastSearched(LocalWeatherForecastResult localWeatherForecastResult, int i) {

    }

    @Override
    public void detach() {
        super.detach();
        myHandler.removeCallbacksAndMessages(null);
    }

}
