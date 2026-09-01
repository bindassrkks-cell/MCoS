cmake_minimum_required(VERSION 3.22.1)
project(neon_backend)

add_library(neon_backend SHARED NeonBackend.cpp)

target_link_libraries(neon_backend
    android
    log
)
