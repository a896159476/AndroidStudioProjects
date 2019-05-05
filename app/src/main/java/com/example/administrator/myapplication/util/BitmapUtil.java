package com.example.administrator.myapplication.util;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

public class BitmapUtil {

    private static volatile BitmapUtil instance;
    private BitmapUtil() {}
    public static BitmapUtil getInstance() {
        if (instance == null) {
            synchronized (BitmapUtil.class) {
                if (instance == null) {
                    instance = new BitmapUtil();
                }
            }
        }
        return instance;
    }

    private Application application;
    public void init(Application application){
        this.application = application;
    }

    /**
     * 从资源文件获取bitmap
     * @param id 资源文件id
     * @return bitmap
     */
    public Bitmap getBitmap(int id){
        return BitmapFactory.decodeResource(application.getResources(), id);
    }

    /**
     * 从sd卡获取bitmap
     * @param path 文件路径，可以为null或空,使用默认文件路径
     * @param fileName 文件名
     * @return bitmap
     */
    public Bitmap getBitmap(String path,String fileName){
        String filePath ;
        if (TextUtils.isEmpty(path)){
            filePath = getFilePath();
        }else {
            filePath = path;
        }
        File file=new File(filePath,fileName);
        if (file.exists()){
            return BitmapFactory.decodeFile(filePath+"/"+fileName, null);
        }
        return null;
    }

