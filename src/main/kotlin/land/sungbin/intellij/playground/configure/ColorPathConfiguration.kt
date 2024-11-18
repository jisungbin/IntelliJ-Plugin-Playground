// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.intellij.playground.configure

import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import javax.inject.Inject
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import land.sungbin.intellij.playground.service.ColorStateService
import land.sungbin.intellij.playground.unsafeLazy

public class ColorPathConfiguration @Inject public constructor(private val project: Project) :
  BoundConfigurable(displayName = "Color Path"), // TODO what is 'helpTopic'?
  Configurable.NoScroll {
  private val colors by lazy { project.service<ColorStateService>() }

  override fun createPanel(): DialogPanel {
    val restoredUrl = colors.url
    var selectedUrl = restoredUrl

    return panel {
      row("Selected Color Path") {
        // TODO 파일 선택기 기본 경로 지정 (restoredUrl)
        val cell = textFieldWithBrowseButton(fileChooserDescriptor = KOTLIN_FILE_DESCRIPTOR) { file ->
          selectedUrl = file.url
          file.presentableUrl
        }
        cell
          .align(AlignX.FILL)
          .onReset { cell.component.text = restoredUrl.orEmpty() }
          .onApply {
            colors.url = selectedUrl

            @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launch { colors.readColorPsiWithBackgroundProgess() }
          }
          .onIsModified { cell.component.text != restoredUrl }
      }
    }
  }

  private companion object {
    private val KOTLIN_FILE_DESCRIPTOR by unsafeLazy { FileChooserDescriptorFactory.createSingleFileDescriptor("kt") }
  }
}
