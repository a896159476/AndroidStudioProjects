package com.example.administrator.myapplication;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.example.administrator.myapplication.cockroach.Cockroach;
import com.example.administrator.myapplication.cockroach.CrashLog;
import com.example.administrator.myapplication.cockroach.DebugSafeModeUI;
import com.example.administrator.myapplication.cockroach.ExceptionHandler;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
//        JPushInterface.setDebugMode(true);
//        JPushInterface.init(this);


        //添加tag标签，发送消息的之后就可以指定tag标签来发送了
//        Set<String> set = new HashSet<>();
//        set.add("andfixdemo");//名字任意，可多添加几个
//        JPushInterface.filterValidTags(set);
//        JPushInterface.setTags(this,1,set);//设置标签


        install();
    }

    private void install() {
        final Thread.UncaughtExceptionHandler sysExcepHandler = Thread.getDefaultUncaughtExceptionHandler();
        Cockroach.install(this, new ExceptionHandler() {
            @Override
            protected void onUncaughtExceptionHappened(Thread thread, Throwable throwable) {
                Log.e("AndroidRuntime", "--->onUncaughtExceptionHappened:" + thread + "<---", throwable);
                CrashLog.saveCrashLog(getApplicationContext(), throwable);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MyApplication.this,"捕获到导致崩溃的异常", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            protected void onBandageExceptionHappened(Throwable throwable) {
                throwable.printStackTrace();//打印警告级别log，该throwable可能是最开始的bug导致的，无需关心
                Toast.makeText(MyApplication.this,"Cockroach Worked", Toast.LENGTH_LONG).show();
            }

            @Override
            protected void onEnterSafeMode() {
                Toast.makeText(MyApplication.this,"已经进入安全模式", Toast.LENGTH_LONG).show();
                DebugSafeModeUI.showSafeModeUI();
            }

            @Override
            protected void onMayBeBlackScreen(Throwable e) {
                Thread thread = Looper.getMainLooper().getThread();
                Log.e("AndroidRuntime", "--->onUncaughtExceptionHappened:" + thread + "<---", e);
                //黑屏时建议直接杀死app
                sysExcepHandler.uncaughtException(thread, new RuntimeException("black screen"));
            }

        });

    }
}
