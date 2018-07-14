package net.chloe.models.spells.druid

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object Efflorescence extends Cooldown with Buff {
  val id = 145205
  val cooldownIndexInAddon = 6
  val buffIndexInAddon = 5
  val hasCharges = false
  val needTarget = false
  val hasCooldownOtherThanGcd = false
  val isInstant = true
}