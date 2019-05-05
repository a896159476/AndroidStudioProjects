package com.example.administrator.myapplication.test;

import android.app.Application;
import android.content.Context;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ScoketUtil {

    private static volatile ScoketUtil instance;
    private ScoketUtil() {}
    public static ScoketUtil getInstance() {
        if (instance == null) {
            synchronized (ScoketUtil.class) {
                if (instance == null) {
                    instance = new ScoketUtil();
                }
            }
        }
        return instance;
    }

    private Context context;
    public void init(Application application){
        context = application;
    }

    private Socket socket;
    private Disposable disposable;
    public void socket(String ip,int port){
        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter)  {
                try {
                    socket = new Socket(ip,port);
                } catch (IOException e) {
                    e.printStackTrace();
                    resetSocket(ip,port);
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
                    public void onNext(String s) {

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
     * 重新发起连接
     * @param ip
     * @param port
     */
    private void resetSocket(String ip,int port){
        while(isServerClose()){
            try{
                socket = new Socket(ip, port);
            }catch (IOException e) {
                resetSocket(ip,port);
            }
        }
    }

    /**
     * 判断是否断开连接，断开返回true,没有返回false
     * @return
     */
    private Boolean isServerClose(){
        try{
            socket.sendUrgentData(0);//发送1个字节的紧急数据，默认情况下，服务器端没有开启紧急数据处理，不影响正常通信
            return false;
        }catch(Exception se){
            return true;
        }
    }

    /**
     * 发送信息
     * @param msg
     */
    public void write(String msg){
        Observable.create(new ObservableOnSubscribe<byte[]>() {
            @Override
            public void subscribe(ObservableEmitter<byte[]> emitter) throws Exception {
                if (socket != null){
                    OutputStream socketOut = socket.getOutputStream();
                    socketOut.write(msg.getBytes());
                    socketOut.close();
                }
                emitter.onComplete();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<byte[]>() {
                    private Disposable disposable;
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                    }

                    @Override
                    public void onNext(byte[] bytes) {

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
     * @param file
     */
    public void writeFile(File file){
        Observable.create(new ObservableOnSubscribe<byte[]>() {
            @Override
            public void subscribe(ObservableEmitter<byte[]> emitter) throws Exception {
                if (socket != null){
                    OutputStream outputStream = socket.getOutputStream();
                    FileInputStream fileInputStream = new FileInputStream(file);
                    byte[] bytes = new byte[1024];
                    while (fileInputStream.read(bytes) != -1){
                        outputStream.write(bytes);
                    }
                    outputStream.close();
                    fileInputStream.close();
                }
                emitter.onComplete();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<byte[]>() {
                    private Disposable disposable;
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                    }

                    @Override
                    public void onNext(byte[] bytes) {

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

    private Disposable readDisposable;
    private InputStream socketIn;

    /**
     * 读取消息
     */
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
                    emitter.onNext(msg.toString());
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        readDisposable = d;
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

    private void close(){
        try {
            if (socket != null){
                socket.close();
            }
            if (socketIn != null){
                socketIn.close();
            }
            if (readDisposable.isDisposed()){
                readDisposable.dispose();
            }
            if (disposable.isDisposed()){
                disposable.dispose();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showToast(String msg){
        Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
    }

}
