// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.intellij.playground

import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.intellij.openapi.application.readAction
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.moduleFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.psiFileFixture
import com.intellij.testFramework.junit5.fixture.sourceRootFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import kotlin.test.Test
import kotlin.test.fail
import kotlinx.coroutines.runBlocking

@TestApplication class CellComponentLineMarkerProviderDescriptorTest {
  private val descriptor = CellComponentLineMarkerProviderDescriptor()

  @Test fun testCellA() {
    runBlocking {
      readAction {
        val target = testComponentsFile.offsetForWindow("fun Ce|llA() = Unit")
        val identifier = testComponentsFile.findElementAt(target) ?: fail("No identifier found")
        val lineInfo = descriptor.getLineMarkerInfo(identifier)

        assertThat(lineInfo).isNull()
      }
    }
  }

  @Test fun testB() {
    runBlocking {
      readAction {
        val target = testComponentsFile.offsetForWindow("fun B|() = Unit")
        val identifier = testComponentsFile.findElementAt(target) ?: fail("No identifier found")
        val lineInfo = descriptor.getLineMarkerInfo(identifier)

        assertThat(lineInfo).isNull()
      }
    }
  }

  @Test fun testCellC() {
    runBlocking {
      readAction {
        val target = cellComponentsFile.offsetForWindow("fun Ce|llC() = Unit")
        val identifier = cellComponentsFile.findElementAt(target) ?: fail("No identifier found")
        val lineInfo = descriptor.getLineMarkerInfo(identifier)

        assertThat(lineInfo).isNotNull()
      }
    }
  }

  @Test fun testtD() {
    runBlocking {
      readAction {
        val target = cellComponentsFile.offsetForWindow("fun D|() = Unit")
        val identifier = cellComponentsFile.findElementAt(target) ?: fail("No identifier found")
        val lineInfo = descriptor.getLineMarkerInfo(identifier)

        assertThat(lineInfo).isNull()
      }
    }
  }

  @Test fun testCellE() {
    runBlocking {
      readAction {
        val target = cellComponentsFile.offsetForWindow("fun Ce|llE() = Unit")
        val identifier = cellComponentsFile.findElementAt(target) ?: fail("No identifier found")
        val lineInfo = descriptor.getLineMarkerInfo(identifier)

        assertThat(lineInfo).isNull()
      }
    }
  }

  private companion object {
    private val tempDir = tempPathFixture()
    private val project by projectFixture(tempDir, defaultOpenProjectTask, openAfterCreation = true)
    private val module by ::project.asFixture().moduleFixture(defaultModuleName)
    private val sourceRoot = ::module.asFixture().sourceRootFixture()
    private val testComponentsFile by sourceRoot.psiFileFixture(
      "TestComponents.kt",
      """
        |package test.component
        |
        |import androidx.compose.runtime.Composable
        |
        |@Composable
        |fun CellA() = Unit
        |
        |@Composable
        |fun B() = Unit
      """.trimMargin(),
    )
    private val cellComponentsFile by sourceRoot.psiFileFixture(
      "CellComponents.kt",
      """
        |package cell.component
        |
        |import androidx.compose.runtime.Composable
        |
        |@Composable
        |fun CellC() = Unit
        |
        |@Composable
        |fun D() = Unit
        |
        |fun CellE() = Unit
      """.trimMargin(),
    )

    init {
      sourceRoot.stubComposableAnnotation()
    }
  }
}
