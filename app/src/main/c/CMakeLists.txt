cmake_minimum_required(VERSION 3.22.1)

project(camerapp)

add_subdirectory(
        ${CMAKE_CURRENT_SOURCE_DIR}/../../../../stamp-native
        ${CMAKE_BINARY_DIR}/stamp-native
)

file(GLOB_RECURSE dynbuf CONFIGURE_DEPENDS "dynbuf/src/*.c")

add_library(
        camerapp
        SHARED
        fs_stamp_repository.c
        fs_stamp_collection_repository.c
        direct_byte_buffer.c
        ${dynbuf}
)

target_include_directories(
        camerapp
        PRIVATE
        dynbuf/inc
)

target_link_libraries(
        camerapp
        stamp-native
        android
        log
)

target_link_options(
        camerapp
        PRIVATE
        "-Wl,--gc-sections"
        "-Wl,--icf=all"
        "-Wl,-z,max-page-size=16384"
        "-Wl,-z,common-page-size=16384"
)

target_link_options(
        stamp-native
        PRIVATE
        "-Wl,--gc-sections"
        "-Wl,--icf=all"
        "-Wl,-z,max-page-size=16384"
        "-Wl,-z,common-page-size=16384"
)

target_compile_options(
        camerapp
        PRIVATE
        -Os
        -O3
        -ffunction-sections
        -fdata-sections
)

add_custom_command(
        TARGET
        camerapp
        POST_BUILD
        COMMAND ${CMAKE_STRIP} --strip-unneeded $<TARGET_FILE:camerapp>
)
