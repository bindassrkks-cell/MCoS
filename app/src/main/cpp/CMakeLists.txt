cmake_minimum_required(VERSION 3.22.1)
project(mcos_native_core)

add_library(mcos_core SHARED
    McosCore.cpp
    NeonStorage.cpp
)

target_link_libraries(mcos_core
    android
    log
)
