#include <queue>
#include "pix-lib.h"

#define MAX_FEATURES 500
//好的特征点数
#define GOOD_MATCH_PERCENT 0.15f
struct thread_mat{
    Mat mat1;
    Mat mat2;
};

Mat ImageEnhance(Mat src){
    //基于对数Log变换的图像增强
    /*Mat dst(src.size(), CV_32FC3);
    for (int i = 0; i < src.rows; i++)
    {
        for (int j = 0; j < src.cols; j++)
        {
            dst.at<Vec3f>(i, j)[0] = log(1 + src.at<Vec3b>(i, j)[0]);
            dst.at<Vec3f>(i, j)[1] = log(1 + src.at<Vec3b>(i, j)[1]);
            dst.at<Vec3f>(i, j)[2] = log(1 + src.at<Vec3b>(i, j)[2]);
        }
    }
    //归一化到0~255
    normalize(dst, dst, 0, 255);
    //转换成8bit图像显示
    convertScaleAbs(dst, dst);
    return dst;*/

    //基于拉普拉斯算子的图像增强
    /*Mat imageEnhance;
    Mat kernel = (Mat_<float>(3, 3) << 0, -1, 0, 0, 5, 0, 0, -1, 0);
    filter2D(src, imageEnhance, CV_8UC3, kernel);
    return imageEnhance;*/
    //基于直方图均衡化的图像增强

    /*cvtColor(src, src, COLOR_BGR2HSV);
    Mat imageRGB[3];
    split(src, imageRGB);
   *//* for (int i = 0; i < 3; i++)
    {
        equalizeHist(imageRGB[2], imageRGB[2]);
    }*//*
    equalizeHist(imageRGB[2], imageRGB[2]);
    merge(imageRGB, 3, src);
    cvtColor(src, src, COLOR_HSV2BGR);
    return src;*/
}


bool comp(vector<DMatch>& a,vector<DMatch>& b)
{
    return a[0].distance/a[1].distance < b[0].distance/b[1].distance;
}

void refineMatchesWithHomography(
        const std::vector<cv::KeyPoint>& queryKeypoints,
        const std::vector<cv::KeyPoint>& trainKeypoints,
        std::vector<cv::DMatch>& matches,
        cv::Mat& homography){

    // Prepare data for cv::findHomography
    std::vector<cv::Point2f> srcPoints(matches.size());
    std::vector<cv::Point2f> dstPoints(matches.size());

    for (size_t i = 0; i < matches.size(); i++) {
        srcPoints[i] = trainKeypoints[matches[i].trainIdx].pt;
        dstPoints[i] = queryKeypoints[matches[i].queryIdx].pt;
    }

    //std::vector<unsigned char> inliersMask(srcPoints.size());
    //homography = cv::findHomography(srcPoints, dstPoints, RANSAC,reprojectionThreshold, inliersMask);
    homography = cv::findHomography(srcPoints, dstPoints, RANSAC,4);
/*
    std::vector<cv::DMatch> inliers;
    for (size_t i = 0; i < inliersMask.size(); i++) {
        if (inliersMask[i])
            inliers.push_back(matches[i]);
    }
    matches.swap(inliers);*/
}

