//
// Created by 18793 on 2020/5/2.
//
#ifndef CAMERAPRO_PIXFORMULA_H
#define CAMERAPRO_PIXFORMULA_H


#include <jni.h>
#include <arm_neon.h>
#include <string>
#include <time.h>
#include <math.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <iostream>
#include <fstream>
#include <vector>
#define   __NEON__
#define   __NEON_ASM__


#define TAG    "myhello-jni-test"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,TAG ,__VA_ARGS__)

//extern "C" void pix_avg_neon(uint8_t *dest, uint8_t *src, int n) __asm__("pix_avg_neon");

/*
void neon_average2 (uint8_t * __restrict src1, uint8_t * __restrict src2, int n)
{
    for(int i = 0 ; i < n; i ++) {

        uint8x8x3_t v1 = vld3_u8 (src1);
        uint8x8x3_t v2 = vld3_u8 (src2);
        //r
        v1.val[0] = vadd_u8(v1.val[0]/2,v2.val[0]/2);
        //g
        v1.val[1] = vadd_u8(v1.val[1]/2,v2.val[1]/2);
        //b
        v1.val[2] = vadd_u8(v1.val[2]/2,v2.val[2]/2);

        vst3_u8(src1, v1);

        src1 += 4;
        src2 += 4;
    }

}*/

long get_current_ms() {
    struct timespec res;
    clock_gettime(CLOCK_REALTIME, &res);
    return 1000 * res.tv_sec + res.tv_nsec / 1e6;
}

int getColor(int idx, int *pixels){
    return pixels[idx] + 2 * pixels[idx + 1] + 3 * pixels[idx + 2];
}

void pointColor(int idx, int *pixels){
    pixels[idx] = 255;
    pixels[idx+1] = 0;
    pixels[idx+2] = 0;
}

bool isContainedLastValue(int *set, int size ,int offset){
    for(int i = 0; i < size - 1; i ++){
        if(abs(set[i] - set[size]) < offset)
            return true;
    }
    return false;
}

void feature_pointing(int* &src1, int w, int h, int psize ,int fpnum){

    int *rows = new int[fpnum];
    int *cols = new int[fpnum];
    int *fv1 = new int[fpnum];

    srand((unsigned)time(NULL));

    for(int i = 0; i < fpnum; i++){

        rows[i] = rand() % (h-psize);

        cols[i] = rand() % (w-psize);

        if(i>0 && (isContainedLastValue(rows,i,psize) || isContainedLastValue(cols,i,psize))){
            i --;
            continue;
        }

        LOGD("POINT r%d ,c%d",rows[i],cols[i]);

        int loopCount = 0;
        for(int r = 0; r < psize; r++){
            for(int c = 0; c < psize; c++){
                int row = rows[i] + r;
                int col = cols[i] + c;
                int idx = (row * w + col) * 4;

                fv1[i] += (loopCount + 1) * getColor(idx, src1);
                pointColor(idx, src1);
                loopCount++;
            }
        }
    }
}


int red(int pixel){
    return (pixel >> 16) & 0xFF;
}

int green(int pixel){
    return (pixel >> 8) & 0xFF;
}

int blue(int pixel){
    return pixel & 0xFF;
}

int rgba2pixel(int r, int g, int b, int a){
    return (a << 24) + (r << 16) + (g << 8) + b;
}

int rgb2pixel(int r, int g, int b){
    if(r > 255){
        r = 255;
    }
    if(g > 255){
        g = 255;
    }
    if(b > 255){
        b = 255;
    }
    return (r << 16) + (g << 8) + b;
}

#endif CAMERAPRO_PIXFORMULA_H
