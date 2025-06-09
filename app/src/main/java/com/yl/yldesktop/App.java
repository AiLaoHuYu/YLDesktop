package com.yl.yldesktop;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;

import com.amap.api.location.AMapLocationClient;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AMapLocationClient.setApiKey("e97204b0edc6ffe6a89023f0d6296192");
        AMapLocationClient.updatePrivacyAgree(this, true);
        AMapLocationClient.updatePrivacyShow(this, true, true);
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.yl.ylleftbar", "com.yl.ylleftbar.service.LeftBarService"));
        startService(intent);
    }
}
