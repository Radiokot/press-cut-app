cmake_minimum_required(VERSION 3.10)

project("stamp-native")

set(CMAKE_C_STANDARD 11)

add_library(${PROJECT_NAME} STATIC main.c)
target_include_directories(${PROJECT_NAME} PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})

target_compile_options(
        ${PROJECT_NAME}
        PUBLIC
        -Os
        -O3
        -ffunction-sections
        -fdata-sections
)

add_custom_command(
        TARGET
        ${PROJECT_NAME}
        POST_BUILD
        COMMAND ${CMAKE_STRIP} --strip-unneeded $<TARGET_FILE:${PROJECT_NAME}>
)

# Mini-XML
file(GLOB_RECURSE mxml_sources CONFIGURE_DEPENDS "lib/mxml/*.c")
add_library(mxml STATIC ${mxml_sources})
target_link_libraries(${PROJECT_NAME} PRIVATE mxml)
target_include_directories(${PROJECT_NAME} PRIVATE lib/mxml)

# libwebp
add_subdirectory(lib/libwebp)
target_link_libraries(${PROJECT_NAME} PUBLIC webpdemux libwebpmux)
