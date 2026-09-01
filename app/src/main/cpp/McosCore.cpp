#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_mcos_McosNativeCore_getSupabaseUrl(JNIEnv *env, jobject /* this */) {
    return env->NewStringUTF("https://mcos-realtime.supabase.co");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mcos_McosNativeCore_getSupabaseAnonKey(JNIEnv *env, jobject /* this */) {
    return env->NewStringUTF("sb_anon_live_eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.mcos_realtime_node");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mcos_McosNativeCore_getSystemStatus(JNIEnv *env, jobject /* this */) {
    return env->NewStringUTF("ALL SYSTEMS OPERATIONAL (C++ NDK Core v1.0)");
}
