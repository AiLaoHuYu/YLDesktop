package com.yl.yldesktop.adapter;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.yl.basemvp.BaseRecyclerViewAdapter;
import com.yl.basemvp.ScreenUtil;
import com.yl.yldesktop.R;
import com.yl.yldesktop.model.AppInfoModel;
import com.yl.yldesktop.utils.AnimationUtil;

import java.util.List;

public class AppRecyAdapter extends BaseRecyclerViewAdapter<RecyclerView.ViewHolder, AppInfoModel> {

    public AppRecyAdapter(Context mContext, List<AppInfoModel> dataList) {
        super(mContext, dataList);
    }

    @Override
    protected RecyclerView.ViewHolder baseCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == 0) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.main_left_recy_item, parent, false);
            return new MainLeftViewHolder(view);
        } else {
            View view = LayoutInflater.from(mContext).inflate(R.layout.all_app_recy_item, parent, false);
            return new AllAppViewHolder(view);
        }
    }

    @Override
    protected int baseGetItemViewType(int position) {
        return dataList.get(position).getViewHolderType();
    }

    @Override
    protected void baseItemClick(View v, int position) {
        try {
            Intent intentForPackage = mContext.getPackageManager().getLaunchIntentForPackage(dataList.get(position).getPkgName());
            intentForPackage.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            mContext.startActivity(intentForPackage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected boolean baseOnTouch(RecyclerView.ViewHolder holder, MotionEvent event, int position) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                AnimationUtil.startScaleUpAnimation(holder.itemView);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                AnimationUtil.startScaleDownAnimation(holder.itemView);
                break;
        }
        return false;
    }

    @Override
    protected void bindView(RecyclerView.ViewHolder holder, int position) {
        RequestOptions options = new RequestOptions().transform(new RoundedCorners(ScreenUtil.dp2px(mContext, 10)));
        if (holder.getItemViewType() == 0) {
            Glide.with(mContext).load(dataList.get(position).getDrawable()).apply(options).into(((MainLeftViewHolder) holder).imageView);
        } else if (holder.getItemViewType() == 1) {
            Glide.with(mContext).load(dataList.get(position).getDrawable()).apply(options).into(((AllAppViewHolder) holder).imageView);
            ((AllAppViewHolder) holder).textView.setText(dataList.get(position).getLabelName());
        }

    }

    class MainLeftViewHolder extends RecyclerView.ViewHolder {

        private ImageView imageView;

        public MainLeftViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.main_left_item_img);
        }
    }

    class AllAppViewHolder extends RecyclerView.ViewHolder {

        private ImageView imageView;
        private TextView textView;

        public AllAppViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.all_app_item_img);
            textView = itemView.findViewById(R.id.all_app_item_text);
        }
    }


}
