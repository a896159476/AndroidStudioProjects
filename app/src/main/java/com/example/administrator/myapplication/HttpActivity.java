package com.example.administrator.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

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

public class HttpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_http);

        upgrade();
    }

    /*************************************** 1.权限检查******************************************************/

    /**
     * 需要进行检测的权限数组
     */
    protected String[] needPermissions = {
            Manifest.permission.INTERNET,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final int PERMISSON_REQUESTCODE = 0;

    /**
     * 判断是否需要检测，防止不停的弹框
     */
    private boolean isNeedCheck = true;

    @Override
    protected void onResume() {
        try{
            super.onResume();
            if (Build.VERSION.SDK_INT >= 23) {
                if (isNeedCheck) {
                    checkPermissions(needPermissions);
                }
            }
        }catch(Throwable e){
            e.printStackTrace();
        }

    }

    /**
     * @param
     * @since 2.5.0
     */
    @TargetApi(23)
    private void checkPermissions(String... permissions) {
        try{
            if (Build.VERSION.SDK_INT >= 23 && getApplicationInfo().targetSdkVersion >= 23) {
                List<String> needRequestPermissonList = findDeniedPermissions(permissions);
                if (null != needRequestPermissonList
                        && needRequestPermissonList.size() > 0) {
                    try {
                        String[] array = needRequestPermissonList.toArray(new String[needRequestPermissonList.size()]);
                        Method method = getClass().getMethod("requestPermissions", new Class[]{String[].class, int.class});
                        method.invoke(this, array, 0);
                    } catch (Throwable e) {

                    }
                }
            }

        }catch(Throwable e){
            e.printStackTrace();
        }
    }

    /**
     * 获取权限集中需要申请权限的列表
     *
     * @param permissions
     * @return
     * @since 2.5.0
     */
    @TargetApi(23)
    private List<String> findDeniedPermissions(String[] permissions) {
        try{
            List<String> needRequestPermissonList = new ArrayList<>();
            if (Build.VERSION.SDK_INT >= 23 && getApplicationInfo().targetSdkVersion >= 23) {
                for (String perm : permissions) {
                    if (checkMySelfPermission(perm) != PackageManager.PERMISSION_GRANTED
                            || shouldShowMyRequestPermissionRationale(perm)) {
                        needRequestPermissonList.add(perm);
                    }
                }
            }
            return needRequestPermissonList;
        }catch(Throwable e){
            e.printStackTrace();
        }
        return null;
    }

    private int checkMySelfPermission(String perm) {
        try {
            Method method = getClass().getMethod("checkSelfPermission", new Class[]{String.class});
            Integer permissionInt = (Integer) method.invoke(this, perm);
            return permissionInt;
        } catch (Throwable e) {
        }
        return -1;
    }

    private boolean shouldShowMyRequestPermissionRationale(String perm) {
        try {
            Method method = getClass().getMethod("shouldShowRequestPermissionRationale", new Class[]{String.class});
            Boolean permissionInt = (Boolean) method.invoke(this, perm);
            return permissionInt;
        } catch (Throwable e) {
        }
        return false;
    }

    /**
     * 检测是否说有的权限都已经授权
     *
     * @param grantResults
     * @return
     * @since 2.5.0
     */
    private boolean verifyPermissions(int[] grantResults) {
        try{
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }catch(Throwable e){
            e.printStackTrace();
        }
        return true;
    }

    @TargetApi(23)
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] paramArrayOfInt) {
        try{
            if (Build.VERSION.SDK_INT >= 23) {
                if (requestCode == PERMISSON_REQUESTCODE) {
                    if (!verifyPermissions(paramArrayOfInt)) {
                        showMissingPermissionDialog();
                        isNeedCheck = false;
                    }
                }
            }
        }catch(Throwable e){
            e.printStackTrace();
        }
    }

    /**
     * 显示提示信息
     *
     * @since 2.5.0
     */
    private void showMissingPermissionDialog() {
        try{
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("提示");
            builder.setMessage("当前应用缺少必要权限。\\n\\n请点击\\\"设置\\\"-\\\"权限\\\"-打开所需权限");

            // 拒绝, 退出应用
            builder.setNegativeButton("取消",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try{
                                finish();
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        }
                    });

            builder.setPositiveButton("设置",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                startAppSettings();
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        }
                    });

            builder.setCancelable(false);

            builder.show();
        }catch(Throwable e){
            e.printStackTrace();
        }
    }

    /**
     * 启动应用的设置
     *
     * @since 2.5.0
     */
    private void startAppSettings() {
        try{
            Intent intent = new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /*************************************** 2.更新检查******************************************************/
    private  OkHttpClient client;
    private Call call;
    private Disposable downDisposable;
    private long downloadLength=0;
    private long contentLength=0;
    private String versionUrl="";
    private String apkUrl="https://raw.githubusercontent.com/xuexiangjys/XUpdate/master/apk/xupdate_demo_1.0.2.apk";
    private String fileName="updateDemo.apk";
    private String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private Button upgrade;
    private ProgressBar progressBar;
    private TextView textView4;

    //判断版本是否最新，如果不是最新版本则更新
    private void upgrade(){
        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                client = new OkHttpClient();

//                RequestBody formBody = new FormBody.Builder()
//                        .add("cmd", "857")
//                        .add("type", "2")
//                        .add("password", Util.ApkMd5)
//                        .build();
//
//                Request request = new Request.Builder()
//                        .url(CalculateUtil.app_http)
//                        .post(formBody)
//                        .build();
//
//                client.newCall(request).enqueue(new okhttp3.Callback() {
//                    @Override
//                    public void onFailure(Call call, IOException e) {
//                        emitter.onError(e);
//                    }
//
//                    @Override
//                    public void onResponse(Call call, Response response) throws IOException {
//                        String result="";
//                        if (response.body()!=null) {
//                            result=response.body().string();
//                        }else {
//                            //返回数据错误
//                            emitter.onComplete();
//                            return;
//                        }
//                        emitter.onNext(result);
//                    }
//                });

                emitter.onNext("5");
            }
        }).subscribeOn(Schedulers.io())// 将被观察者切换到子线程
                .observeOn(AndroidSchedulers.mainThread())// 将观察者切换到主线程
                .subscribe(new Observer<String>() {
                    private Disposable mDisposable;
                    @Override
                    public void onSubscribe(Disposable d) {
                        mDisposable = d;
                    }
                    @Override
                    public void onNext(String result) {
                        if (result.isEmpty()){
                            return;
                        }
                        int versionCode=Integer.parseInt(result);
                        String title="是否升级到"+versionCode+"版本？";
                        String ModifyContent="";
                        final String downloadUrl=apkUrl;
                        try {
                            int version = getPackageManager().
                                    getPackageInfo(getPackageName(), 0).versionCode;
                            if (versionCode>version){
                                LayoutInflater inflater = LayoutInflater.from(HttpActivity.this);
                                View view = inflater.inflate(R.layout.layout_dialog, null);
                                AlertDialog.Builder mDialog = new AlertDialog.Builder(HttpActivity.this,R.style.Translucent_NoTitle);
                                mDialog.setView(view);
                                mDialog.setCancelable(true);
                                mDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                                    @Override
                                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                                        return keyCode == KeyEvent.KEYCODE_BACK;
                                    }
                                });
                                upgrade= view.findViewById(R.id.button);
                                TextView textView1= view.findViewById(R.id.textView1);
                                TextView textView2= view.findViewById(R.id.textView2);
                                TextView textView3= view.findViewById(R.id.textView3);
                                textView4= view.findViewById(R.id.textView4);
                                ImageView iv_close= view.findViewById(R.id.iv_close);
                                progressBar= view.findViewById(R.id.progressBar);
                                progressBar.setMax(100);
                                textView1.setText(title);
                                textView3.setText(ModifyContent);
                                AlertDialog dialog=mDialog.show();
                                upgrade.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        //动态询问是否授权
                                        int permission = ActivityCompat.checkSelfPermission(getApplication(),
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE);
                                        if (permission != PackageManager.PERMISSION_GRANTED) {
                                            ActivityCompat.requestPermissions(HttpActivity.this, PERMISSIONS_STORAGE,
                                                    1);
                                        }else {
                                            upgrade.setVisibility(View.INVISIBLE);
                                            down(downloadUrl);
                                        }
                                    }
                                });
                                iv_close.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {

                                    }
                                });
                            }else {
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }
                        mDisposable.dispose();
                    }
                    @Override
                    public void onError(Throwable e) {
                    }
                    @Override
                    public void onComplete() {
                    }
                });
    }

    //下载apk并更新进度条
    private void down(final String downloadUrl){
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                downApk(downloadUrl,emitter);
            }
        }).subscribeOn(Schedulers.io())// 将被观察者切换到子线程
                .observeOn(AndroidSchedulers.mainThread())// 将观察者切换到主线程
                .subscribe(new Observer<Integer>() {

                    @Override
                    public void onSubscribe(Disposable d) {
                        downDisposable = d;
                    }
                    @Override
                    public void onNext(Integer result) {
                        //设置ProgressDialog 进度条进度
                        progressBar.setProgress(result);
                        textView4.setText(result+"%");
                    }
                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(getApplication(),"网络异常！请重新下载！",Toast.LENGTH_SHORT).show();
                        upgrade.setEnabled(true);
                    }
                    @Override
                    public void onComplete() {
                        Toast.makeText(getApplication(),"服务器异常！请重新下载！",Toast.LENGTH_SHORT).show();
                        upgrade.setEnabled(true);
                    }
                });
    }

    //下载apk
    private void downApk(final String downloadUrl, final ObservableEmitter<Integer> emitter){
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        call=client.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //下载失败
                if (!call.isCanceled()){
                    breakpoint(downloadUrl,emitter);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() == null) {
                    //下载失败
                    if (!call.isCanceled()){
                        breakpoint(downloadUrl,emitter);
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
                        emitter.onNext(progress);
                        downloadLength=sum;
                    }
                    fos.flush();
                    try {
                        is.close();
                        fos.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //4.下载完成，安装apk
                    onSilentInstall(file);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (!call.isCanceled() && is!=null){
                        breakpoint(downloadUrl,emitter);
                    }
                }
            }
        });

    }

    //断点续传
    private void breakpoint(final String downloadUrl, final ObservableEmitter<Integer> emitter){
        Request request = new Request.Builder()
                .url(downloadUrl)
                .addHeader("RANGE", "bytes=" + downloadLength + "-" + contentLength)
                .build();
        call=client.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //下载失败
                if (!call.isCanceled()){
                    breakpoint(downloadUrl,emitter);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() == null) {
                    //下载失败
                    if (!call.isCanceled()){
                        breakpoint(downloadUrl,emitter);
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
                        emitter.onNext(progress);
                        downloadLength=sum;
                    }
                    try {
                        is.close();
                        randomFile.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //4.下载完成，安装apk
                    onSilentInstall(file);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (!call.isCanceled() && is!=null){
                        breakpoint(downloadUrl,emitter);
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
            Intent intent= new Intent(HttpActivity.this, MyBroadcastReceiver.class);
            intent.setAction("INSTALL_AND_START");
            PendingIntent sender = PendingIntent.getBroadcast(HttpActivity.this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager ALARM = (AlarmManager) getSystemService(ALARM_SERVICE);
            assert ALARM != null;
            ALARM.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 10*1000, sender);
        }else {
            Toast.makeText(this, "失败", Toast.LENGTH_SHORT).show();
        }
    }
    private Process process = null;
    public boolean install(String apkPath) {
        boolean result = false;

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (downDisposable!=null){
            downDisposable.dispose();//取消订阅
        }
        if (call!=null){
            call.cancel();
        }
    }

}