void alignImages(Mat &im1, Mat &im2, Mat &im1Reg)
{
    // Convert images to grayscale
    Mat im1Gray, im2Gray;
    //转换为灰度图
    cvtColor(im1, im1Gray, COLOR_BGR2GRAY);
    cvtColor(im2, im2Gray, COLOR_BGR2GRAY);

    // Variables to store keypoints and descriptors
    //关键点
    std::vector<KeyPoint> keypoints1, keypoints2;
    //特征描述符
    Mat descriptors1, descriptors2;

    // Detect ORB features and compute descriptors. 计算ORB特征和描述子
    Ptr<Feature2D> orb = ORB::create(MAX_FEATURES);
    orb->detectAndCompute(im1Gray, Mat(), keypoints1, descriptors1);
    orb->detectAndCompute(im2Gray, Mat(), keypoints2, descriptors2);

    // Match features. 特征点匹配
    std::vector<DMatch> matches;
    //汉明距离进行特征点匹配
    Ptr<DescriptorMatcher> matcher = DescriptorMatcher::create("BruteForce-Hamming");
    matcher->match(descriptors1, descriptors2, matches, Mat());

    // Sort matches by score 按照特征点匹配结果从优到差排列
    std::sort(matches.begin(), matches.end());

    // Remove not so good matches 移除不好的特征点
    const int numGoodMatches = matches.size() * GOOD_MATCH_PERCENT;
    matches.erase(matches.begin() + numGoodMatches, matches.end());

    // Draw top matches
    Mat imMatches;
    //画出特征点匹配图
    drawMatches(im1, keypoints1, im2, keypoints2, matches, imMatches);
    imwrite("matches.jpg", imMatches);

    // Extract location of good matches
    std::vector<Point2f> points1, points2;

    //保存对应点
    for (size_t i = 0; i < matches.size(); i++)
    {
        //queryIdx是对齐图像的描述子和特征点的下标。
        points1.push_back(keypoints1[matches[i].queryIdx].pt);
        //queryIdx是是样本图像的描述子和特征点的下标。
        points2.push_back(keypoints2[matches[i].trainIdx].pt);
    }

    // Find homography 计算Homography，RANSAC随机抽样一致性算法
    Mat h = findHomography(points1, points2, RANSAC);

    // Use homography to warp image 映射
    warpPerspective(im1, im1Reg, h, im2.size());
}

void MatAlignORB(Mat img1,Mat &img2){

    long t1 = get_current_ms();

    Mat im1_gray, im2_gray;

    cvtColor(img1,im1_gray,COLOR_BGR2GRAY);
    cvtColor(img2,im2_gray,COLOR_BGR2GRAY);

    vector<KeyPoint> keypoints1,keypoints2;
    Mat descriptors1, descriptors2;
    Ptr<Feature2D> orb = ORB::create(400);

    orb->detect(im1_gray,keypoints1);
    orb->detect(im2_gray,keypoints2);
    orb->detectAndCompute(im1_gray,Mat(),keypoints1,descriptors1);
    orb->detectAndCompute(im2_gray,Mat(),keypoints2,descriptors2);
    BFMatcher matcher(NORM_HAMMING,true); //汉明距离做为相似度度量
    vector<DMatch> matches;
    matcher.match(descriptors1, descriptors2, matches);
    Mat match_img;
    drawMatches(im1_gray,keypoints1,im2_gray,keypoints2,matches,match_img);
    //保存匹配对序号
    vector<int> queryIdxs( matches.size() ), trainIdxs( matches.size() );
    for( size_t i = 0; i < matches.size(); i++ ){
        queryIdxs[i] = matches[i].queryIdx;
        trainIdxs[i] = matches[i].trainIdx;
    }
    Mat H12;   //变换矩阵
    vector<Point2f> points1; KeyPoint::convert(keypoints1, points1, queryIdxs);
    vector<Point2f> points2; KeyPoint::convert(keypoints2, points2, trainIdxs);
    int ransacReprojThreshold = 1;  //拒绝阈值

    H12 = findHomography( Mat(points1), Mat(points2), RANSAC, 5 );  //寻找目标到检测场景的变换进行筛选内点

    Mat imageTransform1;
    warpPerspective(im2_gray,imageTransform1,H12,Size(im1_gray.cols,im1_gray.rows));

    int n=0;
    vector<char> matchesMask( matches.size(), 0 );
    Mat points1t;
    vector<Point2f> imagePoints1,imagePoints2;
    perspectiveTransform(Mat(points1), points1t, H12);

    for( size_t i = 0; i < points1.size(); i++ )  //保存‘内点’
    {
        if( norm(points2[i] - points1t.at<Point2f>((int)i,0)) <= ransacReprojThreshold ) //给内点做标记,变换前后的距离进行筛选
        {
            matchesMask[i] = 1;

            imagePoints1.push_back(keypoints1[matches[i].queryIdx].pt);
            imagePoints2.push_back(keypoints2[matches[i].trainIdx].pt);
            n++;
        }
    }
    Mat match_img2;   //滤除‘外点’后
    drawMatches(im1_gray,keypoints1,im2_gray,keypoints2,matches,match_img2,Scalar(0,0,255),Scalar::all(-1),matchesMask);
    vector<Point2f> imagePoints11,imagePoints22;
    for(int i=0;i<50;i++){
        imagePoints11.push_back(keypoints1[matches[i].queryIdx].pt);
        imagePoints22.push_back(keypoints2[matches[i].trainIdx].pt);
    }
    Mat homo=findHomography(Mat(imagePoints22),Mat(imagePoints11),RANSAC);
    //图像配准
    warpPerspective(img2,imageTransform1,homo,Size(im1_gray.cols,im1_gray.rows));
    long t2 = get_current_ms();
    LOGD("MatAlign对齐完成，%d", t2 - t1);
}

