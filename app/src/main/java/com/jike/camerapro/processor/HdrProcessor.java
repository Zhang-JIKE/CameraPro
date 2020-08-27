package com.jike.camerapro.processor;

import android.graphics.Bitmap;
import android.os.Handler;

import com.jike.camerapro.cameradata.CamSize;
import com.jike.camerapro.interfaces.CalculateListener;
import com.jike.camerapro.pixfomula.PixFormula;
import com.jike.camerapro.utils.BitmapUtils;

import java.util.ArrayList;

import static com.jike.camerapro.imagereader.CvBytesReader.createBmpByPixels;

public class HdrProcessor extends BaseImageProcessor {


    public HdrProcessor(Handler handler, CalculateListener listener, int maxBitmapsSize) {
        super(handler, listener, maxBitmapsSize);
    }

    @Override
    protected void process(ArrayList<Bitmap> bitmaps) {
        //byte[] res = PixFormula.bitmapMerge(bitmaps.get(0), bitmaps.get(1), bitmaps.get(2));

//        byte[] res = PixFormula.featurePointing(bitmaps.get(0), 10, 5);
        //BitmapUtils.createBmpByPixels(res, "HDR", CamSize.getPicSize().getWidth(), CamSize.getPicSize().getHeight());
    }
}
