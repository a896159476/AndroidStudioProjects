package com.example.administrator.myapplication;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import com.chad.library.adapter.base.BaseQuickAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class TestActivity extends BaseActivity {

    private List<Bluetooth> bluetoothDevice;
    private RecyclerView recyclerView;
    private RecyclerView recyclerView2;
    private Button button2;
    private Button button3;
    private BleBluetoothAdater bleBluetoothAdater;
    private BleBluetoothAdater2 bleBluetoothAdater2;
    private List<Bluetooth> bluetoothList;
    private List<BluetoothDevice> bluetoothDeviceList;
    private boolean isFirst = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_http);

        init();

    }

    public void init(){

        button2 = findViewById(R.id.button2);
        button3 = findViewById(R.id.button3);
        recyclerView =findViewById(R.id.recyclerView);
        recyclerView2 =findViewById(R.id.recyclerView2);

        //初始化
        BleBluetoothUtil.getInstance().init(getApplication());
        //开启蓝牙
        BleBluetoothUtil.getInstance().startBluetooth(this);

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Observable.create(new ObservableOnSubscribe<List<Bluetooth>>() {
                    @Override
                    public void subscribe(ObservableEmitter<List<Bluetooth>> emitter) throws Exception {
                        //搜索蓝牙
                        BleBluetoothUtil.getInstance().searchBluetooth(TestActivity.this,emitter);
                        Thread.sleep(10000);
                        //停止搜索
                        BleBluetoothUtil.getInstance().stopScan();
                    }
                }).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<List<Bluetooth>>() {
                            private Disposable disposable;
                            @Override
                            public void onSubscribe(Disposable d) {
                                disposable = d;
                            }

                            @Override
                            public void onNext(List<Bluetooth> results) {
                                if (isFirst){
                                    recyclerView.setLayoutManager(new LinearLayoutManager(TestActivity.this));
                                    bluetoothList = new ArrayList<>(results);
                                    bleBluetoothAdater = new BleBluetoothAdater(R.layout.ble_bluetooth,bluetoothList);
                                    recyclerView.setAdapter(bleBluetoothAdater);

                                    recyclerView2.setLayoutManager(new LinearLayoutManager(TestActivity.this));
                                    bluetoothDeviceList = BleBluetoothUtil.getInstance().getBluetoothDevice();
                                    bleBluetoothAdater2 = new BleBluetoothAdater2(R.layout.ble_bluetooth2,bluetoothDeviceList);
                                    recyclerView2.setAdapter(bleBluetoothAdater2);

                                    isFirst = false;
                                }else {
                                    bluetoothList = new ArrayList<>(results);
                                    bluetoothDeviceList = BleBluetoothUtil.getInstance().getBluetoothDevice();
                                    bleBluetoothAdater.notifyDataSetChanged();
                                    bleBluetoothAdater2.notifyDataSetChanged();
                                }
                                event();
                                disposable.dispose();
                            }

                            @Override
                            public void onError(Throwable e) {

                            }

                            @Override
                            public void onComplete() {

                            }
                        });

            }
        });
    }

    @Override
    public void event() {
        bleBluetoothAdater.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                BleBluetoothUtil.getInstance().connect(bluetoothList,position);
            }
        });

        bleBluetoothAdater2.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                BleBluetoothUtil.getInstance().connectBluetoothDevice(bluetoothDeviceList,position);
            }
        });

        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    BleBluetoothUtil.getInstance().disconnect();
                }
            }
        });
    }



}
