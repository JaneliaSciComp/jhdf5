#! /bin/bash

source version.sh

if [ -n "$POSTFIX" ]; then
  VERSION="$VERSION-$POSTFIX"
fi

if [ -z "$JAVA_HOME" ]; then
  JAVA_HOME=`java -XshowSettings:properties -version 2>&1 | grep "java.home" | cut -d"=" -f2`
fi

rm -fR build/jni
rm -f build/libjhdf5.so
cp -a jni build/
cp -a *.c build/jni/
cd build
cp hdf5-$VERSION/src/H5win32defs.h jni/
cp hdf5-$VERSION/src/H5private.h jni/

echo "JHDF5 building..."
gcc -shared -O3 -fPIC -Wl,--exclude-libs,ALL jni/*.c -Ihdf5-${VERSION}-aarch64/include -I$JAVA_HOME/include -I$JAVA_HOME/include/linux hdf5-${VERSION}-aarch64/lib/libhdf5.a -o libjhdf5.so -lz &> jhdf5_build.log

if [ -f libjhdf5.so ]; then
  mkdir -p ../../../libs/native/jhdf5/aarch64-Linux/
  cp -pf libjhdf5.so ../../../libs/native/jhdf5/aarch64-Linux/
  echo "JHDF5 Build deployed"
else
  echo "JHDF5 ERROR"
fi