void MatAlignSIFT(Mat img1,Mat &img2){

    long t1 = get_current_ms();

    Mat im1_gray, im2_gray;

    cvtColor(img1,im1_gray,COLOR_BGR2GRAY);
    cvtColor(img2,im2_gray,COLOR_BGR2GRAY);

    //resize(im1_gray,im1_gray,Size(im1_gray.cols/2, im1_gray.rows/2),INTER_AREA);
    //resize(im2_gray,im2_gray,Size(im2_gray.cols/2, im2_gray.rows/2),INTER_AREA);


    vector<KeyPoint> keypoints1,keypoints2;
    Mat descriptors1, descriptors2;
    //Ptr<SiftFeatureDetector> sift = SiftFeatureDetector::create(100);

    Ptr<Feature2D> sift = SIFT::create(0,1,0.04,10);

    sift->detectAndCompute(im1_gray,noArray(),keypoints1,descriptors1);
    sift->detectAndCompute(im2_gray,noArray(),keypoints2,descriptors2);
    vector<DMatch>matches;
    vector<vector<DMatch> >Dmatches;
    Ptr<cv::DescriptorMatcher> matcher_knn = new BFMatcher();
    Ptr<cv::DescriptorMatcher> matcher = new BFMatcher(NORM_L2,true);
    matcher->match(descriptors1,descriptors2,matches);

    matcher_knn->knnMatch(descriptors1,descriptors2,Dmatches,2);
    sort(Dmatches.begin(),Dmatches.end(),comp);

    vector<DMatch> good;
    for(int i=0;i<Dmatches.size();i++){
        if(Dmatches[i][0].distance < 0.5*Dmatches[i][1].distance)
            good.push_back(Dmatches[i][0]);
    }

    vector<Point2f> srcPoints(matches.size());
    vector<Point2f> dstPoints(matches.size());

    for (size_t i = 0; i < matches.size(); i++) {
        srcPoints[i] = keypoints2[matches[i].trainIdx].pt;
        dstPoints[i] = keypoints1[matches[i].queryIdx].pt;
    }
    Mat homo;

    homo = findHomography(srcPoints, dstPoints, RANSAC,4);

    //refineMatchesWithHomography(keypoints1, keypoints2, matches, homo);
    warpPerspective(img2,img2,homo,Size(im1_gray.cols,im1_gray.rows));

    long t2 = get_current_ms();
    LOGD("MatAlign对齐完成，%d", t2 - t1);
}

