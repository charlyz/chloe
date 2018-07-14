package net.chloe.models.spells.hunter

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object BestialWrath extends Cooldown with Buff {
  val id = 19574
  val cooldownIndexInAddon = 5
  val buffIndexInAddon = 11
  val hasCharges = false
  val needTarget = false
  val hasCooldownOtherThanGcd = true
  val isInstant = true
}