package com.yl.yldesktop.model;

public class DeepseekSettingModel {

    private String title;
    private int imgId;
    private String value;

    public DeepseekSettingModel(String title, int imgId, String value) {
        this.title = title;
        this.imgId = imgId;
        this.value = value;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getImgId() {
        return imgId;
    }

    public void setImgId(int imgId) {
        this.imgId = imgId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
