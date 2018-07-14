package net.chloe.models.spells.druid

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object WildGrowth extends Cooldown {
  val id = 48438
  val cooldownIndexInAddon = 5
  val buffIndexInAddon = 4
  val hasCharges = false
  val needTarget = false
  val hasCooldownOtherThanGcd = true
  val isInstant = false
}