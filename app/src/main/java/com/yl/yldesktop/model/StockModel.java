package com.yl.yldesktop.model;

public class StockModel {

    private int stockImgId;
    private String stockName;
    private String stockUpsPercent;
    private boolean stockUpsAndDowns;
    private String stockUpsNum;


    public StockModel() {
    }

    public StockModel(int stockImgId, String stockName, String stockUpsPercent, boolean stockUpsAndDowns, String stockUpsNum) {
        this.stockImgId = stockImgId;
        this.stockName = stockName;
        this.stockUpsPercent = stockUpsPercent;
        this.stockUpsAndDowns = stockUpsAndDowns;
        this.stockUpsNum = stockUpsNum;
    }

    public int getStockImgId() {
        return stockImgId;
    }

    public void setStockImgId(int stockImgId) {
        this.stockImgId = stockImgId;
    }

    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public String getStockUpsPercent() {
        return stockUpsPercent;
    }

    public void setStockUpsPercent(String stockUpsPercent) {
        this.stockUpsPercent = stockUpsPercent;
    }

    public boolean isStockUpsAndDowns() {
        return stockUpsAndDowns;
    }

    public void setStockUpsAndDowns(boolean stockUpsAndDowns) {
        this.stockUpsAndDowns = stockUpsAndDowns;
    }

    public String getStockUpsNum() {
        return stockUpsNum;
    }

    public void setStockUpsNum(String stockUpsNum) {
        this.stockUpsNum = stockUpsNum;
    }
}
