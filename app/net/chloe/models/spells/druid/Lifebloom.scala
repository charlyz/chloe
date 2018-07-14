package net.chloe.models.spells.druid

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object Lifebloom extends Cooldown with Buff {
  // Limit 1 per druid.
  val id = 33763
  val cooldownIndexInAddon = 4
  val buffIndexInAddon = 3
  val hasCharges = false
  val needTarget = false
  val hasCooldownOtherThanGcd = false
  val isInstant = true
}