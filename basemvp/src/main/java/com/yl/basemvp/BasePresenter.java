package com.yl.basemvp;

import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import java.lang.ref.WeakReference;

public abstract class BasePresenter<T extends AppCompatActivity> implements View.OnClickListener {

    protected WeakReference<T> mActivity;

    public void attach(T activity) {
        mActivity = new WeakReference<>(activity);
    }

    public void detach() {
        if (mActivity != null) {
            mActivity.clear();
        }
    }


    @Override
    public void onClick(View v) {
        onItemClick(v);
    }

    protected abstract void onItemClick(View view);

}
