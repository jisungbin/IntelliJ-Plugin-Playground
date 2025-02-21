// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.intellij.playground

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.single
import com.intellij.lang.documentation.ide.IdeDocumentationTargetProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.writeAction
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
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.intellij.lang.annotations.Language
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.junit.jupiter.api.BeforeAll

@TestApplication class SnapshotAwarePsiDocumentationProviderTest {
  private val editor by ::file.asFixture().editorFixture()
  private val expectedSnapshotRoot = tempDir.resolve(defaultModuleName).resolve("images")

  @Test fun test() {
    runBlocking {
      withContext(Dispatchers.EDT) {
        assertDocContains(
          """
            |/**
            |  * my test function
            |  *
            |  * %snapshot test 1000x2000% 
            |  */
            |fun te<caret>st() {}
          """.trimMargin(),
          """
            <img src="file:${expectedSnapshotRoot.resolve("test.png")}" alt="test" width="1000" height="2000">
          """.trimIndent(),
        )
      }
    }
  }

  @Test fun test2() {
    runBlocking {
      withContext(Dispatchers.EDT) {
        assertDocContains(
          """
            |/** my test function */
            |fun te<caret>st() {}
          """.trimMargin(),
          null,
        )
      }
    }
  }

  @Test fun test3() {
    runBlocking {
      withContext(Dispatchers.EDT) {
        assertDocContains(
          """
            |/**
            |  * my test function
            |  *
            |  * %snapshot test% 
            |  */
            |fun te<caret>st() {}
          """.trimMargin(),
          """
            <img src="file:${expectedSnapshotRoot.resolve("test.png")}" alt="test">
          """.trimIndent(),
        )
      }
    }
  }

  @Test fun test4() {
    runBlocking {
      withContext(Dispatchers.EDT) {
        assertDocContains(
          """
            |/**
            |  * my test function
            |  *
            |  * %snapshot test 1000x2000% 
            |  */
            |fun test() {}
          """.trimMargin(),
          null,
        )
      }
    }
  }

  private suspend fun assertDocContains(
    code: String,
    @Language("HTML") expectedSnapshotTag: String?,
  ) {
    val caretOffset = EditorTestUtil.getCaretPosition(code).coerceAtLeast(0)
    val cleanCode = code.replace(EditorTestUtil.CARET_TAG, "")

    val psiDocsManager = PsiDocumentManager.getInstance(project)
    val docsTargetProvider = IdeDocumentationTargetProvider.getInstance(project)

    writeAction {
      editor.document.setText(cleanCode)
      editor.caretModel.moveToOffset(caretOffset)

      val newDocument = file.fileDocument.apply { setText(cleanCode) }
      psiDocsManager.commitDocument(newDocument)
    }

    val targets = docsTargetProvider.documentationTargets(editor, file, caretOffset)
    assertThat(targets, name = "single target").single()

    val target = targets.first()
    val caretDocs = computeDocumentationBlocking(target.createPointer())
    assertThat(caretDocs, name = "exists caret docs").isNotNull()

    val imageTags = Jsoup.parse(caretDocs!!.html).select("img")
    if (expectedSnapshotTag == null)
      assertThat(imageTags, name = "no image").isEmpty()
    else
      assertThat(imageTags, name = "single image")
        .single()
        .transform(name = "image tag", transform = Element::toString)
        .isEqualTo(expectedSnapshotTag)
  }

  private companion object {
    private val tempDir by tempPathFixture()
    private val project by projectFixture(::tempDir.asFixture(), defaultOpenProjectTask, openAfterCreation = true)
    private val module by ::project.asFixture().moduleFixture(defaultModuleName)
    private val sourceRoot by ::module.asFixture().sourceRootFixture()
    private val file by ::sourceRoot.asFixture().psiFileFixture("test-file.kt", "")

    @BeforeAll
    @JvmStatic fun prepare() {
      ApplicationManager.getApplication()
        .extensionArea
        .getExtensionPoint(PsiDocumentationTargetProvider.EP_NAME)
        .registerExtension(SnapshotAwarePsiDocumentationProvider(), project)
    }
  }
}
