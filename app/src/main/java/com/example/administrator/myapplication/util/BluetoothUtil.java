package com.example.administrator.myapplication.util;

import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.provider.Settings;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class BluetoothUtil {

    private static volatile BluetoothUtil instance;
    private BluetoothUtil() {}
    public static BluetoothUtil getInstance() {
        if (instance == null) {
            synchronized (BluetoothUtil.class) {
                if (instance == null) {
                    instance = new BluetoothUtil();
                }
            }
        }
        return instance;
    }

    private BluetoothAdapter mBluetoothAdapter;
    private Context context;
    //是否支持蓝牙
    private boolean isBluetooth = true;

    //只需初始化一次，必须在Application中调用。
    public void init(Application application){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null){
            isBluetooth = false;
        }
        context = application;
        application.registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
    }

    /**
     * 开启蓝牙
     * @param activity
     */
    public void startBluetooth(Activity activity){
        if (!isBluetooth){
            showToast("该设备不支持蓝牙！");
            return;
        }
        //未打开蓝牙
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivity(enableBtIntent);
        }else {
            //检测gps是否打开（定位权限已经申请，如果没有申请需要检测）
            if (!isOpen(activity)){
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                activity.startActivity(intent);
            }
        }
    }

    /**
     * 搜索蓝牙
     */
    public void searchBluetooth(Activity activity){
        if (!isBluetooth){
            showToast("该设备不支持蓝牙！");
            return;
        }
        //未打开蓝牙
        if (!mBluetoothAdapter.isEnabled()) {
            startBluetooth(activity);
            return;
        }
        //检测gps是否打开（定位权限已经申请，如果没有申请需要检测）
        if (!isOpen(activity)){
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            activity.startActivity(intent);
        }else {
            Set<BluetoothDevice> pairedDevices = getBondedDevices();
            if (pairedDevices != null) {
                devices = new ArrayList<>(pairedDevices);
            }
            //搜索蓝牙，通过广播返回搜索结果
            mBluetoothAdapter.startDiscovery();
        }
    }

    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //自定义

    private BluetoothSocket socket;
    /**
     * 连接蓝牙
     */
    public void connect(final BluetoothDevice devices){
        Observable.create((ObservableOnSubscribe<String>) emitter -> {
            boolean isFailure = false;
            try {
                if (mBluetoothAdapter.isDiscovering()){
                    mBluetoothAdapter.cancelDiscovery();
                }
                if (socket == null){
                    socket = devices.createRfcommSocketToServiceRecord(SPP_UUID);//UUID.randomUUID()
                    socket.connect();
                }
            } catch (IOException e) {
                e.printStackTrace();
                isFailure = true;
            }
            while (socket == null) {
                if (isFailure) {
                    emitter.onNext("连接失败！");
                    return;
                }
            }
            read();
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    private Disposable disposable;
                    @Override
                    public void onSubscribe(Disposable d) {
                        this.disposable = d;
                    }

                    @Override
                    public void onNext(String str) {
                        showToast(str);
                        disposable.dispose();
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                        disposable.dispose();
                    }
                });
    }


    private BluetoothSocket bluetoothSocket;
    /**
     * 服务器(蓝牙服务端1对1对应客户端)
     */
    public void bluetoothServerSocket(){
        Observable.create((ObservableOnSubscribe<String>) emitter -> {
            try {
                BluetoothServerSocket  bluetoothServerSocket = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord("coffee", SPP_UUID);
                while (true) {
                    bluetoothSocket = bluetoothServerSocket.accept();
                    if (bluetoothSocket != null){
                        //发送消息给客户端
                        serverWrite("连接成功！");
                        //开启一个新线程接收客户端的消息
                        serverRead();
                        //关闭流
                        bluetoothServerSocket.close();
                        emitter.onNext("收到客户端请求!连接成功！");
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    private Disposable disposable;
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                    }

                    @Override
                    public void onNext(String bluetoothDevice) {
                        showToast(bluetoothDevice);
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

    private InputStream bluetoothSocketIn;
    private Disposable bluetoothSocketInDisposable;
    private InputStream socketIn;
    private Disposable socketInDisposable;



    //服务端发送
    public void serverWrite(final String msg){
        Observable.create((ObservableOnSubscribe<String>) emitter -> {
            if (bluetoothSocket != null){
                OutputStream bluetoothSocketOut = bluetoothSocket.getOutputStream();
                bluetoothSocketOut.write(msg.getBytes());
                bluetoothSocketOut.close();
            }else {
                emitter.onNext("未连接客户端");
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    private Disposable disposable;
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                    }

                    @Override
                    public void onNext(String msg) {
                        showToast(msg);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                        disposable.dispose();
                    }
                });
    }

    //服务端接收
    public void serverRead(){
        Observable.create((ObservableOnSubscribe<String>) emitter -> {
            bluetoothSocketIn = bluetoothSocket.getInputStream();
            byte[] bytes = new byte[1024];
            while (true) {
                int len ;
                StringBuilder msg = new StringBuilder();
                while ((len = bluetoothSocketIn.read(bytes)) != -1){
                    String string = new String(bytes,0,len);
                    msg.append(string);
                }
//                    bluetoothSocketIn.read(bytes);
                emitter.onNext(msg.toString());
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        bluetoothSocketInDisposable = d;
                    }

                    @Override
                    public void onNext(String msg) {
                        showToast(msg);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }


    //客户端发送信息
    public void write(final String msg){
        Observable.create((ObservableOnSubscribe<String>) emitter -> {
            if (socket != null){
                OutputStream socketOut = socket.getOutputStream();
                socketOut.write(msg.getBytes());
                socketOut.close();
            }else {
                emitter.onNext("无法连接到服务器");
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    private Disposable disposable;
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                    }

                    @Override
                    public void onNext(String msg) {
                        showToast(msg);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                        disposable.dispose();
                    }
                });
    }

    /**
     * 发送文件
     * @param file 文件
     */
    public void writeFile(File file){
        Observable.create((ObservableOnSubscribe<String>) emitter -> {
            if (socket != null){
                OutputStream outputStream = socket.getOutputStream();
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] bytes = new byte[1024];
                while (fileInputStream.read(bytes) != -1){
                    outputStream.write(bytes);
                }
                outputStream.close();
                fileInputStream.close();
            }else {
                emitter.onNext("无法连接到服务器");
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    private Disposable disposable;
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                    }

                    @Override
                    public void onNext(String msg) {
                        showToast(msg);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                        disposable.dispose();
                    }
                });
    }

    //客户端接收
    public void read(){
        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                socketIn = socket.getInputStream();
                byte[] bytes = new byte[1024];
                while (true){
                    int len ;
                    StringBuilder msg = new StringBuilder();
                    while ((len = socketIn.read(bytes)) != -1){
                        String string = new String(bytes,0,len);
                        msg.append(string);
                    }
//                    socketIn.read(bytes);
                    emitter.onNext(msg.toString());
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        socketInDisposable = d;
                    }

                    @Override
                    public void onNext(String msg) {
                        showToast(msg);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private List<BluetoothDevice> devices;
    public List<BluetoothDevice>  getDevices(){
        return devices;
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {//获取未配对设备
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                emitter.onNext(device);
            }
        }
    };


    public void onDestroy(){
        context.unregisterReceiver(mReceiver);
        close();
    }

    private void close(){
        try {
            if (bluetoothSocket != null){
                bluetoothSocket.close();
            }
            if (socket != null){
                socket.close();
            }
            if (bluetoothSocketIn !=null){
                bluetoothSocketIn.close();
            }
            if (socketIn !=null){
                socketIn.close();
            }
            if (bluetoothSocketInDisposable != null){
                bluetoothSocketInDisposable.dispose();
            }
            if (socketInDisposable != null){
                socketInDisposable.dispose();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ObservableEmitter<BluetoothDevice> emitter;
    public void setEmitter(ObservableEmitter<BluetoothDevice> emitter){
        this.emitter = emitter;
    }

    /**
     * 判断GPS是否开启，GPS或者AGPS开启一个就认为是开启的
     * @param context 上下文
     * @return true 表示开启
     */
    private Boolean isOpen(Context context) {
        LocationManager locationManager
                = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        // 通过GPS卫星定位，定位级别可以精确到街（通过24颗卫星定位，在室外和空旷的地方定位准确、速度快）
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // 通过WLAN或移动网络(3G/2G)确定的位置（也称作AGPS，辅助GPS定位。主要用于在室内或遮盖物（建筑群或茂密的深林等）密集的地方定位）
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        return gps || network;

    }

    //查询已配对的设备集
    private Set<BluetoothDevice> getBondedDevices(){
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() >0){
            return pairedDevices;
        }
        return null;
    }

    private void showToast(String msg){
        Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
    }
}
