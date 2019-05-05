package com.example.administrator.myapplication;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.Toast;

public class MyBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("INSTALL_AND_START")) {
            start(context);
        }
    }

    private void start(Context context){
        if (!isRunningForeground(context)){
            startAPP("com.example.administrator.myapplication", context);//字符串中输入自己app的包名
        }

    }

    /**
     * 启动一个app
     */
    public void startAPP(String appPackageName, Context context){
        try{
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(appPackageName);
            context.startActivity(intent);
        }catch(Exception e){
            Toast.makeText(context, "尚未安装", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 判断app是否在前台运行
     * @param context
     * @return
     */
    private boolean isRunningForeground (Context context) {
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        String currentPackageName = cn.getPackageName();
        if(!TextUtils.isEmpty(currentPackageName) && currentPackageName.equals(context.getPackageName())) {
            return true ;
        }
        return false ;
    }

}
