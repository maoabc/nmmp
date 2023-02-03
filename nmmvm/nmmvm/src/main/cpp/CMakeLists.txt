cmake_minimum_required(VERSION 3.6)
project(nmmp)


add_subdirectory(vm)
add_subdirectory(libdex)

#测试

include_directories(.)
include_directories(test)
include_directories(custom)

#测试自动生成的代码
file(GLOB GEN_SOURCES test/*_functions.c)

set(TEST_SRC
        #        test/classes_native_functions.c
        test.cpp
        temp_jni_init.c
        temp_functions.c
        )


set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -fvisibility=hidden")


if (${ANDROID_ABI} STREQUAL "armeabi-v7a" OR ${ANDROID_ABI} STREQUAL "x86")
    add_definitions("-D_32_BIT")
elseif (${ANDROID_ABI} STREQUAL "arm64-v8a" OR ${ANDROID_ABI} STREQUAL "x86_64")
    add_definitions("-D_64_BIT")
endif ()


add_library(nmmp SHARED ConstantPool.c ${TEST_SRC})


target_link_libraries(nmmp ${LIBNMMVM_NAME} dex log)


add_library(test-jna SHARED test_jna.c)

target_link_libraries(test-jna log)