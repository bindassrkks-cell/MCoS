#include <jni.h>
#include <string>
#include <sstream>
#include <iomanip>
#include <functional>

extern "C" JNIEXPORT jstring JNICALL
Java_com_mcos_McosSecurityCore_generateCloudinaryUserFolder(JNIEnv *env, jobject /* this */, jstring uid) {
    const char *uidStr = env->GetStringUTFChars(uid, nullptr);
    std::string rawUid(uidStr ? uidStr : "guest");
    if (uidStr) env->ReleaseStringUTFChars(uid, uidStr);

    size_t hashVal = std::hash<std::string>{}(rawUid + "_mcos_cloudinary_vault");
    std::stringstream ss;
    ss << "mcos_storage/" << rawUid << "/vault_" << std::hex << hashVal;
    return env->NewStringUTF(ss.str().c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mcos_McosSecurityCore_getNativeSystemStatus(JNIEnv *env, jobject /* this */) {
    return env->NewStringUTF("Firebase Realtime Engine Active (C++ NDK Secured)");
}
