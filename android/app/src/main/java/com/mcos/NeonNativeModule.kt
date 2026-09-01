package com.mcos

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class NeonNativeModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    companion object {
        init {
            System.loadLibrary("neon_backend")
        }
    }

    override fun getName(): String = "NeonBackend"

    private external fun getNeonS3Endpoint(): String
    private external fun getNeonRegion(): String
    private external fun getNeonAccessKey(): String
    private external fun getNeonSecretKey(): String
    private external fun getNeonAiKey(): String
    private external fun authenticateUser(email: String, pass: String): String

    @ReactMethod
    fun getNeonConfig(promise: Promise) {
        try {
            val map = Arguments.createMap().apply {
                putString("endpoint", getNeonS3Endpoint())
                putString("region", getNeonRegion())
                putString("accessKey", getNeonAccessKey())
                putString("secretKey", getNeonSecretKey())
                putString("aiKey", getNeonAiKey())
            }
            promise.resolve(map)
        } catch (e: Exception) {
            promise.reject("NATIVE_ERROR", e.message)
        }
    }

    @ReactMethod
    fun nativeLogin(email: String, pass: String, promise: Promise) {
        try {
            val session = authenticateUser(email, pass)
            if (session.isNotEmpty()) {
                promise.resolve(session)
            } else {
                promise.reject("AUTH_FAILED", "Invalid credentials. Password must be at least 4 characters.")
            }
        } catch (e: Exception) {
            promise.reject("AUTH_ERROR", e.message)
        }
    }
}
