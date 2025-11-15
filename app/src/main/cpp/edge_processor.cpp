#include "edge_processor.h"
#include <opencv2/imgproc.hpp>
#include <opencv2/opencv.hpp>

std::vector<unsigned char> process_edge(
        const std::vector<unsigned char>& rgb,
        int w, int h) {

    // Convert input bytes to Mat
    cv::Mat src(cv::Size(w, h), CV_8UC3, (void *) rgb.data());

    cv::Mat gray, edges;

    // Convert to grayscale
    cv::cvtColor(src, gray, cv::COLOR_RGB2GRAY);

    // Apply Canny
    cv::Canny(gray, edges, 80, 160);

    // Copy result into byte vector
    return std::vector<unsigned char>(
            edges.data,
            edges.data + (w * h)
    );
}

// ------------------------------------------------------
// FIXED: Implement missing function
// ------------------------------------------------------
std::vector<unsigned char> edge_process_frame(
        const std::vector<unsigned char> &input,
        int width,
        int height
) {
    // Simply call process_edge()
    return process_edge(input, width, height);
}
