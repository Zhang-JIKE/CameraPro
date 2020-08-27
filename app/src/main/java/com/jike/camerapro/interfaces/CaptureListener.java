package com.jike.camerapro.interfaces;

public interface CaptureListener{
    void onStartToCapture();
    void onCaptureFinished();
    void onStartToCalculate();
    void onCalculateFinished();
}