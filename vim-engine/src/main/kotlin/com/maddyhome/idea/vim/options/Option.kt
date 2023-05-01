/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.options

import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.datatypes.parseNumber
import java.util.*

/**
 * The base class of option types
 *
 * This class is generic on the datatype of the option, which must be a type derived from [VimDataType], such as
 * [VimInt] or [VimString]. Vim data types are used so that we can easily use options as VimScript variables.
 *
 * A note on variance: derived classes will also use a derived type of [VimDataType], which means that e.g.
 * `StringOption` would derive from `Option<VimString>`, which is not assignable to `Option<VimDataType>`. This can work
 * if we make the type covariant (e.g. `Option<out T : VimDataType>`) however the type is not covariant - it's not
 * solely a producer ([checkIfValueValid] is a consumer, for example), so we must keep [T] as invariant. Furthermore,
 * if we make it covariant, then we also lose some type safety, with something like
 * `setValue(numberOption, VimString("foo"))` not treated as an error.
 *
 * We also want to avoid a sealed hierarchy, since we create object instances with custom validation for some options.
 *
 * @param name  The name of the option
 * @param declaredScope The declared scope of the option - global, global-local, local-to-buffer, local-to-window
 * @param abbrev  An abbreviated name for the option, recognised by `:set`
 * @param defaultValue  The default value of the option, if not set by the user
 * @param unsetValue    The value of the local part of a global-local option, if the local part has not been set
 */
public abstract class Option<T : VimDataType>(public val name: String,
                                              public val declaredScope: OptionDeclaredScope,
                                              public val abbrev: String,
                                              defaultValue: T,
                                              public val unsetValue: T) {
  private var defaultValueField = defaultValue

  public open val defaultValue: T
    get() = defaultValueField

  internal fun overrideDefaultValue(newDefaultValue: T) {
    defaultValueField = newDefaultValue
  }

  // todo 1.9 should return Result with exceptions
  public abstract fun checkIfValueValid(value: VimDataType, token: String)
  public abstract fun parseValue(value: String, token: String): VimDataType
}

public open class StringOption(
  name: String,
  declaredScope: OptionDeclaredScope,
  abbrev: String,
  defaultValue: VimString,
  unsetValue: VimString = VimString.EMPTY,
  public val boundedValues: Collection<String>? = null,
) : Option<VimString>(name, declaredScope, abbrev, defaultValue, unsetValue) {

  public constructor(
    name: String,
    declaredScope: OptionDeclaredScope,
    abbrev: String,
    defaultValue: String,
    boundedValues: Collection<String>? = null,
  ) : this(name, declaredScope, abbrev, VimString(defaultValue), boundedValues = boundedValues)

  override fun checkIfValueValid(value: VimDataType, token: String) {
    if (value !is VimString) {
      throw exExceptionMessage("E474", token)
    }

    if (value.value.isEmpty()) {
      return
    }

    if (boundedValues != null && !boundedValues.contains(value.value)) {
      throw exExceptionMessage("E474", token)
    }
  }

  override fun parseValue(value: String, token: String): VimString =
    VimString(value).also { checkIfValueValid(it, token) }

  public fun appendValue(currentValue: VimString, value: VimString): VimString =
    VimString(currentValue.value + value.value)

  public fun prependValue(currentValue: VimString, value: VimString): VimString =
    VimString(value.value + currentValue.value)

  public fun removeValue(currentValue: VimString, value: VimString): VimString {
    // TODO: Not sure this is correct. Should replace just the first occurrence?
    return VimString(currentValue.value.replace(value.value, ""))
  }
}

/**
 * Represents a string that is a comma-separated list of values
 *
 * Note that we have tried multiple ways to represent a string list option, from a separate class similar to
 * [StringListOption] or a combined string option. While a string list option "is-a" string option, its operations
 * (append, prepend and remove) are implemented very differently to the string option. Unless there is a good reason to
 * do so, we do not expect this to change again.
 */
