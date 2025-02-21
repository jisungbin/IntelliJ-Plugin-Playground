// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.intellij.playground

import com.intellij.project.stateStore
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.base.util.module

private val regex = Regex("%snapshot\\s+(?<name>\\S+)(?:\\s+(?<width>\\d+)x(?<height>\\d+))?%")

internal fun String.handleSnapshotDocs(element: PsiElement): String {
  val snapshotRoot = element.project.stateStore.projectBasePath.resolve(element.module?.name.orEmpty())
  return replace(regex) {
    val name = it.groups["name"]!!.value
    val snapshotDir = snapshotRoot.resolve("images/$name.png")
    val width = it.groups["width"]?.value?.toIntOrNull()
    val height = it.groups["height"]?.value?.toIntOrNull()

    buildString {
      append("<img src=\"file:$snapshotDir\"")
      append(" alt=\"$name\"")
      if (width != null && height != null) append(" width=\"$width\" height=\"$height\"")
      append(">")
    }
  }
}
