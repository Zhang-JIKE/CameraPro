package com.jike.camerapro.cameradata;

import android.content.SharedPreferences;

import com.daily.flexui.util.AppContextUtils;

import static android.content.Context.MODE_PRIVATE;
import static com.jike.camerapro.utils.DataUtils.saveSettings;

public class CamSetting {

    public static final String IS_CLICK_SOUND_OPENED ="IS_CLICK_SOUND_OPENED";
    public static final String IS_GEO_OPENED ="IS_GEO_OPENED";
    public static final String IS_MIRROR_OPENED ="IS_MIRROR_OPENED";
    public static final String IS_LINE_OPENED ="IN_LINE_OPENED";
    public static final String IS_FACE_DETECT_OPENED ="IN_FACE_DETECT_OPENED";
    public static final String IS_AI_SCENE_OPENED ="IS_AI_SCENE_OPENED";
    public static final String IS_YUV ="IS_YUV";

    public static boolean isClickSoundsOpened = true;
    public static boolean isGeoOpened = true;
    public static boolean isMirrorOpend = true;

    public static boolean isLineOpend = true;
    public static boolean isFaceDetectOpend = false;
    public static boolean isAiSceneOpend = false;
    public static boolean isYuv = false;
    public static boolean mFlashSupported = false;

    public static void initSettings(){
        SharedPreferences mSpf = AppContextUtils.getAppContext().getSharedPreferences("Settings",MODE_PRIVATE);
        isClickSoundsOpened = mSpf.getBoolean(IS_CLICK_SOUND_OPENED, true);
        isGeoOpened = mSpf.getBoolean(IS_GEO_OPENED, true);
        isMirrorOpend = mSpf.getBoolean(IS_MIRROR_OPENED, true);

        isLineOpend = mSpf.getBoolean(IS_LINE_OPENED, true);
        isFaceDetectOpend = mSpf.getBoolean(IS_FACE_DETECT_OPENED, false);
        isAiSceneOpend = mSpf.getBoolean(IS_AI_SCENE_OPENED, false);
        isYuv = mSpf.getBoolean(IS_YUV, false);
    }

    public static void setIsClickSoundsOpened(boolean isClickSoundsOpened) {
        CamSetting.isClickSoundsOpened = isClickSoundsOpened;
        saveSettings(IS_CLICK_SOUND_OPENED, isClickSoundsOpened);
    }

    public static void setIsGeoOpened(boolean isGeoOpened) {
        CamSetting.isGeoOpened = isGeoOpened;
        saveSettings(IS_GEO_OPENED, isGeoOpened);
    }

    public static void setIsMirrorOpend(boolean isMirrorOpend) {
        CamSetting.isMirrorOpend = isMirrorOpend;
        saveSettings(IS_MIRROR_OPENED, isMirrorOpend);
    }

    public static void setIsLineOpend(boolean isLineOpend) {
        CamSetting.isLineOpend = isLineOpend;
        saveSettings(IS_LINE_OPENED, isLineOpend);
    }

    public static void setIsFaceDetectOpend(boolean isFaceDetectOpend) {
        CamSetting.isFaceDetectOpend = isFaceDetectOpend;
        saveSettings(IS_FACE_DETECT_OPENED, isFaceDetectOpend);
    }

    public static void setIsAiSceneOpend(boolean isAiSceneOpend) {
        CamSetting.isAiSceneOpend = isAiSceneOpend;
        saveSettings(IS_AI_SCENE_OPENED, isAiSceneOpend);
        if(!isAiSceneOpend){
            CamMode.mode = CamMode.Mode.NORMAL;
        }
    }

    public static void setIsYuv(boolean isYuv){
        CamSetting.isYuv = isYuv;
        saveSettings(IS_YUV, isYuv);
    }

}
