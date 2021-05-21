

# java通过环境变量传递编译所需要的参数

#ANDROID_SDK_HOME=/opt/android-sdk

# ANDROID_NDK_HOME=${ANDROID_SDK_HOME}/ndk/22.1.7171670

#CMAKE_PATH=${ANDROID_SDK_HOME}/cmake/3.18.1

#API_LEVEL=21

# PROJECT_ROOT=$(pwd)
#ABI=armeabi-v7a
#ABI=arm64-v8a
#ABI=x86
#ABI=x86_64
#ABIS="armeabi-v7a arm64-v8a x86 x86_64"

#BUILD_TYPE=Release

BUILD_PATH=${PROJECT_ROOT}/.cxx/cmake/${BUILD_TYPE}/${ABI}

if [ -f "${BUILD_PATH}" ];then
    rm -rf ${BUILD_PATH}
fi
# LIBRARY_OUTPUT_DIRECTORY=${PROJECT_ROOT}/obj/${ABI}

STRIP_PATH="strip"

if [ "$ABI" == "armeabi-v7a" ];then
    STRIP_PATH=$ANDROID_NDK_HOME/toolchains/arm-linux-androideabi-4.9/prebuilt/linux-x86_64/bin/arm-linux-androideabi-strip
elif [ "$ABI" == "arm64-v8a" ];then
    STRIP_PATH=$ANDROID_NDK_HOME/toolchains/aarch64-linux-android-4.9/prebuilt/linux-x86_64/bin/aarch64-linux-android-strip
elif [ "$ABI" == "x86" ];then
    STRIP_PATH=$ANDROID_NDK_HOME/toolchains/x86-4.9/prebuilt/linux-x86_64/bin/i686-linux-android-strip
elif [ "$ABI" == "x86_64" ];then
    STRIP_PATH=$ANDROID_NDK_HOME/toolchains/x86_64-4.9/prebuilt/linux-x86_64/bin/x86_64-linux-android-strip
else
    echo "Unsupported abi";
    exit -1;
fi






${CMAKE_PATH}/bin/cmake  \
-H${PROJECT_ROOT}/dex2c \
-DCMAKE_TOOLCHAIN_FILE=${ANDROID_NDK_HOME}/build/cmake/android.toolchain.cmake \
-DCMAKE_BUILD_TYPE=${BUILD_TYPE} \
-DANDROID_ABI=${ABI} \
-DANDROID_NDK=${ANDROID_NDK_HOME} \
-DANDROID_PLATFORM=android-${API_LEVEL} \
-DCMAKE_ANDROID_ARCH_ABI=${ABI} \
-DCMAKE_ANDROID_NDK=${ANDROID_NDK_HOME} \
-DCMAKE_EXPORT_COMPILE_COMMANDS=ON  \
-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=${LIBRARY_OUTPUT_DIRECTORY} \
-DCMAKE_MAKE_PROGRAM=${CMAKE_PATH}/bin/ninja \
-DCMAKE_SYSTEM_NAME=Android \
-DCMAKE_SYSTEM_VERSION=${API_LEVEL} \
-B${BUILD_PATH} \
-GNinja

${CMAKE_PATH}/bin/cmake --build ${BUILD_PATH}


echo "strip"
# strip
${STRIP_PATH} --strip-unneeded ${LIBRARY_OUTPUT_DIRECTORY}/libnmmvm.so
${STRIP_PATH} --strip-unneeded ${LIBRARY_OUTPUT_DIRECTORY}/libnmmp.so
