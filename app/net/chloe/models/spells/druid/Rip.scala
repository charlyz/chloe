package net.chloe.models.spells.druid

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object Rip extends Cooldown with Debuff {
  val id = 5221
  val cooldownIndexInAddon = 20
  val debuffIndexInAddon = 3
  val hasCharges = false
  val needTarget = true
  val hasCooldownOtherThanGcd = false
  val isInstant = true
  val radiusOpt: Option[Float] = None
}