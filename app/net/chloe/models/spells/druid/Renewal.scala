package net.chloe.models.spells.druid

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object Renewal extends Cooldown {
  // Limit 1 per druid.
  val id = 108238
  val cooldownIndexInAddon = 16
  val hasCharges = false
  val needTarget = false
  val hasCooldownOtherThanGcd = true
  val isInstant = true
}