package com.example.administrator.myapplication.util;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

public class DialogUtil {



    private static AlertDialog alertDialog;
    /**
     *  显示单独按钮的Dialog
     */
    public static void showSimpleDialog(Activity context, String title, String message , DialogInterface.OnClickListener clickListener) {
        AlertDialog.Builder builder =new AlertDialog.Builder(context);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(message);
        //监听事件
        if (clickListener != null){
            builder.setPositiveButton("确认",clickListener);
        }else{
            builder.setNegativeButton("知道了",null);
        }
        alertDialog=builder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        if(!((Activity) context).isFinishing()){
            alertDialog.show();
        }
    }

}
