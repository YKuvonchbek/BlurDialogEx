cmake_minimum_required(VERSION 3.6.0)

set(NATIVE_LIB "image")
add_library(${NATIVE_LIB}
        SHARED

        src/main/cpp/image.cpp)

#target_compile_options(${NATIVE_LIB} PUBLIC
#        -ffast-math -Os -funroll-loops -ffast-math -fno-strict-aliasing -fno-math-errno)
#
#target_compile_definitions(${NATIVE_LIB} PUBLIC
#        BSD=1 NULL=0 SOCKLEN_T=socklen_t ANDROID_NDK DISABLE_IMPORTGL AVOID_TABLES ANDROID_TILE_BASED_DECODE __STDC_CONSTANT_MACROS ANDROID_ARMV6_IDCT OPUS_BUILD FIXED_POINT USE_ALLOCA restrict= __EMX__ LOCALE_NOT_USED HAVE_LRINT HAVE_LRINTF)
#
#if (${ANDROID_ABI} STREQUAL "armeabi-v7a")
#    set_target_properties(${NATIVE_LIB} PROPERTIES
#            ANDROID_ARM_NEON FALSE
#            ANDROID_ARM_MODE arm)
#    target_compile_definitions(${NATIVE_LIB} PUBLIC
#            ANDROID_ARM_NEON=false)
#else()
#    set_target_properties(${NATIVE_LIB} PROPERTIES
#            ANDROID_ARM_NEON FALSE
#            ANDROID_ARM_MODE arm)
#endif()

target_link_libraries(${NATIVE_LIB}
        -ljnigraphics
        ${log-lib})

if (${ANDROID_ABI} STREQUAL "x86" OR ${ANDROID_ABI} STREQUAL "x86_64")
    target_link_libraries(${NATIVE_LIB}
            -Wl,--whole-archive libvpx_yasm -Wl,--no-whole-archive, -ljnigraphics)
endif ()

include(AndroidNdkModules)
android_ndk_import_module_cpufeatures()