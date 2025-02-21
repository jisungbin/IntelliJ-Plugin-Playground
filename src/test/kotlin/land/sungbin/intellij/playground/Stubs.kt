// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.intellij.playground

import com.intellij.psi.PsiDirectory
import com.intellij.testFramework.junit5.fixture.TestFixture
import com.intellij.testFramework.junit5.fixture.psiFileFixture

fun TestFixture<PsiDirectory>.stubComposableAnnotation() =
  psiFileFixture(
    "Composable.kt",
    """
      |package androidx.compose.runtime
      |
      |@Target(
      |    AnnotationTarget.FUNCTION,
      |    AnnotationTarget.TYPE_USAGE,
      |    AnnotationTarget.TYPE,
      |    AnnotationTarget.TYPE_PARAMETER,
      |    AnnotationTarget.PROPERTY_GETTER,
      |)
      |annotation class Composable
    """.trimMargin(),
  )
