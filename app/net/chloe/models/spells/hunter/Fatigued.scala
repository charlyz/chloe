package net.chloe.models.spells.hunter

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object Fatigued extends Debuff {
  val id = 264689
  val debuffIndexInAddon = 3
  val hasCharges = false
  val needTarget = false
  val hasCooldownOtherThanGcd = false
  val isInstant = true
  val radiusOpt = None
}