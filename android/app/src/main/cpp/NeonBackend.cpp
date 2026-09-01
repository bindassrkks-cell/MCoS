#include <jni.h>
#include <string>
#include <sstream>

extern "C" JNIEXPORT jstring JNICALL
Java_com_mcos_NeonNativeModule_getNeonS3Endpoint(JNIEnv *env, jobject /* this */) {
    return env->NewStringUTF("https://br-cold-tree-ay0sxicu.storage.c-5.us-east-2.aws.neon.tech");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mcos_NeonNativeModule_getNeonRegion(JNIEnv *env, jobject /* this */) {
    return env->NewStringUTF("us-east-2");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mcos_NeonNativeModule_getNeonAccessKey(JNIEnv *env, jobject /* this */) {
    return env->NewStringUTF("nak_live_8bff28857082488eb6ba25c7006aabec");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mcos_NeonNativeModule_getNeonSecretKey(JNIEnv *env, jobject /* this */) {
    return env->NewStringUTF("nsk_live_fbb596d3e48491ea1fc1fe6dd8fc29084747e355df467919dfb1c1dc01e88a33");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mcos_NeonNativeModule_getNeonAiKey(JNIEnv *env, jobject /* this */) {
    return env->NewStringUTF("nt_live_8bff28857082_cgP7mO4L61b7sp2hOX608out2L7pDjjo");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mcos_NeonNativeModule_authenticateUser(JNIEnv *env, jobject /* this */, jstring email, jstring password) {
    const char *emailStr = env->GetStringUTFChars(email, nullptr);
    const char *passStr = env->GetStringUTFChars(password, nullptr);

    std::string emailCpp(emailStr ? emailStr : "");
    std::string passCpp(passStr ? passStr : "");

    if (emailStr) env->ReleaseStringUTFChars(email, emailStr);
    if (passStr) env->ReleaseStringUTFChars(password, passStr);

    if (!emailCpp.empty() && passCpp.length() >= 4) {
        std::string token = "neon_auth_" + std::to_string(std::hash<std::string>{}(emailCpp + ":" + passCpp));
        return env->NewStringUTF(token.c_str());
    }
    return env->NewStringUTF("");
}
