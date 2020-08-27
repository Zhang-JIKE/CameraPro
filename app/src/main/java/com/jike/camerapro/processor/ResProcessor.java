package com.jike.camerapro.processor;

import android.graphics.Bitmap;
import android.os.Handler;

import com.jike.camerapro.cameradata.CamSize;
import com.jike.camerapro.interfaces.CalculateListener;
import com.jike.camerapro.pixfomula.PixFormula;
import com.jike.camerapro.utils.BitmapUtils;

import java.util.ArrayList;

public class ResProcessor extends BaseImageProcessor {


    public ResProcessor(Handler handler, CalculateListener listener, int maxBitmapsSize) {
        super(handler, listener, maxBitmapsSize);
    }

    @Override
    protected void process(ArrayList<Bitmap> bitmaps) {
        byte[] res = PixFormula.bitmapSuperResolution(
                bitmaps.get(bitmaps.size() - 4),
                bitmaps.get(bitmaps.size() - 3),
                bitmaps.get(bitmaps.size() - 2),
                bitmaps.get(bitmaps.size() - 1));
        
        BitmapUtils.createBmpByPixels(res, "Super-Resolution", CamSize.getPicSize().getWidth()*2, CamSize.getPicSize().getHeight()*2);
    }
}
