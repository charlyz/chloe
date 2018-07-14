package net.chloe.models.spells.druid

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object Barkskin extends Cooldown with Buff {
  val id = 22812
  val cooldownIndexInAddon = 15
  val buffIndexInAddon = 9
  val hasCharges = false
  val needTarget = false
  val hasCooldownOtherThanGcd = true
  val isInstant = true
}