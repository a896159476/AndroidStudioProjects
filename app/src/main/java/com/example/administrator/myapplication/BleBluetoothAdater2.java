package com.example.administrator.myapplication;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

public class BleBluetoothAdater2 extends BaseQuickAdapter<BluetoothDevice, BaseViewHolder> {

    public BleBluetoothAdater2(int layoutResId, @Nullable List<BluetoothDevice> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, BluetoothDevice item) {
        if (item.getName() == null){
            helper.setText(R.id.name,item.getAddress());
        }else {
            helper.setText(R.id.name,item.getName());
        }
    }
}
