/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class JoinLinesCommandTest : VimTestCase() {
  @VimBehaviorDiffers(description = "Different caret position")
  @Test
  fun `test simple join`() {
    doTest(
      exCommand("j"),
      """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                Lorem ipsum dolor sit amet,$c consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @VimBehaviorDiffers(description = "Different caret position")
  @Test
  fun `test simple join full command`() {
    doTest(
      exCommand("join"),
      """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                Lorem ipsum dolor sit amet,$c consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @VimBehaviorDiffers(description = "Different caret position")
  @Test
  fun `test join with range`() {
    doTest(
      exCommand("4,6j"),
      """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      """
                Lorem Ipsum

                Lorem ipsum dolor sit amet,
                consectetur adipiscing elit Sed in orci mauris.$c Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
      Mode.NORMAL(),
    )
  }

  @Test
  fun `test join multicaret`() {
    configureByText(
      """
                Lorem Ipsum

                ${c}Lorem ipsum dolor sit amet,
                consectetur adipiscing elit
                Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("Vjj"))
    typeText(commandToKeys("join"))
    assertState(
      """
                Lorem Ipsum

                Lorem ipsum dolor sit amet, consectetur adipiscing elit$c Sed in orci mauris.
                Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
  }
}
