package com.mcos

object McosSecurityCore {
    init {
        System.loadLibrary("mcos_security")
    }

    external fun generateCloudinaryUserFolder(uid: String): String
    external fun getNativeSystemStatus(): String
}
