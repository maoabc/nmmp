cmake_minimum_required(VERSION 3.6)
project(safe-iop)


#include_directories(openssl/include)

set(CMAKE_C_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Werror -Wno-unused-parameter ")

add_library(libsafe_iop STATIC src/safe_iop.c)

target_link_libraries(libsafe_iop)