package com.mcos

object NeonBackend {
    init {
        System.loadLibrary("neon_backend")
    }

    external fun getNeonProjectId(): String
    external fun getNeonBucket(): String
    external fun getNeonDataApi(): String
    external fun getNeonS3Endpoint(): String
    external fun getNeonRegion(): String
    external fun getNeonAccessKey(): String
    external fun getNeonSecretKey(): String
    external fun getNeonAiKey(): String
}
