/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.change.insert

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import javax.swing.KeyStroke

@CommandOrMotion(keys = ["<C-V>", "<C-Q>"], modes = [Mode.INSERT, Mode.CMD_LINE])
public class InsertCompletedLiteralAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.INSERT
  override val argumentType: Argument.Type = Argument.Type.DIGRAPH

  override fun execute(editor: VimEditor, context: ExecutionContext, cmd: Command, operatorArguments: OperatorArguments): Boolean {
    // The converted literal character has been captured as an argument, push it back through key handler
    val keyStroke = KeyStroke.getKeyStroke(cmd.argument!!.character)
    val keyHandler = KeyHandler.getInstance()
    keyHandler.handleKey(editor, keyStroke, context, keyHandler.keyHandlerState)
    return true
  }
}
