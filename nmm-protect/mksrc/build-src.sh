#!/bin/sh

#以存储模式创建vmsrc.zip

VM_DIR="`pwd`/../../nmmvm/nmmvm/src/main/cpp"

VM_SRC="vm cutils ConstantPool.c ConstantPool.h"

OUT="`pwd`/../apkprotect/src/main/resources/vmsrc.zip"

pushd ${VM_DIR}&&zip -0 -r -D ${OUT} ${VM_SRC}
popd

zip -0 -u -D ${OUT}  CMakeLists.txt

