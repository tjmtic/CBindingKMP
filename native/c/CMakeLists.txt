cmake_minimum_required(VERSION 3.10)

project(MyLib C)

add_library(mylib STATIC mylib.c)
# Also build a shared lib for JNI/Desktop dynamic loading if needed
# Point to valid generated file. We assume Gradle ran generateJni before this.
add_library(mylib_shared SHARED mylib.c ../../shared/build/generated/jni/jni_gen_bridge.c)

# Ensure the shared lib has a standard name or one we expect
set_target_properties(mylib_shared PROPERTIES OUTPUT_NAME "mylib")