void MatAlignSIFT2(Mat img1,Mat &img2){
    long t1 = get_current_ms();


    Mat im1_gray, im2_gray;

    cvtColor(img1,im1_gray,COLOR_BGR2GRAY);
    cvtColor(img2,im2_gray,COLOR_BGR2GRAY);

    //resize(im1_gray,im1_gray,Size(im1_gray.cols/2, im1_gray.rows/2),INTER_AREA);
    //resize(im2_gray,im2_gray,Size(im2_gray.cols/2, im2_gray.rows/2),INTER_AREA);


   /* vector<KeyPoint> keypoints1,keypoints2;
    Mat descriptors1, descriptors2;*/
    Ptr<Feature2D> sift = SIFT::create(0,1,0.04,10);

    vector<KeyPoint> keypoints_1, keypoints_2;
    sift->detect(im1_gray, keypoints_1);
    sift->detect(im2_gray, keypoints_2);
    //第二步,计算特征向量
    Mat descriptors_1, descriptors_2;
    sift->compute(im1_gray, keypoints_1, descriptors_1);
    sift->compute(im2_gray, keypoints_2, descriptors_2);
    // 第三步,用BFMatcher进行匹配特征向量
    BFMatcher matcher;
    vector<DMatch> matches;
    matcher.match(descriptors_1, descriptors_2, matches);
    //第四步,提取出前20个最佳匹配结果
    std::nth_element(matches.begin(),     //匹配器算子的初始位置
                     matches.begin() + 19,   // 排序的数量
                     matches.end());       // 结束位置
    //剔除掉其余的匹配结果
    matches.erase(matches.begin() + 20, matches.end());

    vector<Point2f> srcPoints(matches.size());
    vector<Point2f> dstPoints(matches.size());

    for (size_t i = 0; i < matches.size(); i++) {
        srcPoints[i] = keypoints_2[matches[i].trainIdx].pt;
        dstPoints[i] = keypoints_1[matches[i].queryIdx].pt;
    }
    Mat homo;

    homo = findHomography(srcPoints, dstPoints, RANSAC,4);

    //refineMatchesWithHomography(keypoints1, keypoints2, matches, homo);
    warpPerspective(img2,img2,homo,Size(im1_gray.cols,im1_gray.rows));

    long t2 = get_current_ms();
    LOGD("MatAlign对齐完成，%d", t2 - t1);
}

void MatAlignSURF(Mat img1,Mat &img2){
    long t1 = get_current_ms();

    Ptr<SiftFeatureDetector> detector = SiftFeatureDetector::create(800);
    Mat im1_gray,im2_gray;
    cvtColor(img1,im1_gray,COLOR_BGR2GRAY);
    cvtColor(img2,im2_gray,COLOR_BGR2GRAY);
    vector<cv::KeyPoint> key_points_1, key_points_2;

    Mat dstImage1, dstImage2;
    detector->detectAndCompute(im1_gray, Mat(), key_points_1, dstImage1);
    detector->detectAndCompute(im2_gray, Mat(), key_points_2, dstImage2);//可以分成detect和compute

    /*Mat img_keypoints_1, img_keypoints_2;
    drawKeypoints(srcImage1, key_points_1, img_keypoints_1, Scalar::all(-1), DrawMatchesFlags::DEFAULT);
    drawKeypoints(srcImage2, key_points_2, img_keypoints_2, Scalar::all(-1), DrawMatchesFlags::DEFAULT);*/

    Ptr<DescriptorMatcher> matcher = DescriptorMatcher::create("FlannBased");
    vector<DMatch>mach;

    matcher->match(dstImage1, dstImage2, mach);

    sort(mach.begin(), mach.end()); //特征点排序
    double Max_dist = 0;
    double Min_dist = 100;
    for (int i = 0; i < dstImage1.rows; i++){
        double dist = mach[i].distance;
        if (dist < Min_dist)Min_dist = dist;
        if (dist > Max_dist)Max_dist = dist;
    }

    vector<DMatch>goodmaches;
    for (int i = 0; i < dstImage1.rows; i++){
        if (mach[i].distance < 2 * Min_dist)
            goodmaches.push_back(mach[i]);
    }

    vector<Point2f> imagePoints1, imagePoints2;

    for (int i = 0; i<10; i++){
        imagePoints1.push_back(key_points_1[mach[i].queryIdx].pt);
        imagePoints2.push_back(key_points_2[mach[i].trainIdx].pt);
    }

    Mat homo = findHomography(imagePoints1, imagePoints2, RANSAC);
    Mat imageTransform;
    warpPerspective(img2,imageTransform,homo,Size(im1_gray.cols,im1_gray.rows));

    long t2 = get_current_ms();
    LOGD("MatAlign对齐完成，%d", t2 - t1);
}

