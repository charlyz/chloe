package net.chloe.models.spells.druid

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object Regrowth extends Cooldown with Buff {
  val id = 8936
  val cooldownIndexInAddon = 2
  val buffIndexInAddon = 2
  val hasCharges = false
  val needTarget = false
  val hasCooldownOtherThanGcd = false
  val isInstant = false
}