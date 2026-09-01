cmake_minimum_required(VERSION 3.22.1)
project(mcos_security_core)

add_library(mcos_security SHARED
    McosSecurityCore.cpp
)

target_link_libraries(mcos_security
    android
    log
)
