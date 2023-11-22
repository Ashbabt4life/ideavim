/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action

import com.intellij.idea.TestFor
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class ActionsTest : VimTestCase() {
  @Test
  @TestFor(issues = ["VIM-3203"])
  fun `split line action`() {
    configureByText(
      """
      Lorem Ipsum

      Lorem ipsum dolor sit amet,$c consectetur adipiscing elit
      Sed in orci mauris.
      Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    )

    fixture.performEditorAction("EditorSplitLine")

    assertState(
      """
      Lorem Ipsum

      Lorem ipsum dolor sit amet,$c
       consectetur adipiscing elit
      Sed in orci mauris.
      Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    )
  }
}
