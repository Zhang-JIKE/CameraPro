package com.jike.camerapro.cvprocessor;

import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Handler;

import com.jike.camerapro.interfaces.CalculateListener;
import com.jike.camerapro.pixfomula.PixFormula;
import com.jike.camerapro.utils.BitmapUtils;
import com.jike.camerapro.utils.Camera2Utils;

import org.opencv.core.Mat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class CvFusionProcessor extends CvImageProcessor {

    public CvFusionProcessor(CalculateListener listener, int maxSize) {
        super(listener, maxSize);
    }

    @Override
    protected void process(long[] addrs) {
        SimpleDateFormat sTimeFormat=new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
        String date = sTimeFormat.format(new Date());
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath()+"/Camera"+"/Fusion"+date+".jpg";

        PixFormula.cvMatFusion(addrs,path);
        File file = new File(path);
        Camera2Utils.galleryAddPic(file);
    }

    @Override
    protected void release(Mat[] mats) {

    }

}
