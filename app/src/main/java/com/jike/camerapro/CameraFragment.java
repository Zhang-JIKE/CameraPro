package com.jike.camerapro;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.daily.flexui.util.AppContextUtils;
import com.daily.flexui.view.CircleImageView;
import com.google.android.material.tabs.TabLayout;
import com.jike.camerapro.cameradata.CamMode;
import com.jike.camerapro.cameradata.CamSetting;
import com.jike.camerapro.interfaces.OnHandFocusListener;
import com.jike.camerapro.interfaces.OnImageDetectedListener;
import com.jike.camerapro.interfaces.ScalerChangedListener;
import com.jike.camerapro.view.CameraView;
import com.jike.camerapro.view.ShutterView;

import java.text.DecimalFormat;

public class CameraFragment extends Fragment implements View.OnClickListener {

    final int[] pos = {6};
    private float oldX;
    private float oldY;

    private boolean isMultipleFingerDown = false;
    private float oldX1,oldX2,oldY1,oldY2;

    private CameraView cameraView;

    private ImageView ivFlash;
    private ImageView ivHdr;
    private ImageView ivFilter;
    private ImageView ivSuperRes;
    private ImageView ivSettings;

    private ShutterView shutterView;

    private FrameLayout textureLayout;
    private CircleImageView ivPicture;
    private TextView tvInfo;
    private TextView tvScaler;
    private TextView tips;

