#ifndef MYLIB_H
#define MYLIB_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

// A simple function that adds two integers
int add_numbers(int a, int b);

// Buffer-marshalling demos: sum a byte buffer, and convert RGBA pixels to grayscale.
int32_t sum_bytes(const uint8_t* data, int32_t len);
void rgba_to_gray(const uint8_t* rgba, int32_t count, uint8_t* out);

#ifdef __cplusplus
}
#endif

#endif // MYLIB_H
