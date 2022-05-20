#! /bin/bash

source version.sh

if [ -n "$POSTFIX" ]; then
  VERSION="$VERSION-$POSTFIX"
fi

if [ -z "$JAVA_HOME" ]; then
  #JAVA_HOME=`java -XshowSettings:properties -version 2>&1 | grep "java.home" | cut -d"=" -f2`
  JAVA_HOME=`/usr/libexec/java_home`
fi
echo $JAVA_HOME

rm -fR build/jni
rm -f build/libjhdf5.jnilib
cp -a jni build/
cp -a *.c build/jni/
cd build
cp hdf5-$VERSION/src/H5win32defs.h jni/
cp hdf5-$VERSION/src/H5private.h jni/

echo "JHDF5 building..."
pwd
gcc -Wno-error=implicit-function-declaration -m64 -mmacosx-version-min=10.11 -dynamiclib -O3 jni/*.c -Ihdf5-${VERSION}-aarch64/include -I${JAVA_HOME}/include -I${JAVA_HOME}/include/darwin hdf5-${VERSION}-aarch64/lib/libhdf5.dylib -o libjhdf5.jnilib -lz &> jhdf5_build.log

if [ -f "hdf5-${VERSION}-aarch64/lib/libhdf5.dylib" ]; then
  mkdir -p "../../../libs/native/hdf5/aarch64-Mac OS X"
  cp -pf "hdf5-${VERSION}-aarch64/lib/libhdf5.dylib" "../../../libs/native/hdf5/aarch64-Mac OS X/libhdf5.jnilib"
  echo "HDF5 Build deployed"
else
  echo "HDF5 ERROR"
fi

if [ -f libjhdf5.jnilib ]; then
  mkdir -p "../../../libs/native/jhdf5/aarch64-Mac OS X"
  cp -pf libjhdf5.jnilib "../../../libs/native/jhdf5/aarch64-Mac OS X"
  echo "JHDF5 Build deployed"
else
  echo "JHDF5 ERROR"
fi
