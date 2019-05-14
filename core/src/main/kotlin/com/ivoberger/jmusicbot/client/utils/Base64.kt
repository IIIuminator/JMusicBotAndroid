/*
* Copyright 2019 Ivo Berger
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.ivoberger.jmusicbot.client.utils

private const val BASE64_SET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
private val RX_BASE64_CLEANR = "[^=" + BASE64_SET + "]".toRegex()

/**
 * Base64 encode a string.
 */
val String.base64encoded: String
    get() {
        val pad = when (this.length % 3) {
            1 -> "=="
            2 -> "="
            else -> ""
        }
        var raw = this
        (1..pad.length).forEach { raw += 0.toChar() }
        return StringBuilder().apply {
            (0 until raw.length step 3).forEach {
                val n: Int = (0xFF.and(raw[it].toInt()) shl 16) +
                    (0xFF.and(raw[it + 1].toInt()) shl 8) +
                    0xFF.and(raw[it + 2].toInt())
                listOf<Int>(
                    (n shr 18) and 0x3F,
                    (n shr 12) and 0x3F,
                    (n shr 6) and 0x3F,
                    n and 0x3F
                ).forEach { append(BASE64_SET[it]) }
            }
        }.dropLast(pad.length)
            .toString() + pad
    }

/**
 * Decode a Base64 string.
 */
val String.base64decoded: String
    get() {
        if (this.length % 4 != 0) throw IllegalArgumentException("The string \"${this}\" does not comply with BASE64 length requirement.")
        val clean = this.replace(RX_BASE64_CLEANR, "").replace("=", "A")
        val padLen = this.filter { it == '=' }.length
        return StringBuilder().apply {
            (0 until clean.length step 4).forEach {
                val n: Int = (BASE64_SET.indexOf(clean[it]) shl 18) +
                    (BASE64_SET.indexOf(clean[it + 1]) shl 12) +
                    (BASE64_SET.indexOf(clean[it + 2]) shl 6) +
                    BASE64_SET.indexOf(clean[it + 3])
                listOf<Int>(
                    0xFF.and(n shr 16),
                    0xFF.and(n shr 8),
                    0xFF.and(n)
                ).forEach { append(it.toChar()) }
            }
        }.dropLast(padLen)
            .toString()
    }
