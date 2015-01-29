package dregex.impl

import dregex.UnsupportedException
import dregex.impl.MetaTrees.AtomTree
import dregex.impl.MetaTrees.TreeOperation
import dregex.impl.MetaTrees.MetaTree
import dregex.impl.Operations.Operation

/**
 * A meta regular expression is the intersection or substraction of 2 other (meta or simple) regular expressions.
 * Lookaround are transformed in equivalent meta simple regular expressions for processing.
 * A(?=B)C is transformed into AC ∩ AB.*
 * A(?!B)C is transformed into AC - AB.*
 * A(?<=B)C is transformed into AC ∩ .*BC
 * A(?<!B)C is transformed into AC - .*BC
 *
 * In the case of more than one lookaround, the transformation is applied recursively.
 *
 * Only top level lookarounds that are part of a juxtaposition are permitted, i.e. they are no allowed inside
 * parenthesis, nested or as members of a conjunction. Additionally negative lookaheads are not allowed at the
 * end of the expression. Examples:
 *
 * Allowed:
 * A(?!B)C
 * (?!B)C
 *
 * Not allowed:
 * A(?!B)      at the end
 * (?!B)|B     part of a conjuction
 * (?!B)       unique element, not a juxtaposition
 * (?!(?!B))   lookaround inside lookaround
 * (A(?!B))B   lookaround inside parenthesis
 *
 * NOTE: Only lookahead is actually implemented, lookbehind is not.
 */
object LookaroundExpander {

  import RegexTree._

  /**
   * Optimization: combination of consecutive negative lookaheads
   * (?!a)(?!b)(?!c) gets combined to (?!a|b|c), which is faster to process.
   * This optimization should be applied before the lookarounds are expanded to regex intersections and differences.
   */
  private def combineNegLookaheads(vals: Seq[Node]) = {
    import Direction._
    import Condition._
    vals.foldLeft(Seq[Node]()) { (acc, x) =>
      (acc, x) match {
        case (init :+ Lookaround(Ahead, Negative, v1), Lookaround(Ahead, Negative, v2)) =>
          init ++ Seq(Lookaround(Ahead, Negative, Disj(Seq(v1, v2))))
        case _ =>
          acc :+ x
      }
    }
  }

  def expandLookarounds(ast: Node) = ast match {
    case Juxt(values) => expandImpl(combineNegLookaheads(values))
    case _ => AtomTree(ast)
  }

  private def expandImpl(args: Seq[Node]): MetaTree = args match {
    case first +: second +: rest =>
      // more than one element
      first match {
        case Lookaround(Direction.Ahead, cond, value) =>
          val op = cond match {
            case Condition.Positive => Operation.Intersect
            case Condition.Negative => Operation.Substract
          }
          TreeOperation(op, expandImpl(second +: rest), AtomTree(Juxt(Seq(value, Quant(Cardinality.ZeroToInf, Wildcard())))))
        case Lookaround(Direction.Behind, cond, value) =>
          throw new UnsupportedException("lookbehind")
        case _ =>
          mergeAst(first, expandImpl(second +: rest))
      }
    case first +: rest =>
      // only one element (and also the last)
      first match {
        case Lookaround(Direction.Ahead, cond, value) => throw new UnsupportedException("lookahead in trailing position")
        case Lookaround(Direction.Behind, cond, value) => throw new UnsupportedException("lookbehind")
        case _ => AtomTree(first)
      }
  }

  private def mergeAst(first: Node, second: MetaTree): MetaTree = (first, second) match {
    case (Juxt(firstValues), AtomTree(Juxt(secondValues))) => AtomTree(Juxt(firstValues ++ secondValues))
    case (Juxt(firstValues), AtomTree(second)) => AtomTree(Juxt(firstValues :+ second))
    case (first, AtomTree(Juxt(secondValues))) => AtomTree(Juxt(first +: secondValues))
    case (first, AtomTree(second)) => AtomTree(Juxt(Seq(first, second)))
    case (first, TreeOperation(op, left, right)) => TreeOperation(op, mergeAst(first, left), mergeAst(first, right))
  }

}