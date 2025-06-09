package com.yl.yldesktop.adapter;

import android.content.ComponentName;
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

import com.yl.basemvp.BaseRecyclerViewAdapter;
import com.yl.yldesktop.R;
import com.yl.yldesktop.model.DeepseekSettingModel;
import com.yl.yldesktop.utils.AnimationUtil;

import java.util.List;

public class DeepSeekRecyAdapter extends BaseRecyclerViewAdapter<DeepSeekRecyAdapter.DeepSeekViewHolder, DeepseekSettingModel> {

    public DeepSeekRecyAdapter(Context mContext, List<DeepseekSettingModel> dataList) {
        super(mContext, dataList);
    }

    @Override
    protected DeepSeekViewHolder baseCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.deepseek_recy_item, parent, false);
        return new DeepSeekViewHolder(view);
    }

    @Override
    protected int baseGetItemViewType(int position) {
        return 0;
    }

    @Override
    protected void baseItemClick(View v, int position) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.yl.yldeepseeksettings", "com.yl.yldeepseeksettings.MainActivity"));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        mContext.startActivity(intent);
    }

    @Override
    protected boolean baseOnTouch(DeepSeekViewHolder holder, MotionEvent event, int position) {
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
    protected void bindView(DeepSeekViewHolder holder, int position) {
        holder.title.setText(dataList.get(position).getTitle());
        holder.img.setImageResource(dataList.get(position).getImgId());
        holder.value.setText(dataList.get(position).getValue());
    }

    class DeepSeekViewHolder extends RecyclerView.ViewHolder {

        private TextView title;
        private ImageView img;
        private TextView value;

        public DeepSeekViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.deepseek_recy_item_left_text);
            img = itemView.findViewById(R.id.deepseek_recy_item_img);
            value = itemView.findViewById(R.id.deepseek_recy_item_right_text);
        }
    }

}
