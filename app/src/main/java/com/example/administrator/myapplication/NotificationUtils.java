package com.example.administrator.myapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

public class NotificationUtils extends ContextWrapper {

    private NotificationManager mManager;
    public static final String ANDROID_CHANNEL_ID = "com.gaoxin.industry.ANDROID";
    public static final String ANDROID_CHANNEL_NAME = "ANDROID CHANNEL";
    private Notification notification;

    public NotificationUtils(Context base) {
        super(base);
        if (Build.VERSION.SDK_INT >= 26){
            createChannels();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void createChannels() {

        // create android channel
        NotificationChannel androidChannel = new NotificationChannel(ANDROID_CHANNEL_ID,
                ANDROID_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
        // Sets whether notifications posted to this channel should display notification lights
        androidChannel.enableLights(true);
        // Sets whether notification posted to this channel should vibrate.
        androidChannel.enableVibration(true);
        // Sets the notification light color for notifications posted to this channel
        androidChannel.setLightColor(Color.GREEN);
        // Sets whether notifications posted to this channel appear on the lockscreen or not
        androidChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        getManager().createNotificationChannel(androidChannel);
    }

    public NotificationManager getManager() {
        if (mManager == null) {
            mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return mManager;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public Notification.Builder getAndroidChannelNotification(String title,String content) {
        return new Notification.Builder(getApplicationContext(), ANDROID_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(android.R.drawable.stat_notify_more)
                ;
    }
    public NotificationCompat.Builder getNotification_25(String title, String content){
        return new NotificationCompat.Builder(getApplicationContext())
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(android.R.drawable.stat_notify_more)
                ;
    }
    public void sendNotification(int id, String title, String content, RemoteViews remoteViews, PendingIntent intent){
        if (Build.VERSION.SDK_INT>=26){

            notification = getAndroidChannelNotification(title, content).setCustomContentView(remoteViews).setContentIntent(intent)
                    .build();
            getManager().notify(id,notification);
        }else{
            notification = getNotification_25(title, content).setCustomContentView(remoteViews).setContentIntent(intent).build();
            getManager().notify(id,notification);
        }
    }
    public void cancelNotification(int id){
        getManager().cancel(id);
    }
    public Notification getNotification(){
        if (notification != null){
            return notification;
        }
        return null;
    }
}
