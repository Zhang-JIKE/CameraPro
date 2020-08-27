package com.jike.camerapro.imagereader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.daily.flexui.util.AppContextUtils;
import com.jike.camerapro.cameradata.CamMode;
import com.jike.camerapro.cameradata.CamSetting;
import com.jike.camerapro.cameradata.CamSize;
import com.jike.camerapro.cvprocessor.CvFusionProcessor;

import com.jike.camerapro.cvprocessor.CvHdrProcessor;
import com.jike.camerapro.cvprocessor.CvNightProcessor;
import com.jike.camerapro.interfaces.CalculateListener;
import com.jike.camerapro.interfaces.CaptureListener;
import com.jike.camerapro.processor.NormalProcessor;
import com.jike.camerapro.processor.ResProcessor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import static com.jike.camerapro.utils.Camera2Utils.galleryAddPic;

public class CvBytesReader {

    public static final int HDR_COUNT = 7;
    public static final int FUSION_COUNT = 8;
    public static final int NIGHT_COUNT = 8;
    public static final int RES_COUNT = 4;

    public static boolean hdrKey = false;
    public static boolean fusionKey = false;
    public static boolean nightKey = false;
    public static boolean resKey = false;

    private int stepHdr = 0;
    private int stepFusion = 0;
    private int stepNight = 0;
    private int stepRes = 0;

    private CaptureListener captureListener;

    public void setCaptureListener(CaptureListener captureListener) {
        this.captureListener = captureListener;
    }

    private NormalProcessor normalProcessor;
    private CvFusionProcessor fusionProcessor;
    private CvHdrProcessor hdrProcessor;
    private ResProcessor resProcessor;
    private CvNightProcessor nightProcessor;

    private CalculateListener listener = new CalculateListener() {
        @Override
        public void onStartToCalculate() {
            if(captureListener!=null){
                captureListener.onStartToCalculate();
            }
        }

        @Override
        public void onCalculateFinished() {
            if(captureListener!=null){
                captureListener.onCalculateFinished();
            }
        }
    };

    public ImageReader imageReader;