void MatAlignSIFT3(Mat img1,Mat &img2){
    long t1 = get_current_ms();

    //读取原始基准图和待匹配图
    //Mat srcImg1 = imread("1.JPG");      //待配准图
    //Mat srcImg2 = imread("2.JPG");      //基准图

    Mat im1_gray, im2_gray;

    cvtColor(img1,im1_gray,COLOR_BGR2GRAY);
    cvtColor(img2,im2_gray,COLOR_BGR2GRAY);

    //定义SIFT特征检测类对象
    SiftFeatureDetector siftDetector1;
    SiftFeatureDetector siftDetector2;

    //定义KeyPoint变量
    vector<KeyPoint>keyPoints1;
    vector<KeyPoint>keyPoints2;

    //特征点检测
    siftDetector1.detect(im1_gray, keyPoints1);
    siftDetector2.detect(im2_gray, keyPoints2);

    //计算特征点描述符 / 特征向量提取
    SiftDescriptorExtractor descriptor;
    Mat description1;
    descriptor.compute(im1_gray, keyPoints1, description1);
    Mat description2;
    descriptor.compute(im2_gray, keyPoints2, description2);

    //进行BFMatch暴力匹配
    //BruteForceMatcher<L2<float>>matcher;    //实例化暴力匹配器
    FlannBasedMatcher matcher;  //实例化FLANN匹配器
    vector<DMatch>matches;   //定义匹配结果变量
    matcher.match(description1, description2, matches);  //实现描述符之间的匹配

    //中间变量
    int i,j,k;double sum=0;double b;

    double max_dist = 0;
    double min_dist = 100;
    for(int i=0; i<matches.size(); i++)
    {
        double dist = matches[i].distance;
        if(dist < min_dist)
            min_dist = dist;
        if(dist > max_dist)
            max_dist = dist;
    }
    cout<<"最大距离："<<max_dist<<endl;
    cout<<"最小距离："<<min_dist<<endl;

    //筛选出较好的匹配点
    vector<DMatch> good_matches;
    double dThreshold = 0.5;    //匹配的阈值，越大匹配的点数越多
    for(int i=0; i<matches.size(); i++)
    {
        if(matches[i].distance < dThreshold * max_dist)
        {
            good_matches.push_back(matches[i]);
        }
    }

    //RANSAC 消除误匹配特征点 主要分为三个部分：
    //1）根据matches将特征点对齐,将坐标转换为float类型
    //2）使用求基础矩阵方法findFundamentalMat,得到RansacStatus
    //3）根据RansacStatus来将误匹配的点也即RansacStatus[i]=0的点删除

    //根据matches将特征点对齐,将坐标转换为float类型
    vector<KeyPoint> R_keypoint01,R_keypoint02;
    for (i=0;i<good_matches.size();i++)
    {
        R_keypoint01.push_back(keyPoints1[good_matches[i].queryIdx]);
        R_keypoint02.push_back(keyPoints2[good_matches[i].trainIdx]);
        // 这两句话的理解：R_keypoint1是要存储img01中能与img02匹配的特征点，
        // matches中存储了这些匹配点对的img01和img02的索引值
    }

    //坐标转换
    vector<Point2f>p01,p02;
    for (i=0;i<good_matches.size();i++)
    {
        p01.push_back(R_keypoint01[i].pt);
        p02.push_back(R_keypoint02[i].pt);
    }

    //计算基础矩阵并剔除误匹配点
    vector<uchar> RansacStatus;
    Mat Fundamental= findHomography(p01,p02,RansacStatus,RANSAC);
    Mat dst;
    warpPerspective(img2, dst, Fundamental,Size(img1.cols,img1.rows));

    long t2 = get_current_ms();
    LOGD("MatAlign对齐完成，%d", t2 - t1);
    //imshow("配准后的图",dst );
    //imwrite("dst.jpg", dst);

    /*//剔除误匹配的点对
    vector<KeyPoint> RR_keypoint01,RR_keypoint02;
    vector<DMatch> RR_matches;            //重新定义RR_keypoint 和RR_matches来存储新的关键点和匹配矩阵
    int index=0;
    for (i=0;i<good_matches.size();i++)
    {
        if (RansacStatus[i]!=0)
        {
            RR_keypoint01.push_back(R_keypoint01[i]);
            RR_keypoint02.push_back(R_keypoint02[i]);
            good_matches[i].queryIdx=index;
            good_matches[i].trainIdx=index;
            RR_matches.push_back(good_matches[i]);
            index++;
        }
    }*/
    //cout<<"找到的特征点对："<<RR_matches.size()<<endl;

    //画出消除误匹配后的图
    //Mat img_RR_matches;
    //drawMatches(srcImg1,RR_keypoint01,srcImg2,RR_keypoint02,RR_matches,img_RR_matches, Scalar(0, 255, 0), Scalar::all(-1));
    //imshow("消除误匹配点后",img_RR_matches);
    //imwrite("匹配图.jpg", img_RR_matches);
}

