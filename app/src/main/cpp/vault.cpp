#include <jni.h>
#include <vector>

// High speed byte level encryption/decryption algorithm
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_mcos_VaultActivity_nativeEncryptDecrypt(
        JNIEnv* env,
        jobject /* this */,
        jbyteArray data,
        jstring pinKey) {
    
    jsize len = env->GetArrayLength(data);
    jbyte* buffer = env->GetByteArrayElements(data, nullptr);
    const char* keyChars = env->GetStringUTFChars(pinKey, nullptr);
    int keyLen = env->GetStringUTFLength(pinKey);

    if (keyLen == 0) keyLen = 1;

    for (int i = 0; i < len; i++) {
        buffer[i] = buffer[i] ^ keyChars[i % keyLen] ^ 0xAA; // Dual XOR cipher
    }

    jbyteArray result = env->NewByteArray(len);
    env->SetByteArrayRegion(result, 0, len, buffer);

    env->ReleaseByteArrayElements(data, buffer, 0);
    env->ReleaseStringUTFChars(pinKey, keyChars);

    return result;
}
