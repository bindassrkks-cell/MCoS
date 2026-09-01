#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_mcos_NeonBackend_getNeonProjectId(JNIEnv *env, jobject /* this */) {
    return env->NewStringUTF("dry-king-57780977");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mcos_NeonBackend_getNeonBucket(JNIEnv *env, jobject /* this */) {
    return env->NewStringUTF("binday");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mcos_NeonBackend_getNeonDataApi(JNIEnv *env, jobject /* this */) {
    return env->NewStringUTF("https://ep-little-haze-ayplq02h.apirest.c-5.us-east-2.aws.neon.tech/neondb/rest/v1");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mcos_NeonBackend_getNeonS3Endpoint(JNIEnv *env, jobject /* this */) {
    return env->NewStringUTF("https://br-cold-tree-ay0sxicu.storage.c-5.us-east-2.aws.neon.tech");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mcos_NeonBackend_getNeonRegion(JNIEnv *env, jobject /* this */) {
    return env->NewStringUTF("us-east-2");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mcos_NeonBackend_getNeonAccessKey(JNIEnv *env, jobject /* this */) {
    return env->NewStringUTF("nak_live_8bff28857082488eb6ba25c7006aabec");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mcos_NeonBackend_getNeonSecretKey(JNIEnv *env, jobject /* this */) {
    return env->NewStringUTF("nsk_live_fbb596d3e48491ea1fc1fe6dd8fc29084747e355df467919dfb1c1dc01e88a33");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mcos_NeonBackend_getNeonAiKey(JNIEnv *env, jobject /* this */) {
    return env->NewStringUTF("nt_live_8bff28857082_cgP7mO4L61b7sp2hOX608out2L7pDjjo");
}
