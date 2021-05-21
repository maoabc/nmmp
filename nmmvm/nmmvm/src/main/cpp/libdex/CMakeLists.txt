cmake_minimum_required(VERSION 3.6)
project(libdex)


include_directories(../)
include_directories(../safe-iop/include)


set(DEX_SRC_FILES
        CmdUtils.cpp
        DexCatch.cpp
        DexClass.cpp
        DexDataMap.cpp
        DexDebugInfo.cpp
        DexFile.cpp
        DexInlines.cpp
        DexOptData.cpp
        DexOpcodes.cpp
        DexProto.cpp
        DexSwapVerify.cpp
        DexUtf.cpp
        InstrUtils.cpp
        Leb128.cpp
        OptInvocation.cpp
        sha1.cpp
        SysUtil.cpp
        ZipArchive.cpp
        )


#include_directories(openssl/include)

#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DLITTLE_ENDIAN")

add_library(dex STATIC ${DEX_SRC_FILES})

target_link_libraries(dex z log)