package com.yl.yldesktop.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.yl.basemvp.BaseRecyclerViewAdapter;
import com.yl.yldesktop.R;
import com.yl.yldesktop.model.StockModel;

import java.util.List;

public class StockAdapter extends BaseRecyclerViewAdapter<StockAdapter.StockViewHolder, StockModel> {

    public StockAdapter(Context mContext, List<StockModel> dataList) {
        super(mContext, dataList);
    }

    @Override
    protected StockViewHolder baseCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.stock_recy_item, parent, false);
        return new StockViewHolder(view);
    }

    @Override
    protected int baseGetItemViewType(int position) {
        return 0;
    }

    @Override
    protected void baseItemClick(View v, int position) {
        //TODO 暂时不处理
    }

    @Override
    protected boolean baseOnTouch(StockViewHolder holder, MotionEvent event, int position) {
        return false;
    }

    @Override
    protected void bindView(StockViewHolder holder, int position) {
        holder.stockImage.setImageResource(dataList.get(position).getStockImgId());
        holder.stockUpsPercent.setText(dataList.get(position).getStockUpsPercent());
        holder.stockName.setText(dataList.get(position).getStockName());
        if (dataList.get(position).isStockUpsAndDowns()) {
            holder.stockUpsOrDowns.setImageResource(R.drawable.ups);
            holder.constraintLayout.setBackgroundResource(R.drawable.stock_ups_bg);
        } else {
            holder.stockUpsOrDowns.setImageResource(R.drawable.downs);
            holder.constraintLayout.setBackgroundResource(R.drawable.stock_downs_bg);
        }
        holder.stockUpsNum.setText(dataList.get(position).getStockUpsNum());
    }

    protected class StockViewHolder extends RecyclerView.ViewHolder {
        private ConstraintLayout constraintLayout;
        private ImageView stockImage;
        private TextView stockUpsPercent;
        private TextView stockName;
        private ImageView stockUpsOrDowns;
        private TextView stockUpsNum;

        public StockViewHolder(@NonNull View itemView) {
            super(itemView);
            constraintLayout = itemView.findViewById(R.id.stock_item_parent);
            stockImage = itemView.findViewById(R.id.stock_item_image);
            stockUpsPercent = itemView.findViewById(R.id.stock_item_ups_percent);
            stockName = itemView.findViewById(R.id.stock_item_name);
            stockUpsOrDowns = itemView.findViewById(R.id.stock_image_ups_or_downs);
            stockUpsNum = itemView.findViewById(R.id.stock_item_ups_num);
        }
    }


}
