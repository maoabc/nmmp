cmake_minimum_required(VERSION 3.6)
project(nmmp)


add_subdirectory(vm)


include_directories(.)
include_directories(generated/)


file(GLOB GEN_SOURCES generated/*_functions.c generated/jni_init.c)


set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -fvisibility=hidden ")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -fvisibility=hidden ")

#外部程序处理时替换 LIBNAME_PLACEHOLDER
set(LIBNAME_PLACEHOLDER "nmmp")


add_library(${LIBNAME_PLACEHOLDER} SHARED ConstantPool.c ${GEN_SOURCES} )

target_link_libraries(${LIBNAME_PLACEHOLDER} ${LIBNMMVM_NAME} log)

