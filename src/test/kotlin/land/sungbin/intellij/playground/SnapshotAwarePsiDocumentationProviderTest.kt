// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.intellij.playground

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.prop
import assertk.assertions.single
import com.intellij.lang.documentation.ide.IdeDocumentationTargetProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.writeAction
import com.intellij.platform.backend.documentation.DocumentationData
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.platform.backend.documentation.impl.computeDocumentationBlocking
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.editorFixture
import com.intellij.testFramework.junit5.fixture.moduleFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.psiFileFixture
import com.intellij.testFramework.junit5.fixture.sourceRootFixture
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeAll

@TestApplication class SnapshotAwarePsiDocumentationProviderTest {
  @Test fun test() {
    runBlocking {
      withContext(Dispatchers.EDT) {
        assertDocContains(
          """
            |/** %snapshot test 1x1% */
            |fun te<caret>st() {}
          """.trimMargin(),
          "hello",
        )
      }
    }
  }

  private suspend fun assertDocContains(
    code: String,
    @Language("HTML") expectedDoc: String,
  ) {
    val caretOffset = EditorTestUtil.getCaretPosition(code).coerceAtLeast(0)
    val cleanCode = code.replace(EditorTestUtil.CARET_TAG, "")

    val psiDocsManager = PsiDocumentManager.getInstance(project.get())
    val docsTargetProvider = IdeDocumentationTargetProvider.getInstance(project.get())

    writeAction {
      editor.get().document.setText(cleanCode)
      editor.get().caretModel.moveToOffset(caretOffset)

      val newDocument = file.get().fileDocument.apply { setText(cleanCode) }
      psiDocsManager.commitDocument(newDocument)
    }

    val targets = docsTargetProvider.documentationTargets(editor.get(), file.get(), caretOffset)
    assertThat(targets, name = "single target").single()

    val target = targets.first()
    val caretDocs = computeDocumentationBlocking(target.createPointer())

    assertThat(caretDocs, name = "caret docs")
      .isNotNull()
      .prop(DocumentationData::html)
      .isEqualTo(expectedDoc)
  }

  private companion object {
    // TODO property delegation으로 변경하기: TestFixture<T>.get() 중복 호출 제거
    private val project = projectFixture(openAfterCreation = true)
    private val module = project.moduleFixture()
    private val sourceRoot = module.sourceRootFixture()
    private val file = sourceRoot.psiFileFixture("test-file.kt", "")
    private val editor = file.editorFixture()

    @BeforeAll
    @JvmStatic fun prepare() {
      ApplicationManager.getApplication()
        .extensionArea
        .getExtensionPoint(PsiDocumentationTargetProvider.EP_NAME)
        .registerExtension(SnapshotAwarePsiDocumentationProvider(), project.get())
    }
  }
}
