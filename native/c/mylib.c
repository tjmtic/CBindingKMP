#include "mylib.h"

#include <string.h>

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

int32_t utf8_length(const char* text) {
    int32_t n = 0;
    for (const unsigned char* p = (const unsigned char*)text; *p; p++) {
        if ((*p & 0xC0) != 0x80) n++; /* count lead bytes, skip continuation bytes */
    }
    return n;
}

int32_t greet(const char* name, char* out, int32_t cap) {
    static const char prefix[] = "Hello, ";
    const size_t prefix_len = sizeof(prefix) - 1;
    const size_t name_len = strlen(name);
    const size_t need = prefix_len + name_len + 1; /* + "!" */
    if (need > (size_t)cap) return -(int32_t)need;
    memcpy(out, prefix, prefix_len);
    memcpy(out + prefix_len, name, name_len);
    out[need - 1] = '!';
    return (int32_t)need;
}
