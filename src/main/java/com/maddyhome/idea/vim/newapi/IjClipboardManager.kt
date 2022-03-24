package com.maddyhome.idea.vim.newapi

import com.intellij.codeInsight.editorActions.TextBlockTransferable
import com.intellij.codeInsight.editorActions.TextBlockTransferableData
import com.intellij.ide.CopyPasteManagerEx
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.CaretStateTransferableData
import com.intellij.openapi.editor.RawText
import com.maddyhome.idea.vim.api.VimClipboardManager
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.helper.TestClipboardModel
import java.awt.HeadlessException
import java.awt.datatransfer.Transferable

@Service
class IjClipboardManager : VimClipboardManager {
  override fun setClipboardText(text: String, rawText: String, transferableData: List<Any>): Any? {
    val transferableData1 = (transferableData as List<TextBlockTransferableData>).toMutableList()
    try {
      val s = TextBlockTransferable.convertLineSeparators(text, "\n", transferableData1)
      if (transferableData1.none { it is CaretStateTransferableData }) {
        // Manually add CaretStateTransferableData to avoid adjustment of copied text to multicaret
        transferableData1+= CaretStateTransferableData(intArrayOf(0), intArrayOf(s.length))
      }
      logger.debug { "Paste text with transferable data: ${transferableData1.joinToString { it.javaClass.name }}" }
      val content = TextBlockTransferable(s, transferableData1, RawText(rawText))
      setContents(content)
      return content
    } catch (ignored: HeadlessException) {
    }
    return null
  }

  private fun setContents(contents: Transferable) {
    if (ApplicationManager.getApplication().isUnitTestMode) {
      TestClipboardModel.contents = contents
      CopyPasteManagerEx.getInstanceEx().setContents(contents)
    } else {
      CopyPasteManagerEx.getInstanceEx().setContents(contents)
    }
  }

  companion object {
    val logger = vimLogger<IjClipboardManager>()
  }
}
