package com.example.administrator.myapplication.test;

import android.app.Application;
import android.content.Context;
import android.widget.Toast;

import com.example.administrator.myapplication.util.WifiUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ServerSocketUtil {

    private static volatile ServerSocketUtil instance;
    private ServerSocketUtil() {}
    public static ServerSocketUtil getInstance() {
        if (instance == null) {
            synchronized (ServerSocketUtil.class) {
                if (instance == null) {
                    instance = new ServerSocketUtil();
                }
            }
        }
        return instance;
    }

    private Context context;
    public void init(Application application){
        context = application;
    }


    private ServerSocket serverSocket;
    private Disposable disposable;
    private Disposable readDisposable;
    private List<Socket> socketList;
    private InputStream inputStream;

    public void close(){
        try {
            for (Socket socket:socketList){
                if (socket != null){
                    socket.close();
                }
            }
            if (serverSocket != null){
                serverSocket.close();
            }
            if (inputStream != null){
                inputStream.close();
            }
            if (!disposable.isDisposed()){
                disposable.dispose();
            }
            if (!readDisposable.isDisposed()){
                readDisposable.dispose();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void serverSocket(int port){
        Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> emitter) throws Exception {
                serverSocket = new ServerSocket(port);
                socketList = new ArrayList<>();
                while (true){
                    Socket socket =serverSocket.accept();
                    if (socket != null){
                        socketList.add(socket);
                        //接收信息
                        serverRead(socket);
                    }
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Object>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                    }

                    @Override
                    public void onNext(Object o) {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }


    public void serverRead(Socket socket){
        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                inputStream = socket.getInputStream();
                byte[] bytes = new byte[1024];
                while (true) {
                    int len ;
                    StringBuilder msg = new StringBuilder();
                    //接收信息
                    while ((len = inputStream.read(bytes)) != -1){
                        String string = new String(bytes,0,len);
                        msg.append(string);
                    }
                    emitter.onNext(msg.toString());
                    //接收到信息后发送信息
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(msg.toString().getBytes());
                    outputStream.close();
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


    /**
     * 发送信息
     * @param msg
     * @param socket
     */
    public void serverWrite(String msg,Socket socket){
        Observable.create(new ObservableOnSubscribe<byte[]>() {
            @Override
            public void subscribe(ObservableEmitter<byte[]> emitter) throws Exception {
                if (socket != null){
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(msg.getBytes());
                    outputStream.close();
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
     * @param socket
     */
    public void serverWriteFile(File file, Socket socket){
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

    private void showToast(String msg){
        Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
    }
}
