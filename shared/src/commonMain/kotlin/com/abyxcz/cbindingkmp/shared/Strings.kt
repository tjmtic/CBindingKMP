package com.abyxcz.cbindingkmp.shared

/**
 * String round-trip through the C library: `const char*` in, caller-buffer out.
 * [capacity] is the first buffer size tried; the C side reports the size it needs
 * and the call is retried once.
 */
expect fun greet(name: String, capacity: Int = 256): String

/** Unicode code points in [text], counted by C over its UTF-8 bytes. */
expect fun utf8Length(text: String): Int
