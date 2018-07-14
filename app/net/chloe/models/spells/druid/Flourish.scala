package net.chloe.models.spells.druid

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object Flourish extends Cooldown {
  // Limit 1 per druid.
  val id = 197721
  val cooldownIndexInAddon = 17
  val hasCharges = false
  val needTarget = false
  val hasCooldownOtherThanGcd = true
  val isInstant = true
}