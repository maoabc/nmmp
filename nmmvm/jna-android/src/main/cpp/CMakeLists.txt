cmake_minimum_required(VERSION 3.10.2)

project(jna)


add_definitions(-DNO_JAWT)

add_subdirectory(mylibffi)

#message(FATAL_ERROR "unknown arch ${LIBFFI_INCLUDES}")

find_library(log-lib log)


add_library(jnidispatch SHARED dispatch.c callback.c)

target_link_libraries(jnidispatch
        ffi_static
        ${log-lib})