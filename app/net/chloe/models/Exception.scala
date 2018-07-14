package net.chloe.models

case object NoDimensionException extends Exception("Dimensions have not been found for the window.")
case object NotCastingException extends Exception("Not casting.")
case object NoPetException extends Exception("No pet.")
case object NoLastCastedSpellException extends Exception("No last casted spell.")
case object NoDebuffOnTargetException extends Exception("No debuff on target.")
case object SpellNotFoundException extends Exception("Spell not found.")