package com.example.administrator.myapplication;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.example.administrator.myapplication.util.BluetoothUtil;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class TestActivity2 extends BaseActivity {

    private RecyclerView recyclerView;
    private Button button2;
    private Button button3;

    private Disposable disposable;
    private BluetoothAdater bluetoothAdater;
    private List<BluetoothDevice> devices;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_http2);

        init();
        event();
    }

    public void init(){
        button2 = findViewById(R.id.button2);
        button3 = findViewById(R.id.button3);
        recyclerView =findViewById(R.id.recyclerView);

        //初始化
        BluetoothUtil.getInstance().init(getApplication());
        //开启蓝牙
        BluetoothUtil.getInstance().startBluetooth(this);
        //搜索蓝牙
        BluetoothUtil.getInstance().searchBluetooth(TestActivity2.this);
        //开启服务端
        BluetoothUtil.getInstance().bluetoothServerSocket();

        //显示
        recyclerView.setLayoutManager(new LinearLayoutManager(TestActivity2.this));
        devices = BluetoothUtil.getInstance().getDevices();
        bluetoothAdater = new BluetoothAdater(R.layout.ble_bluetooth2,devices);
        recyclerView.setAdapter(bluetoothAdater);
        //实时更新蓝牙列表
        Observable.create(new ObservableOnSubscribe<BluetoothDevice>() {
            @Override
            public void subscribe(ObservableEmitter<BluetoothDevice> emitter) throws Exception {
                BluetoothUtil.getInstance().setEmitter(emitter);
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<BluetoothDevice>() {

                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                    }

                    @Override
                    public void onNext(BluetoothDevice bluetoothDevice) {
                        devices.add(bluetoothDevice);
                        bluetoothAdater.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });



        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BluetoothUtil.getInstance().serverWrite("服务端给客服端");
            }
        });
    }

    @Override
    public void event() {
        bluetoothAdater.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, final int position) {
                BluetoothUtil.getInstance().connect(devices.get(position));
            }
        });

        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BluetoothUtil.getInstance().write("客户端发送给服务端");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BluetoothUtil.getInstance().onDestroy();
        disposable.dispose();
    }
}
