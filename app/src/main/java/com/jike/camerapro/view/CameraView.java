package com.jike.camerapro.view;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.camera2.params.TonemapCurve;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.renderscript.RenderScript;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.daily.flexui.util.AppContextUtils;
import com.jike.camerapro.cameradata.CamMode;
import com.jike.camerapro.cameradata.CamPara;
import com.jike.camerapro.cameradata.CamRates;
import com.jike.camerapro.cameradata.CamSetting;
import com.jike.camerapro.cameradata.CamSize;
import com.jike.camerapro.cvprocessor.HdrRenderProcessor;
import com.jike.camerapro.imagereader.CvBytesReader;
import com.jike.camerapro.interfaces.CaptureListener;
import com.jike.camerapro.interfaces.ScalerChangedListener;
import com.jike.camerapro.pixfomula.PixFormula;
import com.jike.camerapro.utils.Camera2Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CameraView extends BaseCameraView {

  public static final long ONE_SECOND = 1000000000;
  public static final int ONE_SECOND_SENSITIVITY = 228;
  private CvBytesReader cvBytesReader;

  public long exposureTime;
  public int iso;
  public int isoBoost;
  public int ae;
  public int brightNess;

  public int viewWidth = 0;
  public int viewHeight = 0;


  private ShutterView shutterView;
  private TextView tips;

  public void setShutterView(ShutterView shutterView) {
    this.shutterView = shutterView;
  }

  public void setTips(TextView tips) {
    this.tips = tips;
  }

  public Camera2Helper camera2Helper = new Camera2Helper();

  public CameraView(Context context) {
    super(context);
  }

  public CameraView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  private ScalerChangedListener scalerChangedListener;

  public void setScalerChangedListener(ScalerChangedListener scalerChangedListener) {
    this.scalerChangedListener = scalerChangedListener;
  }

  public void openCamera(int width, int height) {
    if (ContextCompat.checkSelfPermission(AppContextUtils.getAppActivity(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
      return;
    }
    viewHeight = height;
    viewWidth = width;

    camera2Helper.setUpCameraOutputs();
    configureTransform(width,height);
    setAspectRatio(CamSize.getPicSize().getHeight(), CamSize.getPicSize().getWidth());

    CameraManager manager = (CameraManager) AppContextUtils.getAppActivity().getSystemService(Context.CAMERA_SERVICE);
    try {
      if (!Camera2Utils.mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
        throw new RuntimeException("Time out waiting to lock camera opening.");
      }
      assert manager != null;
      manager.openCamera(camera2Helper.mCameraId, camera2Helper.cameraDeviceStateCallback, camera2Helper.mBackgroundHandler);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
    }
  }

  public void closeCamera() {
    try {
      Camera2Utils.mCameraOpenCloseLock.acquire();
      if (null != camera2Helper.cameraCaptureSession) {
        camera2Helper.cameraCaptureSession.close();
        camera2Helper.cameraCaptureSession = null;
      }
      if (null != camera2Helper.mCameraDevice) {
        camera2Helper.mCameraDevice.close();
        camera2Helper.mCameraDevice = null;
      }
      cvBytesReader.closeAllReader();
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
    } finally {
      Camera2Utils.mCameraOpenCloseLock.release();
    }
  }

  public void startBackgroundThread() {
    camera2Helper.mBackgroundThread = new HandlerThread("CameraBackground");
    camera2Helper.mBackgroundThread.start();
    camera2Helper.mBackgroundHandler = new Handler(camera2Helper.mBackgroundThread.getLooper());
    camera2Helper.mBackgroundHandler.post(camera2Helper.detectRunnable);
  }

  public void stopBackgroundThread() {
    if(camera2Helper.mBackgroundThread!=null) {
      camera2Helper.mBackgroundThread.quitSafely();
      try {
        camera2Helper.mBackgroundThread.join();
        camera2Helper.mBackgroundThread = null;
        camera2Helper.mBackgroundHandler = null;
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public void takePicture(){
    camera2Helper.takePicture();
  }

  public class Camera2Helper{
    private String mCameraId;
    private Integer afState;

    private CameraDevice mCameraDevice;
    private CameraCharacteristics mCameraCharacteristics;

    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    private CaptureRequest.Builder previewRequestBuilder;
    private CaptureRequest previewRequest;

    private CameraCaptureSession cameraCaptureSession;

    private Surface surface;
    private Surface processSurface;
    public Range<Integer> isoRange;

    RenderScript mRS;
    HdrRenderProcessor mProcessor;

    private CameraCaptureSession.CaptureCallback takePicCaptureCallback = new CameraCaptureSession.CaptureCallback() {
      @Override
      public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
        //unlockFocus();
      }

    };

    private CameraCaptureSession.CaptureCallback previewCaptureCallback = new CameraCaptureSession.CaptureCallback() {

      private void process(CaptureResult result) {
        switch (Camera2Utils.mState) {
          case Camera2Utils.STATE_PREVIEW: {
            break;
          }
          case Camera2Utils.STATE_WAITING_LOCK: {
            afState = result.get(CaptureResult.CONTROL_AF_STATE);
            if (afState == null) {
              captureStillPicture();
            } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                    CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
              Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
              if (aeState == null ||
                      aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED||
                      CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState||
                      CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                Camera2Utils.mState = Camera2Utils.STATE_PICTURE_TAKEN;
                captureStillPicture();
              } else {
                runPrecaptureSequence();
              }
            }
            break;
          }
          case Camera2Utils.STATE_WAITING_PRECAPTURE: {
            Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
            if (aeState == null ||
                    aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                    aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
              Camera2Utils.mState = Camera2Utils.STATE_WAITING_NON_PRECAPTURE;
            }
            break;
          }
          case Camera2Utils.STATE_WAITING_NON_PRECAPTURE: {
            Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
            if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
              Camera2Utils.mState = Camera2Utils.STATE_PICTURE_TAKEN;
              captureStillPicture();
              Log.e("afState","null3");
            }
            break;
          }
        }
      }

      @Override
      public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
        process(partialResult);
      }

      @Override
      public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
        process(result);
        faceDetect(result);

        try {
          exposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
          iso = result.get(CaptureResult.SENSOR_SENSITIVITY);
          ae = result.get(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION);
          isoBoost = result.get(CaptureResult.CONTROL_POST_RAW_SENSITIVITY_BOOST);
        }catch (NullPointerException e){
          e.printStackTrace();
        }
      }

    };

    private CameraCaptureSession.StateCallback captureSessionStateCallback = new CameraCaptureSession.StateCallback() {
      @Override
      public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
        if (null == mCameraDevice) {
          return;
        }
        camera2Helper.cameraCaptureSession = cameraCaptureSession;
        try {
          previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                  CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

          previewRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE,CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY);
          previewRequestBuilder.set(CaptureRequest.HOT_PIXEL_MODE,CaptureRequest.HOT_PIXEL_MODE_HIGH_QUALITY);

          previewRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE,CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY);

          previewRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE,CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY);
          previewRequestBuilder.set(CaptureRequest.EDGE_MODE,CaptureRequest.EDGE_MODE_HIGH_QUALITY);
          previewRequestBuilder.set(CaptureRequest.SHADING_MODE,CaptureRequest.SHADING_MODE_HIGH_QUALITY);
          previewRequestBuilder.set(CaptureRequest.TONEMAP_MODE,CaptureRequest.TONEMAP_MODE_HIGH_QUALITY);

          previewRequestBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON);
          previewRequestBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);
