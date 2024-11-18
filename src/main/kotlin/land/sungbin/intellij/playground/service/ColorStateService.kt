// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.intellij.playground.service

import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.isFile
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.psi.PsiManager
import com.intellij.util.concurrency.annotations.RequiresReadLock
import javax.inject.Inject
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import land.sungbin.intellij.playground.psi.ColorPsi
import land.sungbin.intellij.playground.psi.NamedColor
import land.sungbin.intellij.playground.psi.SemanticColor

@[Service(Service.Level.PROJECT) Storage(value = "colorPath.xml")]
public class ColorStateService @Inject public constructor(private val project: Project) : BaseState() {
  public var url: String? by string()

  @Transient private val _names = mutableListOf<NamedColor>()
  public val names: List<NamedColor> get() = _names

  @Transient private val _semantics = mutableListOf<SemanticColor>()
  public val semantics: List<SemanticColor> get() = _semantics

  // TODO 서비스 초기화와 동시에 readColorPsi()가 올바른 동작인가?
  init {
    @OptIn(DelicateCoroutinesApi::class)
    GlobalScope.launch { readColorPsiWithBackgroundProgess() }
  }

  @RequiresReadLock public fun readColorPsi() {
    val target = VirtualFileManager.getInstance().findFileByUrl(url ?: return)?.takeIf(VirtualFile::isFile) ?: return
    val file = PsiManager.getInstance(project).findFile(target)!!
    val colors = ColorPsi()

    _names += colors.parseColors(file)
    _semantics += colors.parseSemanticColors(file, names)
  }

  @Suppress("UnstableApiUsage") // withBackgroundProgress
  public suspend fun readColorPsiWithBackgroundProgess() {
    withBackgroundProgress(
      project = project,
      title = "Load Colors",
      cancellable = true,
    ) {
      readAction(::readColorPsi)
    }
  }
}
