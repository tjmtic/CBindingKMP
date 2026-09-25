cmake_minimum_required(VERSION 3.10)

project(MyLib C)

add_library(mylib STATIC mylib.c)
# Also build a shared lib for JNI/Desktop dynamic loading if needed
# Point to valid generated file. We assume Gradle ran generateJni before this.
# CBindingKMP passes the generated bridge as CBINDING_JNI_BRIDGE (1.2.0+).
if(NOT DEFINED CBINDING_JNI_BRIDGE)
    set(CBINDING_JNI_BRIDGE ${CMAKE_CURRENT_SOURCE_DIR}/../../shared/build/generated/jni/jni_gen_bridge.c)
endif()
add_library(mylib_shared SHARED mylib.c ${CBINDING_JNI_BRIDGE})

# The generated bridge lives in build/generated/jni/ and includes headers by bare
# name; make this directory (where the .h files live) visible to it.
target_include_directories(mylib_shared PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

# Ensure the shared lib has a standard name or one we expect
set_target_properties(mylib_shared PROPERTIES OUTPUT_NAME "mylib")
