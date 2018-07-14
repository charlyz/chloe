package net.chloe.models.spells.druid

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object Rejuvenation extends Cooldown with Buff {
  val id = 774
  val cooldownIndexInAddon = 1
  val buffIndexInAddon = 1
  val hasCharges = false
  val needTarget = false
  val hasCooldownOtherThanGcd = false
  val isInstant = true
}