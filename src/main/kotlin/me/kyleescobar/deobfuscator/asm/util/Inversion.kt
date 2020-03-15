package me.kyleescobar.deobfuscator.asm.util

import java.math.BigInteger

/**
 * Copyright (c) 2020 Kyle Escobar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

private val UNSIGNED_INT_MODULUS = BigInteger.ONE.shiftLeft(Integer.SIZE)

private val UNSIGNED_LONG_MODULUS = BigInteger.ONE.shiftLeft(java.lang.Long.SIZE)

fun invert(n: Int): Int = n.toBigInteger().modInverse(UNSIGNED_INT_MODULUS).toInt()

fun invert(n: Long): Long = n.toBigInteger().modInverse(UNSIGNED_LONG_MODULUS).toLong()

fun invert(n: Number): Number {
    return when(n) {
        is Int -> invert(n)
        is Long -> invert(n)
        else -> error(n)
    }
}

fun isInvertible(n: Int): Boolean = n and 1 == 1

fun isInvertible(n: Long): Boolean = isInvertible(n.toInt())

fun isInvertible(n: Number): Boolean {
    return when(n) {
        is Int, is Long -> isInvertible(n.toInt())
        else -> error(n)
    }
}