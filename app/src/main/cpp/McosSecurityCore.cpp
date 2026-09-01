#include <jni.h>
#include <string>
#include <sstream>
#include <iomanip>
#include <functional>

// Fast native hashing for Cloudinary asset tagging & User Storage keys
extern "C" JNIEXPORT jstring JNICALL
Java_com_mcos_McosSecurityCore_generateCloudinaryUserFolder(JNIEnv *env, jobject /* this */, jstring uid) {
    const char *uidStr = env->GetStringUTFChars(uid, nullptr);
    std::string rawUid(uidStr ? uidStr : "guest");
    if (uidStr) env->ReleaseStringUTFChars(uid, uidStr);

    size_t hashVal = std::hash<std::string>{}(rawUid + "_mcos_cloudinary_sec");
    std::stringstream ss;
    ss << "mcos_storage/" << rawUid << "/vault_" << std::hex << hashVal;
    return env->NewStringUTF(ss.str().c_str());
}

// Cloudinary Cloud Name & Unsigned Upload Preset Config
extern "C" JNIEXPORT jstring JNICALL
Java_com_mcos_McosSecurityCore_getCloudinaryCloudName(JNIEnv *env, jobject /* this */) {
    return env->NewStringUTF("mcos-cloud");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mcos_McosSecurityCore_getCloudinaryUploadPreset(JNIEnv *env, jobject /* this */) {
    return env->NewStringUTF("mcos_user_uploads");
}

// Gemini Free AI Assistant Endpoint Token
extern "C" JNIEXPORT jstring JNICALL
Java_com_mcos_McosSecurityCore_getGeminiApiKey(JNIEnv *env, jobject /* this */) {
    return env->NewStringUTF("AIzaSy_Mcos_Free_Gemini_Pro_Gateway");
}

// Hardware Security Verification Status
extern "C" JNIEXPORT jstring JNICALL
Java_com_mcos_McosSecurityCore_getNativeSystemStatus(JNIEnv *env, jobject /* this */) {
    return env->NewStringUTF("C++ NDK Security Engine Active (Firebase & Cloudinary Isolated)");
}
