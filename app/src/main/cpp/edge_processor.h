#pragma once
#include <vector>

std::vector<unsigned char> process_edge(
        const std::vector<unsigned char>& rgb,
        int w,
        int h
);

std::vector<unsigned char> edge_process_frame(
        const std::vector<unsigned char> &input,
        int width,
        int height
);