    /**
     * 获取文件路径
     * @return 文件路径
     */
    private String getFilePath(){
        String path;
        //判断SD卡是否可用 
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ) {
            //app的sd卡目录
            path = Objects.requireNonNull(application.getExternalFilesDir(Environment.DIRECTORY_PICTURES)).getAbsolutePath();
        }else{
            //app私有目录
            path = application.getFilesDir()+ File.separator+Environment.DIRECTORY_PICTURES;
        }
        return path;
    }

    /**
     * 文件IO流获取bitmap
     * @param path 文件路径
     * @param fileName 文件名字
     * @return bitmap
     */
    public Bitmap getBitmapInputStream(String path,String fileName){
        String filePath ;
        if (path == null || path.equals("")){
            filePath = getFilePath();
        }else {
            filePath = path ;
        }
        File file=new File(filePath,fileName);
        if (file.exists()){
            try {
                FileInputStream fileInputStream=new FileInputStream(file);
                return BitmapFactory.decodeStream(fileInputStream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 设置圆角
     * @param bitmap 原图
     * @param pixels 角度
     * @return 圆角bitmap
     */
    public Bitmap toRoundCorner(Bitmap bitmap, int pixels) {
        Bitmap roundCornerBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(roundCornerBitmap);
        int color = 0xff424242;//int color = 0xff424242;
        Paint paint = new Paint();
        paint.setColor(color);
        //防止锯齿
        paint.setAntiAlias(true);
        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        RectF rectF = new RectF(rect);
        //相当于清屏
        canvas.drawARGB(0, 0, 0, 0);
        //先画了一个带圆角的矩形
        canvas.drawRoundRect(rectF, (float) pixels, (float) pixels, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        //再把原来的bitmap画到现在的bitmap！！！注意这个理解
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return roundCornerBitmap;
    }

    /**
     * 压缩图片,宽和高以及kB大小均会变化
     * @param bitmap 原图
     * @return 压缩bitmap
     */
    public Bitmap compression(Bitmap bitmap){
        // 得到图片原始的高宽
        int rawHeight = bitmap.getHeight();
        int rawWidth = bitmap.getWidth();
        // 设定图片新的高宽
        int newHeight = rawHeight/4;
        int newWidth = rawWidth/4;
        // 计算缩放因子
        float heightScale = ((float) newHeight) / rawHeight;
        float widthScale = ((float) newWidth) / rawWidth;
        // 新建立矩阵
        Matrix matrix = new Matrix();
        matrix.postScale(heightScale, widthScale);
        // 设置图片的旋转角度
        //matrix.postRotate(-30);
        // 设置图片的倾斜
        //matrix.postSkew(0.1f, 0.1f);
        //压缩后图片的宽和高以及kB大小均会变化
        return Bitmap.createBitmap(bitmap, 0, 0, newWidth ,newHeight, matrix, true);
    }

    /**
     * 压缩且保存图片到指定目录
     * @param bitmap 原图
     * @param path 文件路径
     * @param fileName 文件名字
     * @param quality 图像压缩比的值，0-100.0 意味着小尺寸压缩,100意味着高质量压缩
     */
    private void compressAndSave(Bitmap bitmap,String path,String fileName,int quality){
        String filePath ;
        if (path == null || path.equals("")){
            filePath = getFilePath();
        }else {
            filePath = path ;
        }
        File file=new File(filePath,fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
                FileOutputStream fileOutputStream=new FileOutputStream(file);
                //把位图的压缩信息写入到一个指定的输出流中
                //第一个参数format为压缩的格式
                //第二个参数quality为图像压缩比的值,0-100.0 意味着小尺寸压缩,100意味着高质量压缩
                //第三个参数stream为输出流
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fileOutputStream);
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取缩略图
     * @param bitmap 原图
     * @return 缩略图
     */
    public Bitmap getBitmapThumbnail(Bitmap bitmap){
        return ThumbnailUtils.extractThumbnail(bitmap, 100, 100);
    }

    /**
     * 获取缩略图
     * @param filePath 文件路径+"/"+文件名字
     * @return 缩略图
     */
    private Bitmap getBitmapThumbnail(String filePath){
        BitmapFactory.Options options=new BitmapFactory.Options();
        //true那么将不返回实际的bitmap对象,不给其分配内存空间但是可以得到一些解码边界信息即图片大小等信息
        options.inJustDecodeBounds=true;

        //计算sampleSize
        int sampleSize = computeSampleSize(options, 150, 200*200);
        //为了读到图片,必须把options.inJustDecodeBounds设回false
        options.inJustDecodeBounds = false;
        options.inSampleSize = sampleSize;

        return BitmapFactory.decodeFile(filePath, options);
    }

    /**
     * 压缩比
     * @param options 原本Bitmap的options
     * @param minSideLength 希望生成的缩略图的宽高中的较小的值
     * @param maxNumOfPixels 希望生成的缩量图的总像素
     * @return 压缩比
     */
    private int computeSampleSize(BitmapFactory.Options options,int minSideLength, int maxNumOfPixels) {
        //inSampleSize表示缩略图大小为原始图片大小的几分之一,若该值为3
        //则取出的缩略图的宽和高都是原始图片的1/3,图片大小就为原始大小的1/9
        int initialSize = computeInitialSampleSize(options, minSideLength,maxNumOfPixels);
        int roundedSize;
        if (initialSize <= 8) {
            roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }
        return roundedSize;
    }

    /**
     * 缩略图大小为原始图片大小的几分之一
     * @param options 原本Bitmap的options
     * @param minSideLength 希望生成的缩略图的宽高中的较小的值
     * @param maxNumOfPixels 希望生成的缩量图的总像素
     * @return 压缩比
     */
    private int computeInitialSampleSize(BitmapFactory.Options options,int minSideLength, int maxNumOfPixels) {
        //原始图片的宽
        double w = options.outWidth;
        //原始图片的高
        double h = options.outHeight;
        int lowerBound = (maxNumOfPixels == -1) ? 1 : (int) Math.ceil(Math.sqrt(w * h / maxNumOfPixels));
        int upperBound = (minSideLength == -1) ? 128 : (int) Math.min(
                Math.floor(w / minSideLength), Math.floor(h / minSideLength));
        if (upperBound < lowerBound) {
            return lowerBound;
        }
        if ((maxNumOfPixels == -1) && (minSideLength == -1)) {
            return 1;
        } else if (minSideLength == -1) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }
}
