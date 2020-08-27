package com.jike.camerapro.cvprocessor;

import android.graphics.Bitmap;
import android.media.Image;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import com.jike.camerapro.cameradata.CamSetting;
import com.jike.camerapro.imagereader.CvBytesReader;
import com.jike.camerapro.interfaces.CalculateListener;
import com.jike.camerapro.utils.Jpeg;
import com.jike.camerapro.utils.Yuv420;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

public abstract class CvImageProcessor{

    public static final int START_CAL = 0x00001;
    public static final int OVER_CAL = 0x00002;

    protected static int index = 0;

    protected CalculateListener listener;

    protected int maxSize;

    private Mat[] mats;
    private long[] matAddrs;

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            if(msg.what == START_CAL){
                if(listener!=null){
                    listener.onStartToCalculate();
                }
            }else if(msg.what == OVER_CAL){
                if(listener!=null){
                    listener.onCalculateFinished();
                }
                index = 0;
            }
        }
    };

    public CvImageProcessor(CalculateListener listener, int maxSize) {
        this.listener = listener;
        this.maxSize = maxSize;
        this.mats = new Mat[maxSize];
        this.matAddrs = new long[maxSize];
    }

    public void addImage(Image image){
        ImageThread imageThread = new ImageThread(image);
        imageThread.start();
        try {
            imageThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //protected abstract String getPath();

    class ImageThread extends Thread{

        private Image image;

        public ImageThread(Image image){
            this.image = image;
        }

        @Override
        public void run() {
            int width = image.getWidth();
            int height = image.getHeight();

            index ++;

            if(index == maxSize) {
                Message message = new Message();
                message.what = START_CAL;
                handler.sendMessage(message);
            }

            if(CamSetting.isYuv) {
                byte[] bytes = CvBytesReader.getYUVBytesFromImage(image);
                Bitmap bitmap = CvBytesReader.getBitmapImageFromYUV(bytes,"",false,width,height);
                Mat mat = new Mat(image.getWidth(), image.getHeight(), CvType.CV_8UC3);
                Utils.bitmapToMat(bitmap, mat);
                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2BGR);
                //Mat mat = Yuv420.rgb(image);
                mats[index-1]=mat;
                matAddrs[index-1] = mats[index-1].getNativeObjAddr();
                image.close();
            } else {
                mats[index-1]=Jpeg.rgb(image);
                matAddrs[index-1] = mats[index-1].getNativeObjAddr();
                image.close();
            }

            if(index == maxSize){
                process(matAddrs);
                for(int i = 0;i<mats.length;i++){
                    mats[index-1].release();
                }

                Message message2 = new Message();
                message2.what = OVER_CAL;
                handler.sendMessage(message2);

                index = 0;
            }
        }
    }


    /*public void saveYuvImage(byte[] data, int width, int height) {
        YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect(0, 0, width, height), 100, baos);
        byte[] jdata = baos.toByteArray();
        BitmapFactory.Options bitmapFatoryOptions = new BitmapFactory.Options();
        bitmapFatoryOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length, bitmapFatoryOptions);

        File mFile = new File(getPath());
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mFile);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            galleryAddPic(mFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    protected abstract void process(long[] matAddrs);
    protected abstract void release(Mat[] mats);

}