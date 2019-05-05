package com.example.administrator.myapplication.util;

import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.widget.EditText;
import android.widget.Toast;

import com.example.administrator.myapplication.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class WifiUtil {

    private static volatile WifiUtil instance;
    private WifiUtil() {}
    public static WifiUtil getInstance() {
        if (instance == null) {
            synchronized (WifiUtil.class) {
                if (instance == null) {
                    instance = new WifiUtil();
                }
            }
        }
        return instance;
    }
    private Context context;
    private WifiManager mWifiManager;

    /**
     * 初始化
     * @param application
     */
    public void init(Application application){
        context = application;
        mWifiManager = (WifiManager) application.getSystemService(Context.WIFI_SERVICE);

        IntentFilter intentFilter = new IntentFilter();
        //扫描wifi
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        //wifi状态
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        application.registerReceiver(receiver,intentFilter);

        if (!isOpen(application)){
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            application.startActivity(intent);
        }
        if (!mWifiManager.isWifiEnabled()){
            mWifiManager.setWifiEnabled(true);
        }
    }

    /**
     * 获取wifi连接信息
     * @return WifiInfo
     */
    public WifiInfo getWifiInfo(){
        if (mWifiManager != null) {
            return mWifiManager.getConnectionInfo();
        }
        return null;
    }

    /**
     * 获取wifi的ip
     * @return
     */
    public String getIp(){
        int ipAdd = getWifiInfo().getIpAddress();
        try {
            String ip = InetAddress.getByAddress(
                    ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipAdd).array())
                    .getHostAddress();
            return ip;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return "";
    }



    /**
     * 搜索wifi，必须使用onDestroy（）注销广播
     */
    public void search(){
        //如果用户拒绝打开wifi权限，setWifiEnabled(true)将不生效
        if (!mWifiManager.isWifiEnabled()){
            mWifiManager.setWifiEnabled(true);
        }

        if (!isOpen(context)){
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            context.startActivity(intent);
            return;
        }
        mWifiManager.startScan();
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                // wifi已成功扫描到可用wifi。
                List<ScanResult> scanResults = mWifiManager.getScanResults();
                emitter.onNext(scanResults);
            }
            if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)){
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                if (info.getState().equals(NetworkInfo.State.DISCONNECTED)) {
                    showToast(wifiInfo.getSSID()+"已经断开连接");
                } else if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
                    showToast("已连接到网络:" + wifiInfo.getSSID());
                } else {
                    NetworkInfo.DetailedState state = info.getDetailedState();
                    if (state == NetworkInfo.DetailedState.CONNECTING) {
                        showToast("连接中...");
                    } else if (state == NetworkInfo.DetailedState.AUTHENTICATING) {
                        showToast("正在验证身份信息...");
                    } else if (state == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
                        showToast("正在获取IP地址...");
                    } else if (state == NetworkInfo.DetailedState.FAILED) {
                        showToast("连接失败");
                    }
                }
            }
        }
    };

    private ObservableEmitter<List<ScanResult>> emitter;
    public void setEmitter(ObservableEmitter<List<ScanResult>> emitter){
        this.emitter = emitter;
    }

    public void onDestroy(){
        context.unregisterReceiver(receiver);
    }

    public void connect(ScanResult scanResult, Activity activity){
        if (mWifiManager == null){
            return;
        }
        if (!mWifiManager.isWifiEnabled()){
            mWifiManager.setWifiEnabled(true);
        }
        if (!isOpen(context)){
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            context.startActivity(intent);
            return;
        }
        while (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
            try {
                // 为了避免程序一直while循环，让它睡个100毫秒在检测……
                Thread.currentThread();
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }

        //获取已经配置过的wifi的networkId
        int networkId = getNetworkId(scanResult);
        //如果没有已经配置过的wifi
        if (networkId == -1){
            String capabilities = scanResult.capabilities;
            if (!TextUtils.isEmpty(capabilities)) {
                if (capabilities.contains("WPA") || capabilities.contains("wpa")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setTitle("提示");
                    builder.setMessage("请输入wifi密码！");

                    EditText editText = new EditText(activity);
                    builder.setView(editText);

                    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            WifiConfiguration wifiConfiguration = new WifiConfiguration();
                            wifiConfiguration.SSID = "\""+scanResult.SSID+"\"";//\"转义字符，代表"
                            wifiConfiguration.preSharedKey = "\""+editText.getText().toString()+"\"";//WPA-PSK密码
                            wifiConfiguration.hiddenSSID = false;
                            wifiConfiguration.status = WifiConfiguration.Status.ENABLED;

                            int networkId = mWifiManager.addNetwork(wifiConfiguration);
                            boolean  isConnect = mWifiManager.enableNetwork(networkId, true);
                        }
                    });
                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });

                    builder.setCancelable(false);

                    builder.show();
                }else {
                    WifiConfiguration wifiConfiguration = new WifiConfiguration();
                    wifiConfiguration.SSID = "\""+scanResult.SSID+"\"";//\"转义字符，代表"
                    wifiConfiguration.preSharedKey = "";//WPA-PSK密码
                    wifiConfiguration.hiddenSSID = false;
                    wifiConfiguration.status = WifiConfiguration.Status.ENABLED;

                    boolean  isConnect = mWifiManager.enableNetwork(mWifiManager.addNetwork(wifiConfiguration), true);
                }
            }
        }else {
            //连接wifi
            boolean  isConnect = mWifiManager.enableNetwork(networkId, true);
        }
    }

    /**
     * 取消保存wifi
     * @param scanResult
     */
    public void remove(ScanResult scanResult){
        int networkId =  getNetworkId(scanResult);
        if (networkId !=-1){
            mWifiManager.removeNetwork(networkId);
            mWifiManager.saveConfiguration();
        }
    }

    /**
     * 获取已经配置过的wifi的networkId
     * @param scanResult
     * @return 返回-1则没有配置
     */
    private int getNetworkId(ScanResult scanResult){
        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + scanResult.SSID + "\"")) {
                return existingConfig.networkId;
            }
        }
        return -1;
    }


    /**
     * 开启热点
     * @param name 热点名字
     * @param password 热点密码
     */
    public void createWifiHotspot(String name,String password){
        //因为wifi和热点不能同时打开，所以打开热点的时候需要关闭wifi
        if (mWifiManager.isWifiEnabled()){
            mWifiManager.setWifiEnabled(false);
        }
        if(Build.VERSION.SDK_INT >= 26){
            ((Activity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    setWifiApEnabledForAndroidO(context, enabled);
                }
            });
            return;
        }


        //7.0或7.0以下版本
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        //热点名字
        wifiConfiguration.SSID = name;
        //密码
        wifiConfiguration.preSharedKey = password;
        //设置该项会使一个网络不广播其SSID，因此这种特定的SSID只能用于浏览
        wifiConfiguration.hiddenSSID = true;
        //该配置支持的身份验证协议集合
        wifiConfiguration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        //该配置所支持的密钥管理集合
        wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        //该配置所支持的组密码集合
        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        //该配置所支持的WPA配对密码集合
        wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        //该配置所支持的组密码集合
        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        //获取当前网络的状态
        wifiConfiguration.status = WifiConfiguration.Status.ENABLED;

        //通过反射调用设置热点
        try {
            Method method = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
            //true为热点开启成功
            boolean enable = (Boolean) method.invoke(mWifiManager, wifiConfiguration, true);
            if (enable) {
                showToast("热点开启成功！");
            } else {
                showToast("热点开启失败！");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToast("热点开启失败！");
        }

    }

    /**
     * 关闭热点
     */
    public void closeWifiHotspot(){
        try {
            Method method = mWifiManager.getClass().getMethod("getWifiApConfiguration");
            method.setAccessible(true);
            WifiConfiguration config = (WifiConfiguration) method.invoke(mWifiManager);
            Method method2 = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method2.invoke(mWifiManager, config, false);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }












    private Disposable disposable;
    private Socket clientSocket;
    private PrintStream clientSocketOut;
    private BufferedReader clientSocketIn;

    /**
     * 开启服务端并等待接收信息
     * @param port 端口
     */
    private void serverSocket(final int port){
        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                try {
                    ServerSocket serverSocket = new ServerSocket(port);
                    clientSocket = serverSocket.accept();
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String remoteIP = clientSocket.getInetAddress().getHostAddress();
                int remotePort = clientSocket.getLocalPort();

                clientSocketIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                clientSocketOut = new PrintStream(clientSocket.getOutputStream(), true, "utf-8");

                while (true){
                    String result=clientSocketIn.readLine();
                    emitter.onNext(result);
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {

                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                    }

                    @Override
                    public void onNext(String result) {
                        showToast(result);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }

    /**
     * 服务端发送的信息
     * @param msg
     */
    private void println(String msg){
        clientSocketOut.println(msg);
    }

    private void showToast(String msg){
        Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
    }




    private void close(){
        try {
            if (clientSocket != null){
                clientSocket.close();
            }
            if (clientSocketIn != null){
                clientSocketIn.close();
            }
            if (clientSocketOut != null){
                clientSocketOut.close();
            }
            if (disposable != null){
                disposable.dispose();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断GPS是否开启，GPS或者AGPS开启一个就认为是开启的
     * @param context
     * @return true 表示开启
     */
    public  boolean isOpen(Context context) {
        LocationManager locationManager
                = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        // 通过GPS卫星定位，定位级别可以精确到街（通过24颗卫星定位，在室外和空旷的地方定位准确、速度快）
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // 通过WLAN或移动网络(3G/2G)确定的位置（也称作AGPS，辅助GPS定位。主要用于在室内或遮盖物（建筑群或茂密的深林等）密集的地方定位）
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (gps || network) {
            return true;
        }

        return false;
    }
}
