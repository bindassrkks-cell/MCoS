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

    private external fun getNeonProjectId(): String
    private external fun getNeonBucket(): String
    private external fun getNeonDataApi(): String
    private external fun getNeonS3Endpoint(): String
    private external fun getNeonRegion(): String
    private external fun getNeonAccessKey(): String
    private external fun getNeonSecretKey(): String
    private external fun getNeonAiKey(): String

    @ReactMethod
    fun getFullConfig(promise: Promise) {
        try {
            val map = Arguments.createMap().apply {
                putString("projectId", getNeonProjectId())
                putString("bucket", getNeonBucket())
                putString("dataApiUrl", getNeonDataApi())
                putString("endpoint", getNeonS3Endpoint())
                putString("region", getNeonRegion())
                putString("accessKey", getNeonAccessKey())
                putString("secretKey", getNeonSecretKey())
                putString("aiKey", getNeonAiKey())
            }
            promise.resolve(map)
        } catch (e: Exception) {
            promise.reject("NATIVE_CONFIG_ERROR", e.message)
        }
    }
}
