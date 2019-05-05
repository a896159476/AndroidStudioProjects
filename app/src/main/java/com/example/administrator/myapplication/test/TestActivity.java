package com.example.administrator.myapplication.test;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.example.administrator.myapplication.BaseActivity;
import com.example.administrator.myapplication.R;
import com.example.administrator.myapplication.util.BitmapUtil;
import com.example.administrator.myapplication.util.UdpUtil;
import com.example.administrator.myapplication.util.WifiUtil;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class TestActivity extends BaseActivity {

    private List<ScanResult> scanResultList;
    private TestAdapter testAdapter;
    private Button button;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        init();

    }

    @Override
    public void init() {

        Observable.create(new ObservableOnSubscribe<List<ScanResult>>() {
            @Override
            public void subscribe(ObservableEmitter<List<ScanResult>> emitter) throws Exception {
                WifiUtil.getInstance().setEmitter(emitter);
                WifiUtil.getInstance().init(getApplication());
                WifiUtil.getInstance().search();

                UdpUtil.getInstance().init(getApplication());
//                UdpUtil.getInstance().socket();
                UdpUtil.getInstance().serverSocket();
            }
        }).subscribe(new Observer<List<ScanResult>>() {
                    private Disposable disposable;
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                    }

                    @Override
                    public void onNext(List<ScanResult> scanResults) {
                        RecyclerView recyclerView = findViewById(R.id.recyclerView);
                        recyclerView.setLayoutManager(new LinearLayoutManager(TestActivity.this));
                        scanResultList = new ArrayList<>(scanResults);
                        testAdapter = new TestAdapter(R.layout.wifi,scanResultList);
                        recyclerView.setAdapter(testAdapter);
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


        button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                WifiUtil.getInstance().search();
            }
        });
    }

    @Override
    public void event() {
        testAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
//                WifiUtil.getInstance().connect(scanResultList.get(position),TestActivity.this);
//                WifiUtil.getInstance().remove(scanResultList.get(position));
                UdpUtil.getInstance().send();
            }
        });


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WifiUtil.getInstance().onDestroy();
    }
}