void MatAlignECC(Mat img1,Mat &img2){

    long t1 = get_current_ms();

    Mat im1_gray, im2_gray;
    //resize(im1_gray, im1_gray, Size(im1_gray.cols*0.5,im1_gray.rows*0.5));
    //resize(im2_gray, im2_gray, Size(im2_gray.cols*0.5,im2_gray.rows*0.5));
    cvtColor(img1, im1_gray, COLOR_BGR2GRAY);
    cvtColor(img2, im2_gray, COLOR_BGR2GRAY);

    const int warp_mode = MOTION_HOMOGRAPHY;
    Mat warp_matrix;
    if (warp_mode == MOTION_HOMOGRAPHY)
        warp_matrix = Mat::eye(3, 3, CV_32F);
    else
        warp_matrix = Mat::eye(2, 3, CV_32F);
    int number_of_iterations = 15;
    double termination_eps = 1e-8;
    TermCriteria criteria(TermCriteria::EPS + TermCriteria::COUNT, number_of_iterations,termination_eps);
    findTransformECC(im1_gray,im2_gray,warp_matrix,warp_mode,criteria);

    if (warp_mode != MOTION_HOMOGRAPHY)
        warpAffine(img2, img2, warp_matrix, img1.size(), INTER_LINEAR + WARP_INVERSE_MAP);
    else
        warpPerspective(img2, img2, warp_matrix, img1.size(), INTER_LINEAR + WARP_INVERSE_MAP);
    //resize(warp_matrix, warp_matrix, Size(warp_matrix.cols*2,warp_matrix.rows*2));
    long t2 = get_current_ms();
    LOGD("MatAlign对齐完成，%d", t2 - t1);
}

double blurCal(Mat src){
    Mat imageGrey;
    cvtColor(src, imageGrey, COLOR_RGB2GRAY);
    Mat imageSobel;

    Laplacian(imageGrey, imageSobel, CV_16U);
    Sobel(imageGrey, imageSobel, CV_16U, 1, 1);

    //图像的平均灰度
    double meanValue = 0.0;
    meanValue = mean(imageSobel)[0];

    return meanValue;
}

void sharpness(Mat &src){
    Mat kernel(3, 3, CV_32F, Scalar(0));
    kernel.at<float>(1, 1) = 5.0;
    kernel.at<float>(0, 1) = -1.0;
    kernel.at<float>(1, 0) = -1.0;
    kernel.at<float>(1, 2) = -1.0;
    kernel.at<float>(2, 1) = -1.0;

    cout << kernel << endl;
    filter2D(src, src, -1, kernel);

}

