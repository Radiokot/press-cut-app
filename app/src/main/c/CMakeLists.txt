cmake_minimum_required(VERSION 3.22.1)

project(camerapp)

add_subdirectory(
        libwebp
)

add_library(
        camerapp
        SHARED
        fsstamps.c
        struuu.c
)

target_include_directories(
        camerapp
        PRIVATE
        libwebp/src
)

target_link_libraries(
        camerapp
        webpdemux
)

target_link_options(
        camerapp
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
        -ffunction-sections
        -fdata-sections
)

add_custom_command(
        TARGET
        camerapp
        POST_BUILD
        COMMAND ${CMAKE_STRIP} --strip-unneeded $<TARGET_FILE:camerapp>
)
