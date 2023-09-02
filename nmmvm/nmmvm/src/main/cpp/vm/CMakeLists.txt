cmake_minimum_required(VERSION 3.6)
project(libvm)


include_directories(..)
include_directories(./include)


set(VM_SRC_FILES
        DexCatch.cpp
        Exception.cpp
        Interp.cpp
        InterpC-portable.cpp
        GlobalCache.cpp
        JNIWrapper.c

        )


set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -DLOG_INSTR ")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -fvisibility=hidden -DNDEBUG")

#设置vm库名称
set(LIBNMMVM_NAME "nmmvm" CACHE INTERNAL "lib nmmvm name")


add_library(${LIBNMMVM_NAME} SHARED ${VM_SRC_FILES})

target_link_libraries(${LIBNMMVM_NAME} log)
target_include_directories(${LIBNMMVM_NAME} PUBLIC include)