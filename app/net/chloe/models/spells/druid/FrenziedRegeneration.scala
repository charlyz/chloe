package net.chloe.models.spells.druid

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object FrenziedRegeneration extends Cooldown with Buff {
  val id = 22842
  val cooldownIndexInAddon = 8
  val buffIndexInAddon = 14
  val hasCharges = true
  val needTarget = false
  val hasCooldownOtherThanGcd = true
  val isInstant = true
}