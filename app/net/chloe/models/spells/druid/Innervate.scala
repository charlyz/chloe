package net.chloe.models.spells.druid

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object Innervate extends Cooldown with Buff {
  val id = 29166
  val cooldownIndexInAddon = 13
  val buffIndexInAddon = 7
  val hasCharges = false
  val needTarget = false
  val hasCooldownOtherThanGcd = true
  val isInstant = true
}