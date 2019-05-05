package com.example.administrator.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;

import cn.jpush.android.api.JPushInterface;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UpgradeService extends Service {

    private OkHttpClient client;
    private Call call;
    private Disposable downDisposable;
    private long downloadLength=0;
    private long contentLength=0;
    private String apkUrl="https://raw.githubusercontent.com/xuexiangjys/XUpdate/master/apk/xupdate_demo_1.0.2.apk";
    private String fileName="updateDemo.apk";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {

                downApk(apkUrl);
            }
        }).subscribeOn(Schedulers.io())// 将被观察者切换到子线程
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        downDisposable = d;
                    }
                    @Override
                    public void onNext(Integer result) {

                    }
                    @Override
                    public void onError(Throwable e) {
                    }
                    @Override
                    public void onComplete() {
                    }
                });
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (downDisposable!=null){
            downDisposable.dispose();//取消订阅
        }
        if (call!=null){
            call.cancel();
        }
    }

    //下载apk
    private void downApk(final String downloadUrl){
        client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        call=client.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //下载失败
                if(e.toString().contains("closed")) {//如果是主动取消的情况下
                    Toast.makeText(getApplicationContext(),"",Toast.LENGTH_SHORT).show();
                }else{
                    if (!call.isCanceled()){
                        breakpoint(downloadUrl);
                    }
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() == null) {
                    //下载失败
                    if (!call.isCanceled()){
                        breakpoint(downloadUrl);
                    }
                    return;
                }
                InputStream is = null;
                FileOutputStream fos = null;
                byte[] buff = new byte[2048];
                int len;
                try {
                    is = response.body().byteStream();
                    File file = createFile();
                    fos = new FileOutputStream(file);
                    long total = response.body().contentLength();
                    contentLength=total;
                    long sum = 0;
                    while ((len = is.read(buff)) != -1) {
                        fos.write(buff,0,len);
                        sum+=len;
                        int progress = (int) (sum * 1.0f / total * 100);
                        //下载中，更新下载进度
//                        emitter.onNext(progress);
                        downloadLength=sum;
                    }
                    fos.flush();
                    try {
                        is.close();
                        fos.close();
                        call.cancel();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //4.下载完成，安装apk
                    onSilentInstall(file);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (!call.isCanceled() && is!=null){
                        breakpoint(downloadUrl);
                    }
                }
            }
        });

    }

    //断点续传
    private void breakpoint(final String downloadUrl){
        Request request = new Request.Builder()
                .url(downloadUrl)
                .addHeader("RANGE", "bytes=" + downloadLength + "-" + contentLength)
                .build();
        call=client.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if(e.toString().contains("closed")) {//如果是主动取消的情况下
                    Toast.makeText(getApplicationContext(),"",Toast.LENGTH_SHORT).show();
                }else{
                    if (!call.isCanceled()){
                        breakpoint(downloadUrl);
                    }
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() == null) {
                    //下载失败
                    if (!call.isCanceled()){
                        breakpoint(downloadUrl);
                    }
                    return;
                }
                InputStream is = null;
                RandomAccessFile randomFile = null;
                byte[] buff = new byte[2048];
                int len;
                try {
                    is = response.body().byteStream();
                    String root = Environment.getExternalStorageDirectory().getPath();
                    File file = new File(root,fileName);
                    randomFile = new RandomAccessFile(file, "rwd");
                    randomFile.seek(downloadLength);
                    long total = contentLength;
                    long sum = downloadLength;
                    while ((len = is.read(buff)) != -1) {
                        randomFile.write(buff,0,len);
                        sum+=len;
                        int progress = (int) (sum * 1.0f / total * 100);
                        //下载中，更新下载进度
//                        emitter.onNext(progress);
                        downloadLength=sum;
                    }
                    try {
                        is.close();
                        randomFile.close();
                        call.cancel();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //4.下载完成，安装apk
                    onSilentInstall(file);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (!call.isCanceled() && is!=null){
                        breakpoint(downloadUrl);
                    }
                }
            }
        });
    }


    /**
     * 路径为根目录
     * 创建文件名称为 updateDemo.apk
     */
    private File createFile() {
        String root = Environment.getExternalStorageDirectory().getPath();
        File file = new File(root,fileName);
        if (file.exists())
            file.delete();
        try {
            file.createNewFile();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null ;
    }


    /*************************************** 3.rot检查******************************************************/
    /**
     * 判断手机是否拥有Root权限。
     * @return 有root权限返回true，否则返回false。
     */
    public boolean isRoot() {
        boolean bool = false;
        try {
            bool = new File("/system/bin/su").exists() || new File("/system/xbin/su").exists();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bool;
    }

    @SuppressLint("ShortAlarm")
    public void onSilentInstall(File file) {
        if (!isRoot()) {
            Toast.makeText(this, "没有ROOT权限，不能使用秒装", Toast.LENGTH_SHORT).show();
            return;
        }
        String apkPath = file.getPath();
        boolean isInstall=install(apkPath);
        if (isInstall){
            Intent intent= new Intent(this, MyBroadcastReceiver.class);
            intent.setAction("INSTALL_AND_START");
            PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager ALARM = (AlarmManager) getSystemService(ALARM_SERVICE);
            assert ALARM != null;
            ALARM.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 10*1000, sender);
        }else {
            Toast.makeText(this, "安装失败", Toast.LENGTH_SHORT).show();
        }
        JPushInterface.clearAllNotifications(getApplicationContext());
        stopSelf();//关闭服务
    }

    public boolean install(String apkPath) {
        boolean result = false;
        Process process = null;
        DataOutputStream dataOutputStream = null;
        BufferedReader errorStream = null;
        try {
            // 申请su权限
            process = Runtime.getRuntime().exec("su");
            dataOutputStream = new DataOutputStream(process.getOutputStream());
            // 执行pm install命令
            String command;
//            String command = "pm install -r " + apkPath + "\n";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                command = "pm install -r " + apkPath + "\n";
            } else {
                command = "pm install -r -i com.example.administrator.myapplication --user 0 " + apkPath + "\n";
            }
            dataOutputStream.write(command.getBytes(Charset.forName("utf-8")));
            dataOutputStream.flush();
            dataOutputStream.writeBytes("exit\n");
            dataOutputStream.flush();
            process.waitFor();
            errorStream = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String msg = "";
            String line;
            // 读取命令的执行结果
            while ((line = errorStream.readLine()) != null) {
                msg += line;
            }
            // 如果执行结果中包含Failure字样就认为是安装失败，否则就认为安装成功
            if (!msg.contains("Failure")) {
                result = true;
            }
            try {
                dataOutputStream.close();
                errorStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                dataOutputStream = null;
                errorStream = null;
                process.destroy();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
