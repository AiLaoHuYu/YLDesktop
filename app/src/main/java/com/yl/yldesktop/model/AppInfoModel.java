package com.yl.yldesktop.model;

import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class AppInfoModel{

    private Drawable drawable;
    private String labelName;
    private String pkgName;
    private int viewHolderType;

    public AppInfoModel(Drawable drawable, String labelName, String pkgName, int viewHolderType) {
        this.drawable = drawable;
        this.labelName = labelName;
        this.pkgName = pkgName;
        this.viewHolderType = viewHolderType;
    }

    protected AppInfoModel(Parcel in) {
        labelName = in.readString();
        pkgName = in.readString();
        viewHolderType = in.readInt();
    }

    public Drawable getDrawable() {
        return drawable;
    }

    public void setDrawable(Drawable drawable) {
        this.drawable = drawable;
    }

    public String getLabelName() {
        return labelName;
    }

    public void setLabelName(String labelName) {
        this.labelName = labelName;
    }

    public String getPkgName() {
        return pkgName;
    }

    public int getViewHolderType() {
        return viewHolderType;
    }

    public void setViewHolderType(int viewHolderType) {
        this.viewHolderType = viewHolderType;
    }

    public void setPkgName(String pkgName) {
        this.pkgName = pkgName;
    }

}
