package com.jike.camerapro.processor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Handler;

import com.jike.camerapro.cameradata.CamSetting;
import com.jike.camerapro.imagereader.CvBytesReader;
import com.jike.camerapro.interfaces.CalculateListener;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public abstract class BaseImageProcessor implements Runnable {

    protected Handler handler;

    private int maxBitmapsSize;

    private CalculateListener calculateListener;

    private Queue<Image> mImageQueue = new LinkedList<>();

    private static ArrayList<Bitmap> bitmaps = new ArrayList<>();

    public BaseImageProcessor(Handler handler, CalculateListener listener ,int maxBitmapsSize){
        this.handler = handler;
        this.maxBitmapsSize = maxBitmapsSize;
        this.calculateListener = listener;
    }

    public void addImage(Image image){
        synchronized (mImageQueue) {
            mImageQueue.add(image);
        }
    }


    @Override
    public void run() {
        while (!mImageQueue.isEmpty()) {
            Image image = mImageQueue.poll();

           int width = image.getWidth();
           int height = image.getHeight();

           byte[] bytes;

           if(CamSetting.isYuv){
               bytes = CvBytesReader.getYUVBytesFromImage(image);
           }else {
               ByteBuffer buffer = image.getPlanes()[0].getBuffer();
               bytes = new byte[buffer.capacity()];
               buffer.get(bytes);
           }
           image.close();

           synchronized (bitmaps) {
               Bitmap bitmap;
               if(CamSetting.isYuv){
                   bitmap = CvBytesReader.getBitmapImageFromYUV(bytes,"",false,width,height);
               }else {
                   bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
               }
               bitmaps.add(bitmap);
               handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(calculateListener != null){
                            calculateListener.onStartToCalculate();
                        }
                    }
               });

               if (bitmaps.size() == maxBitmapsSize) {
                   process(bitmaps);
                   release();
                   handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(calculateListener!=null){
                                calculateListener.onCalculateFinished();
                            } }
                    });

                }
            }
        }

    }

    protected abstract void process(ArrayList<Bitmap> bitmaps);

    private void release(){
        for(int i = bitmaps.size()-1; i>=0;i--){
            bitmaps.get(i).recycle();
            bitmaps.remove(i);
        }

    }

    public void start(){
        handler.post(this);
    }
}
