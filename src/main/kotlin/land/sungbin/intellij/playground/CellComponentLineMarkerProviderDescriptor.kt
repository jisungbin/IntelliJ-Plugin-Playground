// Copyright 2023 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.intellij.playground

import androidx.compose.compiler.plugins.kotlin.ComposeClassIds
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.ui.IconManager
import javax.swing.Icon
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.utils.addToStdlib.cast

public class CellComponentLineMarkerProviderDescriptor : LineMarkerProviderDescriptor() {
  override fun getName(): String = "Cell component call"
  override fun getIcon(): Icon = ICON

  override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
    if (element !is LeafPsiElement || element.elementType != KtTokens.IDENTIFIER)
      return null

    val parentFunction = element.parent.parent as? KtCallExpression ?: return null
    if (!isCellComponentInvocation(parentFunction)) return null

    return LineMarkerInfo<PsiElement>(
      element,
      element.textRange,
      ICON,
      /* tooltipProvider = */ { getName() },
      /* navHandler = */ null,
      GutterIconRenderer.Alignment.RIGHT,
      /* accessibleNameProvider = */ { getName() },
    )
  }

  private fun isCellComponentInvocation(parentFunction: KtCallExpression): Boolean =
    analyze(parentFunction) {
      val callInfo = parentFunction.cast<KtElement>().resolveToCall() ?: return false
      val symbol = callInfo.singleFunctionCallOrNull()?.symbol ?: return false

      val isCellComponentPackage =
        symbol.callableId?.packageName?.asString()?.startsWith("cell.component") == true &&
          symbol.callableId?.callableName?.asString()?.startsWith("Cell") == true
      val isComposable = ComposeClassIds.Composable in symbol.annotations

      return isCellComponentPackage && isComposable
    }

  private companion object {
    private val ICON by unsafeLazy {
      IconManager.getInstance()
        .getIcon(
          "icons/cell.svg",
          CellComponentLineMarkerProviderDescriptor::class.java.classLoader,
        )
    }
  }
}
