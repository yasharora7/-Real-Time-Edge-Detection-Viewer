#include <jni.h>
#include <vector>
#include <opencv2/imgproc.hpp>
#include <opencv2/opencv.hpp>

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_example_edgeviewer_NativeEdge_processFrame(
        JNIEnv *env,
        jobject /* this */,
        jbyteArray frameData,
        jint width,
        jint height
) {

    jsize length = env->GetArrayLength(frameData);
    std::vector<unsigned char> yData(length);
    env->GetByteArrayRegion(frameData, 0, length, reinterpret_cast<jbyte*>(yData.data()));

    cv::Mat gray(height, width, CV_8UC1, yData.data());

    cv::Mat edges;
    cv::Canny(gray, edges, 80, 160);

    jbyteArray result = env->NewByteArray(width * height);
    env->SetByteArrayRegion(
            result,
            0,
            width * height,
            reinterpret_cast<const jbyte*>(edges.data)
    );

    return result;
}
