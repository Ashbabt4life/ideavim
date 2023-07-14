/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.put

import com.maddyhome.idea.vim.command.SelectionType

public data class TextData(
  val text: String,
  val typeInRegister: SelectionType,
  val transferableData: List<Any>,
  val registerChar: Char?,
)