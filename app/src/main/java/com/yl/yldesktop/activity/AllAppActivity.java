package com.yl.yldesktop.activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yl.yldesktop.R;
import com.yl.yldesktop.adapter.AppRecyAdapter;
import com.yl.yldesktop.model.AppInfoModel;

import java.util.ArrayList;
import java.util.List;

public class AllAppActivity extends AppCompatActivity {

    private RecyclerView allAppRecycler;
    private AppRecyAdapter recyAdapter;
    private List<AppInfoModel> appInfoModels;
    private List<PackageInfo> packageInfos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_all_app);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initView();
    }

    private void initView() {
        allAppRecycler = findViewById(R.id.all_app_recy);
        allAppRecycler.setLayoutManager(new GridLayoutManager(this, 4));
        injectAppInfoList();
        if (appInfoModels == null) {
            recyAdapter = new AppRecyAdapter(this, new ArrayList<>());
        } else {
            recyAdapter = new AppRecyAdapter(this, appInfoModels);
        }
        allAppRecycler.setAdapter(recyAdapter);
    }

    public void scanAllAndCompare() {
        PackageManager packageManager = getPackageManager();
        List<PackageInfo> installedPackages = packageManager.getInstalledPackages(0);
        if (installedPackages.size() != packageInfos.size()) {
            packageInfos = installedPackages;
            injectAppInfoList();
        }
    }

    public void injectAppInfoList() {
        new Handler().postDelayed(runnable, 100);
    }


    String[] excludePkg = {"org.chromium.webview_shell", "com.mxnavi.travel.assistant", "com.mapgoo.diruite", "com.cneeds.settings"
            , "com.baidu.carlife", "com.zzx.factorytest", "com.yl.yldesktop", "com.cneeds.tsp", "com.aispeech.lyra.daemon"};

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (appInfoModels == null) {
                appInfoModels = new ArrayList<>();
            } else {
                appInfoModels.clear();
            }
            PackageManager packageManager = getPackageManager();
            packageInfos = packageManager.getInstalledPackages(0);
            for (PackageInfo packageInfo : packageInfos) {
//                Log.e("TAG123", "appName: " + packageInfo.applicationInfo.loadLabel(getPackageManager()).toString() + ":: packageName: " + packageInfo.packageName);
                if (packageInfo.packageName.equals("com.yl.deepseekxunfei") || packageInfo.packageName.equals("com.android.settings")
                        || packageInfo.packageName.equals("cn.kuwo.kwmusiccar")) {
                    //得到手机上已经安装的应用的名字,即在AndriodMainfest.xml中的app_name。
                    String appName = packageInfo.applicationInfo.loadLabel(getPackageManager()).toString();
                    //得到手机上已经安装的应用的图标,即在AndriodMainfest.xml中的icon。
                    Drawable drawable = packageInfo.applicationInfo.loadIcon(getPackageManager());
                    //得到应用所在包的名字,即在AndriodMainfest.xml中的package的值。
                    String packageName = packageInfo.packageName;
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
                    if (launchIntent != null) {
                        appInfoModels.add(new AppInfoModel(drawable, appName, packageName, 1));
                    }
                    continue;
                }
//                if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
//                    continue;
//                }
                if (!packageInfo.packageName.startsWith("com.android") && !packageInfo.packageName.contains("debug")
                        && !packageInfo.packageName.startsWith("com.mediatek") && !contains(excludePkg, packageInfo.packageName)) {
                    //得到手机上已经安装的应用的名字,即在AndriodMainfest.xml中的app_name。
                    String appName = packageInfo.applicationInfo.loadLabel(getPackageManager()).toString();
                    //得到手机上已经安装的应用的图标,即在AndriodMainfest.xml中的icon。
                    Drawable drawable = packageInfo.applicationInfo.loadIcon(getPackageManager());
                    //得到应用所在包的名字,即在AndriodMainfest.xml中的package的值。
                    String packageName = packageInfo.packageName;
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
                    if (launchIntent != null) {
                        appInfoModels.add(new AppInfoModel(drawable, appName, packageName, 1));
                    }
                }
            }
            setDataList(appInfoModels);
        }
    };

    public boolean contains(String[] array, String target) {
        for (String str : array) {
            if (str.equals(target)) {
                return true;
            }
        }
        return false;
    }

    private void setDataList(List<AppInfoModel> appInfoModels) {
        recyAdapter.setDataList(appInfoModels);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (packageInfos != null && packageInfos.size() > 0) {
            scanAllAndCompare();
        }
    }
}