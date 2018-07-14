package net.chloe.models.spells.hunter

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object AspectOfTheTurtle extends Cooldown with Buff {
  val id = 186265
  val cooldownIndexInAddon = 8
  val buffIndexInAddon = 13
  val hasCharges = false
  val needTarget = false
  val hasCooldownOtherThanGcd = true
  val isInstant = true
}