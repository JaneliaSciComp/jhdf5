gcc -m64 -dynamiclib -O3 *.c -I/System/Library/Frameworks/JavaVM.framework/Versions/Current/Headers -I/opt/hdf5-1.8.3-x86_64/include /opt/hdf5-1.8.3-x86_64/lib/libhdf5.a -lz -o libjhdf5.jnilib
