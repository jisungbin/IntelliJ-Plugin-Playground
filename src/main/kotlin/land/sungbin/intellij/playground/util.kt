// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.intellij.playground

@Suppress("NOTHING_TO_INLINE") // Syntactic sugar.
public inline fun <T> unsafeLazy(noinline initializer: () -> T): Lazy<T> =
  lazy(LazyThreadSafetyMode.NONE, initializer)
