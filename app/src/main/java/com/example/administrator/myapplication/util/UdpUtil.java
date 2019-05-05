package com.example.administrator.myapplication.util;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Enumeration;
import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class UdpUtil {

    private static volatile UdpUtil instance;
    private UdpUtil() {}
    public static UdpUtil getInstance() {
        if (instance == null) {
            synchronized (UdpUtil.class) {
                if (instance == null) {
                    instance = new UdpUtil();
                }
            }
        }
        return instance;
    }

    private Context context;
    public void init(Application application){
        context = application;
    }

    private Disposable serverDisposable;
    private DatagramSocket serverSocket;

    public void close(){
        if (serverSocket!=null){
            serverSocket.close();
        }
        if (!serverDisposable.isDisposed()){
            serverDisposable.dispose();
        }
    }

    public void serverSocket(){
        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                serverSocket = new DatagramSocket(8888);
                byte[] bytes = new byte[1024];
                DatagramPacket packet = new DatagramPacket(bytes,bytes.length);
                while (true){
                    serverSocket.receive(packet);
                    String result = new String(packet.getData(),packet.getOffset(),packet.getLength());
                    emitter.onNext(result);
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        serverDisposable = d;
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

    private Disposable disposable;
    private DatagramSocket socket;
    private InetAddress serverAddress;
    public void socket(){
        try {
            //192.168.1.126为服务器的ip
            serverAddress = InetAddress.getByName("192.168.1.126");
            socket = new DatagramSocket(8888);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void send(){
        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                String sendData = "hello world";
                byte data[] = sendData.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, 8888);
                socket.send(packet);
                emitter.onComplete();
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
                    public void onNext(String result) {

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
     * 获取手机ip
     * @param context
     * @return ip
     */
    public static String getIPAddress(Context context) {
        NetworkInfo info = ((ConnectivityManager) Objects.requireNonNull(context
                .getSystemService(Context.CONNECTIVITY_SERVICE))).getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            if (info.getType() == ConnectivityManager.TYPE_MOBILE) {//当前使用2G/3G/4G网络
                try {
                    //Enumeration<NetworkInterface> en=NetworkInterface.getNetworkInterfaces();
                    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                        NetworkInterface intf = en.nextElement();
                        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                            InetAddress inetAddress = enumIpAddr.nextElement();
                            if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                                return inetAddress.getHostAddress();
                            }
                        }
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                }

            } else if (info.getType() == ConnectivityManager.TYPE_WIFI) {//当前使用无线网络
                WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = null;
                if (wifiManager != null) {
                    wifiInfo = wifiManager.getConnectionInfo();
                }
                int ipAdd = 0;
                if (wifiInfo != null) {
                    ipAdd = wifiInfo.getIpAddress();
                }
                String ip = null;
                try {
                    ip = InetAddress.getByAddress(
                            ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipAdd).array())
                            .getHostAddress();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                return ip;
            }
        } else {
            //当前无网络连接,请在设置中打开网络
        }
        return null;
    }

    private void showToast(String msg){
        Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
    }
}
