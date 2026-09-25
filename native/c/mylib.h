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

// String demos. Input: NUL-terminated UTF-8. Output: the caller-buffer convention —
// write UTF-8 into out (no NUL needed), return the bytes written, or -(capacity needed)
// without writing anything when cap is too small.
int32_t utf8_length(const char* text);                     // Unicode code points
int32_t greet(const char* name, char* out, int32_t cap);   // "Hello, <name>!"

#ifdef __cplusplus
}
#endif

#endif // MYLIB_H
