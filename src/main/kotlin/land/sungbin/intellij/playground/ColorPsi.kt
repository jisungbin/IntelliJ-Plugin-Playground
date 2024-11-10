package land.sungbin.intellij.playground

import com.intellij.openapi.application.readAction
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parentsOfType
import java.util.Collections
import kotlin.collections.plusAssign
import org.jetbrains.kotlin.analysis.utils.collections.mapToSet
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.isSimpleName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtVisitorVoidWithParameter
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

public data class NamedColor(
  public val containerName: String,
  public val colorName: String,
  public val hexCode: String,
)

public data class SemanticColor(
  public val groupNames: List<String>,
  public val colorName: String,
  public val colorAlias: String?,
  public val hexCode: String,
)

public class ColorPsi {
  private fun isValidForColorParsing(file: PsiFile): Boolean =
    !file.isDirectory && file.isValid && file.language == KotlinLanguage.INSTANCE

  public suspend fun parseColors(file: PsiFile): List<NamedColor> =
    readAction {
      if (!isValidForColorParsing(file)) return@readAction emptyList()
      ArrayList<NamedColor>().apply { ColorCollector().visitKtElementVoid(file.cast(), this) }
    }

  public suspend fun parseSemanticColors(file: PsiFile, colorNames: List<NamedColor>): List<SemanticColor> =
    readAction {
      if (!isValidForColorParsing(file)) return@readAction emptyList()
      ArrayList<SemanticColor>().apply { SemanticColorCollector(colorNames).visitKtElementVoid(file.cast(), this) }
    }

  private class ColorCollector : KtVisitorVoidWithParameter<MutableCollection<NamedColor>>() {
    override fun visitKtElementVoid(element: KtElement, data: MutableCollection<NamedColor>) {
      element.acceptChildren(this, data)
    }

    override fun visitPropertyVoid(property: KtProperty, data: MutableCollection<NamedColor>) {
      val containerName = property.parentOfType<KtObjectDeclaration>()?.name ?: return
      val colorName = property.nameIdentifier?.text ?: return

      val initializeExpression = property.initializer.safeAs<KtCallExpression>() ?: return
      val initializeCallee = initializeExpression.calleeExpression

      // TODO FQN으로 Color 타입 검사. 근데 PSI로 이게 가능?
      if (initializeCallee?.isSimpleName(COLOR_N) != true) return

      @Suppress("UsePropertyAccessSyntax") // -> KT-67467
      val hexCode = initializeExpression.valueArguments
        .mapNotNull { argument -> argument.getArgumentExpression().safeAs<KtConstantExpression>() }
        .singleOrNull { constant -> constant.elementType == KtStubElementTypes.INTEGER_CONSTANT }
        ?.text ?: return

      data += NamedColor(containerName, colorName, hexCode)
    }

    private companion object {
      private val COLOR_N = Name.identifier("Color")
    }
  }

  private class SemanticColorCollector(private val namedColors: List<NamedColor>) : KtVisitorVoidWithParameter<MutableCollection<SemanticColor>>() {
    private val availableContainerNames = namedColors.mapToSet(NamedColor::containerName)

    override fun visitKtElementVoid(element: KtElement, data: MutableCollection<SemanticColor>) {
      element.acceptChildren(this, data)
    }

    override fun visitPropertyVoid(property: KtProperty, data: MutableCollection<SemanticColor>) {
      val groupNames = property.parentsOfType<KtObjectDeclaration>().toList()
        .mapNotNull(KtObjectDeclaration::getName)
        .ifEmpty { return }
        .asReversed()
        .let(Collections::unmodifiableList)
      val colorName = property.nameIdentifier?.text ?: return

      // val ColorName [inline] get() = ContainerName.ColorName
      if (property.initializer == null) {
        val accessor = property.accessors.singleOrNull() ?: return

        val dotQualifiedInitializer = accessor.getChildOfType<KtDotQualifiedExpression>()
        if (dotQualifiedInitializer != null)
          return handleDotQualifiedInitializer(groupNames, colorName, dotQualifiedInitializer, data)

        val callInitializer = accessor.getChildOfType<KtCallExpression>()
        if (callInitializer != null)
          return handleCallInitializer(groupNames, colorName, callInitializer, data)
      } else { // val ColorName = [Color(hexCode)] or [ContainerName.ColorName]
        when (val initializer = property.initializer) {
          is KtDotQualifiedExpression -> handleDotQualifiedInitializer(groupNames, colorName, initializer, data)
          is KtCallExpression -> handleCallInitializer(groupNames, colorName, initializer, data)
        }
      }
    }

    private fun KtElement?.simpleNameOrNull(): String? =
      this?.safeAs<KtSimpleNameExpression>()?.getIdentifier()?.text

    private fun handleDotQualifiedInitializer(
      groupNames: List<String>,
      colorName: String,
      expression: KtDotQualifiedExpression,
      destination: MutableCollection<SemanticColor>,
    ) {
      val containerName = expression.receiverExpression.simpleNameOrNull() ?: return
      val colorAlias = expression.selectorExpression.simpleNameOrNull() ?: return

      if (containerName in availableContainerNames) {
        val hexColor = namedColors.find { named ->
          named.containerName == containerName && named.colorName == colorAlias
        }
          ?.hexCode ?: return

        destination += SemanticColor(groupNames, colorName, colorAlias, hexColor)
      }
    }

    private fun handleCallInitializer(
      groupNames: List<String>,
      colorName: String,
      expression: KtCallExpression,
      destination: MutableCollection<SemanticColor>,
    ) {
      val initializeExpression = expression.safeAs<KtCallExpression>() ?: return
      val initializeCallee = expression.safeAs<KtCallExpression>()?.calleeExpression
      if (initializeCallee?.isSimpleName(COLOR_N) != true) return

      @Suppress("UsePropertyAccessSyntax")
      val hexCode = initializeExpression.valueArguments
        .mapNotNull { argument -> argument.getArgumentExpression().safeAs<KtConstantExpression>() }
        .singleOrNull { constant -> constant.elementType == KtStubElementTypes.INTEGER_CONSTANT }
        ?.text ?: return

      destination += SemanticColor(groupNames, colorName, colorAlias = null, hexCode)
    }

    private companion object {
      private val COLOR_N = Name.identifier("Color")
    }
  }
}
