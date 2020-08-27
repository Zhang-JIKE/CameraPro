
#pragma version(1)
#pragma rs java_package_name(com.jike.camerapro)
#pragma rs_fp_relaxed

rs_allocation gCurrentFrame;
rs_allocation gPrevFrame;

int gCutPointX = 0;
int gDoMerge = 0;
int gFrameCounter = 0;

uchar4 __attribute__((kernel)) mergeHdrFrames(uchar4 prevPixel, uint32_t x, uint32_t y) {

    // Read in pixel values from latest frame - YUV color space

    uchar4 curPixel;
    curPixel.r = rsGetElementAtYuv_uchar_Y(gCurrentFrame, x, y);
    curPixel.g = rsGetElementAtYuv_uchar_U(gCurrentFrame, x, y);
    curPixel.b = rsGetElementAtYuv_uchar_V(gCurrentFrame, x, y);
    curPixel.a = 255;

    uchar4 mergedPixel;

    mergedPixel = curPixel / 2 + prevPixel / 2;

    // Experimental color saturation boosting merge
    mergedPixel.r = curPixel.r / 2 + prevPixel.r / 2;

    // Convert YUV to RGB, JFIF transform with fixed-point math
    // R = Y + 1.402 * (V - 128)
    // G = Y - 0.34414 * (U - 128) - 0.71414 * (V - 128)
    // B = Y + 1.772 * (U - 128)

    int4 rgb;
    rgb.r = mergedPixel.r +
            mergedPixel.b * 1436 / 1024 - 179;
    rgb.g = mergedPixel.r -
            mergedPixel.g * 46549 / 131072 + 44 -
            mergedPixel.b * 93604 / 131072 + 91;
    rgb.b = mergedPixel.r +
            mergedPixel.g * 1814 / 1024 - 227;
    rgb.a = 255;

    // Store current pixel for next frame
    rsSetElementAt_uchar4(gPrevFrame, curPixel, x, y);

    // Write out merged HDR result
    uchar4 out = convert_uchar4(clamp(rgb, 0, 255));

    return out;
}
