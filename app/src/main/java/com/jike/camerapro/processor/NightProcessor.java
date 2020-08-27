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

public class NightProcessor extends BaseImageProcessor {


    public NightProcessor(Handler handler, CalculateListener listener, int maxBitmapsSize) {
        super(handler, listener, maxBitmapsSize);
    }

    @Override
    protected void process(ArrayList<Bitmap> bitmaps) {
        Bitmap[] bitmapLists = new Bitmap[CvBytesReader.NIGHT_COUNT];

        for(int i = 0; i < bitmapLists.length; i++){
            bitmapLists[i] = bitmaps.get(i);
        }
        long st = System.currentTimeMillis();
        byte[] res = PixFormula.bitmapXAvergeByPixel(bitmapLists);
        long et = System.currentTimeMillis();
        Log.e("ProcessTime","Night "+(et - st));

        BitmapUtils.createBmpByPixels(res, "Night", CamSize.getPicSize().getWidth(), CamSize.getPicSize().getHeight());
    }
}