void *threadMatAlign(void *args) {
    pthread_t myid = pthread_self();
    thread_mat para = (*((thread_mat*)args));
    MatAlignORB(para.mat1,para.mat2);
    pthread_exit(NULL);
}

string operator+(string &content, int number) {
    string temp = "";
    char t = 0;
    while (true) {
        t = number % 10 + '0';
        LOGD("CCCCCCCCCCCC,%c",t);
        temp = t + temp;
        number /= 10;
        if (number == 0) {
            return content + temp;
        }
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_jike_camerapro_pixfomula_PixFormula_cvMatFusion(JNIEnv *env, jclass clazz,
                                                         jlongArray mat_addrs,jstring path) {

    char *fpath = (char *)env->GetStringUTFChars(path, NULL);

    vector<Mat> matv;
    Mat res;

    int size = env->GetArrayLength(mat_addrs);
    jlong *addrs = env->GetLongArrayElements(mat_addrs,NULL);

    for(int i = 0; i < size; i++) {
        Mat mat=(*((Mat*)addrs[i]));
        //resize(mat, mat, Size(mat.cols/2, mat.rows/2), 0, 0, INTER_LINEAR);
        //
        //sharpness(mat);
        matv.push_back(mat);
    }
    /*Ptr<AlignMTB> alignMTB = createAlignMTB();
       alignMTB->process(matv, matv);*/
    for(int i = 1; i < size; i++) {
        MatAlignSIFT(matv[0],matv[i]);
    }
    /*Ptr<AlignMTB> alignMTB = createAlignMTB();
    alignMTB->process(matv, matv);*/

    /*pthread_t pt[size];    //创建THREAD_NUMS个子线程
    thread_mat mats[size];
    for (int i = 1; i < size; i++) {
        mats[i].mat1 = matv[0];
        mats[i].mat2 = matv[i];
        pthread_create(&pt[i], NULL, &threadMatAlign, (void *)&mats[i]);
    }
    //等待全部子线程处理完毕
    for (int i = 1; i < size; i++) {
        pthread_join(pt[i], NULL);
    }*/

    matv[0] /= size;
    /*Ptr<MergeMertens> merge = createMergeMertens();
    merge->process(matv,matv[0]);*/
    for(int i = 1; i < size;i++) {
        matv[0] += matv[i]/=size;
    }

    transpose(matv[0], matv[0]);
    flip(matv[0],matv[0],1);

    imwrite(fpath, matv[0]);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_jike_camerapro_pixfomula_PixFormula_cvMatFusionWithTime(JNIEnv *env, jclass clazz,
                                                         jlongArray mat_addrs,jstring path, jfloatArray times) {

    char *fpath = (char *)env->GetStringUTFChars(path, NULL);

    vector<Mat> matv;
    vector<float> timesv;
    Mat res;

    int size = env->GetArrayLength(mat_addrs);
    jlong *addrs = env->GetLongArrayElements(mat_addrs,NULL);
    float *time = env->GetFloatArrayElements(times,NULL);

    timesv.assign(time, time + size);

    for(int i = 0; i < size; i++) {
        Mat mat=(*((Mat*)addrs[i]));
        matv.push_back(mat);
    }

    Ptr<AlignMTB> alignMTB = createAlignMTB();
    alignMTB->process(matv, matv);

    Mat responseDebevec;
    Ptr<CalibrateDebevec> calibrateDebevec = createCalibrateDebevec();
    calibrateDebevec->process(matv, responseDebevec, timesv);

    Mat hdrDebevec;
    Ptr<MergeDebevec> mergeDebevec = createMergeDebevec();
    mergeDebevec->process(matv, hdrDebevec, timesv, responseDebevec);

    Mat ldrDrago;

    Ptr<TonemapDrago> tonemapDrago = createTonemapDrago(1.0, 0.7);

    tonemapDrago->process(hdrDebevec, ldrDrago);

    ldrDrago = 3 * ldrDrago;

    imwrite(fpath, ldrDrago * 255);
}
