package com.jike.camerapro.cvprocessor;

import android.os.Environment;

import com.jike.camerapro.interfaces.CalculateListener;
import com.jike.camerapro.pixfomula.PixFormula;
import com.jike.camerapro.utils.Camera2Utils;

import org.opencv.core.Mat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;


public class CvNightProcessor extends CvImageProcessor {

    public float[] times = new float[]{1/32f,1/16f,1/8f,1/4f};

    public CvNightProcessor(CalculateListener listener, int maxSize) {
        super(listener, maxSize);
    }

    @Override
    protected void process(long[] addrs) {
        SimpleDateFormat sTimeFormat=new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
        String date = sTimeFormat.format(new Date());
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath()+"/Camera"+"/Night-Merge"+date+".jpg";

        PixFormula.cvMatFusion(addrs,path);

        File file2 = new File(path);
        Camera2Utils.galleryAddPic(file2);
    }

    @Override
    protected void release(Mat[] mats) {

    }
}