    private boolean hdr = false;
    private float oldScale = 0;

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            if(msg.what == 1){
                tvInfo.setText(msg.obj.toString());
            }
        }
    };

    private String[] tabs = new String[]{"","","","Night", "Picture", "Fusion","","",""};
    private View[] views = new View[9];

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            cameraView.openCamera(width, height);
            cameraView.setUpTexturePadding(textureLayout);

            cameraView.setControllerView(textureLayout);
            cameraView.cameraControllerView.setOnHandFocusListener(new OnHandFocusListener() {
                @Override
                public void onHandFocus(Point point) {
                    cameraView.camera2Helper.setFocus(point);
                }

                @Override
                public void onFocusFallBack() {
                    cameraView.camera2Helper.fallBackFocus();
                }
            });

            cameraView.startBackgroundThread();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            cameraView.setUpTexturePadding(textureLayout);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    public static CameraFragment newInstance() {
        return new CameraFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        initView(view);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (cameraView.isAvailable()) {
            cameraView.openCamera(cameraView.getWidth(), cameraView.getHeight());
            cameraView.setUpTexturePadding(textureLayout);
            cameraView.setControllerView(textureLayout);
            cameraView.startBackgroundThread();
        } else {
            cameraView.setSurfaceTextureListener(mSurfaceTextureListener);
        }

        cameraView.mScaleTime = 1;

        DecimalFormat mFormat = new DecimalFormat(".0");
        String formatNum = mFormat.format(cameraView.mScaleTime);
        if(formatNum.contains(".0")){
            formatNum=formatNum.substring(0,formatNum.indexOf("."));
        }
        tvScaler.setText(formatNum+"x");
    }

    @Override
    public void onPause() {
        super.onPause();
        cameraView.closeCamera();
        cameraView.stopBackgroundThread();
    }

    private void initView(View view){
        tips = view.findViewById(R.id.tips);
        tvInfo = view.findViewById(R.id.information);
        tvScaler = view.findViewById(R.id.scaler);

        cameraView = (CameraView) view.findViewById(R.id.texture);

        cameraView.setScalerChangedListener(new ScalerChangedListener() {
            @Override
            public void onScalerChanged(float scalerTime) {
                DecimalFormat  mFormat = new DecimalFormat(".0");

                String formatNum = mFormat.format(scalerTime);
                if(formatNum.contains(".0")){
                    formatNum=formatNum.substring(0,formatNum.indexOf("."));
                }
                tvScaler.setText(formatNum+"x");
            }
        });

        cameraView.setOnImageDetectedListener(new OnImageDetectedListener() {
            @Override
            public void onSceneDetected(String tag) {
                Message message = Message.obtain();
                message.what = 1;
                message.obj = tag;
                handler.sendMessage(message);
            }

            @Override
            public void onFaceDetected() {

            }
        });

        textureLayout = view.findViewById(R.id.texture_container);
        ivPicture = view.findViewById(R.id.iv_picture);

        ivFlash = view.findViewById(R.id.iv_flash);
        ivHdr = view.findViewById(R.id.iv_hdr);
        ivFilter = view.findViewById(R.id.iv_filter);
        ivSuperRes = view.findViewById(R.id.iv_super_res);
        ivSettings = view.findViewById(R.id.iv_settings);
        shutterView = view.findViewById(R.id.shutter);

        ivSettings.setOnClickListener(this);
        ivHdr.setOnClickListener(this);
        ivSuperRes.setOnClickListener(this);
        shutterView.setOnClickListener(this);

        cameraView.setShutterView(shutterView);
        cameraView.setTips(tips);

        initTab(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initTab(View view){
        final TabLayout tabLayout = view.findViewById(R.id.tablayout);
        cameraView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    oldX = event.getX();
                    oldY = event.getY();
                }else if(event.getAction() == MotionEvent.ACTION_MOVE){
                    if(event.getPointerCount()==2){
                        if(!isMultipleFingerDown){
                            oldX1 = event.getX(0);
                            oldX2 = event.getX(1);

                            oldY1 = event.getY(0);
                            oldY2 = event.getY(1);

                            isMultipleFingerDown = true;
                        }
                        float dx = event.getX(0) - event.getX(1);
                        float dy = event.getY(0) - event.getY(1);
                        float odx = oldX1 - oldX2 ;
                        float ody = oldY1 - oldY2 ;

                        int vW = cameraView.viewWidth;
                        int vH = cameraView.viewHeight;
                        float value = 10*(float)(Math.sqrt(dx * dx + dy * dy)-Math.sqrt(odx * odx + ody * ody))/(float) Math.sqrt(vW * vW + vH * vH);
                        float scaleTime = value + oldScale;
                        if(scaleTime<1) {
                            scaleTime = 1;
                        }
                        if(scaleTime>10){
                            scaleTime = 10;
                        }
                        cameraView.camera2Helper.setScaleTime(scaleTime);
                    }
                }else if(event.getAction() == MotionEvent.ACTION_UP) {
                    if (isMultipleFingerDown) {
                        isMultipleFingerDown = false;
                    } else {
                        float deltaX = event.getX() - oldX;
                        float deltaY = event.getY() - oldY;
                        if (Math.abs(deltaX) < 10 && Math.abs(deltaY) < 10) {
                            cameraView.cameraControllerView.setControlledFocus(new Point((int) event.getX(), (int) event.getY()));
                        } else if (Math.abs(deltaX) > Math.abs(deltaY)) {
                            if (deltaX > 10)
                            {
                                if (pos[0] > 3) {
                                    int postion = --pos[0];
                                    tabLayout.selectTab(tabLayout.getTabAt(postion));
                                }

                            } else if (deltaX < -10) {
                                if (pos[0] < 5) {
                                    int postion = ++pos[0];
                                    tabLayout.selectTab(tabLayout.getTabAt(postion));
                                }
                            }
                        }
                    }
                    oldScale = cameraView.mScaleTime;
                }
                return true;
            }
        });
        for(int i = 0; i < tabs.length; i++) {
            //tablayout.addTab(tablayout.newTab().setText(tab));
            String title = tabs[i];
            TabLayout.Tab tab = tabLayout.newTab();
            View inflate = View.inflate(getContext(), R.layout.view_tab, null);
            TextView textView = inflate.findViewById(R.id.text);
            textView.setText(title);
            if(i == 0 ||i == 1 ||i == 2 ||i == 6 ||i == 7 ||i == 8){
                inflate.setVisibility(View.GONE);
            }
            tab.setCustomView(inflate);
            views[i] = inflate;
            tabLayout.addTab(tab);
        }


        tabLayout.getTabAt(1).view.setClickable(false);
        tabLayout.getTabAt(2).view.setClickable(false);
        tabLayout.getTabAt(6).view.setClickable(false);
        tabLayout.getTabAt(7).view.setClickable(false);
        tabLayout.getTabAt(8).view.setClickable(false);

        tabLayout.post(new Runnable() {
            @Override
            public void run() {
                tabLayout.getTabAt(4).select();
            }
        });

        tabLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tabLayout.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                pos[0] = tab.getPosition();

                if(pos[0] == 3){
                    setNightOn();
                }else if(pos[0] == 4){
                    setNormal();
                }else if(pos[0] == 5){
                    setSuperResOn();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {


            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == ivFlash.getId()){

        } else if(id == ivHdr.getId()){

            if (CamMode.mode != CamMode.Mode.HDR) {
                setHdrOn();
            } else {
                setNormal();
            }

        } else if(id == ivFilter.getId()){

        } else if(id == ivSuperRes.getId()){

            if(CamMode.mode != CamMode.Mode.PIX_FUSION){
                setSuperResOn();
            }else {
                setNormal();
            }

        } else if(id == ivSettings.getId()){

            Intent intent = new Intent(AppContextUtils.getAppContext(), SettingsActivity.class);
            startActivity(intent);

        } else if(id == shutterView.getId()){
            if (shutterView.isEnabled) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_RELEASE);
                MediaPlayer player1 = MediaPlayer.create(getContext(), R.raw.camera_click_short);
                player1.start();

                cameraView.takePicture();

                if(CamMode.mode != CamMode.Mode.NORMAL) {
                    shutterView.startCapture();
                }
            }
        }
    }

    private void setHdrOn(){
        setAllOff();
        ivHdr.setImageResource(R.drawable.ic_hdr_on);
        CamSetting.setIsAiSceneOpend(false);
        CamMode.mode = CamMode.Mode.HDR;
    }

    private void setSuperResOn(){
        setAllOff();
        ivSuperRes.setImageResource(R.drawable.ic_super_res_on);
        CamSetting.setIsAiSceneOpend(false);
        CamMode.mode = CamMode.Mode.PIX_FUSION;
    }

    private void setNightOn(){
        setAllOff();
        ivFlash.setVisibility(View.INVISIBLE);
        ivHdr.setVisibility(View.INVISIBLE);
        ivFilter.setVisibility(View.INVISIBLE);
        ivSuperRes.setVisibility(View.INVISIBLE);
        CamSetting.setIsAiSceneOpend(false);
        CamMode.mode = CamMode.Mode.NIGHT;
    }

    private void setAllOff(){
        ivHdr.setImageResource(R.drawable.ic_hdr_off);
        ivSuperRes.setImageResource(R.drawable.ic_super_res_off);
    }

    private void setNormal(){
        setAllOff();
        ivFlash.setVisibility(View.VISIBLE);
        ivHdr.setVisibility(View.VISIBLE);
        ivFilter.setVisibility(View.VISIBLE);
        ivSuperRes.setVisibility(View.VISIBLE);
        CamMode.mode = CamMode.Mode.NORMAL;
    }

    private void setSuperResolution(){
        setAllOff();
        ivFlash.setVisibility(View.VISIBLE);
        ivHdr.setVisibility(View.VISIBLE);
        ivFilter.setVisibility(View.VISIBLE);
        ivSuperRes.setVisibility(View.VISIBLE);
        CamSetting.setIsAiSceneOpend(false);
        CamMode.mode = CamMode.Mode.SUPER_RESOLUTION;
    }
}
