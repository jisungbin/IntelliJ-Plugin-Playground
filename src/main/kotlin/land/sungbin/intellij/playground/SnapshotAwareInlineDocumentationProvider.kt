// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.intellij.playground

import com.intellij.openapi.util.TextRange
import com.intellij.platform.backend.documentation.InlineDocumentation
import com.intellij.platform.backend.documentation.InlineDocumentationProvider
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.k2.codeinsight.quickDoc.KotlinInlineDocumentationProvider
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

public class SnapshotAwareInlineDocumentationProvider : InlineDocumentationProvider {
  private val delegate = KotlinInlineDocumentationProvider()

  override fun inlineDocumentationItems(file: PsiFile?): Collection<InlineDocumentation> =
    delegate.inlineDocumentationItems(file).map(::SnapshotAwareInlineDocumentation)

  override fun findInlineDocumentation(file: PsiFile, textRange: TextRange): InlineDocumentation? =
    delegate.findInlineDocumentation(file, textRange)?.let(::SnapshotAwareInlineDocumentation)
}

private class SnapshotAwareInlineDocumentation(private val delegate: InlineDocumentation) : InlineDocumentation by delegate {
  override fun renderText(): String? {
    val html = delegate.renderText() ?: return null
    val project = delegate.ownerTarget?.safeAs<NavigatablePsiElement>()?.project ?: return html
    return html.handleSnapshotDocs(project)
  }
}
