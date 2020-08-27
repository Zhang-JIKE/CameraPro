package com.jike.camerapro.pixfomula;

import android.graphics.Bitmap;
import android.graphics.Point;

import org.opencv.core.Mat;

public class PixFormula {

    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("opencv_java4");
    }

    public static native byte[] bitmapXAvergeByPixel(Bitmap[] bitmaps);


    public static native void cvMatFusion(long[] matAddrs, String path);

    public static native byte[] bitmapSuperResolution(Bitmap bitmapA, Bitmap bitmapB,
                                             Bitmap bitmapC, Bitmap bitmapD);

    public static native int getBrightness(Bitmap bitmap);

    public static native Mat resize(long matAddr);

}
