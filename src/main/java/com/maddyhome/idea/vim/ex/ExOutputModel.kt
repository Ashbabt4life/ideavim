/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.ex

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.api.VimExOutputPanel
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.vimExOutput
import com.maddyhome.idea.vim.ui.ExOutputPanel

/**
 * @author vlan
 */
public class ExOutputModel private constructor(private val myEditor: Editor) : VimExOutputPanel {
  override var text: String? = null
    private set

  override fun output(text: String) {
    this.text = text
    if (!ApplicationManager.getApplication().isUnitTestMode) {
      injector.application.invokeAndWait {
        ExOutputPanel.getInstance(myEditor).setText(text)
      }
    }
  }

  override fun clear() {
    text = null
    if (!ApplicationManager.getApplication().isUnitTestMode) {
      injector.application.invokeAndWait {
        ExOutputPanel.getInstance(myEditor).deactivate(false)
      }
    }
  }

  public companion object {
    @JvmStatic
    public fun getInstance(editor: Editor): ExOutputModel {
      var model = editor.vimExOutput
      if (model == null) {
        model = ExOutputModel(editor)
        editor.vimExOutput = model
      }
      return model
    }
  }
}
