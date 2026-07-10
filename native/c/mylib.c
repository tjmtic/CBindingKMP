#include "mylib.h"

int add_numbers(int a, int b) {
    return a + b;
}

int32_t sum_bytes(const uint8_t* data, int32_t len) {
    int32_t sum = 0;
    for (int32_t i = 0; i < len; i++) {
        sum += data[i];
    }
    return sum;
}

void rgba_to_gray(const uint8_t* rgba, int32_t count, uint8_t* out) {
    for (int32_t i = 0; i < count; i++) {
        const uint8_t* px = rgba + i * 4;
        /* BT.601 integer luma */
        out[i] = (uint8_t)((299 * px[0] + 587 * px[1] + 114 * px[2]) / 1000);
    }
}
