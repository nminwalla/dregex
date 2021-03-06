package dregex

import dregex.impl.Dfa

/**
 * A regular expression that was generated by an operation between others (not parsing a string), so it lacks a 
 * literal expression or NFA.
 */
class SynteticRegex private[dregex] (val dfa: Dfa, val universe: Universe) extends Regex {
  
  override def toString = s"[synthetic] (DFA states: ${dfa.stateCount})"
  
}

