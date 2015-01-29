package dregex.impl

object Direction extends Enumeration {
  val Behind, Ahead = Value
}

object Condition extends Enumeration {
  val Positive, Negative = Value
}

object Cardinality extends Enumeration {
  val ZeroToOne, ZeroToInf, OneToInf = Value
}

object RegexTree {

  trait Node

  trait ComplexPart extends Node {
    def values: Seq[Node]
  }

  trait SingleComplexPart extends ComplexPart {
    def value: Node
    def values = Seq(value)
  }

  trait AtomPart extends Node {
    def atoms: Seq[Char]
  }

  case class Lit(char: Char) extends AtomPart {
    override def toString = char.toString
    def atoms = Seq(char)
  }

  case class EmptyLit() extends AtomPart {
    override def toString = "empty-lit"
    def atoms = Seq()
  }

  object Lit {
    def apply(str: String) = {
      if (str.length != 1)
        throw new Exception("String is no char: " + str)
      new Lit(str.head)
    }
  }

  case class Wildcard() extends AtomPart {
    def atoms = Seq()
  }

  case class CharClass(chars: Seq[Lit]) extends AtomPart {
    def atoms = chars.map(_.char)
  }

  case class NegatedCharClass(chars: Seq[Lit]) extends AtomPart {
    def atoms = chars.map(_.char)
  }

  case class Disj(values: Seq[Node]) extends ComplexPart {
    override def toString = s"Disj(${values.mkString(", ")})"
  }

  case class Lookaround(dir: Direction.Value, cond: Condition.Value, value: Node) extends SingleComplexPart

  case class Quant(card: Cardinality.Value, value: Node) extends SingleComplexPart

  case class Rep(min: Int, max: Int, value: Node) extends SingleComplexPart 

  case class Juxt(values: Seq[Node]) extends ComplexPart {
    override def toString = s"Juxt(${values.mkString(", ")})"
  }

}