    public void setUpImageReader(final Handler mBackgroundHandler){
        normalProcessor = new NormalProcessor();
        hdrProcessor = new CvHdrProcessor(listener ,HDR_COUNT);
        fusionProcessor = new CvFusionProcessor(listener, FUSION_COUNT);
        resProcessor = new ResProcessor(mBackgroundHandler, listener, RES_COUNT);
        nightProcessor = new CvNightProcessor(listener, NIGHT_COUNT);


        imageReader = ImageReader.newInstance(CamSize.getPicSize().getWidth(),
                CamSize.getPicSize().getHeight(), CamSetting.isYuv ? ImageFormat.YUV_420_888 : ImageFormat.JPEG, /*maxImages*/20);

        imageReader.setOnImageAvailableListener(
                new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader reader) {
                        Log.e("TIMESTAMP","time:"+System.currentTimeMillis());
                        if(CamMode.mode == CamMode.Mode.NORMAL) {
                            processNormal(reader,mBackgroundHandler);
                        }else if(CamMode.mode == CamMode.Mode.HDR){
                            processHdr(reader,mBackgroundHandler);
                        }else if(CamMode.mode == CamMode.Mode.PIX_FUSION){
                            processPixFusion(reader,mBackgroundHandler);
                        }else if(CamMode.mode == CamMode.Mode.NIGHT){
                            processNight(reader,mBackgroundHandler);
                        }else if(CamMode.mode == CamMode.Mode.SUPER_RESOLUTION){
                            processRes(reader,mBackgroundHandler);
                        }
                    }
                }, mBackgroundHandler);

    }

    private void processNormal(ImageReader reader, Handler handler){
        normalProcessor.addImage(reader.acquireNextImage());
        handler.post(normalProcessor);
    }

    private void processHdr(ImageReader reader,Handler handler){
        if(hdrKey) {
            if(captureListener!=null){
                captureListener.onStartToCapture();
            }
            stepHdr++;

            if(stepHdr == HDR_COUNT){
                if(captureListener!=null){
                    captureListener.onCaptureFinished();
                }
                hdrKey = false;
                stepHdr = 0;
            }
            hdrProcessor.addImage(reader.acquireNextImage());
        }
    }

    private void processPixFusion(ImageReader reader, Handler handler){
        if(fusionKey) {
            if(captureListener!=null){
                captureListener.onStartToCapture();
            }
            stepFusion++;

            if(stepFusion == FUSION_COUNT){
                if(captureListener!=null){
                    captureListener.onCaptureFinished();
                }
                fusionKey = false;
                stepFusion = 0;
            }
            fusionProcessor.addImage(reader.acquireNextImage());
        }
    }

    private void processRes(ImageReader reader, Handler handler){
        if(resKey) {
            if(captureListener!=null){
                captureListener.onStartToCapture();
            }
            stepRes++;
            resProcessor.addImage(reader.acquireNextImage());
            resProcessor.start();
            if(stepRes == RES_COUNT){
                if(captureListener!=null){
                    captureListener.onCaptureFinished();
                }
                resKey = false;
                stepRes = 0;
            }
        }
    }

    private void processNight(ImageReader reader, Handler handler){
        if(nightKey) {
            if(captureListener!=null){
                captureListener.onStartToCapture();
            }
            stepNight++;

            if(stepNight == NIGHT_COUNT){
                if(captureListener!=null){
                    captureListener.onCaptureFinished();
                }
                nightKey = false;
                stepNight = 0;
            }
            nightProcessor.addImage(reader.acquireNextImage());
        }
    }

    public void setUpSuperResReaderYUV(final Handler mBackgroundHandler){
        imageReader = ImageReader.newInstance(CamSize.getPicSize().getWidth(),
                CamSize.getPicSize().getHeight(), ImageFormat.YUV_420_888, /*maxImages*/8);

        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireNextImage();
                if (image == null) {
                    return;
                }
                try {
                    int w = image.getWidth(), h = image.getHeight();
                    // size是宽乘高的1.5倍 可以通过ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888)得到
                    int i420Size = w * h * 3 / 2;

                    Image.Plane[] planes = image.getPlanes();
                    //remaining0 = rowStride*(h-1)+w => 27632= 192*143+176 Y分量byte数组的size
                    int remaining0 = planes[0].getBuffer().remaining();
                    int remaining1 = planes[1].getBuffer().remaining();
                    //remaining2 = rowStride*(h/2-1)+w-1 =>  13807=  192*71+176-1 V分量byte数组的size
                    int remaining2 = planes[2].getBuffer().remaining();
                    //获取pixelStride，可能跟width相等，可能不相等
                    int pixelStride = planes[2].getPixelStride();
                    int rowOffest = planes[2].getRowStride();
                    byte[] nv21 = new byte[i420Size];
                    //分别准备三个数组接收YUV分量。
                    byte[] yRawSrcBytes = new byte[remaining0];
                    byte[] uRawSrcBytes = new byte[remaining1];
                    byte[] vRawSrcBytes = new byte[remaining2];
                    planes[0].getBuffer().get(yRawSrcBytes);
                    planes[1].getBuffer().get(uRawSrcBytes);
                    planes[2].getBuffer().get(vRawSrcBytes);
                    if (pixelStride == w) {
                        //两者相等，说明每个YUV块紧密相连，可以直接拷贝
                        System.arraycopy(yRawSrcBytes, 0, nv21, 0, rowOffest * h);
                        System.arraycopy(vRawSrcBytes, 0, nv21, rowOffest * h, rowOffest * h / 2 - 1);
                    } else {
                        //根据每个分量的size先生成byte数组
                        byte[] ySrcBytes = new byte[w * h];
                        byte[] uSrcBytes = new byte[w * h / 2 - 1];
                        byte[] vSrcBytes = new byte[w * h / 2 - 1];
                        for (int row = 0; row < h; row++) {
                            //源数组每隔 rowOffest 个bytes 拷贝 w 个bytes到目标数组
                            System.arraycopy(yRawSrcBytes, rowOffest * row, ySrcBytes, w * row, w);
                            //y执行两次，uv执行一次
                            if (row % 2 == 0) {
                                //最后一行需要减一
                                if (row == h - 2) {
                                    System.arraycopy(vRawSrcBytes, rowOffest * row / 2, vSrcBytes, w * row / 2, w - 1);
                                } else {
                                    System.arraycopy(vRawSrcBytes, rowOffest * row / 2, vSrcBytes, w * row / 2, w);
                                }
                            }
                        }
                        //yuv拷贝到一个数组里面
                        System.arraycopy(ySrcBytes, 0, nv21, 0, w * h);
                        System.arraycopy(vSrcBytes, 0, nv21, w * h, w * h / 2 - 1);
                    }
                    //这里使用了YuvImage，接收NV21的数据，得到一个Bitmap
                    Bitmap bitmap = getBitmapImageFromYUV(nv21,"YUV",true, w, h);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                image.close();
            }
        },mBackgroundHandler);
    }


    public void closeAllReader(){
        if(imageReader!=null){
            imageReader.close();
        }
    }

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

        SimpleDateFormat sTimeFormat=new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String date=sTimeFormat.format(new Date());
        File mFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath()+"/Camera"+"/CamPro-"+title+date+".jpg");
        Toast.makeText(AppContextUtils.getAppContext(),"保存"+mFile.getAbsolutePath(),Toast.LENGTH_LONG).show();
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

    public static byte[] getYUVBytesFromImage(Image image) {
        int w = image.getWidth(), h = image.getHeight();
        // size是宽乘高的1.5倍 可以通过ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888)得到
        int i420Size = w * h * 3 / 2;

        Image.Plane[] planes = image.getPlanes();
            //remaining0 = rowStride*(h-1)+w => 27632= 192*143+176 Y分量byte数组的size
            int remaining0 = planes[0].getBuffer().remaining();
            int remaining1 = planes[1].getBuffer().remaining();
            //remaining2 = rowStride*(h/2-1)+w-1 =>  13807=  192*71+176-1 V分量byte数组的size
            int remaining2 = planes[2].getBuffer().remaining();
            //获取pixelStride，可能跟width相等，可能不相等
            int pixelStride = planes[2].getPixelStride();
            int rowOffest = planes[2].getRowStride();
            byte[] nv21 = new byte[i420Size];
            //分别准备三个数组接收YUV分量。
            byte[] yRawSrcBytes = new byte[remaining0];
            byte[] uRawSrcBytes = new byte[remaining1];
            byte[] vRawSrcBytes = new byte[remaining2];
            planes[0].getBuffer().get(yRawSrcBytes);
            planes[1].getBuffer().get(uRawSrcBytes);
            planes[2].getBuffer().get(vRawSrcBytes);

            if (pixelStride == w) {
                //两者相等，说明每个YUV块紧密相连，可以直接拷贝
                System.arraycopy(yRawSrcBytes, 0, nv21, 0, rowOffest * h);
                System.arraycopy(vRawSrcBytes, 0, nv21, rowOffest * h, rowOffest * h / 2 - 1);
            } else {
                //根据每个分量的size先生成byte数组
                byte[] ySrcBytes = new byte[w * h];
                byte[] uSrcBytes = new byte[w * h / 2 - 1];
                byte[] vSrcBytes = new byte[w * h / 2 - 1];
                for (int row = 0; row < h; row++) {
                    //源数组每隔 rowOffest 个bytes 拷贝 w 个bytes到目标数组
                    System.arraycopy(yRawSrcBytes, rowOffest * row, ySrcBytes, w * row, w);
                    //y执行两次，uv执行一次
                    if (row % 2 == 0) {
                        //最后一行需要减一
                        if (row == h - 2) {
                            System.arraycopy(vRawSrcBytes, rowOffest * row / 2, vSrcBytes, w * row / 2, w - 1);
                        } else {
                            System.arraycopy(vRawSrcBytes, rowOffest * row / 2, vSrcBytes, w * row / 2, w);
                        }
                    }
                }
                //yuv拷贝到一个数组里面
                System.arraycopy(ySrcBytes, 0, nv21, 0, w * h);
                System.arraycopy(vSrcBytes, 0, nv21, w * h, w * h / 2 - 1);
            }

        return nv21;
    }

    public static Bitmap getBitmapImageFromYUV(byte[] data, String title, boolean isSaved, int width, int height) {
        YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect(0, 0, width, height), 100, baos);
        byte[] jdata = baos.toByteArray();
        BitmapFactory.Options bitmapFatoryOptions = new BitmapFactory.Options();
        bitmapFatoryOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length, bitmapFatoryOptions);

        SimpleDateFormat sTimeFormat=new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss-SS");
        String date=sTimeFormat.format(new Date());


        if(isSaved) {
            File mFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/Camera" + "/CamPro-" + title+date + ".jpg");
            Toast.makeText(AppContextUtils.getAppContext(), "保存" + mFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
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
        return bmp;
    }

}