/*
          List<CaptureRequest> mHdrRequests = new ArrayList<>(2);
          previewRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,-10);
          mHdrRequests.add(previewRequestBuilder.build());
          previewRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,0);
          mHdrRequests.add(previewRequestBuilder.build());*/
          cameraCaptureSession.setRepeatingRequest(previewRequestBuilder.build(),
                  previewCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onConfigureFailed(
              @NonNull CameraCaptureSession cameraCaptureSession) {

      }
    };

    private void setupProcessor() {
      //mProcessor.setOutputSurface(surface);
      //processSurface = mProcessor.getInputSurface();
    }

    private final CameraDevice.StateCallback cameraDeviceStateCallback = new CameraDevice.StateCallback() {
      @Override
      public void onOpened(@NonNull CameraDevice cameraDevice) {
        cvBytesReader = new CvBytesReader();
        cvBytesReader.setCaptureListener(new CaptureListener() {
          @Override
          public void onStartToCapture() {
            tips.post(new Runnable() {
              @Override
              public void run() {
                tips.setVisibility(VISIBLE);
              }
            });
          }

          @Override
          public void onCaptureFinished() {
            tips.post(new Runnable() {
              @Override
              public void run() {
                tips.setVisibility(GONE);
              }
            });
          }

          @Override
          public void onStartToCalculate() {
            shutterView.startProcess();
          }

          @Override
          public void onCalculateFinished() {
            shutterView.backToNormal();
          }
        });

        Camera2Utils.mCameraOpenCloseLock.release();
        mCameraDevice = cameraDevice;
        createCameraPreviewSession();
      }

      @Override
      public void onDisconnected(@NonNull CameraDevice cameraDevice) {
        Camera2Utils.mCameraOpenCloseLock.release();
        cameraDevice.close();
        mCameraDevice = null;
      }

      @Override
      public void onError(@NonNull CameraDevice cameraDevice, int error) {
        Camera2Utils.mCameraOpenCloseLock.release();
        cameraDevice.close();
        mCameraDevice = null;
        AppContextUtils.getAppActivity().finish();
      }
    };

    private void setUpCameraOutputs() {
      CameraManager manager = (CameraManager) AppContextUtils.getAppActivity().getSystemService(Context.CAMERA_SERVICE);
      try {
        for (String cameraId : manager.getCameraIdList()) {
          mCameraCharacteristics = manager.getCameraCharacteristics(cameraId);

          Integer facing = mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
          if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
            continue;
          }
          mCameraId = cameraId;
          Log.e("cameraId",""+cameraId);
          camera2Helper.getCameraArgs();

          return;
        }

      } catch (CameraAccessException e) {
        e.printStackTrace();
      } catch (NullPointerException e) {
      }
    }

    private void createCameraPreviewSession() {
      try {

        SurfaceTexture texture = getSurfaceTexture();
        texture.setDefaultBufferSize(CamSize.getPicSize().getWidth(), CamSize.getPicSize().getHeight());
        surface = new Surface(texture);
        previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        previewRequestBuilder.addTarget(surface);
        setCameraArgs();
        createPreviewSession();
      } catch (CameraAccessException e) {
        e.printStackTrace();
      }
    }

    public void createPreviewSession(){
      if(mCameraDevice == null){
        return;
      }

      try {
        cvBytesReader.setUpImageReader(mBackgroundHandler);
        mCameraDevice.createCaptureSession(Arrays.asList(surface,
                cvBytesReader.imageReader.getSurface()),
                captureSessionStateCallback,
                mBackgroundHandler
        );
      } catch (CameraAccessException e) {
        e.printStackTrace();
      }

    }

    private void captureStillPicture() {
      try {
        if (null == AppContextUtils.getAppActivity() || null == mCameraDevice) {
          return;
        }
        Rect rectSensor = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        int pixW = rectSensor.width();
        int pixH = rectSensor.height();
        int l = (int)((pixW-pixW/mScaleTime)*0.5);
        int t = (int)((pixH-pixH/mScaleTime)*0.5);
        int r = (int)((pixW+pixW/mScaleTime)*0.5);
        int b = (int)((pixH+pixH/mScaleTime)*0.5);

        switch (CamMode.mode){
          case NORMAL:
            CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.set(CaptureRequest.SCALER_CROP_REGION,new Rect(l,t,r,b));
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            builder.set(CaptureRequest.JPEG_ORIENTATION, 90);
            builder.addTarget(cvBytesReader.imageReader.getSurface());

            cameraCaptureSession.capture(builder.build(), null, null);
            break;

          case HDR:
            if(!CvBytesReader.hdrKey) {
              CvBytesReader.hdrKey = true;
              List<CaptureRequest> buildersHdr = new ArrayList<>();
              for (int i = 0; i < CvBytesReader.HDR_COUNT; i++) {
                CaptureRequest.Builder builderHdr = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                builderHdr.set(CaptureRequest.SCALER_CROP_REGION, new Rect(l, t, r, b));

                if(i==0) {
                  builderHdr.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 9);
                }else if(i==1){
                  builderHdr.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 6);
                }else if(i==2){
                  builderHdr.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 3);
                }else if(i==3){
                  builderHdr.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0);
                }else if(i==4){
                  builderHdr.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0);
                }else if(i==5){
                  builderHdr.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, -3);
                }else if(i==6){
                  builderHdr.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, -4);
                }

                /*builderHdr.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                builderHdr.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, CamRates.rawFpsRanges[CamRates.rawFpsRanges.length - 2]);
                if (i == CvBytesReader.HDR_COUNT - 1) {
                  builderHdr.set(CaptureRequest.SENSOR_EXPOSURE_TIME,
                          (long) (CamPara.exposureTime[CamPara.getExposureTimeIndex(exposureTime) + CvBytesReader.HDR_COUNT] * ONE_SECOND));
                } else {
                  builderHdr.set(CaptureRequest.SENSOR_EXPOSURE_TIME,
                          (long) (CamPara.exposureTime[CamPara.getExposureTimeIndex(exposureTime) - 1 + i] * ONE_SECOND));
                }
                builderHdr.set(CaptureRequest.SENSOR_SENSITIVITY, iso);
                builderHdr.set(CaptureRequest.CONTROL_POST_RAW_SENSITIVITY_BOOST, isoBoost);*/

                builderHdr.addTarget(cvBytesReader.imageReader.getSurface());
                builderHdr.addTarget(new Surface(getSurfaceTexture()));
                buildersHdr.add(builderHdr.build());
              }
              cameraCaptureSession.captureBurst(buildersHdr, null, null);
            }
            break;

          case PIX_FUSION:
            if(!CvBytesReader.fusionKey) {
              CvBytesReader.fusionKey = true;
              List<CaptureRequest> buildersSuperRes = new ArrayList<>();
              for (int i = 0; i < CvBytesReader.FUSION_COUNT; i++) {
                CaptureRequest.Builder builderSuperRes = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                builderSuperRes.set(CaptureRequest.SCALER_CROP_REGION, new Rect(l, t, r, b));

                builderSuperRes.addTarget(cvBytesReader.imageReader.getSurface());
                builderSuperRes.addTarget(new Surface(getSurfaceTexture()));

                buildersSuperRes.add(builderSuperRes.build());
              }
              cameraCaptureSession.captureBurst(buildersSuperRes, null, null);
            }
            break;

          case NIGHT:
            if(!CvBytesReader.nightKey) {
              CvBytesReader.nightKey = true;
              List<CaptureRequest> buildersSuperNight = new ArrayList<>();
              for (int i = 0; i < CvBytesReader.NIGHT_COUNT; i++) {
                CaptureRequest.Builder builderSuperNight =  mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                builderSuperNight.set(CaptureRequest.SCALER_CROP_REGION, new Rect(l, t, r, b));

                builderSuperNight.addTarget(cvBytesReader.imageReader.getSurface());
                builderSuperNight.addTarget(new Surface(getSurfaceTexture()));

                builderSuperNight.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                builderSuperNight.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, CamRates.rawFpsRanges[0]);

                builderSuperNight.set(CaptureRequest.SENSOR_EXPOSURE_TIME, CamPara.timeIncrease(exposureTime,1));
                builderSuperNight.set(CaptureRequest.CONTROL_POST_RAW_SENSITIVITY_BOOST,isoBoost+(127-brightNess)+50);
                builderSuperNight.set(CaptureRequest.SENSOR_SENSITIVITY, iso+(255-brightNess));
                //极限夜景
                if(iso > 1400 && brightNess < 127){
                  builderSuperNight.set(CaptureRequest.SENSOR_EXPOSURE_TIME, (long) (ONE_SECOND/8f));
                  builderSuperNight.set(CaptureRequest.SENSOR_SENSITIVITY, iso*(200/brightNess));
                  builderSuperNight.set(CaptureRequest.CONTROL_POST_RAW_SENSITIVITY_BOOST, (int) (isoBoost/((ONE_SECOND/8)/exposureTime)) - (127 - 55));
                }

                buildersSuperNight.add(builderSuperNight.build());
              }
              cameraCaptureSession.captureBurst(buildersSuperNight, null, null);
            }

            break;

          case SUPER_RESOLUTION:
            if(!CvBytesReader.resKey) {
              CvBytesReader.resKey = true;
              List<CaptureRequest> captureRequests = new ArrayList<>();
              for (int i = 0; i < CvBytesReader.RES_COUNT; i++) {
                CaptureRequest.Builder builder1 = previewRequestBuilder;
                builder1.set(CaptureRequest.SCALER_CROP_REGION, new Rect(l, t, r, b));
                builder1.set(CaptureRequest.NOISE_REDUCTION_MODE,CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY);
                builder1.addTarget(cvBytesReader.imageReader.getSurface());
                captureRequests.add(builder1.build());
              }
              cameraCaptureSession.captureBurst(captureRequests, null, null);
            }
            break;
        }
      } catch (CameraAccessException e) {
        e.printStackTrace();
      }
    }

    private void runPrecaptureSequence() {
      try {

        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);

        Camera2Utils.mState = Camera2Utils.STATE_WAITING_PRECAPTURE;
        cameraCaptureSession.capture(previewRequestBuilder.build(), previewCaptureCallback,
                mBackgroundHandler);
      } catch (CameraAccessException e) {
        e.printStackTrace();
      }
    }

    private void lockFocus() {
      try {

        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_START);

        Camera2Utils.mState = Camera2Utils.STATE_WAITING_LOCK;
        cameraCaptureSession.capture(previewRequestBuilder.build(), previewCaptureCallback,
                mBackgroundHandler);
      } catch (CameraAccessException e) {
        e.printStackTrace();
      }
    }

    public void takePicture() {
      camera2Helper.lockFocus();
      File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath()+"/Camera");
      if (!file.exists()) {
        file.mkdirs();
      }
    }

    public void getCameraArgs(){
      StreamConfigurationMap map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

      CamSize.setPicSizes(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)));
      CamSize.setVideoSizes(Arrays.asList(map.getOutputSizes(MediaRecorder.class)));
      CamRates.rawFpsRanges = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
      for(Range r : CamRates.rawFpsRanges){
        Log.e("fps",""+r.toString());
      }

      Boolean available = mCameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
      CamSetting.mFlashSupported = available == null ? false : available;

      isoRange = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);

     /* //获取支持的图像曝光时间范围，单位纳秒
      Range<Long> timeRange = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);

      float maxScaler = mCameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);

      int maxRaw = mCameraCharacteristics.get(CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_RAW);
      Log.e("camerargs","maxRaw"+maxRaw);

      int[] n = mCameraCharacteristics.get(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES);
      for(int i:n){
        Log.e("camerargs","NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES"+i);
      }*/
      //mRS = RenderScript.create(getContext());

      //mProcessor = new HdrRenderProcessor(mRS, new Size(CamSize.getPicSize().getHeight(),CamSize.getPicSize().getWidth()));

      setupProcessor();
    }

    public void setCameraArgs(){
      Range<Integer> fpsRange = CamRates.rawFpsRanges[CamRates.rawFpsRanges.length-2];

      if(CamRates.isForcedOpen60Fps){
        fpsRange = new Range<>(60,60);
      }
      previewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);

      previewRequestBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON);
      previewRequestBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);

      int[] faceDetectModes = mCameraCharacteristics.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES);
      for(int i : faceDetectModes){
        if(i ==CameraCharacteristics.STATISTICS_FACE_DETECT_MODE_SIMPLE && CamSetting.isFaceDetectOpend) {
          previewRequestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, CameraCharacteristics.STATISTICS_FACE_DETECT_MODE_SIMPLE);
          break;
        }
      }
    }

    private void faceDetect(TotalCaptureResult result){
      Face[] faces = result.get(CaptureResult.STATISTICS_FACES);
      Point[] points = new Point[faces.length];
      Rect rectSensor = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
      if(faces!=null) {
        final Rect[] rects = new Rect[faces.length];
        int screenW = getWidth();
        int screenH = getHeight();
        int i = 0;
        for (Face face : faces) {
          Point point = face.getRightEyePosition();
          Rect faceBounds = face.getBounds();
          int l = (int) (screenH * ((float) faceBounds.left / rectSensor.width()));
          int t = (int) (screenW * ((float) faceBounds.top / rectSensor.height()));
          int r = (int) (screenH * ((float) faceBounds.right / rectSensor.width()));
          int b = (int) (screenW * ((float) faceBounds.bottom / rectSensor.height()));

          Rect rect = new Rect( screenW - b,l,screenW-t,r);
          if(point!=null) {
            point.x = (int) (screenH * ((float) point.x / CamSize.getPicSize().getWidth()));
            point.y = (int) (screenW - screenW * ((float) point.y / CamSize.getPicSize().getHeight()));
            points[i] = point;
          }
          rects[i]=rect;
          i++;
        }
        if(cameraControllerView !=null) {
          cameraControllerView.setDetectedFaces(rects);
          cameraControllerView.setDetectedEye(points);
        }
      }
    }

    private Runnable detectRunnable = new Runnable() {
      @Override
      public void run() {
        brightNessDetect();
        if(mBackgroundThread.isAlive()&&mBackgroundHandler!=null) {
          mBackgroundHandler.postDelayed(detectRunnable,300);
        }

      }
    };

    private void brightNessDetect(){
      Bitmap bitmap = getBitmap(3, 3);
      int[] pixels = new int[bitmap.getWidth()*bitmap.getHeight()];
      bitmap.getPixels(pixels,0,bitmap.getWidth(),0,0,bitmap.getWidth(),bitmap.getHeight());

      brightNess = PixFormula.getBrightness(bitmap);

      if(iso>=1200){
        if(brightNess<60){
          CamMode.suggestMode = CamMode.Mode.NIGHT;
        } else{
          CamMode.suggestMode = CamMode.Mode.PIX_FUSION;
        }
      } else {
        CamMode.suggestMode = CamMode.Mode.NORMAL;
        for(int i = 0; i < pixels.length; i++){
          int r = ((pixels[i] & 0x00ff0000) >> 16);
          int g = ((pixels[i] & 0x0000ff00) >> 8);
          int b = (pixels[i] & 0x000000ff);
          int pix1BrightNess = (int) (0.299f*r + 0.587f*g + 0.114f*b);

          r = ((pixels[4] & 0x00ff0000) >> 16);
          g = ((pixels[4] & 0x0000ff00) >> 8);
          b = (pixels[4] & 0x000000ff);

          int pix2BrightNess = (int) (0.299f*r + 0.587f*g + 0.114f*b);

          if(Math.abs(pix1BrightNess-pix2BrightNess)>130){
            CamMode.suggestMode = CamMode.Mode.HDR;
            break;
          }
        }
      }

      if(CamSetting.isAiSceneOpend){
        CamMode.mode = CamMode.suggestMode;
      }

      if(onImageDetectedListener!=null){
        onImageDetectedListener.onSceneDetected("brightNess: "+brightNess+
                "\ntime: "+ (float)exposureTime/ONE_SECOND +
                "\niso: "+ iso +
                "\nisoBoost: "+ isoBoost +
                "\nae: "+ ae +
                "\nmode: "+ CamMode.mode +
                "\nsuggestMode :"+ CamMode.suggestMode);
      }
    }

    private void unlockFocus() {
      try {
        // Reset the auto-focus trigger
        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);

        cameraCaptureSession.capture(previewRequestBuilder.build(), previewCaptureCallback,
                mBackgroundHandler);

        Camera2Utils.mState = Camera2Utils.STATE_PREVIEW;
        cameraCaptureSession.setRepeatingRequest(previewRequest, previewCaptureCallback,
                mBackgroundHandler);
      } catch (CameraAccessException e) {
        e.printStackTrace();
      }
    }

    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
      if (CamSetting.mFlashSupported) {
        requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
      }
    }

    public void setFocus(Point point){
      int screenW = getWidth();
      int screenH = getHeight();

      Rect size = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
      float realPreviewWidth = size.height();
      float realPreviewHeight = size.width();

      //根据预览像素与拍照最大像素的比例，调整手指点击的对焦区域的位置
      float focusX = (float) realPreviewWidth / screenW * point.x;
      float focusY = (float) realPreviewHeight / screenH * point.y;

      Rect totalPicSize = previewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION);

      Log.e("CFocus","x"+focusX+"y"+focusY);
      float cutDx = 0;//(totalPicSize.height() - size.height()) / 2;
      Rect rect2 = new Rect((int)focusY,
              (int)realPreviewWidth - (int)focusX,
              (int)(focusY + 1000),
              (int)realPreviewWidth - (int)(focusX) + 1000);

      Log.e("CFocus","l:"+rect2.left+"t:"+rect2.top);

      previewRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{new MeteringRectangle(rect2,1000)});
      previewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{new MeteringRectangle(rect2,1000)});
      previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_START);
      previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
      try {
        if(cameraCaptureSession!=null) {
          cameraCaptureSession.setRepeatingRequest(previewRequestBuilder.build(), previewCaptureCallback, mBackgroundHandler);
        }
      } catch (CameraAccessException e) {
        e.printStackTrace();
      }
    }

    public void setBuilderFocus(Point point, CaptureRequest.Builder builder){
      int screenW = getWidth();
      int screenH = getHeight();

      Rect size = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
      float realPreviewWidth = size.height();
      float realPreviewHeight = size.width();

      //根据预览像素与拍照最大像素的比例，调整手指点击的对焦区域的位置
      float focusX = (float) realPreviewWidth / screenW * point.x;
      float focusY = (float) realPreviewHeight / screenH * point.y;

      Log.e("CFocus","x"+focusX+"y"+focusY);
      float cutDx = 0;//(totalPicSize.height() - size.height()) / 2;
      Rect rect2 = new Rect((int)focusY,
              (int)realPreviewWidth - (int)focusX,
              (int)(focusY + 1000),
              (int)realPreviewWidth - (int)(focusX) + 1000);

      builder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{new MeteringRectangle(rect2,1000)});
      builder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{new MeteringRectangle(rect2,1000)});
    }

    public void fallBackFocus() {
      previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
      previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
      previewRequest = previewRequestBuilder.build();
      try {
        if(cameraCaptureSession!=null) {
          cameraCaptureSession.setRepeatingRequest(previewRequest, null, mBackgroundHandler);
        }
      } catch (CameraAccessException e) {
        e.printStackTrace();
      }
    }

    public void setScaleTime(float time){
      mScaleTime = time;
      Rect rectSensor = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
      int pixW = rectSensor.width();
      int pixH = rectSensor.height();
      previewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION,new Rect(
              (int)((pixW-pixW/mScaleTime)*0.5),
              (int)((pixH-pixH/mScaleTime)*0.5),
              (int)((pixW+pixW/mScaleTime)*0.5),
              (int)((pixH+pixH/mScaleTime)*0.5)));

      previewRequest = previewRequestBuilder.build();
      try {
        if(cameraCaptureSession!=null) {
          cameraCaptureSession.setRepeatingRequest(previewRequest, null, null);
        }
        if(scalerChangedListener != null){
          scalerChangedListener.onScalerChanged(mScaleTime);
        }
      } catch (CameraAccessException e) {
        e.printStackTrace();
      }

    }
  }

}
