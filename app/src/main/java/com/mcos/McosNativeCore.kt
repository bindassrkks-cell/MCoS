package com.mcos

object McosNativeCore {
    init {
        System.loadLibrary("mcos_native")
    }

    external fun getSupabaseUrl(): String
    external fun getSupabaseAnonKey(): String
    external fun getGeminiApiKey(): String
    external fun getNeonProjectId(): String
    external fun getNeonBucket(): String
    external fun getNeonDataApi(): String
    external fun getNeonS3Endpoint(): String
    external fun getNeonRegion(): String
    external fun getNeonAccessKey(): String
    external fun getNeonSecretKey(): String
    external fun getNeonAiKey(): String
}
