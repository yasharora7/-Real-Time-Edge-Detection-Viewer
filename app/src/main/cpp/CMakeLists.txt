cmake_minimum_required(VERSION 3.10.2)
project(edgeviewer)

# Set the path to the OpenCV Android SDK (change this path)
set(OpenCV_DIR "${OpenCV_DIR}")

# Tell CMake to find OpenCV
find_package(OpenCV REQUIRED)

message(STATUS "OpenCV version: ${OpenCV_VERSION}")
message(STATUS "OpenCV include dirs: ${OpenCV_INCLUDE_DIRS}")
message(STATUS "OpenCV libs: ${OpenCV_LIBS}")

# Create your native library
add_library(native-lib SHARED
        native-lib.cpp
        edge_processor.cpp
)

target_include_directories(native-lib PRIVATE ${OpenCV_INCLUDE_DIRS})

target_link_libraries(
        native-lib
        ${OpenCV_LIBS}
        log
)