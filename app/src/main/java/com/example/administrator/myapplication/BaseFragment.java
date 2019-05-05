package com.example.administrator.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.Toast;

/*
 * Fragment基类
 * 添加抽象方法（init，event）；重写startActivity；简化Toast；获取宿主Activity
 */
public abstract class BaseFragment extends Fragment {


    protected BaseActivity mActivity;

    /**
     * 初始化
     */
    public abstract void init();

    /**
     * 点击事件
     */
    public abstract void event();

    /*
    * 获取宿主Activity
    */
    protected BaseActivity getHoldingActivity() {
        return mActivity;
    }
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mActivity = (BaseActivity) activity;
    }

    /**
     * 重写startActivity
     * @param clz 跳转页面的class
     */
    public void startActivity(Class<?> clz) {
        startActivity(new Intent(getHoldingActivity(),clz));
    }

    /**
     * 重写startActivity
     * @param clz 跳转页面的class
     * @param bundle 携带的数据
     */
    public void startActivity(Class<?> clz, Bundle bundle) {
        Intent intent = new Intent(getHoldingActivity(),clz);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        startActivity(intent);
    }

    /**
     * 简化Toast
     * @param msg 显示的信息
     */
    protected void showToast(String msg){
        Toast.makeText(getHoldingActivity(),msg,Toast.LENGTH_SHORT).show();
    }
}
