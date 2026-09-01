#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_mcos_McosActivity_getStreamEngineToken(JNIEnv* env, jobject /* this */) {
    std::string token = "STREAM_ENGINE_PRO_HW_ACCELERATED";
    return env->NewStringUTF(token.c_str());
}
