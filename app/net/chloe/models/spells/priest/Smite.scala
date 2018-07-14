package net.chloe.models.spells.priest

import net.chloe.models.spells.Spell

case object Smite extends Spell {
  val id = 585
  val hasCharges = false
  val needTarget = true
  val hasCooldownOtherThanGcd = false
  val isInstant = false
}