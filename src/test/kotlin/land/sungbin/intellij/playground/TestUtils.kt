// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.intellij.playground

import com.intellij.psi.PsiFile
import kotlin.test.fail

fun PsiFile.offsetForWindow(window: String, startIndex: Int = 0): Int =
  text.offsetForWindow(window, startIndex)

private fun String.offsetForWindow(window: String, startIndex: Int = 0): Int {
  require(window.count { it == '|' } == 1) {
    "Must provide exactly one '|' character in window. Got \"$window\""
  }

  val delta = window.indexOf("|")
  val target = window.substring(0, delta) + window.substring(delta + 1)
  val start = indexOf(target, startIndex - delta)
  if (start < 0) fail("Didn't find the string $target in the source of $this")

  return start + delta
}
