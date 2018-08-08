package net.chloe.models.spells.druid

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

trait Moonfire extends Cooldown with Debuff {
  val id = 8921
  val debuffIndexInAddon = 1
  val hasCharges = false
  val needTarget = true
  val hasCooldownOtherThanGcd = false
  val isInstant = true
  val radiusOpt = None
}

case object RestoMoonfire extends Moonfire {
  val cooldownIndexInAddon = 9
}

case object GuardianMoonfire extends Moonfire {
  val cooldownIndexInAddon = 12
}