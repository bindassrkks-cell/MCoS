#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_mcos_MainActivity_getNativeCoreVersion(JNIEnv* env, jobject /* this */) {
    std::string coreVer = "MCoS Native Kernel v4.2 - Secure Engine Active";
    return env->NewStringUTF(coreVer.c_str());
}
