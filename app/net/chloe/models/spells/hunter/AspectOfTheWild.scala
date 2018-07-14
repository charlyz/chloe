package net.chloe.models.spells.hunter

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object AspectOfTheWild extends Cooldown with Buff {
  val id = 193530
  val cooldownIndexInAddon = 6
  val buffIndexInAddon = 12
  val hasCharges = false
  val needTarget = false
  val hasCooldownOtherThanGcd = true
  val isInstant = true
}