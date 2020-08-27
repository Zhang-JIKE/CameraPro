package com.jike.camerapro.processor;

import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;

import com.jike.camerapro.cameradata.CamSize;
import com.jike.camerapro.imagereader.CvBytesReader;
import com.jike.camerapro.interfaces.CalculateListener;
import com.jike.camerapro.pixfomula.PixFormula;
import com.jike.camerapro.utils.BitmapUtils;
import java.util.ArrayList;


public class FusionProcessor extends BaseImageProcessor {


    public FusionProcessor(Handler handler, CalculateListener listener, int maxBitmapsSize) {
        super(handler, listener, maxBitmapsSize);
    }

    @Override
    protected void process(ArrayList<Bitmap> bitmaps) {
        /*byte[] res = PixFormula.bitmapAverge(
                bitmaps.get(bitmaps.size() - 8),
                bitmaps.get(bitmaps.size() - 7),
                bitmaps.get(bitmaps.size() - 6),
                bitmaps.get(bitmaps.size() - 5),
                bitmaps.get(bitmaps.size() - 4),
                bitmaps.get(bitmaps.size() - 3),
                bitmaps.get(bitmaps.size() - 2),
                bitmaps.get(bitmaps.size() - 1));
*/
    /*    Mat mat1 = new Mat();
        Mat mat2 = new Mat();
        Utils.bitmapToMat(bitmaps.get(bitmaps.size() - 8),mat1);
        Utils.bitmapToMat(bitmaps.get(bitmaps.size() - 7),mat2);

        mat1.convertTo(mat1, CV_32FC1, 255);
        mat2.convertTo(mat2, CV_32FC1, 255);

        Point point = Imgproc.phaseCorrelate(mat1,mat2);
        Log.e("Point","x"+point.x+"y"+point.y);
        //Core.addWeighted();*/
        Log.e("PixFusion","PixFusion ");
        Bitmap[] bitmapLists = new Bitmap[CvBytesReader.FUSION_COUNT];

        for(int i = 0; i < bitmapLists.length; i++){
            bitmapLists[i] = bitmaps.get(i);
        }

        long st = System.currentTimeMillis();
        byte[] res = PixFormula.bitmapXAvergeByPixel(bitmapLists);
        long et = System.currentTimeMillis();
        Log.e("ProcessTime","PixFusion "+(et - st));

        BitmapUtils.createBmpByPixels(res, "PixFusion", CamSize.getPicSize().getWidth(), CamSize.getPicSize().getHeight());

    }
}