public open class StringListOption(
  name: String,
  declaredScope: OptionDeclaredScope,
  abbrev: String,
  defaultValue: VimString,
  public val boundedValues: Collection<String>? = null,
) : Option<VimString>(name, declaredScope, abbrev, defaultValue, VimString.EMPTY) {

  public constructor(
    name: String,
    declaredScope: OptionDeclaredScope,
    abbrev: String,
    defaultValue: String,
    boundedValues: Collection<String>? = null,
  ) : this(name, declaredScope, abbrev, VimString(defaultValue), boundedValues)

  override fun checkIfValueValid(value: VimDataType, token: String) {
    if (value !is VimString) {
      throw exExceptionMessage("E474", token)
    }

    if (value.value.isEmpty()) {
      return
    }

    if (boundedValues != null && split(value.value).any { !boundedValues.contains(it) }) {
      throw exExceptionMessage("E474", token)
    }
  }

  override fun parseValue(value: String, token: String): VimString =
    VimString(value).also { checkIfValueValid(it, token) }

  public fun appendValue(currentValue: VimString, value: VimString): VimString {
    // TODO: What happens if we're trying to add a sublist that already exists?
    if (split(currentValue.value).contains(value.value)) return currentValue
    return VimString(joinValues(currentValue.value, value.value))
  }

  public fun prependValue(currentValue: VimString, value: VimString): VimString {
    // TODO: What happens if we're trying to add a sublist that already exists?
    if (split(currentValue.value).contains(value.value)) return currentValue
    return VimString(joinValues(value.value, currentValue.value))
  }

  public fun removeValue(currentValue: VimString, value: VimString): VimString {
    val valuesToRemove = split(value.value)
    val elements = split(currentValue.value).toMutableList()
    if (Collections.indexOfSubList(elements, valuesToRemove) != -1) {
      // see `:help set`
      // When the option is a list of flags, {value} must be
      // exactly as they appear in the option.  Remove flags
      // one by one to avoid problems.
      elements.removeAll(valuesToRemove)
    }
    return VimString(elements.joinToString(separator = ","))
  }

  public open fun split(value: String): List<String> = value.split(",")

  private fun joinValues(first: String, second: String): String {
    val separator = if (first.isNotEmpty()) "," else ""
    return first + separator + second
  }
}

public open class NumberOption(
  name: String,
  declaredScope: OptionDeclaredScope,
  abbrev: String,
  defaultValue: VimInt,
  unsetValue: VimInt = VimInt.MINUS_ONE
) :
  Option<VimInt>(name, declaredScope, abbrev, defaultValue, unsetValue) {

  public constructor(name: String, declaredScope: OptionDeclaredScope, abbrev: String, defaultValue: Int, unsetValue: Int = -1) : this(
    name,
    declaredScope,
    abbrev,
    VimInt(defaultValue),
    if (unsetValue == -1) VimInt.MINUS_ONE else VimInt(unsetValue)
  )

  override fun checkIfValueValid(value: VimDataType, token: String) {
    if (value !is VimInt) throw exExceptionMessage("E521", token)
  }

  override fun parseValue(value: String, token: String): VimInt =
    VimInt(parseNumber(value) ?: throw exExceptionMessage("E521", token)).also { checkIfValueValid(it, token) }

  public fun addValues(value1: VimInt, value2: VimInt): VimInt = VimInt(value1.value + value2.value)
  public fun multiplyValues(value1: VimInt, value2: VimInt): VimInt = VimInt(value1.value * value2.value)
  public fun subtractValues(value1: VimInt, value2: VimInt): VimInt = VimInt(value1.value - value2.value)
}

public open class UnsignedNumberOption(
  name: String,
  declaredScope: OptionDeclaredScope,
  abbrev: String,
  defaultValue: VimInt,
) : NumberOption(name, declaredScope, abbrev, defaultValue) {

  public constructor(name: String, declaredScope: OptionDeclaredScope, abbrev: String, defaultValue: Int) : this(
    name,
    declaredScope,
    abbrev,
    VimInt(defaultValue)
  )

  override fun checkIfValueValid(value: VimDataType, token: String) {
    super.checkIfValueValid(value, token)
    if ((value as VimInt).value < 0) {
      throw ExException("E487: Argument must be positive: $token")
    }
  }
}

public class ToggleOption(name: String, declaredScope: OptionDeclaredScope, abbrev: String, defaultValue: VimInt) :
  Option<VimInt>(name, declaredScope, abbrev, defaultValue, VimInt.MINUS_ONE) {
  public constructor(name: String, declaredScope: OptionDeclaredScope, abbrev: String, defaultValue: Boolean) : this(
    name,
    declaredScope,
    abbrev,
    if (defaultValue) VimInt.ONE else VimInt.ZERO
  )

  override fun checkIfValueValid(value: VimDataType, token: String) {
    if (value !is VimInt) throw exExceptionMessage("E474", token)
  }

  override fun parseValue(value: String, token: String): Nothing = throw exExceptionMessage("E474", token)
}
