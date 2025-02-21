// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.intellij.playground

import com.intellij.model.Pointer
import com.intellij.platform.backend.documentation.DocumentationData
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.createSmartPointer
import org.jetbrains.kotlin.idea.k2.codeinsight.quickDoc.KotlinPsiDocumentationTargetProvider
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

public class SnapshotAwarePsiDocumentationProvider : PsiDocumentationTargetProvider {
  private val delegate = KotlinPsiDocumentationTargetProvider()

  override fun documentationTarget(element: PsiElement, originalElement: PsiElement?): DocumentationTarget? =
    delegate.documentationTarget(element, originalElement)
      ?.let { SnapshotAwareDocumentationTarget(element, originalElement, it) }
}

private class SnapshotAwareDocumentationTarget(
  private val element: PsiElement,
  private val originalElement: PsiElement?,
  private val delegate: DocumentationTarget,
) : DocumentationTarget by delegate {
  override fun computeDocumentation(): DocumentationResult? {
    val documentation = delegate.computeDocumentation()
    val html = documentation.safeAs<DocumentationData>()?.html ?: return documentation
    return DocumentationResult.documentation(html.handleSnapshotDocs(element))
  }

  override fun createPointer(): Pointer<out DocumentationTarget> {
    val elementPtr = element.createSmartPointer()
    val originalElementPtr = originalElement?.createSmartPointer()
    val delegatePtr = delegate.createPointer()

    return Pointer {
      val element = elementPtr.dereference() ?: return@Pointer null
      val delegate = delegatePtr.dereference() ?: return@Pointer null
      SnapshotAwareDocumentationTarget(element, originalElementPtr?.dereference(), delegate)
    }
  }
}
