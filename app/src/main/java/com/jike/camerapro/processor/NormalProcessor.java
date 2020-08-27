package com.jike.camerapro.processor;

import android.media.Image;
import android.os.Environment;
import android.util.Log;

import com.jike.camerapro.cameradata.CamSetting;
import com.jike.camerapro.imagereader.CvBytesReader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import static com.jike.camerapro.utils.Camera2Utils.galleryAddPic;

public class NormalProcessor implements Runnable {

    private final Queue<Image> mImageQueue = new LinkedList<>();

    public NormalProcessor() {
    }

    public void addImage(Image image){
        synchronized (mImageQueue) {
            mImageQueue.add(image);
            Log.e("ADD","ADD");
        }
    }

    @Override
    public void run() {
        synchronized (mImageQueue) {
            while (!mImageQueue.isEmpty()) {
                Image image = mImageQueue.poll();
                int width = image.getWidth();
                int height = image.getHeight();
                assert image != null;

                if(CamSetting.isYuv) {
                    byte[] bytes = CvBytesReader.getYUVBytesFromImage(image);
                    CvBytesReader.getBitmapImageFromYUV(bytes, "YUV", true, width, height);
                    image.close();

                } else {

                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    FileOutputStream output = null;
                    try {
                        SimpleDateFormat sTimeFormat = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss-SS");
                        String date = sTimeFormat.format(new Date());
                        File mFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/Camera" + "/CamPro" + date + ".jpg");

                        output = new FileOutputStream(mFile);
                        output.write(bytes);

                        galleryAddPic(mFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        image.close();
                        if (null != output) {
                            try {
                                output.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

        }
    }


}