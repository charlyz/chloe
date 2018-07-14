package net.chloe.models.spells.druid

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object RageOfTheSleeper extends Cooldown with Buff {
  val id = 200851
  val cooldownIndexInAddon = 5
  val buffIndexInAddon = 11
  val hasCharges = false
  val needTarget = false
  val hasCooldownOtherThanGcd = true
  val isInstant = true
}