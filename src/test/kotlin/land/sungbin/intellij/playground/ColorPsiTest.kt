package land.sungbin.intellij.playground

import assertk.assertThat
import assertk.assertions.containsExactly
import com.intellij.openapi.application.readAction
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.idea.KotlinLanguage

class ColorPsiTest : BasePlatformTestCase() {
  @BeforeTest fun setup() {
    setUp()
  }

  // https://github.com/JetBrains/intellij-community/blob/690badb9ccf4e668ac8f1e70a43d061e9894f4fa/platform/platform-tests/testSrc/com/intellij/workspaceModel/core/fileIndex/CustomKindFileSetTest.kt#L50-L51
  // runTest가 없나? runBlocking으로 suspend @Test를 진행하고 있다.
  @Test fun parseColors() = runBlocking {
    val file = readAction {
      createLightFile(
        "MyColors.kt",
        KotlinLanguage.INSTANCE,
        /* text = */ MY_COLORS_KT,
      )
    }

    assertThat(ColorPsi().parseColors(file)).containsExactly(
      NamedColor("MyColors", "One", "0xFF111111"),
      NamedColor("MyColors", "Two", "0xFF222222"),
      NamedColor("MyColors", "Three", "0xFF333333"),
      NamedColor("MyColors", "Four", "0xFF444444"),
      NamedColor("MyColors", "Five", "0xFF555555"),
    )
  }

  @Test fun parseSemanticColors() = runBlocking {
    val file = readAction {
      createLightFile(
        "MyColorGroups.kt",
        KotlinLanguage.INSTANCE,
        /* text = */ MY_COLOR_GROUPS_KT,
      )
    }
    val namedColors = listOf(
      NamedColor("MyColors", "One", "0xFF111111"),
      NamedColor("MyColors", "Two", "0xFF222222"),
      NamedColor("MyColors", "Three", "0xFF333333"),
      NamedColor("MyColors", "Four", "0xFF444444"),
      NamedColor("MyColors", "Five", "0xFF555555"),
    )

    assertThat(ColorPsi().parseSemanticColors(file, namedColors)).containsExactly(
      SemanticColor(listOf("MyColorGroups", "First"), "SemanticOne", "One", "0xFF111111"),
      SemanticColor(listOf("MyColorGroups", "First"), "SemanticTwo", "Two", "0xFF222222"),
      SemanticColor(listOf("MyColorGroups", "Second"), "SemanticThree", colorAlias = null, "0xFF333333"),
      SemanticColor(listOf("MyColorGroups", "Second"), "SemanticFour", colorAlias = null, "0xFF444444"),
    )
  }

  @AfterTest fun cleanup() {
    tearDown()
  }

  private companion object {
    @Language("kotlin")
    private val MY_COLORS_KT = """
object MyColors {
  val One = Color(0xFF111111)
  val Two = Color(0xFF222222)
  val Three = Color(0xFF333333)
  val Four = Color(0xFF444444)
  val Five = Color(0xFF555555)
}
    """.trimIndent()

    @Language("kotlin")
    private val MY_COLOR_GROUPS_KT = """
object MyColorGroups {
  object First {
    val SemanticOne inline get() = MyColors.One
    val SemanticTwo get() = MyColors.Two
  }

  object Second {
    val SemanticThree = Color(0xFF333333) // Not from MyColors
    val SemanticFour get() = Color(0xFF444444)
  }
}
    """.trimIndent()
  }
}