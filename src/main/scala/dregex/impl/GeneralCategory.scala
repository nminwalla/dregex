package dregex.impl

import scala.collection.breakOut

object GeneralCategory {

  /**
   * Unicode categories are named in Java using an integer. The mapping 
   * is not explicit, so we need to add it here.
   */
  val categories: Map[Byte, String] = {
    import Character._
    Map(
      UNASSIGNED -> "Cn",
      UPPERCASE_LETTER -> "Lu",
      LOWERCASE_LETTER -> "Ll",
      TITLECASE_LETTER -> "Lt",
      MODIFIER_LETTER -> "Lm",
      OTHER_LETTER -> "Lo",
      NON_SPACING_MARK -> "Mn",
      ENCLOSING_MARK -> "Me",
      COMBINING_SPACING_MARK -> "Mc",
      DECIMAL_DIGIT_NUMBER -> "Nd",
      LETTER_NUMBER -> "Nl",
      OTHER_NUMBER -> "No",
      SPACE_SEPARATOR -> "Zs",
      LINE_SEPARATOR -> "Zl",
      PARAGRAPH_SEPARATOR -> "Zp",
      CONTROL -> "Cc",
      FORMAT -> "Cf",
      PRIVATE_USE -> "Co",
      SURROGATE -> "Cs",
      DASH_PUNCTUATION -> "Pd",
      START_PUNCTUATION -> "Ps",
      END_PUNCTUATION -> "Pe",
      CONNECTOR_PUNCTUATION -> "Pc",
      OTHER_PUNCTUATION -> "Po",
      MATH_SYMBOL -> "Sm",
      CURRENCY_SYMBOL -> "Sc",
      MODIFIER_SYMBOL -> "Sk",
      OTHER_SYMBOL -> "So",
      INITIAL_QUOTE_PUNCTUATION -> "Pi",
      FINAL_QUOTE_PUNCTUATION -> "Pf")
  }

  val binaryProperties: Map[String, Int => Boolean] = {
    val unicodePropClass = Class.forName("java.util.regex.UnicodeProp")
    val isMethod = unicodePropClass.getMethod("is", classOf[Int])
    isMethod.setAccessible(true)
    unicodePropClass.getEnumConstants.map { value =>
      val enumValue = value.asInstanceOf[Enum[_ <: Enum[_]]]
      def evaluationFn(codePoint: Int) = {
        isMethod.invoke(enumValue, codePoint.asInstanceOf[Object]).asInstanceOf[Boolean]
      }
      enumValue.name() -> evaluationFn _
    }(breakOut)
  }

}
