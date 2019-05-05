package com.example.administrator.myapplication;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class MyView extends View {
    private Paint paint;
    private String text;

    public MyView(Context context) {
        this(context,null);
    }

    public MyView(Context context, AttributeSet attributeSet){
        this(context,attributeSet,0);
    }

    public MyView(Context context, AttributeSet attributeSet,int defStyleAttr){
        super(context,attributeSet,defStyleAttr);

        TypedArray typedArray=context.obtainStyledAttributes(attributeSet,R.styleable.MyView);

        typedArray.getDimension(R.styleable.MyView_textSize,12);
        text=typedArray.getString(R.styleable.MyView_text);
        typedArray.getResources();

        typedArray.recycle();

        paint=new Paint();
        paint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawText(text,0,100,paint);
    }
}
