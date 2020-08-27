package com.jike.camerapro.cameradata;

public class CamMode {

    public enum Mode {NORMAL,HDR,PIX_FUSION,SUPER_RESOLUTION,NIGHT};

    public static Mode mode = Mode.NORMAL;
    public static Mode suggestMode = Mode.NORMAL;
}
