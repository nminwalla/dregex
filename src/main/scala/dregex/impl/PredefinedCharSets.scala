package dregex.impl

import dregex.impl.RegexTree.AbstractRange
import dregex.impl.RegexTree.Lit
import dregex.impl.RegexTree.Wildcard
import dregex.impl.UnicodeChar.FromCharConversion
import dregex.impl.UnicodeChar.FromIntConversion
import dregex.impl.RegexTree.CharSet
import dregex.impl.RegexTree.CharRange
import java.lang.Character.UnicodeBlock
import scala.collection.JavaConversions._
import scala.collection.breakOut
import java.lang.Character.UnicodeScript
import com.typesafe.scalalogging.slf4j.StrictLogging
import scala.collection.mutable.MultiMap
import scala.collection.mutable.ArrayBuffer

object PredefinedCharSets extends StrictLogging {

  private def getPrivateStaticField[A](clazz: Class[_], name: String): A = {
    val field = clazz.getDeclaredField(name)
    field.setAccessible(true)
    field.get(null).asInstanceOf[A]
  }

  val unicodeBlocks: Map[String, CharSet] = {
    val blockStarts = getPrivateStaticField[Array[Int]](classOf[UnicodeBlock], "blockStarts")
    val javaBlocks = getPrivateStaticField[Array[UnicodeBlock]](classOf[UnicodeBlock], "blocks").toSeq
    val blockToSetMap: Map[UnicodeBlock, CharSet] = (0 until blockStarts.length).flatMap { i =>
      val from = blockStarts(i)
      val to = if (i == blockStarts.length - 1)
        UnicodeChar.max.codePoint
      else
        blockStarts(i + 1)
      // skip unassigned blocks
      javaBlocks.lift(i).map { block =>
        block -> CharSet.fromRange(CharRange(from.u, to.u))
      }
    }(breakOut)
    val alias = getPrivateStaticField[java.util.Map[String, UnicodeBlock]](classOf[UnicodeBlock], "map").toMap
    alias.mapValues { javaUnicodeBlock =>
      blockToSetMap.get(javaUnicodeBlock).getOrElse {
        /*
         * As of Java 1.8, there exists one deprecated block (UnicodeBlock.SURROGATES_AREA) 
         * that doesn't have any range assigned. Respect Java behavior and make it match nothing. 
         */
        CharSet(Seq())
      }
    }
  }

  val unicodeScripts: Map[String, CharSet] = {
    val scriptStarts = getPrivateStaticField[Array[Int]](classOf[UnicodeScript], "scriptStarts")
    val javaScripts = getPrivateStaticField[Array[UnicodeScript]](classOf[UnicodeScript], "scripts").toSeq
    val scriptToSetMap = {
      val builder = collection.mutable.Map[UnicodeScript, CharSet]()
      for (i <- 0 until scriptStarts.length) {
        val from = scriptStarts(i)
        val to = if (i == scriptStarts.length - 1)
          UnicodeChar.max.codePoint
        else
          scriptStarts(i + 1)
        // skip unassigned scripts
        javaScripts.lift(i).foreach { script =>
          val CharSet(existing) = builder.getOrElse(script, CharSet(Seq()))
          builder.put(script, CharSet(existing :+ CharRange(from.u, to.u)))
        }
      }
      builder.toMap
    }
    val aliases = getPrivateStaticField[java.util.Map[String, UnicodeScript]](classOf[UnicodeScript], "aliases").toMap
    val canonicalNames = scriptToSetMap.map {
      case (script, charSet) =>
        (script.name(), charSet)
    }
    canonicalNames ++ aliases.mapValues(scriptToSetMap)
  }

  /*
   * Use a lazy val because collecting the ranges takes some time: only do it if used.
   */
  lazy val unicodeGeneralCategories: Map[String, CharSet] = {
    val (categories, elapsed) = Util.time {
      val categoryMapping: Map[Int, String] = GeneralCategory.categories.map {
        case (name, value) =>
          value.toInt -> name
      }(breakOut)
      val builder = collection.mutable.Map[String, ArrayBuffer[AbstractRange]]()
      for (codePoint <- UnicodeChar.min.codePoint to UnicodeChar.max.codePoint) {
        val categoryValue = Character.getType(codePoint)
        val category = categoryMapping(categoryValue)
        val parentCategory = category.head.toString // first letter
        val char = Lit(codePoint.u)
        builder.getOrElseUpdate(category, ArrayBuffer()) += char
        builder.getOrElseUpdate(parentCategory, ArrayBuffer()) += char
      }
      builder.mapValues(ranges => CharSet(RangeOps.union(ranges))).toMap
    }
    logger.trace(s"Collected unicode general categories in $elapsed")
    categories
  }

  val lower = CharSet.fromRange(CharRange(from = 'a'.u, to = 'z'.u))
  val upper = CharSet.fromRange(CharRange(from = 'A'.u, to = 'Z'.u))
  val alpha = CharSet.fromCharSets(lower, upper)
  val digit = CharSet.fromRange(CharRange(from = '0'.u, to = '9'.u))
  val alnum = CharSet.fromCharSets(alpha, digit)
  val punct = CharSet("""!"#$%&'()*+,-./:;<=>?@[\]^_`{|}~""".map(char => Lit(UnicodeChar(char))))
  val graph = CharSet.fromCharSets(alnum, punct)
  val space = CharSet(Seq(Lit('\n'.u), Lit('\t'.u), Lit('\r'.u), Lit('\f'.u), Lit(' '.u), Lit(0x0B.u)))
  val wordChar = CharSet(alnum.ranges :+ Lit('_'.u))

  val posixClasses = Map(
    "Lower" -> lower,
    "Upper" -> upper,
    "ASCII" -> CharSet.fromRange(CharRange(from = 0.u, to = 0x7F.u)),
    "Alpha" -> alpha,
    "Digit" -> digit,
    "Alnum" -> alnum,
    "Punct" -> punct,
    "Graph" -> graph,
    "Print" -> CharSet(graph.ranges :+ Lit(0x20.u)),
    "Blank" -> CharSet(Seq(Lit(0x20.u), Lit('\t'.u))),
    "Cntrl" -> CharSet(Seq(CharRange(from = 0.u, to = 0x1F.u), Lit(0x7F.u))),
    "XDigit" -> CharSet(digit.ranges ++ Seq(CharRange(from = 'a'.u, to = 'f'.u), CharRange(from = 'A'.u, to = 'F'.u))),
    "Space" -> space)

}

