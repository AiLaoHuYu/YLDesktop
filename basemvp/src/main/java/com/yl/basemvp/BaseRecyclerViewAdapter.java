package com.yl.basemvp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public abstract class BaseRecyclerViewAdapter<T extends RecyclerView.ViewHolder, P extends Object> extends RecyclerView.Adapter<T> {

    protected Context mContext;
    protected List<P> dataList;

    public BaseRecyclerViewAdapter(Context mContext, List<P> dataList) {
        this.mContext = mContext;
        this.dataList = dataList;
    }

    @NonNull
    @Override
    public T onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return baseCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull T holder, int position) {
        bindView(holder, position);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                baseItemClick(v, holder.getPosition());
            }
        });
        holder.itemView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return baseOnTouch(holder, event, position);
            }
        });
    }

    public List<P> getDataList() {
        return dataList;
    }

    public void setDataList(List<P> dataList) {
        this.dataList = dataList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return baseGetItemViewType(position);
    }

    protected abstract T baseCreateViewHolder(ViewGroup parent, int viewType);

    protected abstract int baseGetItemViewType(int position);

    protected abstract void baseItemClick(View v, int position);
    protected abstract boolean baseOnTouch(T holder, MotionEvent event, int position);

    protected abstract void bindView(T holder, int position);
}
