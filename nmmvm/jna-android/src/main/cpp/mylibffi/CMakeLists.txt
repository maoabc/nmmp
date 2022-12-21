project(libffi LANGUAGES C ASM)

cmake_minimum_required(VERSION 3.10.2)


set(out ${CMAKE_CURRENT_BINARY_DIR})

set(LIB_FFI_SRCS
#        libffi/src/dlmalloc.c
        libffi/src/closures.c
        libffi/src/debug.c
        libffi/src/java_raw_api.c
        libffi/src/prep_cif.c
        libffi/src/raw_api.c
        libffi/src/types.c
        )


set(INCLUDE_DIRS libffi/include ${out})


set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall   -Werror \
        -Wno-error=incompatible-pointer-types \
        -Wno-error=unused-label \
        -Wno-incompatible-pointer-types \
        \
        -Wno-deprecated-declarations \
        -Wno-missing-field-initializers \
        -Wno-null-pointer-arithmetic \
        -Wno-pointer-arith \
        -Wno-sign-compare \
        -Wno-unused-parameter \
        -Wno-unused-result")

set(HAVE_LONG_DOUBLE 0)
set(FFI_EXEC_TRAMPOLINE_TABLE 0)

if (${ANDROID_ABI} STREQUAL "arm64-v8a")
    set(TARGET AARCH64)

    set(ARCH_SRCS
            libffi/src/aarch64/sysv.S
            libffi/src/aarch64/ffi.c
            )
    set(LIBFFI_INCLUDES libffi/src/aarch64 myconfig/aarch64)
elseif (${ANDROID_ABI} STREQUAL "armeabi-v7a")
    set(TARGET ARM)
    set(ARCH_SRCS
            libffi/src/arm/sysv.S
            libffi/src/arm/ffi.c
            )
    set(LIBFFI_INCLUDES libffi/src/arm myconfig/arm)

elseif (${ANDROID_ABI} STREQUAL "x86")
    set(TARGET X86)
    set(ARCH_SRCS
            libffi/src/x86/sysv.S
            libffi/src/x86/ffi.c
            )
    set(CMAKE_ASM_FLAGS "${CMAKE_ASM_FLAGS} -DHAVE_AS_X86_PCREL -DHAVE_AS_ASCII_PSEUDO_OP")
    set(LIBFFI_INCLUDES libffi/src/x86 myconfig/x86)
elseif (${ANDROID_ABI} STREQUAL "x86_64")
    set(TARGET X86_64)
    set(ARCH_SRCS
            libffi/src/x86/ffi64.c
            libffi/src/x86/ffiw64.c
            libffi/src/x86/unix64.S
            libffi/src/x86/win64.S
            )
    set(CMAKE_ASM_FLAGS "${CMAKE_ASM_FLAGS} -DHAVE_AS_X86_PCREL -DHAVE_AS_ASCII_PSEUDO_OP")
    set(LIBFFI_INCLUDES libffi/src/x86 myconfig/x86)
else ()
    message(FATAL_ERROR "unknown arch")
endif ()

#debug include dirs
#message(FATAL_ERROR "${ANDROID_ABI}   ${LIBFFI_INCLUDES}")

configure_file(libffi/include/ffi.h.in ${out}/ffi.h @ONLY)


add_library(ffi_static STATIC ${LIB_FFI_SRCS} ${ARCH_SRCS})


target_include_directories(ffi_static PUBLIC ${LIBFFI_INCLUDES}
        ${INCLUDE_DIRS})

target_link_libraries(ffi_static)

