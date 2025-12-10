cmake_minimum_required(VERSION 3.10.2)
project(example_lib)

# Generated JNI Bridge (automatically created by the plugin)
set(GENERATED_JNI_BRIDGE "../../shared/build/generated/jni/jni_gen_bridge.c")

add_library(example_lib SHARED
    example_lib.c
    ${GENERATED_JNI_BRIDGE}
)

# Static library for iOS
add_library(example_lib_static STATIC
    example_lib.c
    # No JNI bridge needed for iOS static lib
)
set_target_properties(example_lib_static PROPERTIES OUTPUT_NAME "example_lib")
