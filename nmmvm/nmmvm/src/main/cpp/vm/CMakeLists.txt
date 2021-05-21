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

        )



set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -DLOG_INSTR ")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -fvisibility=hidden -DNDEBUG")

add_library(nmmvm SHARED ${VM_SRC_FILES})

target_link_libraries(nmmvm log)