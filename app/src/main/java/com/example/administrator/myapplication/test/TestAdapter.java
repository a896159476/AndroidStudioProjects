package com.example.administrator.myapplication.test;

import android.net.wifi.ScanResult;
import android.support.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.example.administrator.myapplication.Bluetooth;
import com.example.administrator.myapplication.R;

import java.util.List;

public class TestAdapter extends BaseQuickAdapter<ScanResult, BaseViewHolder> {

    public TestAdapter(int layoutResId, @Nullable List<ScanResult> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, ScanResult item) {
        helper.setText(R.id.wifiName,item.SSID)
                .setText(R.id.level,item.level+"");
    }
}
