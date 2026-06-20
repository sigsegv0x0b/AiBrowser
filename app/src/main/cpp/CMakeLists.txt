cmake_minimum_required(VERSION 3.10)
project("mnnbridge")

# Paths
set(MNN_ROOT /data/data/com.termux/files/home/MNN)
set(MNN_INSTALL ${MNN_ROOT}/project/android/build_64)

# Include directories for MNN headers
include_directories(${MNN_ROOT}/include)
include_directories(${MNN_ROOT}/transformers/llm/engine/include)
include_directories(${MNN_ROOT}/3rd_party)

# Import prebuilt libMNN.so
add_library(MNN SHARED IMPORTED)
set_target_properties(MNN PROPERTIES IMPORTED_LOCATION ${MNN_INSTALL}/libMNN.so)

# Sources
add_library(${CMAKE_PROJECT_NAME} SHARED
    mnn_jni.cpp
    llm_session.cpp
    processor.cpp
    video/video_decoder.cpp
    video/byte_buffer_decoder.cpp
    video/image_utils.cpp
    video/video_processor.cpp
)

# Compile options
target_compile_options(${CMAKE_PROJECT_NAME} PRIVATE -std=c++17 -O2)
target_link_libraries(${CMAKE_PROJECT_NAME} MNN android log mediandk)
