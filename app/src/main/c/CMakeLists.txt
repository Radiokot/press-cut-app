cmake_minimum_required(VERSION 3.22.1)

project(camerapp)

add_subdirectory(
        libwebp
)

file(GLOB_RECURSE ezXML CONFIGURE_DEPENDS "ezXML/*.c")
file(GLOB_RECURSE dynbuf CONFIGURE_DEPENDS "dynbuf/src/*.c")

add_library(
        camerapp
        SHARED
        fs_webp_stamp_repository.c
        ${ezXML}
        ${dynbuf}
)

target_include_directories(
        camerapp
        PRIVATE
        libwebp/src
        ezXML
        dynbuf/inc
)

target_link_libraries(
        camerapp
        webpdemux
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
