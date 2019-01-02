package net.chloe.models.spells.hunter

import net.chloe.models.spells._
import net.chloe.models.auras._
import scala.concurrent.duration._

case object FeignDeath extends Cooldown {
  val id = 5384
  val cooldownIndexInAddon = 21
  val hasCharges = false
  val needTarget = false
  val hasCooldownOtherThanGcd = true
  val isInstant = true
}