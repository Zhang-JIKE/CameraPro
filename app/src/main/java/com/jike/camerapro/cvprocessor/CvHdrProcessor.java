package com.jike.camerapro.cvprocessor;

import android.os.Environment;

import com.jike.camerapro.interfaces.CalculateListener;
import com.jike.camerapro.pixfomula.PixFormula;
import com.jike.camerapro.utils.Camera2Utils;

import org.opencv.core.Mat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;


public class CvHdrProcessor extends CvImageProcessor {

    public CvHdrProcessor(CalculateListener listener, int maxSize) {
        super(listener, maxSize);
    }

    @Override
    protected void process(long[] addrs) {
        SimpleDateFormat sTimeFormat=new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
        String date = sTimeFormat.format(new Date());
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath()+"/Camera"+"/Hdr"+date+".jpg";
        PixFormula.cvMatFusion(addrs,path);

        File file = new File(path);
        Camera2Utils.galleryAddPic(file);
    }

    @Override
    protected void release(Mat[] mats) {
    }
}
