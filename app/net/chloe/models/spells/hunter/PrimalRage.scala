package net.chloe.models.spells.hunter

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object PrimalRage extends Cooldown with Buff {
  val id = 264667
  val cooldownIndexInAddon = 13
  val buffIndexInAddon = 14
  val hasCharges = false
  val needTarget = false
  val hasCooldownOtherThanGcd = true
  val isInstant = true
}