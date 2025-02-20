package land.sungbin.intellij.playground

import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.model.Pointer
import com.intellij.openapi.util.TextRange
import com.intellij.platform.backend.documentation.DocumentationData
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.platform.backend.documentation.InlineDocumentation
import com.intellij.platform.backend.documentation.InlineDocumentationProvider
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.createSmartPointer
import org.jetbrains.kotlin.idea.KotlinDocumentationProvider
import org.jetbrains.kotlin.idea.k2.codeinsight.quickDoc.KotlinDocumentationTargetProvider
import org.jetbrains.kotlin.idea.k2.codeinsight.quickDoc.KotlinInlineDocumentationProvider
import org.jetbrains.kotlin.idea.k2.codeinsight.quickDoc.KotlinPsiDocumentationTargetProvider
import org.jetbrains.kotlin.psi.KtDeclaration

private val kdocImageRegex =
  "!<a href=['\"](?<url>.*?)['\"]>((?<width>\\d+)?x(?<height>\\d+)?)?(?<alt>.*?)</a>".toRegex()

private fun renderKdocImages(kotlinDoc: String): String {
  val replace = kotlinDoc.replace(kdocImageRegex) {
    val url = it.groups["url"]?.value ?: return@replace it.value
    val width = it.groups["width"]?.value?.toIntOrNull()
    val height = it.groups["height"]?.value?.toIntOrNull()
    val alt = it.groups["alt"]?.value

    val buildString = buildString {
      append("""<img src="$url" """)
      if (alt != null) append("alt=\"$alt\" ")
      if (width != null) append("width=\"$width\" ")
      if (height != null) append("height=\"$height\" ")
      append(">")
    }
    buildString
  }
  return replace
}

internal class KotlinKDocImagePsiDocumentationTargetProvider : PsiDocumentationTargetProvider {
  private val delegate = KotlinPsiDocumentationTargetProvider()

  override fun documentationTarget(element: PsiElement, originalElement: PsiElement?): DocumentationTarget? =
    delegate.documentationTarget(element, originalElement)?.let {
      KotlinKDocImageDocumentationTarget(element, originalElement, it)
    }
}

internal class KotlinKDocImageDocumentationTargetProvider : DocumentationTargetProvider {
  private val delegate = KotlinDocumentationTargetProvider()
  override fun documentationTargets(file: PsiFile, offset: Int): List<DocumentationTarget> {
    val element = file.findElementAt(offset) ?: return emptyList()
    return delegate.documentationTargets(file, offset)
      .map { KotlinKDocImageDocumentationTarget(element, element, it) }
  }
}

@Suppress("UnstableApiUsage")
internal class KotlinKDocImageInlineDocumentationProvider : InlineDocumentationProvider {
  private val delegate = KotlinInlineDocumentationProvider()
  override fun inlineDocumentationItems(file: PsiFile?): Collection<InlineDocumentation> =
    delegate.inlineDocumentationItems(file).map(::KDocImageInlineDocumentation)

  override fun findInlineDocumentation(file: PsiFile, textRange: TextRange): InlineDocumentation? =
    delegate.findInlineDocumentation(file, textRange)?.let(::KDocImageInlineDocumentation)
}

@Suppress("UnstableApiUsage")
private class KDocImageInlineDocumentation(val delegate: InlineDocumentation) : InlineDocumentation by delegate {
  override fun renderText(): String? = delegate.renderText()?.let(::renderKdocImages)
}

private class KotlinKDocImageDocumentationTarget(
  val element: PsiElement,
  val originalElement: PsiElement?,
  val delegate: DocumentationTarget
) : DocumentationTarget by delegate {

  override fun computeDocumentation(): DocumentationResult? {
    val documentationResult = delegate.computeDocumentation()

    @Suppress("UnstableApiUsage")
    val html = (documentationResult as? DocumentationData)?.html?.let(::renderKdocImages) ?: return null
    return DocumentationResult.documentation(html)
  }

  override fun createPointer(): Pointer<out DocumentationTarget> {
    val elementPtr = element.createSmartPointer()
    val originalElementPtr = originalElement?.createSmartPointer()
    val delegatePtr = delegate.createPointer()
    return Pointer {
      val element = elementPtr.dereference() ?: return@Pointer null
      val delegate = delegatePtr.dereference() ?: return@Pointer null
      KotlinKDocImageDocumentationTarget(element, originalElementPtr?.dereference(), delegate)
    }
  }

  override fun computeDocumentationHint(): String? {
    return delegate.computeDocumentationHint()?.let(::renderKdocImages)
  }
}
