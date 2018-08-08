package net.chloe.models.spells.druid

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

trait Barkskin extends Cooldown with Buff {
  val id = 22812
  val buffIndexInAddon = 9
  val hasCharges = false
  val needTarget = false
  val hasCooldownOtherThanGcd = true
  val isInstant = true
}

case object RestoBarkskin extends Barkskin {
  val cooldownIndexInAddon = 15
}

case object GuardianBarkskin extends Barkskin {
  val cooldownIndexInAddon = 14
}