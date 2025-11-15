#include <jni.h>
#include <vector>
#include "edge_processor.h"

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_example_edgeviewer_NativeEdge_processFrame(
        JNIEnv *env, jobject thiz,
        jbyteArray frame, jint w, jint h) {

    jsize len = env->GetArrayLength(frame);

    std::vector<unsigned char> in(len);
    env->GetByteArrayRegion(frame, 0, len, reinterpret_cast<jbyte*>(in.data()));

    std::vector<unsigned char> out = edge_process_frame(in, w, h);

    jbyteArray result = env->NewByteArray(out.size());
    env->SetByteArrayRegion(result, 0, out.size(), reinterpret_cast<jbyte*>(out.data()));

    return result;
}
