#include "edge_processor.h"
#include <opencv2/imgproc.hpp>

std::vector<unsigned char>
edge_process_frame(const std::vector<unsigned char>& y, int w, int h) {

    cv::Mat gray(h, w, CV_8UC1, (void*)y.data());
    cv::Mat edges;

    cv::Canny(gray, edges, 80, 160);

    std::vector<unsigned char> out(edges.data, edges.data + (w * h));
    return out;
}
