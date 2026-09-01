cmake_minimum_required(VERSION 3.22.1)

project("mcos_native")

# 1. libmcore.so for MainActivity
add_library(mcore SHARED mcore.cpp)

# 2. libmcos.so for McosActivity
add_library(mcos SHARED mcos.cpp)

# 3. libvault.so for VaultActivity
add_library(vault SHARED vault.cpp)

find_library(log-lib log)

target_link_libraries(mcore ${log-lib})
target_link_libraries(mcos ${log-lib})
target_link_libraries(vault ${log-lib})
