package com.jike.camerapro.utils;

import android.graphics.Bitmap;
import android.os.Environment;
import android.widget.Toast;

import com.daily.flexui.util.AppContextUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.jike.camerapro.utils.Camera2Utils.galleryAddPic;

public class BitmapUtils {

    public static void createBmpByPixels(int[] colors,String title,int width, int height) {
        Bitmap bmp = Bitmap.createBitmap(colors,width, height, Bitmap.Config.ARGB_8888);

        SimpleDateFormat sTimeFormat=new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String date=sTimeFormat.format(new Date());
        File mFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath()+"/Camera"+"/CamPro-"+title+date+".jpg");
        Toast.makeText(AppContextUtils.getAppContext(),"保存"+mFile.getAbsolutePath(),Toast.LENGTH_LONG).show();
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
    }

    public static void createBmpByPixels(byte[] res, String title, int width, int height) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(res);
        Bitmap bitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(byteBuffer);

        SimpleDateFormat sTimeFormat=new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
        String date=sTimeFormat.format(new Date());
        File mFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath()+"/Camera"+"/CamPro-"+title+date+".jpg");
        Toast.makeText(AppContextUtils.getAppContext(),"保存"+mFile.getAbsolutePath(),Toast.LENGTH_SHORT).show();
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();

            galleryAddPic(mFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            bitmap.recycle();
        }
    }

    public static byte[] NV21toRGBA(byte[] data, int width, int height) {
        int size = width * height;
        byte[] bytes = new byte[size * 4];
        int y, u, v;
        int r, g, b;
        int index;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                index = j % 2 == 0 ? j : j - 1;

                y = data[width * i + j] & 0xff;
                u = data[width * height + width * (i / 2) + index + 1] & 0xff;
                v = data[width * height + width * (i / 2) + index] & 0xff;

                r = y + (int) 1.370705f * (v - 128);
                g = y - (int) (0.698001f * (v - 128) + 0.337633f * (u - 128));
                b = y + (int) 1.732446f * (u - 128);

                r = r < 0 ? 0 : (r > 255 ? 255 : r);
                g = g < 0 ? 0 : (g > 255 ? 255 : g);
                b = b < 0 ? 0 : (b > 255 ? 255 : b);

                bytes[width * i * 4 + j * 4 + 0] = (byte) r;
                bytes[width * i * 4 + j * 4 + 1] = (byte) g;
                bytes[width * i * 4 + j * 4 + 2] = (byte) b;
                bytes[width * i * 4 + j * 4 + 3] = (byte) 255;//透明度
            }
        }
        return bytes;
    }
}
