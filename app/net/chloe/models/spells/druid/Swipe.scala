package net.chloe.models.spells.druid

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object Swipe extends Cooldown {
  val id = 106785
  val cooldownIndexInAddon = 22
  val hasCharges = false
  val needTarget = true
  val hasCooldownOtherThanGcd = false
  val isInstant = true
}