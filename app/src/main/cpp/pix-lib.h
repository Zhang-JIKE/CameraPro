//
// Created by 18793 on 2020/6/16.
//
#include "pixformula.h"
#include <opencv2/opencv.hpp>
#include <opencv2/photo.hpp>
#include <opencv2/imgcodecs.hpp>
#include <opencv2/highgui.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/features2d.hpp>
#include <opencv2/core/core.hpp>

using namespace std;
using namespace cv;


Mat findHighLight(Mat src) {
    Mat dst;
    threshold(src, dst, 200, 255, THRESH_BINARY);
    return dst;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_jike_camerapro_pixfomula_PixFormula_getBrightness(JNIEnv *env, jclass clazz,
                                                           jobject bitmap) {
    AndroidBitmapInfo info;

    unsigned char* pixel;

    AndroidBitmap_getInfo(env, bitmap, &info);

    AndroidBitmap_lockPixels(env, bitmap, reinterpret_cast<void **>(&pixel));

    float sum = 0;

    for(int i = 0; i < info.width*info.height*4; i+=4) {
        int r = pixel[i];
        int g = pixel[i + 1];
        int b = pixel[i + 2];
        sum += 0.299f*r + 0.587f*g + 0.114f*b;
    }

    sum /= (info.width*info.height);
    AndroidBitmap_unlockPixels(env,bitmap);

    return (int)sum;
}
