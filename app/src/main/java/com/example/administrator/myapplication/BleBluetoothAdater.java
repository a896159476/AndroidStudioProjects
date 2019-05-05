package com.example.administrator.myapplication;

import android.support.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

public class BleBluetoothAdater extends BaseQuickAdapter<Bluetooth, BaseViewHolder> {

    public BleBluetoothAdater(int layoutResId, @Nullable List<Bluetooth> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, Bluetooth item) {
        if (item.getBleDevice().getName() == null){
            helper.setText(R.id.name,item.getBleDevice().getAddress())
                    .setText(R.id.signal,item.getRssi()+"")
                    .addOnClickListener(R.id.connection);
        }else {
            helper.setText(R.id.name,item.getBleDevice().getName())
                    .setText(R.id.signal,item.getRssi()+"")
                    .addOnClickListener(R.id.connection);
        }
    }
}
